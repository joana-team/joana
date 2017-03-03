/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CallGraph;
import edu.kit.joana.wala.summary.EntryPointCache.LoadEntryPointException;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class GraphUtil {

	private GraphUtil() {}

	public static <V, E> Set<V> findLeafs(DirectedGraph<V, E> graph) {
		Set<V> leafs = new HashSet<V>();

		for (V node : graph.vertexSet()) {
			if (isLeaf(graph, node)) {
				leafs.add(node);
			}
		}

		return leafs;
	}

	public static <V, E> boolean isLeaf(DirectedGraph<V, E> graph, V node) {
		return graph.outDegreeOf(node) == 0 && graph.inDegreeOf(node) > 0;
	}

	public static <V, E> void removeLeaf(DirectedGraph<V, E> graph, V leaf) {
		if (!isLeaf(graph, leaf)) {
			// node is no leaf
			throw new IllegalArgumentException("Node " + leaf + " is no leaf.");
		}

		graph.removeVertex(leaf);
	}

	public static class SummaryProperties {
		public final int totalNumActOuts;
		public final int fullyConnectedActOuts;
		public final TIntSet fullyConnectedIds;
		public final TIntObjectMap<List<SDGNode>> out2in;

		SummaryProperties(int totalNumActOuts, int fullyConnectedActOuts, TIntSet fullyConnectedIds, TIntObjectMap<List<SDGNode>> out2in) {
			this.totalNumActOuts = totalNumActOuts;
			this.fullyConnectedActOuts = fullyConnectedActOuts;
			this.fullyConnectedIds = fullyConnectedIds;
			this.out2in = out2in;
		}
	}

	public static SummaryProperties createSummaryProperties(final SDG sdg) {
		int fullyConnectedActOuts = 0;
		int totalNumActOuts = 0;
		final TIntSet fullyConnectedIds = new TIntHashSet();

		for (final SDGNode actOut : sdg.vertexSet()) {
			if (actOut.kind == SDGNode.Kind.ACTUAL_OUT) {
				totalNumActOuts++;
				boolean fully = true;
				final SDGNode call = sdg.getCallSiteFor(actOut);
				for (SDGEdge edge : sdg.outgoingEdgesOf(call)) {
					SDGNode actIn = edge.getTarget();
					if (actIn.kind == SDGNode.Kind.ACTUAL_IN) {
						if (!sdg.containsEdge(actIn, actOut)) {
							fully = false;
							break;
						}
					}
				}

				if (fully) {
					fullyConnectedActOuts++;
					fullyConnectedIds.add(actOut.getId());
					for (SDGNode fOut : sdg.getFormalOuts(actOut)) {
						fullyConnectedIds.add(fOut.getId());
					}
				}
			}
		}

		final TIntObjectMap<List<SDGNode>> out2in = new TIntObjectHashMap<List<SDGNode>>();

        for (final SDGNode node : sdg.vertexSet()) {
        	if ((node.kind == SDGNode.Kind.FORMAL_OUT || node.kind == SDGNode.Kind.EXIT)
        			&& fullyConnectedIds.contains(node.getId())) {
        		final List<SDGNode> ins = new LinkedList<SDGNode>();
        		SDGNode entry = null;
        		for (SDGEdge edge : sdg.incomingEdgesOf(node)) {
        			if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
        					&& edge.getSource().kind == SDGNode.Kind.ENTRY) {
        				entry = edge.getSource();
        				break;
        			}
        		}

        		for (final SDGEdge edge : sdg.outgoingEdgesOf(entry)) {
        			if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
        					&& edge.getTarget().kind == SDGNode.Kind.FORMAL_IN) {
        				ins.add(edge.getTarget());
        			}
        		}

        		out2in.put(node.getId(), ins);
        	} else if (node.kind == SDGNode.Kind.ACTUAL_OUT && fullyConnectedIds.contains(node.getId())) {
        		final List<SDGNode> ins = new LinkedList<SDGNode>();
        		SDGNode entry = null;
        		for (final SDGEdge edge : sdg.incomingEdgesOf(node)) {
        			if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
        					&& edge.getSource().kind == SDGNode.Kind.CALL) {
        				entry = edge.getSource();
        				break;
        			}
        		}

        		for (final SDGEdge edge : sdg.outgoingEdgesOf(entry)) {
        			if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR
        					&& edge.getTarget().kind == SDGNode.Kind.ACTUAL_IN) {
        				ins.add(edge.getTarget());
        			}
        		}

        		out2in.put(node.getId(), ins);
        	}
        }

		return new SummaryProperties(totalNumActOuts, fullyConnectedActOuts, fullyConnectedIds, out2in);
	}

	public static WorkPackage.EntryPoint extractEntryPoint(SDG sdg, SDGNode entry) {
		if (entry == null) {
			throw new IllegalArgumentException();
		} else if (sdg == null) {
			throw new IllegalArgumentException();
		} else if (entry.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException("Node is not an entry node: " + entry);
		}

		TIntCollection formalOuts;
		{
			Collection<SDGNode> fOuts = sdg.getFormalOutsOfProcedure(entry);
			formalOuts = toIntSet(fOuts);
		}

		TIntCollection formalIns;
		{
			Collection<SDGNode> fIns = getFormalInsOfProcedure(sdg, entry);
			formalIns = toIntSet(fIns);
		}

		return new WorkPackage.EntryPoint(entry.getId(), formalIns, formalOuts);
	}

	/**
	 * @param nodes
	 * @return
	 */
	private static TIntCollection toIntSet(Collection<SDGNode> nodes) {
		TIntCollection set = new TIntArrayList(nodes.size());

		for (SDGNode node : nodes) {
			set.add(node.getId());
		}

		return set;
	}

	/** Returns all formal-in nodes of a procedure.
	 *
	 * @param entry  The entry node of the procedure.
	 */
	public static Collection<SDGNode> getFormalInsOfProcedure(SDG sdg, SDGNode entry) {
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		HashSet<SDGNode> fi = new HashSet<SDGNode>();

		wl.add(entry);

		while (!wl.isEmpty()) {
			SDGNode next = wl.poll();

			// traverse the object trees of the formal parameters
			// and collect the encountered formal-out nodes
			for (SDGEdge e : sdg.outgoingEdgesOf(next)) {
				if (e.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
					wl.add(e.getTarget());

				}

				if (e.getTarget().getKind() == SDGNode.Kind.FORMAL_IN) {
					fi.add(e.getTarget());
				}
			}
		}

		return fi;
	}

    /**
     * Extracts the call graph of a given graph that contains an ICFG.
     * The resulting graph consists of entry nodes and call edges
     * summarizing their dependencies in the given graph.
     *
     * @param graph  A graph containing an ICFG.
     * @return       The call graph.
     */
    public static CallGraph buildCallGraph(JoanaGraph graph) {
    	CallGraph call = new CallGraph();
        Set<SDGNode> vertexes = graph.vertexSet();

        // traverse the vertexes and add all entry nodes to the call graph
        for(SDGNode n : vertexes){
            if(n.getKind() == SDGNode.Kind.ENTRY){
                call.addVertex(n);
            }
        }

        // construct call edges between the entry nodes
        LinkedList<SDGEdge> edges = new LinkedList<SDGEdge>();
        Set<SDGNode> entries = call.vertexSet();

        for (SDGNode n : entries) {
            for (SDGEdge e : graph.incomingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.CALL || e.getKind() == SDGEdge.Kind.FORK) {
                	SDGNode callNode = graph.getEdgeSource(e);

                	SDGNode entry = graph.getEntry(callNode);

                	SDGEdge callEdge = new SDGEdge(entry, n, e.getKind());
                	edges.addFirst(callEdge);
                }
            }
        }

        // add all call edges
        for (SDGEdge e : edges) {
            call.addEdge(e);
        }

        return call;
    }

	public static <V, E> Set<V> findEntryNodes(DirectedGraph<V, E> graph, Collection<V> nodeSet) {
		Set<V> entries = new HashSet<V>();

		for (V node : nodeSet) {
			for (E edge : graph.incomingEdgesOf(node)) {
				V source = graph.getEdgeSource(edge);
				if (!nodeSet.contains(source)) {
					// node  can be entered from outside the scc
					entries.add(node);
					break;
				}
			}
		}

		return entries;
	}

	public static SDG stripGraph(SDG sdg, Collection<SDGNode> current) {
		if (current == null || current.size() == 0) {
			throw new IllegalArgumentException();
		} else if (sdg == null) {
			throw new IllegalArgumentException();
		}

		Set<SDGNode> allNodes = new HashSet<SDGNode>();

		for (SDGNode entry : current) {
			if (entry.getKind() != SDGNode.Kind.ENTRY) {
				throw new IllegalStateException("List may only contain entry nodes.");
			}

			Set<SDGNode> procNodes = sdg.getNodesOfProcedure(entry);
			allNodes.addAll(procNodes);
		}

		addCalledEntriesAndFormalNodes(sdg, allNodes);

		SDG stripped = sdg.subgraph(allNodes);

		return stripped;
	}

	public static SDG stripGraph(SDG sdg, SDGNode current) {
		if (current == null || current.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException();
		} else if (sdg == null) {
			throw new IllegalArgumentException();
		}

		Set<SDGNode> procNodes = sdg.getNodesOfProcedure(current);

		addCalledEntriesAndFormalNodes(sdg, procNodes);

		SDG stripped = sdg.subgraph(procNodes);

		return stripped;
	}

	private static void addCalledEntriesAndFormalNodes(SDG sdg, Set<SDGNode> nodes) {
		List<SDGNode> entriesAndFormals = new LinkedList<SDGNode>();

		for (SDGNode node : nodes) {
			if (node.getKind() == SDGNode.Kind.CALL) {
				for (SDGEdge call : sdg.outgoingEdgesOf(node)) {
					if (call.getKind() == SDGEdge.Kind.CALL
							|| call.getKind() == SDGEdge.Kind.FORK) {
						SDGNode callee = call.getTarget();
						assert callee.getKind() == SDGNode.Kind.ENTRY;


						entriesAndFormals.add(callee);
						entriesAndFormals.addAll(sdg.getFormalInsOfProcedure(callee));
						entriesAndFormals.addAll(sdg.getFormalOutsOfProcedure(callee));
					}
				}
			}
		}

		nodes.addAll(entriesAndFormals);
	}

	public static int adjustSubgraphWithSummaries(SDG sdg, TIntCollection exitPoints, EntryPointCache cache) throws LoadEntryPointException {
		int newDependencyEdges = 0;

		TIntIterator it = exitPoints.iterator();
		while (it.hasNext()) {
			int entryId = it.next();
			SDGNode entry = sdg.getNode(entryId);
			if (entry == null) {
				error("No node with id " + entryId + " in sdg of " + sdg.getName());
				throw new IllegalStateException();
			}

			EntryPoint ep = cache.getEntryPoint(entryId);
			if (ep == null) {
				error("No entrypoint information for " + entry.getId() + "|" + entry.getLabel());
				throw new IllegalStateException();
			}

			debug("\t\tadjusting all callsites of " + entry.getId() + "|" + entry.getLabel() + " with " + ep);

			newDependencyEdges += adjustAllCallsites(sdg, entry, ep);
		}

		return newDependencyEdges;
	}

	private static int adjustAllCallsites(SDG sdg, SDGNode calleeEntry, EntryPoint ep) {
		assert calleeEntry.getKind() == SDGNode.Kind.ENTRY;
		assert calleeEntry.getId() == ep.getEntryId();

		int newEdges = 0;

		for (TIntIterator itFin = ep.iterateFormalIns(); itFin.hasNext();) {
			final int fInId = itFin.next();
			final SDGNode formalIn = sdg.getNode(fInId);

			assert (formalIn.getKind() == SDGNode.Kind.FORMAL_IN);

			TIntList influenced = ep.getInfluencedFormOuts(fInId);
			if (influenced == null) {
				continue;
			}

			for (TIntIterator itFout = influenced.iterator(); itFout.hasNext();) {
				final int fOutId = itFout.next();
				final SDGNode formalOut = sdg.getNode(fOutId);

				assert (formalOut.getKind() == SDGNode.Kind.FORMAL_OUT);

				// will lead to direct summary edges at all callsites.
				SDGEdge ddEdge = new SDGEdge(formalIn, formalOut, SDGEdge.Kind.DATA_DEP);
				sdg.addEdge(ddEdge);
				newEdges++;
			}
		}

		return newEdges;
	}

	private static void debug(String str) {
//		System.out.println(str);
	}

	private static void error(String str) {
		System.err.println(str);
	}

}
