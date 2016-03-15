package edu.kit.joana.ifc.orlsod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;
import edu.kit.joana.ifc.sdg.util.sdg.SDGNodePredicate;

public class DynamicityAnalysis {

	private SDG sdg;
	private CFG threadGraph;
	private Set<SDGNode> dynamicNodes = new HashSet<SDGNode>();

	public DynamicityAnalysis(SDG sdg, CFG threadGraph) {
		this.sdg = sdg;
		this.threadGraph = threadGraph;
		computeDynamicNodes();
	}

	private void computeDynamicNodes() {
		// 1.) Find non-trivial SCCs on the call graph - all nodes contained in methods participating in such an SCC are dynamic
		DirectedGraph<SDGNode, DefaultEdge> callGraph = extractCallGraph();
		StrongConnectivityInspector<SDGNode, DefaultEdge> scc = new StrongConnectivityInspector<SDGNode, DefaultEdge>(callGraph);
		for (Set<SDGNode> entries : scc.stronglyConnectedSets()) {
			if (entries.size() > 1) {
				// n is the entry of a method participating in a cycle on the call graph --> mark every node contained in n's method as dynamic
				for (SDGNode n : entries) {
					markAllNodesInMethod(n);
				}
			}
		}
		// 2.) For each method, look for non-trivial SCCs in the intraprocedural control-flow graph - all nodes participating in such an SCC are dynamic
		for (SDGNode entry : threadGraph.sortByProcedures().keySet()) {
			CFG procCFG = extractProcedureCFG(entry);
			StrongConnectivityInspector<SDGNode, SDGEdge> procSCC = new StrongConnectivityInspector<SDGNode, SDGEdge>(procCFG);
			for (Set<SDGNode> comp : procSCC.stronglyConnectedSets()) {
				if (comp.size() > 1) {
					markAllNodes(comp);
				}
			}
		}

		// 3.) saturation: If a method is called from a call site which is dynamic, all its nodes are dynamic
		boolean changed;
		do {
			changed = false;
			for (Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : threadGraph.sortByProcedures().entrySet()) {
				SDGNode entry = entryAndProc.getKey();
				if (isDynamic(entry)) continue;
				for (SDGEdge inc : threadGraph.getIncomingEdgesOfKind(entry, SDGEdge.Kind.CALL)) {
					if (inc.getKind() == SDGEdge.Kind.CALL && isDynamic(inc.getSource())) {
						changed |= markAllNodesInMethod(entry);
						break;
					}
				}
			}
		} while (changed);
	}

	private boolean markAllNodesInMethod(SDGNode entry) {
		return markAllNodes(threadGraph.getNodesOfProcedure(entry));
	}

	private boolean markAllNodes(Collection<? extends SDGNode> nodes) {
		boolean changed = dynamicNodes.addAll(nodes);
		return changed;
	}
	public boolean isDynamic(SDGNode v) {
		return dynamicNodes.contains(v);
	}

	private DirectedGraph<SDGNode, DefaultEdge> extractCallGraph() {
		DirectedGraph<SDGNode, DefaultEdge> ret = new DefaultDirectedGraph<SDGNode, DefaultEdge>(DefaultEdge.class);
		for (Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : threadGraph.sortByProcedures().entrySet()) {
			ret.addVertex(entryAndProc.getKey());
			for (SDGNode n : entryAndProc.getValue()) {
				if (n.getKind() == SDGNode.Kind.CALL) {
					for (SDGEdge callEdge : threadGraph.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CALL)) {
						ret.addVertex(callEdge.getTarget());
						ret.addEdge(entryAndProc.getKey(), threadGraph.getEntry(callEdge.getTarget()));
					}
				}
			}
		}
		return ret;
	}

	private CFG extractProcedureCFG(SDGNode entry) {
		CFG ret = new CFG();
		ret.addAllVertices(threadGraph.getNodesOfProcedure(entry));
		for (SDGNode n : threadGraph.getNodesOfProcedure(entry)) {
			for (SDGEdge e : threadGraph.outgoingEdgesOf(n)) {
				if (ret.containsVertex(e.getTarget())) {
					ret.addEdge(e);
				}
			}
		}
		return ReducedCFGBuilder.extractReducedCFG(ret, new SDGNodePredicate() {
			@Override
			public boolean isInteresting(SDGNode node) {
				return BytecodeLocation.OBJECT_FIELD != node.getBytecodeIndex()
					&& BytecodeLocation.BASE_FIELD != node.getBytecodeIndex()
					&& BytecodeLocation.ARRAY_FIELD != node.getBytecodeIndex()
					&& BytecodeLocation.ARRAY_INDEX != node.getBytecodeIndex()
					&& BytecodeLocation.STATIC_FIELD != node.getBytecodeIndex();
			}
		});
	}
}
