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
	private final SDG sdg;
	private final PreciseMHPAnalysis mhp;
	private DirectedGraph<ThreadRegion, DefaultEdge> regionGraph;
	private Dominators<ThreadRegion, DefaultEdge> domRegions;
	private DFSIntervalOrder<ThreadRegion, DomEdge> dioDomRegions;

	public RegionBasedCDomOracle(final SDG sdg, final PreciseMHPAnalysis mhp) {
		this.sdg = sdg;
		this.mhp = mhp;
	}

	public void buildRegionGraph() {
		this.regionGraph = new DefaultDirectedGraph<ThreadRegion, DefaultEdge>(DefaultEdge.class);
		CFG icfg = ICFGBuilder.extractICFG(sdg);
		for (ThreadRegion region : mhp.getThreadRegions()) {
			regionGraph.addVertex(region);
		}
		for (final SDGNode n : icfg.vertexSet()) {
			for (final int threadN : n.getThreadNumbers()) {
				final ThreadRegion trSource = mhp.getThreadRegion(n, threadN);
				for (final SDGEdge e : icfg.outgoingEdgesOf(n)) {
					if (e.getKind().isThreadEdge()) {
						for (final int threadM : e.getTarget().getThreadNumbers()) {
							if (threadM != threadN) {
								final ThreadRegion trTarget = mhp.getThreadRegion(e.getTarget(), threadM);
								if (!trTarget.equals(trSource)) {
									regionGraph.addVertex(trSource);
									regionGraph.addVertex(trTarget);
									regionGraph.addEdge(trSource, trTarget);
								}
							}
						}
					} else {
						if (possiblyExecutesIn(e.getTarget(), threadN)) {
							final ThreadRegion trTarget = mhp.getThreadRegion(e.getTarget(), threadN);
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
		ThreadRegion root = null;
		for (final ThreadRegion tr : regionGraph.vertexSet()) {
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

	private boolean possiblyExecutesIn(final SDGNode n, final int thread) {
		for (final int t : n.getThreadNumbers()) {
			if (t == thread) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings("unused")
	private boolean differentMHPProperty(final ThreadRegion tr1, final ThreadRegion tr2) {
		for (final ThreadRegion tr : mhp.getThreadRegions()) {
			if (mhp.isParallel(tr1, tr) != mhp.isParallel(tr2, tr)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public VirtualNode cdom(final SDGNode n1, final int threadN1, final SDGNode n2, final int threadN2) {
		// 1.) get dominating thread regions
		final ThreadRegion trs1 = mhp.getThreadRegion(n1, threadN1);
		final ThreadRegion trs2 = mhp.getThreadRegion(n2, threadN2);
		ThreadRegion cur = trs1;
		while ((domRegions.getIDom(cur) != null)
					&& (!dioDomRegions.isLeq(trs2, cur)
						|| mhp.isParallel(cur, trs1)
						|| mhp.isParallel(cur, trs2))) {

			cur = domRegions.getIDom(cur);
		}
		return new VirtualNode(cur.getStart(), cur.getThread());
	}
}
