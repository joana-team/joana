package edu.kit.joana.ifc.sdg.irlsod;


import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
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
 * Optionally, the relations transitivie reduction can be computed. Using JGraphT's current implementation, however,
 * this is only sensible for DAGs, since otherwise a cyclic graph may silently be transformed into a DAG.
 * 
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class DomTree {
	
	private final DirectedGraph<VirtualNode, DefaultEdge> tree = new DefaultDirectedGraph<>(DefaultEdge.class);
	private final SDG sdg;
	private final MHPAnalysis mhp;
	private final ICDomOracle cdomOracle;
	private CFG icfg;
	private boolean isReduced;

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
	 * @throws IllegalArgumentException if the underlying relation does not form a DAG, and reduction is requested
	 */
	
	public DomTree(SDG sdg, ICDomOracle cdomOracle, MHPAnalysis mhp, boolean reduce) {
		this.sdg = sdg;
		this.cdomOracle = cdomOracle;
		this.mhp = mhp;
		this.isReduced = false;
		
		compute();

		if (reduce) {
			try {
				reduce();
				this.isReduced = true;
			} catch (IllegalStateException e) {
				throw new IllegalArgumentException(e);
			}
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
	 * If the underlying relation forms a DAG, reduce it transitively.
	 * 
	 * @throws IllegalStateException if the underlying relation does not forms a DAG
	 */
	public void reduce() {
		if (this.isReduced) return;
		
		final CycleDetector<VirtualNode, DefaultEdge> detector = new CycleDetector<>(tree);
		final Set<VirtualNode> cycles = detector.findCycles();
		if (cycles.size() != 0) {
			throw new IllegalStateException(
			    "Relation is not acyclic, hence currently cannot be correctly reduced"
			);
		}
		TransitiveReduction.INSTANCE.reduce(tree);
	}
}
