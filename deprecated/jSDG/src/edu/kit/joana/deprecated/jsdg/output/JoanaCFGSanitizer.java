/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.output;

import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;


/**
 * Our SDGs include nodes that are not part of the normal control flow. These
 * are
 * 1. Nodes representing a constant value (ConstPhiNode). Their label starts with "CONST ".
 * 2. Nodes representing phi-values (PhiNode). Their label starts with "PHI ".
 * Both nodes are of type EXPR, as they essentially are assignments that define the value of an ssa-variable.
 *
 * e.g. v3 = phi v2, v1
 * v4 = const #42
 *
 * As those nodes do not correspond to a statement in the sdg, they are also not part of the controlflow.
 * However, our thread interference analysis assumes (at least by now) in all its
 * algorithms, that every node is part of the controlflow. As long as this assumption
 * has not been fixed in the interference algorithms, this class can be used to convert
 * an existing SDG with PHI nodes to an SDG without those nodes that conforms to
 * this assumption.
 *
 * We convert the SDG using as follows:
 * 1. Every const phi node is simply deleted from the SDG, as they are leafs, this is no problem.
 * 2. The phi nodes are also deleted, but we keep their data dependencies. So every predecessor
 * is connected to every successor (as long as they are not the same - no self-recursive edges).
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class JoanaCFGSanitizer {

	private final SDG sdg;

	private JoanaCFGSanitizer(final SDG sdg) {
		this.sdg = sdg;
	}

	public static final boolean KEEP_CONSTANTS_IN_SDG = true;

	public static void sanitizeCFG(SDG sdg) {
		sanitizeCFG(sdg, KEEP_CONSTANTS_IN_SDG);
	}
	
	public static boolean silent = true;

	public static void sanitizeCFG(SDG sdg, boolean keepConstants) {
		JoanaCFGSanitizer scfg = new JoanaCFGSanitizer(sdg);
		scfg.run(keepConstants);
	}
	
	private void print(String str) {
		if (!silent) System.out.print(str);
	}

	private void println(String str) {
		if (!silent) System.out.println(str);
	}

	private void run(final boolean keepConstants) {
		println("Sanitizing control flow... ");

		Set<SDGNode> constPhis = new HashSet<SDGNode>();
		Set<SDGNode> phis = new HashSet<SDGNode>();

		for (SDGNode node : sdg.vertexSet()) {
			if (node.kind == Kind.EXPRESSION) {
				String label = node.getLabel();
				if (label != null && label.contains("PHI ")) {
					phis.add(node);
				} else if (label != null && label.contains("CONST ")) {
					constPhis.add(node);
				}
			}
		}

		println("\tfound " + phis.size() + " PHIs and " + constPhis.size() + " constants.");

		if (keepConstants) {
			print("\tadding const nodes to control flow... ");
			// 1. add const nodes to control flow
			assert onlyControlDepToRootOrHelp(constPhis);
			for (SDGNode cnst : constPhis) {
				SDGNode entry = sdg.getEntry(cnst);
				Set<SDGEdge> toRemove = new HashSet<SDGEdge>();
				for (SDGEdge edge : sdg.outgoingEdgesOf(entry)) {
					assert edge.getTarget().kind != Kind.EXIT;
					if (edge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW ) {
						SDGEdge newEdge = edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW.newEdge(cnst, edge.getTarget());
						sdg.addEdge(newEdge);
						toRemove.add(edge);
					}
				}
				sdg.removeAllEdges(toRemove);
				SDGEdge edge = edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW.newEdge(entry, cnst);
				sdg.addEdge(edge);
			}
			println("done.");
		} else {
			print("\tremoving all const nodes... ");
			// 1. remove const nodes
			assert onlyControlDepToRootOrHelp(constPhis);
			sdg.removeAllVertices(constPhis);
			println("done.");
		}

		print("\tadding data deps for phi nodes... ");
		// 2. alter dependencies and remove phi nodes
		for (SDGNode phi : phis) {
			Set<SDGEdge> in = sdg.incomingEdgesOf(phi);
			assert onlyDataDepsOrHelpOrControlDepToRoot(in);
			Set<SDGEdge> out = sdg.outgoingEdgesOf(phi);
			assert onlyDataDepsOrHelpOrControlDepToRoot(out);

			// in order to be robust, we ignore every edge except data dependencies
			for (SDGEdge inEdge : in) {
				if (inEdge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP
						|| inEdge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_HEAP
						|| inEdge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_ALIAS || isControlDep(inEdge)) {
					for (SDGEdge outEdge : out) {
						if ((outEdge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP || outEdge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_HEAP
								|| outEdge.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_ALIAS)
								&& inEdge.getSource() != outEdge.getTarget()) {
							SDGEdge newEdge = inEdge.getKind().newEdge(inEdge.getSource(), outEdge.getTarget());
							sdg.addEdge(newEdge);
						}
					}
				}
			}
		}
		println("done.");

		// now we can remove all phi nodes
		print("\tremoving all phi nodes... ");
		sdg.removeAllVertices(phis);
		println("done.");

		println("Sanitizing control flow done.");
	}

	private static boolean isControlDep(SDGEdge edge) {
		switch (edge.getKind()) {
		case CONTROL_DEP_CALL:
		case CONTROL_DEP_COND:
		case CONTROL_DEP_EXPR:
		case CONTROL_DEP_UNCOND:
			return true;
		default:
			return false;
		}
	}

	private boolean onlyDataDepsOrHelpOrControlDepToRoot(Set<SDGEdge> edges) {
		boolean onlyThoseEdges = true;

		for (SDGEdge edge : edges) {
			edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind kind = edge.getKind();
			if (kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_DEP && kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_HEAP
					&& kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.DATA_ALIAS) {
				if (kind == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_COND || kind == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_UNCOND) {
					// connection to root ok.
					if (edge.getSource().getKind() != Kind.ENTRY) {
						System.err.println("Control dep is not to root node: " + edge);
						onlyThoseEdges = false;
					}
				} else if (kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.HELP) {
					// all except HELP edges are not ok
					System.err.println("Not a HELP edge: " + edge);
					onlyThoseEdges = false;
				}
			}
		}

		return onlyThoseEdges;
	}

	private boolean onlyControlDepToRootOrHelp(Set<SDGNode> nodes) {
		boolean noIncomingEdges = true;

		for (SDGNode node : nodes) {
			if (sdg.inDegreeOf(node) > 0) {
				boolean nodeOk = true;

				for (SDGEdge edge : sdg.incomingEdgesOf(node)) {
					edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind kind = edge.getKind();
					if (kind != edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.HELP) {
						if (kind == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_COND || kind == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_DEP_UNCOND) {
							if (edge.getSource().getKind() != Kind.ENTRY) {
								System.err.println("Control dep is not to root node: " + edge);
								noIncomingEdges = false;
								nodeOk = false;
							}
						} else {
							nodeOk = false;
						}
					}
				}

				if (!nodeOk) {
					StringBuilder sb = new StringBuilder();
					for (SDGEdge edge : sdg.incomingEdgesOf(node)) {
						sb.append(edge.getKind() + "(" +  edge.getSource() + "), ");
					}
					System.err.println(node + "(" + node.getLabel() + ") has incoming edges: " + sb.toString());
				}

				noIncomingEdges = nodeOk;
			}
		}

		return noIncomingEdges;
	}

}
