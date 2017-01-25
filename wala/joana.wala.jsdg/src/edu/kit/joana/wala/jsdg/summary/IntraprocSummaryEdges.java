/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg.summary;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.graph.SimpleGraph;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class IntraprocSummaryEdges {

	public static SummaryGraph<SDGNode> compute(SDG sdg, SDGNode entry) {
		if (entry.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException("Node " + entry + " is not an entry node.");
		}

		Set<SDGNode> paramIn = sdg.getFormalInsOfProcedure(entry);
		Set<SDGNode> paramOut = sdg.getFormalOutsOfProcedure(entry);

		return compute(sdg, entry, paramIn, paramOut);
	}

	public static SummaryGraph<SDGNode> compute(SDG sdg, SDGNode entry, Set<SDGNode> paramIn, Set<SDGNode> paramOut) {
		if (entry.getKind() != SDGNode.Kind.ENTRY) {
			throw new IllegalArgumentException("Node " + entry + " is not an entry node.");
		}

		final HashSet<Edge> pathEdge = new HashSet<IntraprocSummaryEdges.Edge>();
		final SummaryGraph<SDGNode> summary = new SummaryGraph<SDGNode>();
		final LinkedList<Edge> worklist = new LinkedList<IntraprocSummaryEdges.Edge>();

		for (SDGNode node : paramIn) {
			assert node.kind == SDGNode.Kind.FORMAL_IN;
			summary.addVertex(node);
		}

		for (SDGNode node : paramOut) {
			assert node.kind == SDGNode.Kind.FORMAL_OUT;
			summary.addVertex(node);
			pathEdge.add(new Edge(node, node));
			worklist.add(new Edge(node, node));
		}

        while (!worklist.isEmpty()) {
            final Edge next = worklist.poll();
            final SDGNode.Kind k = next.source.getKind();

            switch (k) {
            case FORMAL_IN:
            	if (paramIn.contains(next.source)) {
            		summary.addEdge(next.source, next.target);
            	}
            	break;
            default:
                for (SDGEdge e : sdg.incomingEdgesOf(next.source)) {
                    if (e.getKind().isIntraSDGEdge()) {
                        final Edge newEdge = new Edge(e.getSource(), next.target);
                        if (pathEdge.add(newEdge)) {
                            worklist.add(newEdge);
                        }
                    }
                }
                break;
            }
        }

        return summary;
	}

	public static void writeToDotFile(SummaryGraph<SDGNode> g, String fileName, String title) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(fileName);

		out.print("digraph \"DirectedGraph\" { \n graph [label=\"");
		out.print(g.toString());
		out.print("\", labelloc=t, concentrate=true]; ");
		out.print("center=true;fontsize=12;node [fontsize=12];edge [fontsize=12]; \n");

		for (SDGNode node : g.vertexSet()) {
			out.print("   \"");
			out.print(getId(node));
			out.print("\" ");
			out.print("[label=\"[");
			out.print(node.getId());
			out.print("|");
			out.print(node.getKind());
			out.print("]");
			out.print(node.getLabel());
			out.print("\" shape=\"box\" color=\"blue\" ] \n");
		}

		for (SDGNode src : g.vertexSet()) {
			for (SummaryEdge e : g.edgesOf(src)) {
				if (g.getEdgeSource(e) != src) {
					continue;
				}
				SDGNode tgt = g.getEdgeTarget(e);

				out.print(" \"");
				out.print(getId(src));
				out.print("\" -> \"");
				out.print(getId(tgt));
				out.print("\" ");
				out.print("[label=\"");
				out.print(e.toString());
				out.print("\"]\n");
			}
		}

		out.print("\n}");

		out.flush();
		out.close();
	}

	private static String getId(Object o) {
		return "" + System.identityHashCode(o);
	}

	public static final class SummaryEdge {
		public String toString() {
			return "SU";
		}
	}

	public static final class SummaryGraph<T> extends SimpleGraph<T, SummaryEdge> {

		private static final long serialVersionUID = 407329628126455669L;

		public SummaryGraph() {
			super(SummaryEdge.class);
		}

		public boolean containsEdge(Set<T> fromNodes, Set<T> toNodes) {
			for (T from : fromNodes) {
				for (T to : toNodes) {
					if (containsEdge(from, to)) {
						return true;
					}
				}
			}

			return false;
		}

		public String toString() {
			return "SummaryGraph(" + vertexSet().size() + ", " + edgeSet().size() + ")";
		}

	}

	/*
	 * Only used for internal computation
	 */
    private static class Edge {
        private SDGNode source;
        private SDGNode target;

        private Edge(SDGNode s, SDGNode t) {
            source = s;
            target = t;
        }

        public boolean equals(Object o) {
            if (o instanceof Edge) {
                Edge e = (Edge) o;
                return (e.source == source && e.target == target);
            } else {
                return false;
            }
        }

        public int hashCode() {
            return source.getId() | target.getId() << 16;
        }

        public String toString() {
            return source.getId()+" -> "+target.getId();
        }
    }
}
