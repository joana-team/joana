package edu.kit.joana.ifc.sdg.irlsod;


import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;

public class DomTree {
	
	private final DirectedGraph<VirtualNode, DefaultEdge> tree = new DefaultDirectedGraph<>(DefaultEdge.class);
	private final SDG sdg;
	private final MHPAnalysis mhp;
	private final ICDomOracle cdomOracle;
	private CFG icfg;
//	private final Map<SDGNode,Collection<VirtualNode>> vnodes;

	public DomTree(SDG sdg, ICDomOracle cdomOracle, MHPAnalysis mhp) {
		this.sdg = sdg;
		this.cdomOracle = cdomOracle;
		this.mhp = mhp;
//		this.vnodes = new HashMap<>();
		
		try {
			compute();
		} catch (CycleFoundException c) {
			throw new IllegalArgumentException(c);
		}
		
	}
	
//	public Collection<VirtualNode> getVirtuals(SDGNode n) {
//	}
	
	private void compute() throws CycleFoundException {
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
		TransitiveReduction.INSTANCE.reduce(tree);
	}
	
	public DirectedGraph<VirtualNode, DefaultEdge> getTree() {
		return tree;
	}

}
