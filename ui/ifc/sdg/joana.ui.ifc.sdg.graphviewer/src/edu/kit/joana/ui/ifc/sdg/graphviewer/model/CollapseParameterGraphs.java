/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class CollapseParameterGraphs {

	public static MethodGraph collapse(MethodGraph methodSDG) {
		MethodGraph newMethodSDG = new MethodGraph(methodSDG.getCompleteSDG(), methodSDG.getProcID());
		filterNode(newMethodSDG.getSDG());
		return newMethodSDG;
	}

	private static void filterNode(SDG methodSDG) {
		Set<SDGNode> nodes = methodSDG.vertexSet();
		Set<SDGNode> callNodes = new HashSet<SDGNode>();
		LinkedList<SDGNode> entryNodes = new LinkedList<SDGNode>();
		callNodes(methodSDG, nodes, callNodes);
		actInOutNodes(methodSDG, callNodes);
		entryNodes(nodes,entryNodes);
		SDGNode firstEntryNode= entryNodes.getFirst();
		formInOutNodes(methodSDG, firstEntryNode);
	}

	private static void callNodes(SDG methodSdg, Set<SDGNode> nodes, Set<SDGNode> callNodes) {
		for (SDGNode node : nodes) {
			if (node.getKind() == SDGNode.Kind.CALL) {
				Collection<SDGEdge> edges = methodSdg.outgoingEdgesOf(node);
				if (edges != null) {
					for (SDGEdge edge : edges) {
						if (edge.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
							SDGNode actNode = edge.getTarget();
							if (actNode.getKind() == SDGNode.Kind.ACTUAL_IN
									|| actNode.getKind() == SDGNode.Kind.ACTUAL_OUT) {
								callNodes.add(node);
							}
						}
					}
				}
			}
		}
	}

	private static void entryNodes(Set<SDGNode> nodes, LinkedList<SDGNode> entryNodes) {
		for(SDGNode nextNode : nodes) {
			if(nextNode.getKind()==SDGNode.Kind.ENTRY){
				entryNodes.add(nextNode);
			}
		}
	}

	private static void formInOutNodes(SDG methodSdg, SDGNode entry) {
		LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
		HashSet<SDGNode> marked = new HashSet<SDGNode>();

		LinkedList<SDGNode> formInN = new LinkedList<SDGNode>();
		LinkedList<SDGEdge> formInE = new LinkedList<SDGEdge>();
		LinkedList<SDGNode> formOutN = new LinkedList<SDGNode>();
		LinkedList<SDGEdge> formOutE = new LinkedList<SDGEdge>();

		wl.add(entry);
		marked.add(entry);

		while(!wl.isEmpty()) {
			SDGNode n = wl.poll();

			for (SDGEdge next : methodSdg.outgoingEdgesOf(n)) {
				if (next.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
					SDGNode actNode = next.getTarget();

					if (actNode.getKind() == SDGNode.Kind.FORMAL_IN && marked.add(actNode)) {
						formInN.add(actNode);
						formInE.add(next);
						wl.add(actNode);

					} else if (actNode.getKind() == SDGNode.Kind.FORMAL_OUT && marked.add(actNode)) {
						formOutN.add(actNode);
						formOutE.add(next);
						wl.add(actNode);
					}
				}
			}
		}

		if (formInN.size() > 1) {
			foldTree(methodSdg, entry, formInN, formInE);
		}

		if (formOutN.size() > 1) {
			foldTree(methodSdg, entry, formOutN, formOutE);
		}
	}

	private static void actInOutNodes(SDG methodSdg, Set<SDGNode> callNodes) {
		for (SDGNode callNode : callNodes) {
			LinkedList<SDGNode> wl = new LinkedList<SDGNode>();
			HashSet<SDGNode> marked = new HashSet<SDGNode>();

			LinkedList<SDGNode> actInN = new LinkedList<SDGNode>();
			LinkedList<SDGEdge> actInE = new LinkedList<SDGEdge>();
			LinkedList<SDGNode> actOutN = new LinkedList<SDGNode>();
			LinkedList<SDGEdge> actOutE = new LinkedList<SDGEdge>();

			wl.add(callNode);
			marked.add(callNode);

			while(!wl.isEmpty()) {
				SDGNode n = wl.poll();

				for (SDGEdge next : methodSdg.outgoingEdgesOf(n)) {
					if (next.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR) {
						SDGNode actNode = next.getTarget();

						if (actNode.getKind() == SDGNode.Kind.ACTUAL_IN && marked.add(actNode)) {
							actInN.add(actNode);
							actInE.add(next);
							wl.add(actNode);

						} else if (actNode.getKind() == SDGNode.Kind.ACTUAL_OUT && marked.add(actNode)) {
							actOutN.add(actNode);
							actOutE.add(next);
							wl.add(actNode);
						}
					}
				}
			}

			if (actInN.size() > 1) {
				foldTree(methodSdg, callNode, actInN, actInE);
			}

			if (actOutN.size() > 1) {
				foldTree(methodSdg, callNode, actOutN, actOutE);
			}
		}
	}

	private static void foldTree(SDG methodSdg, SDGNode root, List<SDGNode> tree, List<SDGEdge> treeEdges) {
		LinkedList<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		TreeSet<SDGEdge> toAdd = new TreeSet<SDGEdge>(SDGEdge.getComparator());
		SDGNode anchor = tree.remove(0);

		for (SDGNode n : tree) {
			for (SDGEdge e : methodSdg.outgoingEdgesOf(n)) {
				if (e.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR) {
					// deflect edge
					toRemove.add(e);
					toAdd.add(new SDGEdge(anchor, e.getTarget(), e.getKind()));
				}
			}

			for (SDGEdge e : methodSdg.incomingEdgesOf(n)) {
				if (e.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR) {
					// deflect edge
					toRemove.add(e);
					toAdd.add(new SDGEdge(e.getSource(), anchor, e.getKind()));
				}
			}
		}

		// remove edges
		methodSdg.removeAllEdges(toRemove);
		methodSdg.removeAllEdges(treeEdges);

		// add edge from call to anchor
		toAdd.add(new SDGEdge(root, anchor, SDGEdge.Kind.CONTROL_DEP_EXPR));

		// add edges
		methodSdg.addAllEdges(toAdd);

		// remove nodes
		methodSdg.removeAllVertices(tree);
	}
}
