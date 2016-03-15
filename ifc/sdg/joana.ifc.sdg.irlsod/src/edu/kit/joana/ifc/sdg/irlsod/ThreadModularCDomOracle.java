package edu.kit.joana.ifc.sdg.irlsod;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import tests.JoanaRunner;
import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;

public class ThreadModularCDomOracle implements ICDomOracle {

	private SDG sdg;
	private DirectedGraph<ThreadInstance, DefaultEdge> tct;
	private DFSIntervalOrder<ThreadInstance, DefaultEdge> dioTCT;
	private DynamicityAnalysis dyna;

	public ThreadModularCDomOracle(SDG sdg) {
		this.sdg = sdg;
		this.tct = JoanaRunner.buildThreadCreationTree(sdg.getThreadsInfo());
		this.dioTCT = new DFSIntervalOrder<ThreadInstance, DefaultEdge>(tct);
	}

	@Override
	public VirtualNode cdom(SDGNode n1, int threadN1, SDGNode n2, int threadN2) {
		// 1.) find the lowest common ancestor in the thread creation tree
		ThreadInstance cAnc = lowestNonDynamicThreadAncestor(threadN1, threadN2);
		VirtualNode cdom;
		// 2.) find the fork sites which eventually lead to the creation of the two threads
		if (cAnc.getId() == threadN1) {
			cdom = new VirtualNode(findIndirectForkSite(cAnc, threadN2), threadN1);
		} else if (cAnc.getId() == threadN2) {
			return new VirtualNode(findIndirectForkSite(cAnc, threadN1), threadN2);
		} else {
			SDGNode fork1 = findIndirectForkSite(cAnc, threadN1);
			SDGNode fork2 = findIndirectForkSite(cAnc, threadN2);
			// 3.) find a common dominator of the two forks
			CFG threadGraph = JoanaRunner.unfoldCFGFor(sdg, cAnc.getId());
			this.dyna = new DynamicityAnalysis(sdg, threadGraph);
			Dominators<SDGNode, SDGEdge> threadDomTree = Dominators.compute(threadGraph, cAnc.getEntry());
			cdom = new VirtualNode(lowestNonDynamicCommonDominator(fork1, fork2, threadDomTree), cAnc.getId());
		}
		System.out.println(String.format("cdom(%s, %s) = %s", n1, n2, cdom.getNode()));
		return cdom;
	}

	private SDGNode findIndirectForkSite(ThreadInstance anc, int forkedThread) {
		ThreadInstance descendant = sdg.getThreadsInfo().getThread(forkedThread);
		// find the child of anc which is an ancestor of descendant
		// TODO: there should be exactly one if descendant is really a descendant of anc, right?
		for (DefaultEdge childEdge : tct.outgoingEdgesOf(anc)) {
			ThreadInstance child = tct.getEdgeTarget(childEdge);
			if (dioTCT.isLeq(descendant, child))
				return child.getFork();
		}
		throw new IllegalStateException("should not happen!!");
	}

	private ThreadInstance lowestNonDynamicThreadAncestor(int thread1, int thread2) {
		ThreadInstance ti1 = sdg.getThreadsInfo().getThread(thread1);
		ThreadInstance ti2 = sdg.getThreadsInfo().getThread(thread2);
		ThreadInstance cur = ti1;
		// 1.) goto lca in thread creation tree
		while (parent(cur) != null && !dioTCT.isLeq(ti2, cur)) {
			// go to parent in tct
			cur = parent(cur);
		}
		// 2.) go further up in the tct until a non-multi thread is found
		while (cur.isDynamic()) {
			cur = parent(cur);
		}
		return cur;
	}

	private ThreadInstance parent(ThreadInstance ti) {
		if (isRoot(ti)) return null;
		return tct.getEdgeSource(tct.incomingEdgesOf(ti).iterator().next());
	}

	private boolean isRoot(ThreadInstance ti) {
		return tct.incomingEdgesOf(ti).isEmpty();
	}

	private SDGNode lowestNonDynamicCommonDominator(SDGNode n1, SDGNode n2, Dominators<SDGNode, SDGEdge> dom) {
		DFSIntervalOrder<SDGNode, DomEdge> dio = new DFSIntervalOrder<SDGNode, DomEdge>(dom.getDominationTree());
		SDGNode cur = n1;
		while (!dio.isLeq(n2, cur)) {
			cur = dom.getIDom(cur);
		}
		while (dyna.isDynamic(cur)) {
			cur = dom.getIDom(cur);
		}
		return cur;
	}
}
