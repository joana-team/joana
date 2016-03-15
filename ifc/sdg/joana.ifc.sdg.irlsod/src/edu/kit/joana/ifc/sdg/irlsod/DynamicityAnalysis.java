package edu.kit.joana.ifc.sdg.irlsod;

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

	private final SDG sdg;
	private final CFG threadGraph;
	private final Set<SDGNode> dynamicNodes = new HashSet<SDGNode>();

	public DynamicityAnalysis(final SDG sdg, final CFG threadGraph) {
		this.sdg = sdg;
		this.threadGraph = threadGraph;
		computeDynamicNodes();
	}

	private void computeDynamicNodes() {
		// 1.) Find non-trivial SCCs on the call graph - all nodes contained in
		// methods participating in such an SCC are dynamic
		final DirectedGraph<SDGNode, DefaultEdge> callGraph = extractCallGraph();
		final StrongConnectivityInspector<SDGNode, DefaultEdge> scc = new StrongConnectivityInspector<SDGNode, DefaultEdge>(
				callGraph);
		for (final Set<SDGNode> entries : scc.stronglyConnectedSets()) {
			if (entries.size() > 1) {
				// n is the entry of a method participating in a cycle on the
				// call graph --> mark every node contained in n's method as
				// dynamic
				for (final SDGNode n : entries) {
					markAllNodesInMethod(n);
				}
			}
		}
		// 2.) For each method, look for non-trivial SCCs in the intraprocedural
		// control-flow graph - all nodes participating in such an SCC are
		// dynamic
		for (final SDGNode entry : threadGraph.sortByProcedures().keySet()) {
			final CFG procCFG = extractProcedureCFG(entry);
			final StrongConnectivityInspector<SDGNode, SDGEdge> procSCC = new StrongConnectivityInspector<SDGNode, SDGEdge>(
					procCFG);
			for (final Set<SDGNode> comp : procSCC.stronglyConnectedSets()) {
				if (comp.size() > 1) {
					markAllNodes(comp);
				}
			}
		}

		// 3.) saturation: If a method is called from a call site which is
		// dynamic, all its nodes are dynamic
		boolean changed;
		do {
			changed = false;
			for (final Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : threadGraph.sortByProcedures().entrySet()) {
				final SDGNode entry = entryAndProc.getKey();
				if (isDynamic(entry)) {
					continue;
				}
				for (final SDGEdge inc : threadGraph.getIncomingEdgesOfKind(entry, SDGEdge.Kind.CALL)) {
					if ((inc.getKind() == SDGEdge.Kind.CALL) && isDynamic(inc.getSource())) {
						changed |= markAllNodesInMethod(entry);
						break;
					}
				}
			}
		} while (changed);
	}

	private boolean markAllNodesInMethod(final SDGNode entry) {
		return markAllNodes(threadGraph.getNodesOfProcedure(entry));
	}

	private boolean markAllNodes(final Collection<? extends SDGNode> nodes) {
		final boolean changed = dynamicNodes.addAll(nodes);
		return changed;
	}

	public boolean isDynamic(final SDGNode v) {
		return dynamicNodes.contains(v);
	}

	private DirectedGraph<SDGNode, DefaultEdge> extractCallGraph() {
		final DirectedGraph<SDGNode, DefaultEdge> ret = new DefaultDirectedGraph<SDGNode, DefaultEdge>(
				DefaultEdge.class);
		for (final Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : threadGraph.sortByProcedures().entrySet()) {
			ret.addVertex(entryAndProc.getKey());
			for (final SDGNode n : entryAndProc.getValue()) {
				if (n.getKind() == SDGNode.Kind.CALL) {
					for (final SDGEdge callEdge : threadGraph.getOutgoingEdgesOfKind(n, SDGEdge.Kind.CALL)) {
						ret.addVertex(callEdge.getTarget());
						ret.addEdge(entryAndProc.getKey(), threadGraph.getEntry(callEdge.getTarget()));
					}
				}
			}
		}
		return ret;
	}

	private CFG extractProcedureCFG(final SDGNode entry) {
		final CFG ret = new CFG();
		ret.addAllVertices(threadGraph.getNodesOfProcedure(entry));
		for (final SDGNode n : threadGraph.getNodesOfProcedure(entry)) {
			for (final SDGEdge e : threadGraph.outgoingEdgesOf(n)) {
				if (ret.containsVertex(e.getTarget())) {
					ret.addEdge(e);
				}
			}
		}
		return ReducedCFGBuilder.extractReducedCFG(ret, new SDGNodePredicate() {
			@Override
			public boolean isInteresting(final SDGNode node) {
				return (BytecodeLocation.OBJECT_FIELD != node.getBytecodeIndex())
						&& (BytecodeLocation.BASE_FIELD != node.getBytecodeIndex())
						&& (BytecodeLocation.ARRAY_FIELD != node.getBytecodeIndex())
						&& (BytecodeLocation.ARRAY_INDEX != node.getBytecodeIndex())
						&& (BytecodeLocation.STATIC_FIELD != node.getBytecodeIndex());
			}
		});
	}
}
