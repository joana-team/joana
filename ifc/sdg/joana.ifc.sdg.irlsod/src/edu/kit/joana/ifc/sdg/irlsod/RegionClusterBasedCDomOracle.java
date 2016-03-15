package edu.kit.joana.ifc.sdg.irlsod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

public class RegionClusterBasedCDomOracle implements ICDomOracle {
	private final SDG sdg;
	private final PreciseMHPAnalysis mhp;
	private CFG icfg;
	private Map<ThreadRegion, Set<ThreadRegion>> mhpEq;
	private DirectedGraph<Set<ThreadRegion>, DefaultEdge> regionGraph;
	private Dominators<Set<ThreadRegion>, DefaultEdge> domRegions;
	private DFSIntervalOrder<Set<ThreadRegion>, DomEdge> dioDomRegions;

	public RegionClusterBasedCDomOracle(final SDG sdg, final PreciseMHPAnalysis mhp) {
		this.sdg = sdg;
		this.mhp = mhp;
	}

	public Map<ThreadRegion, Set<ThreadRegion>> buildEquivalenceRelation() {
		final Map<ThreadRegion, Set<ThreadRegion>> mhpEq = new HashMap<ThreadRegion, Set<ThreadRegion>>();
		for (final ThreadRegion tr1 : mhp.getThreadRegions()) {
			final Set<ThreadRegion> tr1Eq = new HashSet<ThreadRegion>();
			tr1Eq.add(tr1);
			mhpEq.put(tr1, tr1Eq);
		}
		boolean changed;
		do {
			changed = false;
			for (final ThreadRegion tr1 : mhp.getThreadRegions()) {
				for (final ThreadRegion tr2 : mhp.getThreadRegions()) {
					if (!mhpEq.get(tr1).contains(tr2) && !differentMHPProperty(tr1, tr2)) {
						mhpEq.get(tr1).add(tr2);
						changed = true;
					}
				}
			}
		} while (changed);
		return mhpEq;
	}

	public void buildRegionGraph() {
		this.regionGraph = new DefaultDirectedGraph<Set<ThreadRegion>, DefaultEdge>(DefaultEdge.class);
		this.mhpEq = buildEquivalenceRelation();
		this.icfg = ICFGBuilder.extractICFG(sdg);
		for (final SDGNode n : icfg.vertexSet()) {
			for (final int threadN : n.getThreadNumbers()) {
				final ThreadRegion trSource = mhp.getThreadRegion(n, threadN);
				for (final SDGEdge e : icfg.outgoingEdgesOf(n)) {
					if (!e.getKind().isThreadEdge()) {
						if (possiblyExecutesIn(e.getTarget(), threadN)) {
							final ThreadRegion trTarget = mhp.getThreadRegion(e.getTarget(), threadN);
							if (!trTarget.equals(trSource) && differentMHPProperty(trSource, trTarget)) {
								regionGraph.addVertex(mhpEq.get(trSource));
								regionGraph.addVertex(mhpEq.get(trTarget));
								regionGraph.addEdge(mhpEq.get(trSource), mhpEq.get(trTarget));
							}
						}
					} else {
						for (final int threadM : e.getTarget().getThreadNumbers()) {
							if (threadM != threadN) {
								final ThreadRegion trTarget = mhp.getThreadRegion(e.getTarget(), threadM);
								if (!trTarget.equals(trSource) && differentMHPProperty(trSource, trTarget)) {
									regionGraph.addVertex(mhpEq.get(trSource));
									regionGraph.addVertex(mhpEq.get(trTarget));
									regionGraph.addEdge(mhpEq.get(trSource), mhpEq.get(trTarget));
								}
							}
						}
					}
				}
			}
		}
		domRegions = Dominators.compute(regionGraph, mhpEq.get(mhp.getThreadRegion(1)));
		dioDomRegions = new DFSIntervalOrder<Set<ThreadRegion>, DomEdge>(domRegions.getDominationTree());
	}

