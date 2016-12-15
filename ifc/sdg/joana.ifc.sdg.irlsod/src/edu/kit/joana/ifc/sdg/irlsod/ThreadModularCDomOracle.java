package edu.kit.joana.ifc.sdg.irlsod;

import java.util.HashMap;
import java.util.Map;

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
	private final CFG icfg;
	private TIntObjectMap<CFG> threadsToCFG = new TIntObjectHashMap<>();
	private TIntObjectMap<DynamicityAnalysis> threadsToDyna = new TIntObjectHashMap<>();
	private TIntObjectMap<Dominators<SDGNode, SDGEdge>> threadsToDom = new TIntObjectHashMap<>();
	private TIntObjectMap<DFSIntervalOrder<SDGNode, DomEdge>> threadsToDIO = new TIntObjectHashMap<>();
	private Map<ForksTuple, VirtualNode> forkCDom = new HashMap<>();

	private final static class ForksTuple {
		private final SDGNode fork1;
		private final SDGNode fork2;
		private final int tid;
		
		public ForksTuple(SDGNode fork1, SDGNode fork2, int tid) {
			this.fork1 = fork1;
			this.fork2 = fork2;
			this.tid = tid;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = ((fork1 == null) ? 0 : fork1.hashCode())
					+((fork2 == null) ? 0 : fork2.hashCode());
			result = prime * result + tid;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof ForksTuple))
				return false;
			ForksTuple other = (ForksTuple) obj;
			if (tid != other.tid)
				return false;
			return (fork1 == other.fork1 && fork2 == other.fork2) ||
					(fork1 == other.fork2 && fork2 == other.fork1);
		}
	}
	
	public ThreadModularCDomOracle(final SDG sdg) {
		this.sdg = sdg;
		icfg = ICFGBuilder.extractICFG(sdg);
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
		// when we are already in the ancestor thread, we use the node itself
		final SDGNode fork1 = (cAnc.getId() == threadN1) ? n1 : findIndirectForkSite(cAnc, threadN1);
		final SDGNode fork2 = (cAnc.getId() == threadN2) ? n2 : findIndirectForkSite(cAnc, threadN2);
		
		VirtualNode cached = forkCDom.get(new ForksTuple(fork1, fork2, cAnc.getId()));
		if (cached != null) {
			return cached;
		}
		
		// 3.) find a common dominator of the two forks
		CFG threadGraph = threadsToCFG.get(cAnc.getId());
		if (threadGraph == null) {
			threadGraph = unfoldCFGFor(cAnc.getId());
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
		
		SDGNode cdomSDGNode = lowestNonDynamicCommonDominator(fork1, fork2, threadDom, threadDIO, dyna);
		if (cdomSDGNode == null) {
			cdomSDGNode = cAnc.getEntry();
		}
		cdom = new VirtualNode(cdomSDGNode, cAnc.getId());
		
		forkCDom.put(new ForksTuple(fork1, fork2, cAnc.getId()), cdom);
		//forkCDom.put(new ForksTuple(fork2, fork1, cAnc.getId()), cdom);
		
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

	private CFG unfoldCFGFor(final int thread) {
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
