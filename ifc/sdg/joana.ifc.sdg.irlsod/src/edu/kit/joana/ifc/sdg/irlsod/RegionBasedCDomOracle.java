package edu.kit.joana.ifc.sdg.irlsod;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.graph.dominators.slca.DFSIntervalOrder;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.wala.core.graphs.Dominators;
import edu.kit.joana.wala.core.graphs.Dominators.DomEdge;
import edu.kit.joana.wala.core.graphs.Dominators.DomTree;

public class RegionBasedCDomOracle implements ICDomOracle {
	private SDG sdg;
	private PreciseMHPAnalysis mhp;
	private CFG icfg;
	private DirectedGraph<ThreadRegion, DefaultEdge> regionGraph;
	private Dominators<ThreadRegion, DefaultEdge> domRegions;
	private DFSIntervalOrder<ThreadRegion, DomEdge> dioDomRegions;

	public RegionBasedCDomOracle(SDG sdg, PreciseMHPAnalysis mhp) {
		this.sdg = sdg;
		this.mhp = mhp;
	}

	public void buildRegionGraph() {
		this.regionGraph = new DefaultDirectedGraph<ThreadRegion, DefaultEdge>(DefaultEdge.class);
		this.icfg = ICFGBuilder.extractICFG(sdg);
		for (SDGNode n : icfg.vertexSet()) {
			for (int threadN : n.getThreadNumbers()) {
				ThreadRegion trSource = mhp.getThreadRegion(n, threadN);
				for (SDGEdge e : icfg.outgoingEdgesOf(n)) {
					if (!e.getKind().isThreadEdge()) {
						if (possiblyExecutesIn(e.getTarget(), threadN)) {
							ThreadRegion trTarget = mhp.getThreadRegion(e.getTarget(), threadN);
							if (!trTarget.equals(trSource)) {
								regionGraph.addVertex(trSource);
								regionGraph.addVertex(trTarget);
								regionGraph.addEdge(trSource, trTarget);
							}
						}
					} else {
						for (int threadM : e.getTarget().getThreadNumbers()) {
							if (threadM != threadN) {
								ThreadRegion trTarget = mhp.getThreadRegion(e.getTarget(), threadM);
								if (!trTarget.equals(trSource)) {
									regionGraph.addVertex(trSource);
									regionGraph.addVertex(trTarget);
									regionGraph.addEdge(trSource, trTarget);
								}
							}
						}
					}
				}
			}
		}
		ThreadRegion root = null;
		for (ThreadRegion tr : regionGraph.vertexSet()) {
			if (regionGraph.incomingEdgesOf(tr).isEmpty()) {
				root = tr;
			}
		}
		domRegions = Dominators.compute(regionGraph, root);
		dioDomRegions = new DFSIntervalOrder<ThreadRegion, DomEdge>(domRegions.getDominationTree());
	}


	public DirectedGraph<ThreadRegion, DefaultEdge> getRegionGraph() {
		return regionGraph;
	}

	public DomTree<ThreadRegion> getRegionsDomTree() {
		return domRegions.getDominationTree();
	}

	private boolean possiblyExecutesIn(SDGNode n, int thread) {
		for (int t : n.getThreadNumbers()) {
			if (t == thread) {
				return true;
			}
		}
		return false;
	}

	private boolean differentMHPProperty(ThreadRegion tr1, ThreadRegion tr2) {
		for (ThreadRegion tr : mhp.getThreadRegions()) {
			if (mhp.isParallel(tr1, tr) != mhp.isParallel(tr2, tr)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public VirtualNode cdom(SDGNode n1, int threadN1, SDGNode n2, int threadN2) {
		// 1.) get dominating thread regions
		ThreadRegion trs1 = mhp.getThreadRegion(n1, threadN1);
		ThreadRegion trs2 = mhp.getThreadRegion(n2, threadN2);
		ThreadRegion cur = trs1;
		while (domRegions.getIDom(cur) != null && !dioDomRegions.isLeq(cur, trs2)) {
			cur = domRegions.getIDom(cur);
		}
		return new VirtualNode(cur.getStart(), cur.getThread());
	}
}