	public DirectedGraph<Set<ThreadRegion>, DefaultEdge> getRegionGraph() {
		return regionGraph;
	}

	public DomTree<Set<ThreadRegion>> getRegionsDomTree() {
		return domRegions.getDominationTree();
	}

	public static boolean possiblyExecutesIn(final SDGNode n, final int thread) {
		for (final int t : n.getThreadNumbers()) {
			if (t == thread) {
				return true;
			}
		}
		return false;
	}

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
		final Set<ThreadRegion> trs1 = mhpEq.get(mhp.getThreadRegion(n1, threadN1));
		final Set<ThreadRegion> trs2 = mhpEq.get(mhp.getThreadRegion(n2, threadN2));
		Set<ThreadRegion> cur = trs1;
		while ((domRegions.getIDom(cur) != null) && !dioDomRegions.isLeq(cur, trs2)) {
			cur = domRegions.getIDom(cur);
		}
		// now look for all the exits in the found thread regions,
		// i.e. the nodes from which the thread region cluster can
		// be left to another cluster which dominates trs1 or trs2
		final Set<SDGNode> exits = new HashSet<SDGNode>();
		for (final ThreadRegion tr : cur) {
			for (final SDGNode n : tr.getNodes()) {
				for (final SDGEdge e : icfg.outgoingEdgesOf(n)) {
					final Set<VirtualNode> tgts = new HashSet<VirtualNode>();
					if (!e.getKind().isThreadEdge()) {
						tgts.add(new VirtualNode(e.getTarget(), tr.getThread()));
					} else {
						for (final int thread : e.getTarget().getThreadNumbers()) {
							if (thread != tr.getThread()) {
								tgts.add(new VirtualNode(e.getTarget(), tr.getThread()));
							}
						}
					}
					for (final VirtualNode vtgt : tgts) {
						final Set<ThreadRegion> vtgtCluster = mhpEq.get(mhp.getThreadRegion(vtgt));
						if (!vtgtCluster.equals(tr)
								&& (dioDomRegions.isLeq(vtgtCluster, trs1) || dioDomRegions.isLeq(vtgtCluster, trs2))) {
							exits.add(n);
						}
					}
				}
			}
		}
		// now, build the dominator tree of the dominating region cluster and
		// find a node which dominates all the exits
		// 1.) build the subgraph of the icfg which contains all nodes from the
		// dominating region cluster
		final CFG regionCFG = new CFG();
		for (final ThreadRegion tr : cur) {
			for (final SDGNode n : tr.getNodes()) {
				regionCFG.addVertex(n);
			}
		}
		for (final SDGEdge e : icfg.edgeSet()) {
			if (regionCFG.vertexSet().contains(e.getSource()) && regionCFG.vertexSet().contains(e.getTarget())) {
				regionCFG.addEdge(e);
			}
		}
		SDGNode start = null;
		for (final SDGNode n : regionCFG.vertexSet()) {
			if (regionCFG.incomingEdgesOf(n).isEmpty()) {
				start = n;
				break;
			}
		}
		// 2.) dom tree
		final Dominators<SDGNode, SDGEdge> regionClusterDom = Dominators.compute(regionCFG, start);
		final DomTree<SDGNode> domTree = regionClusterDom.getDominationTree();
		final DFSIntervalOrder<SDGNode, DomEdge> dioDomTree = new DFSIntervalOrder<SDGNode, DomEdge>(domTree);
		System.out.println(n1 + " " + n2 + " " + cur + " " + exits);
		if (exits.isEmpty()) {
			return new VirtualNode(cur.iterator().next().getStart(), cur.iterator().next().getThread());
		} else {
			SDGNode d = exits.iterator().next();
			while ((regionClusterDom.getIDom(d) != null) && !dioDomTree.isLeq(d, exits)) {
				d = regionClusterDom.getIDom(d);
			}
			return new VirtualNode(d, cur.iterator().next().getThread());
		}
	}
}
