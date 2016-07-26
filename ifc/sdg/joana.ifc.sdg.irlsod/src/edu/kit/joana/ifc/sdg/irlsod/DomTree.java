package edu.kit.joana.ifc.sdg.irlsod;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;

/**
 * An explicit representation of the relation implicit in a given {@link ICDomOracle} instance.
 * This is usually expected to form a tree, but should at least a DAG (if, e.g., an interprocedural dominator variant is used),
 * hence the Name {@link DomTree}.
 * 
 * Optionally, the relations transitive reduction can be computed. Using JGraphT's current implementation,
 * and a pre-processing using SCCs in case the relation is cyclic.
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class DomTree {
	
	private DirectedGraph<VirtualNode, DefaultEdge> tree = new DefaultDirectedGraph<>(DefaultEdge.class);
	private final SDG sdg;
	private final MHPAnalysis mhp;
	private final ICDomOracle cdomOracle;
	private CFG icfg;
	private boolean isReduced;
	private boolean acyclic;

	/**
	 * Does not compute the relations tansitive reduction.
	 *  
	 * @param sdg The underlying {@link SDG}.
	 * @param cdomOracle The {@link ICDomOracle} instance.
	 * @param mhp The "May-Happen-in-Parallel"-analysis. Should match the analysis used by the {@link ICDomOracle} instance, if applicable.
	 */
	public DomTree(SDG sdg, ICDomOracle cdomOracle, MHPAnalysis mhp) {
		this(sdg, cdomOracle, mhp, false);
	}
	
	/**
	 * 
	 * @param sdg The underlying {@link SDG}.
	 * @param cdomOracle The {@link ICDomOracle} instance.
	 * @param mhp The "May-Happen-in-Parallel"-analysis. Should match the analysis used by the {@link ICDomOracle} instance, if applicable.
	 * @param reduce Shall the relations transitive reduction be computed?
	 * 
	 */
	public DomTree(SDG sdg, ICDomOracle cdomOracle, MHPAnalysis mhp, boolean reduce) {
		this.sdg = sdg;
		this.cdomOracle = cdomOracle;
		this.mhp = mhp;
		this.isReduced = false;
		
		compute();

		if (reduce) {
			reduce();
			this.isReduced = true;
		}
		
	}
	
	private void compute() {
		this.icfg = ICFGBuilder.extractICFG(sdg);
		for (SDGNode n : icfg.vertexSet()) {
			for (final int threadN : n.getThreadNumbers()) {
				tree.addVertex(new VirtualNode(n, threadN));
			}
		}
		for (SDGNode n : icfg.vertexSet()) {
			for (final int threadN : n.getThreadNumbers()) {
				for (final SDGNode m : icfg.vertexSet()) {
					for (final int threadM : m.getThreadNumbers()) {
						if (mhp.isParallel(n, threadN, m, threadM)) {
							VirtualNode cdom = cdomOracle.cdom(n, threadN, m, threadM);
							VirtualNode vn = new VirtualNode(n, threadN);
							VirtualNode vm = new VirtualNode(m, threadM);
							if (!cdom.equals(vn)) tree.addEdge(cdom,new VirtualNode(n, threadN));
							if (!cdom.equals(vm)) tree.addEdge(cdom,new VirtualNode(m, threadM));
						}
					}
				}
			}
		}
		
	}
	
	public DirectedGraph<VirtualNode, DefaultEdge> getTree() {
		return tree;
	}

	/**
	 * transitively reduce the underlying relation.
	 * 
	 * @return true iff the underlying relation forms a DAG
	 */
	public boolean reduce() {
		if (this.isReduced) return this.acyclic;
		
		final KosarajuStrongConnectivityInspector<VirtualNode, DefaultEdge> sccInspector =
		    new KosarajuStrongConnectivityInspector<>(this.tree);
		final List<Set<VirtualNode>> sccs = sccInspector.stronglyConnectedSets();
		
		if (sccs.stream().anyMatch(scc -> scc.size() > 1)) {
			final Map<VirtualNode, Set<VirtualNode>> canonicalToSccs = new HashMap<>();
			Map<VirtualNode, VirtualNode> nodeTocanonical = new HashMap<>();
			sccs.stream().forEach( scc -> {
				final VirtualNode canonical = scc.iterator().next();
				canonicalToSccs.put(canonical, scc);
				
				scc.stream().forEach( node -> {
					nodeTocanonical.put(node, canonical);
				});
				
			});
			
			DirectedGraph<VirtualNode, DefaultEdge> g1 = new DefaultDirectedGraph<>(DefaultEdge.class);
			
			canonicalToSccs.entrySet().stream().forEach( entry -> {
				final VirtualNode canonical = entry.getKey();
				g1.addVertex(canonical);
			});
			tree.edgeSet().stream().forEach( e -> {
				g1.addEdge(
				    nodeTocanonical.get(tree.getEdgeSource(e)),
				    nodeTocanonical.get(tree.getEdgeTarget(e))
				);
			});
			
			TransitiveReduction.INSTANCE.reduce(g1);
			DirectedGraph<VirtualNode, DefaultEdge> g2 = new DefaultDirectedGraph<>(DefaultEdge.class);
			tree.vertexSet().stream().forEach( node -> g2.addVertex(node));
			
			canonicalToSccs.entrySet().stream().forEach( entry -> {
				final VirtualNode canonical = entry.getKey();
				final Set<VirtualNode> scc  = entry.getValue();
				Iterator<VirtualNode> n1s = scc.iterator();
				Iterator<VirtualNode> n2s = scc.iterator();
				n2s.next();
				while (n1s.hasNext() && n2s.hasNext()) {
					VirtualNode n1 = n1s.next();
					VirtualNode n2 = n2s.next();
					g2.addEdge(n1, n2);
				};
				
				if (scc.size() > 1) {
					g2.addEdge(n1s.next(), canonical);
				}
			});
			g1.edgeSet().stream().forEach( e -> {
				g2.addEdge(
				    g1.getEdgeSource(e),
				    g1.getEdgeTarget(e)
				);
			});
			this.tree = g2;
			return false;
		} else {
			TransitiveReduction.INSTANCE.reduce(tree);
			return true;
		}
	}
}
