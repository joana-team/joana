/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.IncrementalSummaryBackward;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.util.maps.MultiHashMap;
import edu.kit.joana.util.maps.MultiMap;


/**
 * CAUTION: This algorithm uses the <code>IncrementalSummaryBackward</code> class, which is designed
 * for SDGs computed by Hammer's data flow analysis. It is not clear whether it works with SDGs created
 * by Graf's analysis, because <code>IncrementalSummaryBackward</code> seems to exploit a structural
 * property of Hammer's SDGs.
 *
 * TODO: Refactoring <code>IncrementalSummaryBackward</code>, such that it works with Graf's SDGs.
 *
 * @author hammer
 * @deprecated
 */
@Deprecated
public class DeclassificationSummaryNodes extends IncrementalSummaryBackward {
	private static final SDGEdge.Kind KIND = Kind.HELP;

	IStaticLattice<String> l;
	MultiMap<SecurityNode,PathEdge> pathEdge;
	LinkedList<PathEdge> worklist;

	private static class PathEdge extends SDGEdge {
		public PathEdge(SDGNode source, SDGNode sink) {
			super(source, sink, KIND);
		}

		public PathEdge(SDGNode source, SDGNode sink, Kind kind, String label) {
			super(source, sink, kind, label);
		}

	    @Override
		public int hashCode() {
	    	int hc; //= kind.hashCode();
//	    	if (label != null) hc = 37 * hc + label.hashCode();
	    	hc = /*37 * hc + */ getSource().hashCode();
	        return 37 * hc + getTarget().hashCode();
	    }

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o == null) {
				return false;
			} else if (!(o instanceof PathEdge)) {
				if (!(o instanceof SDGEdge)) {
					return false;
				} else {
					return super.equals(o);
				}
			} else {
				PathEdge edge = (PathEdge) o;

				// if (kind != edge.kind) return false;
				if (!getSource().equals(edge.getSource()))
					return false;
				return getTarget().equals(edge.getTarget());
			}
		}
	}

	/**
	 * @param g
	 */
	public DeclassificationSummaryNodes(SDG g, IStaticLattice<String> lattice) {
		super(g);
		l = lattice;
	}

    public void slice() {
    	initWorklist();

		// Summary edge computation
		while (!worklist.isEmpty()) {
			PathEdge e1 = worklist.remove();
			SecurityNode s = (SecurityNode) reachedNode(e1);

			if (summaryFound(s)) {
				addSummaries(e1);

			} else {
				for (SDGEdge e : edgesToTraverse(s)) {
					if (!phase2Edge(e) || e.getKind() == Kind.PARAMETER_OUT)
						continue;

					SecurityNode w = (SecurityNode) reachedNode(e);
					PathEdge edge = coreAlg(e1, w);
					addToPathEdge(edge);
				}
			}
		}

    	pathEdge = null;
    	worklist = null;
    }

	private void initWorklist() {
		pathEdge = new MultiHashMap<SecurityNode, PathEdge>();
    	worklist = new LinkedList<PathEdge>();
    	boolean isIPDG = true;
    	List<SDGEdge> toRemove = new ArrayList<SDGEdge>();
    	Set<SecurityNode> toAdd = new HashSet<SecurityNode>();
 		for (SDGNode o : graph.vertexSet()) {
			if (o.getKind() == SDGNode.Kind.ACTUAL_OUT) {
				boolean withSummary = false;
				for (SDGEdge e : graph.incomingEdgesOf(o)) {
					switch (e.getKind()) {
					case SUMMARY:
					case SUMMARY_DATA:
					case SUMMARY_NO_ALIAS:
						withSummary = true;
						if (isIPDG) { // if this is the first summary edge found
							pathEdge.clear();
							worklist.clear();
							isIPDG = false;
						}
						toRemove.add(e);
						break;
					case PARAMETER_OUT:
						toAdd.add((SecurityNode) e.getSource());
						assert e.getSource().getKind() == SDGNode.Kind.FORMAL_OUT;
						break;
					default:
						break;
					}
				}
				graph.removeAllEdges(toRemove);
				toRemove.clear();
				if (isIPDG || withSummary) {
					for (SecurityNode node : toAdd) {
						PathEdge e = new PathEdge(node, node, freePathKind(!node.isDeclassification()), l.getTop());
						pathEdge.add(node, e);
						worklist.add(e);
					}
					toAdd.clear();
				}
			}
    	}
	}

	private void addToPathEdge(PathEdge edge) {
		// if pathEdge does not contain edge or something has changed
		SecurityNode source = (SecurityNode) edge.getSource();
		for (PathEdge former : pathEdge.get(source)) {
			if (edge.equals(former)) {
				if (edge.getKind() != former.getKind() || !edge.getLabel().equals(former.getLabel())) {
					pathEdge.get(source).remove(former);
					pathEdge.add(source, edge);
					worklist.add(edge);
				}
				return;
			}
		}
		pathEdge.add(source, edge);
		worklist.add(edge);
	}

	private void addSummaries(PathEdge currentPathEdge) {
		// transitive dependence found
		// connect corresponding actual-in and actual-out nodes
		for (SDGEdge pi : graph.getIncomingEdgesOfKind(reachedNode(currentPathEdge), SDGEdge.Kind.PARAMETER_IN)) {
			SDGNode ai = pi.getSource();

			SDGNode root = root(ai);
			for (SDGEdge po : graph.getOutgoingEdgesOfKind(startedNode(currentPathEdge), SDGEdge.Kind.PARAMETER_OUT)) {
				SDGNode ao = po.getTarget();

				if (root == root(ao)) {

					SDGEdge sum;
					if (freePath(currentPathEdge)) {
						sum = new SDGEdge(ai, ao, SDGEdge.Kind.SUMMARY);
					} else {
						sum = new SDGEdge(ai, ao, SDGEdge.Kind.SUMMARY, currentPathEdge.getLabel());
					}
					for (SDGEdge edge : graph.getAllEdges(ai, ao)) {
						if (edge.getKind() == Kind.SUMMARY) {
							graph.removeEdge(edge);
						}
					}
					graph.addEdge(sum);

					// Iterate through all pathedges that originate in ao
					Set<PathEdge> toCheck = pathEdge.get((SecurityNode) ao);
					for (PathEdge path : new LinkedList<PathEdge>(toCheck)) {
						PathEdge edge = coreAlg(path, sum);
						addToPathEdge(edge);
					}
					break; // corresponding parameter pair done
				}
			}
		}
	}

	private PathEdge coreAlg(PathEdge shorter, SecurityNode w) {
		/* Calculate Target for new longer-pathedge */
		SecurityNode ta = (SecurityNode) startedNode(shorter);
		SecurityNode v = (SecurityNode) reachedNode(shorter);

		PathEdge longer = new PathEdge(w, ta, Kind.SUMMARY, l.getTop());
		for (PathEdge nLonger : pathEdge.get(w)) {
			if (longer.equals(nLonger)) {
				longer = nLonger;
				break;
			}
		}
		String oldSec = longer.getLabel();

		String newSec;
		if (v.isDeclassification()) {
			newSec = l.greatestLowerBound(oldSec, v.getRequired());
		} else {
			newSec = l.greatestLowerBound(oldSec, shorter.getLabel());
		}

		/* Calculate freepath for new longer-pathedge */
		SDGEdge.Kind fp = freePathKind(freePath(longer) ||
				(!w.isDeclassification() && freePath(shorter)));

		return new PathEdge(w, ta, fp, /*in,*/ newSec);
	}

	private PathEdge coreAlg(PathEdge shorter, SDGEdge e) {
		assert e.getKind() == Kind.SUMMARY;
		/* Calculate Target for new longer-pathedge */
		SecurityNode ta = (SecurityNode) startedNode(shorter);
		SecurityNode v = (SecurityNode) reachedNode(shorter);
		SecurityNode w = (SecurityNode) reachedNode(e);

		PathEdge longer = new PathEdge(w, ta, Kind.SUMMARY, l.getTop());
		for (PathEdge nLonger : pathEdge.get(w)) {
			if (longer.equals(nLonger)) {
				longer = nLonger;
				break;
			}
		}
		String oldSec = longer.getLabel();

		String newSec;
		String sumLabel = e.getLabel();
		if (v.isDeclassification()) {
			newSec = l.greatestLowerBound(oldSec,
							sumLabel == null ? v.getRequired() : sumLabel);
		} else {
			newSec = l.greatestLowerBound(oldSec,
							sumLabel == null ? shorter.getLabel() : sumLabel);
		}

		/* Calculate freepath for new longer-pathedge */
		SDGEdge.Kind fp = freePathKind(freePath(longer) ||
				(!w.isDeclassification() && sumLabel == null && freePath(shorter)));

		return new PathEdge(w, ta, fp, /*in,*/ newSec);
	}

	private static SDGEdge.Kind freePathKind(boolean b) {
		return b ? KIND : Kind.SUMMARY;
	}

	private static boolean freePath(SDGEdge shorter) {
		return shorter.getKind() == KIND;
	}
}
