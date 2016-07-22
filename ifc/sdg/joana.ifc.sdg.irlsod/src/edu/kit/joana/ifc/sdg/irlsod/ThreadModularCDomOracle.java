package edu.kit.joana.ifc.sdg.irlsod;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.util.graph.ThreadInformationUtil;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

public class ThreadModularCDomOracle implements ICDomOracle {

	private final SDG sdg;
	private final DirectedGraph<ThreadInstance, DefaultEdge> tct;
	private final DFSIntervalOrder<ThreadInstance, DefaultEdge> dioTCT;
	private TIntObjectMap<CFG> threadsToCFG = new TIntObjectHashMap<>();
	private TIntObjectMap<DynamicityAnalysis> threadsToDyna = new TIntObjectHashMap<>();
	private TIntObjectMap<Dominators<SDGNode, SDGEdge>> threadsToDom = new TIntObjectHashMap<>();
	private TIntObjectMap<DFSIntervalOrder<SDGNode, DomEdge>> threadsToDIO = new TIntObjectHashMap<>();

	public ThreadModularCDomOracle(final SDG sdg) {
		this.sdg = sdg;
		this.tct = ThreadInformationUtil.buildThreadCreationTree(sdg.getThreadsInfo());
		this.dioTCT = new DFSIntervalOrder<ThreadInstance, DefaultEdge>(tct);
	}

	@Override
	public VirtualNode cdom(final SDGNode n1, final int threadN1, final SDGNode n2, final int threadN2) {
		// 1.) find the lowest common ancestor in the thread creation tree
		final ThreadInstance cAnc = lowestNonDynamicThreadAncestor(threadN1, threadN2);
		VirtualNode cdom;
		// 2.) find the fork sites which eventually lead to the creation of the
		// two threads
		if (cAnc.getId() == threadN1) {
			cdom = new VirtualNode(findIndirectForkSite(cAnc, threadN2), threadN1);
		} else if (cAnc.getId() == threadN2) {
			return new VirtualNode(findIndirectForkSite(cAnc, threadN1), threadN2);
		} else {
			final SDGNode fork1 = findIndirectForkSite(cAnc, threadN1);
			final SDGNode fork2 = findIndirectForkSite(cAnc, threadN2);
			// 3.) find a common dominator of the two forks
			CFG threadGraph = threadsToCFG.get(cAnc.getId());
			if (threadGraph == null) {
				threadGraph = ThreadModularCDomOracle.unfoldCFGFor(sdg, cAnc.getId());
				threadsToCFG.put(cAnc.getId(), threadGraph);
			}
			DynamicityAnalysis dyna = threadsToDyna.get(cAnc.getId());
			if (dyna == null) {
				dyna = new DynamicityAnalysis(sdg, threadGraph);
				threadsToDyna.put(cAnc.getId(), dyna);
			}
			Dominators<SDGNode, SDGEdge> threadDom = threadsToDom.get(cAnc.getId());
			if (threadDom == null) {
				threadDom = Dominators.compute(threadGraph, cAnc.getEntry());
				threadsToDom.put(cAnc.getId(), threadDom);
			}
			DFSIntervalOrder<SDGNode, DomEdge> threadDIO = threadsToDIO.get(cAnc.getId());
			if (threadDIO == null) {
				threadDIO = new DFSIntervalOrder<SDGNode, DomEdge>(threadDom.getDominationTree());
				threadsToDIO.put(cAnc.getId(), threadDIO);
			}
			cdom = new VirtualNode(lowestNonDynamicCommonDominator(fork1, fork2, threadDom, threadDIO, dyna), cAnc.getId());
		}
		//System.out.println(String.format("cdom(%s, %s) = %s", n1, n2, cdom.getNode()));
		return cdom;
	}

	private SDGNode findIndirectForkSite(final ThreadInstance anc, final int forkedThread) {
		final ThreadInstance descendant = sdg.getThreadsInfo().getThread(forkedThread);
		// find the child of anc which is an ancestor of descendant
		// TODO: there should be exactly one if descendant is really a
		// descendant of anc, right?
		for (final DefaultEdge childEdge : tct.outgoingEdgesOf(anc)) {
			final ThreadInstance child = tct.getEdgeTarget(childEdge);
			if (dioTCT.isLeq(descendant, child)) {
				return child.getFork();
			}
		}
		throw new IllegalStateException("should not happen!!");
	}

	private ThreadInstance lowestNonDynamicThreadAncestor(final int thread1, final int thread2) {
		final ThreadInstance ti1 = sdg.getThreadsInfo().getThread(thread1);
		final ThreadInstance ti2 = sdg.getThreadsInfo().getThread(thread2);
		ThreadInstance cur = ti1;
		// 1.) goto lca in thread creation tree
		while ((parent(cur) != null) && !dioTCT.isLeq(ti2, cur)) {
			// go to parent in tct
			cur = parent(cur);
		}
		// 2.) go further up in the tct until a non-multi thread is found
		while (cur.isDynamic()) {
			cur = parent(cur);
		}
		return cur;
	}

	private ThreadInstance parent(final ThreadInstance ti) {
		if (isRoot(ti)) {
			return null;
		}
		return tct.getEdgeSource(tct.incomingEdgesOf(ti).iterator().next());
	}

	private boolean isRoot(final ThreadInstance ti) {
		return tct.incomingEdgesOf(ti).isEmpty();
	}

	private SDGNode lowestNonDynamicCommonDominator(final SDGNode n1, final SDGNode n2,
			final Dominators<SDGNode, SDGEdge> dom, final DFSIntervalOrder<SDGNode, DomEdge> dio, final DynamicityAnalysis dyna) {
		SDGNode cur = n1;
		while (!dio.isLeq(n2, cur)) {
			cur = dom.getIDom(cur);
		}
		while (dyna.isDynamic(cur)) {
			cur = dom.getIDom(cur);
		}
		return cur;
	}

	private static CFG unfoldCFGFor(final SDG sdg, final int thread) {
		final CFG icfg = ICFGBuilder.extractICFG(sdg);
		final CFG ret = new CFG();
		for (final SDGNode n : icfg.vertexSet()) {
			if (!RegionClusterBasedCDomOracle.possiblyExecutesIn(n, thread)) {
				continue;
			}
			for (final SDGEdge e : icfg.outgoingEdgesOf(n)) {
				if (!RegionClusterBasedCDomOracle.possiblyExecutesIn(e.getTarget(), thread)) {
					continue;
				}
				ret.addVertex(n);
				ret.addVertex(e.getTarget());
				ret.addEdge(n, e.getTarget(), e);
			}
		}
		return ret;
	}
}
