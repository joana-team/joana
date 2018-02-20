package edu.kit.joana.ifc.sdg.irlsod;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;

public class DynamicityAnalysis {

	private final CFG threadGraph;
	private final Set<SDGNode> dynamicNodes = new HashSet<SDGNode>();
	private Map<SDGNode, Set<SDGNode>> entry2procs;

	public DynamicityAnalysis(final CFG threadGraph) {
		this.threadGraph = threadGraph;
		computeDynamicNodes();
	}

	public DynamicityAnalysis(final SDG sdg, final CFG threadGraph) {
		this(threadGraph);
	}

	private void computeDynamicNodes() {
		entry2procs = threadGraph.sortByProcedures();
		// 1.) Find non-trivial SCCs on the call graph - all nodes contained in
		// methods participating in such an SCC are dynamic
		final DirectedGraph<SDGNode, DefaultEdge> callGraph = extractCallGraph();
		final KosarajuStrongConnectivityInspector<SDGNode, DefaultEdge> scc
					= new KosarajuStrongConnectivityInspector<SDGNode, DefaultEdge>(callGraph);
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
		for (final SDGNode entry : entry2procs.keySet()) {
			final CFG procCFG = extractProcedureCFG(entry);
			final KosarajuStrongConnectivityInspector<SDGNode, SDGEdge> procSCC
					= new KosarajuStrongConnectivityInspector<SDGNode, SDGEdge>(procCFG);
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
			for (final Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : entry2procs.entrySet()) {
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
		entry2procs = null;
	}

	private boolean markAllNodesInMethod(final SDGNode entry) {
		return markAllNodes(entry2procs.get(entry));
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
		for (final Map.Entry<SDGNode, Set<SDGNode>> entryAndProc : entry2procs.entrySet()) {
			ret.addVertex(entryAndProc.getKey());
			for (final SDGNode n : entryAndProc.getValue()) {
				if (n.getKind() == SDGNode.Kind.CALL) {
					for (final SDGEdge callEdge : threadGraph.getOutgoingEdgesOfKindUnsafe(n, SDGEdge.Kind.CALL)) {
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
		Set<SDGNode> procNodes = entry2procs.get(entry);
		ret.addAllVertices(procNodes);
		ret.setRoot(entry);
		for (final SDGNode n : procNodes) {
			for (final SDGEdge e : threadGraph.outgoingEdgesOf(n)) {
				if (ret.containsVertex(e.getTarget())) {
					ret.addEdge(e);
				}
			}
		}
		return ret;
	}
}
