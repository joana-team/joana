package edu.kit.joana.ifc.sdg.irlsod;



import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.DirectedGraphBuilder;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.graph.TransitiveReductionGeneral;

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
	private final Set<Pair<VirtualNode, VirtualNode>> set;
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
		this.set = new HashSet<>();
		
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
							addEdge(cdom,vn);
							addEdge(cdom,vm);
						}
					}
				}
			}
		}
	}
	
	private void addEdge(VirtualNode v1, VirtualNode v2) {
		if (v1.equals(v2)) return;
		Pair<VirtualNode, VirtualNode> p = Pair.pair(v1, v2);
		if (!this.set.contains(p)) {
			this.set.add(p);
			this.tree.addEdge(v1, v2);
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
		
		KosarajuStrongConnectivityInspector<VirtualNode,DefaultEdge> sccInspector =
			new KosarajuStrongConnectivityInspector<>(this.tree);
		this.acyclic = sccInspector.stronglyConnectedSets().stream().allMatch(scc -> scc.size() == 1);
		
		DirectedGraphBuilder<VirtualNode, DefaultEdge, DefaultDirectedGraph<VirtualNode, DefaultEdge>> builder =
			new DirectedGraphBuilder<VirtualNode, DefaultEdge, DefaultDirectedGraph<VirtualNode, DefaultEdge>>(
				new DefaultDirectedGraph<VirtualNode, DefaultEdge>(DefaultEdge.class)
			);
		
		this.tree = TransitiveReductionGeneral.INSTANCE.reduction(this.tree, builder);
		this.isReduced = true;
		return this.acyclic;
	}
}
