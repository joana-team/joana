/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class ContextGraph {
	public static class ContextEdge {
		private final TopologicalNumber source;
		private final TopologicalNumber target;
		private final SDGEdge.Kind kind;

		public ContextEdge(TopologicalNumber s, TopologicalNumber t, SDGEdge.Kind k) {
			source = s;
			target = t;
			kind = k;
		}

		public TopologicalNumber getSource() {
			return source;
		}

		public TopologicalNumber getTarget() {
			return target;
		}

		public SDGEdge.Kind getKind() {
			return kind;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(source.toString());
			sb.append(" --"+kind+"-> ");
			sb.append(target.toString());
			return sb.toString();
		}
	}

	private static class Edges {
		private final Collection<ContextEdge> incEdges;
		private final Collection<ContextEdge> outEdges;

		private Edges() {
			incEdges = new LinkedList<ContextEdge>();
			outEdges = new LinkedList<ContextEdge>();
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("inc edges: "+incEdges);
			sb.append("out edges: "+outEdges);
			return sb.toString();
		}
	}


//	private TopologicalNumber root;
	private final HashMap<TopologicalNumber, Edges> edges;
	private HashMap<SDGNode, List<TopologicalNumber>> map;
	private HashSet<TopologicalNumber>[] forks; // array of HashSet<TopologicalNumber>

	public ContextGraph() {
		edges = new HashMap<TopologicalNumber, Edges>();
	}

	public Collection<TopologicalNumber> getAllNodes() {
		return edges.keySet();
	}

	// this one is expensive - use it carefully
	public Collection<ContextEdge> getAllEdges() {
		HashSet<ContextEdge> allEdges = new HashSet<ContextEdge>();
		for (TopologicalNumber n : edges.keySet()) {
			allEdges.addAll(edges.get(n).outEdges);
		}
		return allEdges;
	}

	public boolean addContext(TopologicalNumber c) {
		if (edges.get(c) == null) {
			edges.put(c, new Edges());
			return true;

		} else {
			return false;
		}
	}

	public boolean contains(TopologicalNumber nr) {
		return edges.keySet().contains(nr);
	}

	public void addEdge(ContextEdge e) {
		edges.get(e.source).outEdges.add(e);
		edges.get(e.target).incEdges.add(e);
	}

	public Collection<ContextEdge> incomingEdgesOf(TopologicalNumber c) {
		return edges.get(c).incEdges;
	}

	public Collection<ContextEdge> outgoingEdgesOf(TopologicalNumber c) {
		return edges.get(c).outEdges;
	}

	public List<TopologicalNumber> getTopologicalNumbers(SDGNode node) {
		return map.get(node);
	}

	public HashSet<TopologicalNumber> getForkSites(int thread) {
		return forks[thread];
	}

	public HashSet<TopologicalNumber> getAllForkSites() {
		HashSet<TopologicalNumber> result = new HashSet<TopologicalNumber>();
		for (HashSet<TopologicalNumber> s : forks) {
			result.addAll(s);
		}
		return result;
	}

	void setNodeMap(HashMap<SDGNode, List<TopologicalNumber>> map) {
		this.map = map;
	}

	void setForkSites(HashSet<TopologicalNumber>[] forks) {
		this.forks = forks;
	}

	public LinkedList<TopologicalNumber> getPredecessors(TopologicalNumber nr) {
		LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
		for (ContextEdge e : incomingEdgesOf(nr)) {
			if (e.getKind() == SDGEdge.Kind.RETURN
					|| e.getKind() == SDGEdge.Kind.CALL
            		|| e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
				l.add(e.getSource());
			}
		}

		return l;
	}

	public LinkedList<TopologicalNumber> getSuccessors(TopologicalNumber nr) {
		LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
		for (ContextEdge e : outgoingEdgesOf(nr)) {
			if (e.getKind() == SDGEdge.Kind.RETURN
					|| e.getKind() == SDGEdge.Kind.CALL
            		|| e.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
				l.add(e.getTarget());
			}
		}
		return l;
	}

	public Collection<TopologicalNumber> getPredecessorsPlusNoFlow(TopologicalNumber nr) {
		LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
		for (ContextEdge e : incomingEdgesOf(nr)) {
			if (e.getKind() == SDGEdge.Kind.RETURN
					|| e.getKind() == SDGEdge.Kind.CALL
            		|| e.getKind() == SDGEdge.Kind.CONTROL_FLOW
            		|| e.getKind() == SDGEdge.Kind.NO_FLOW) {
				l.add(e.getSource());
			}
		}

		return l;
	}

	public Collection<TopologicalNumber> getSuccessorsPlusNoFlow(TopologicalNumber nr) {
		LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
		for (ContextEdge e : outgoingEdgesOf(nr)) {
			if (e.getKind() == SDGEdge.Kind.RETURN
					|| e.getKind() == SDGEdge.Kind.CALL
            		|| e.getKind() == SDGEdge.Kind.CONTROL_FLOW
            		|| e.getKind() == SDGEdge.Kind.NO_FLOW) {
				l.add(e.getTarget());
			}
		}
		return l;
	}

	/** Checks whether a topological number can reach another one.
     *
     * @param from  The source topological number.
     * @param to  The target topological number.
     */
    public boolean reach(TopologicalNumber from, TopologicalNumber to) {
        if (from.getNumber() == to.getNumber()) return true;
        if (from.getNumber() > to.getNumber()) return false;

        LinkedList<TopologicalNumber> worklist = new LinkedList<TopologicalNumber>();
        HashSet<TopologicalNumber> marked = new HashSet<TopologicalNumber>();

        worklist.addFirst(to);
        marked.add(to);

        while(!worklist.isEmpty()) {
            to = worklist.poll();
            if (from.getNumber() == to.getNumber()) return true;
            if (from.getNumber() > to.getNumber()) continue;

            for (ContextEdge e : incomingEdgesOf(to)) {
            	if (e.getKind() == SDGEdge.Kind.RETURN) {
            		TopologicalNumber pre = e.getSource();
                    List<TopologicalNumber> calls = getCalls(pre);

                    for (TopologicalNumber call : calls) {
                        // Note that Page 27 (1113) of https://doi.org/10.1145/1186632.1186636             tests "<=" instead of ">=", while
                        //           Page 76        of https://www.cse.iitb.ac.in/~ramesh/gow-thesis.ps.gz tests ">=", which fits the description in https://doi.org/10.1145/1186632.1186636.
                        if (call.getNumber() >= from.getNumber()) {
                            // skip procedure
                            if (marked.add(call)) {
                                worklist.addFirst(call);
                            }

                        } else if (pre.getNumber() >= from.getNumber()) {
                            // descend
                        	if (marked.add(pre)) {
                                worklist.addFirst(pre);
                            }
                        }
                    }

                } else if (e.getKind() == SDGEdge.Kind.CALL
                		|| e.getKind() == SDGEdge.Kind.CONTROL_FLOW
                		|| e.getKind() == SDGEdge.Kind.NO_FLOW) { // don't traverse thread edges

            		TopologicalNumber pre = e.getSource();
                    if (marked.add(pre)) {
                        worklist.addFirst(pre);
                    }
                }
            }
        }

        return false;
    }

    private List<TopologicalNumber> getCalls(TopologicalNumber exit) {
    	LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
    	for (ContextEdge e : incomingEdgesOf(exit)) {
    		if (e.getKind() == SDGEdge.Kind.HELP) {
    			l.add(e.getSource());
    		}
    	}
    	return l;
    }

    public String toString() {
    	return edges.toString();
    }
}
