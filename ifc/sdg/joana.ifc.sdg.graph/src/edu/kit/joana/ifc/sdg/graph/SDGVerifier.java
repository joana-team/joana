/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;


/**
 * Tries to find some common syntactic and semantic errors in the SDG.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SDGVerifier {

	private final SDG sdg;
	private final boolean directConnectedClinits;
	private final boolean checkControlFlow;
	private int error = 0;
	private int warn = 0;

	private SDGVerifier(final SDG sdg, final boolean directConnectedClinits,
			final boolean hasControlFlow) {
		this.sdg = sdg;
		this.directConnectedClinits = directConnectedClinits;
		this.checkControlFlow = hasControlFlow;
	}

	/**
	 * Tries to find errors in the sdg. Returns number of errors found.
	 * @param sdg SDG to check for errors
	 * @return number of errors
	 */
	public static int verify(final SDG sdg, boolean directConnectedClinits, boolean hasControlFlow) {
		SDGVerifier vrfy = new SDGVerifier(sdg, directConnectedClinits, hasControlFlow);
		vrfy.verify();
		return vrfy.error;
	}

	private void error(String msg) {
		System.err.println(msg);
		error++;
	}

	private void warn(String msg) {
		System.out.println(msg);
		warn++;
	}

	private void verify() {
		Set<SDGNode> nodes = sdg.vertexSet();
		final int total = nodes.size();
		final int tenPercent = total / 10;
		int done = 0;

		Set<Integer> clinits = new HashSet<Integer>();

		if (directConnectedClinits) {
			for (SDGNode node : nodes) {
				if (node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY && node.getLabel().contains("<clinit>()")) {
					clinits.add(node.getProc());
				}
			}
		}

		System.err.println("VERIFIER START");
		for (SDGNode node : nodes) {
			done++;
			if (done % tenPercent == 0) {
				System.out.println((100 * done) / total + "% done");
			}

			if (sdg.outDegreeOf(node) == 0 && sdg.inDegreeOf(node) == 0) {
				error(descr(node) + " has no edges.");
				continue;
			}

			// check for self rerefences
			if (sdg.containsEdge(node, node)) {
				// interference self recursion is ok, as the same statement may be executed in different threads
				boolean noInterferenceEdge = false;

				String msg = descr(node) + " has self recursive edges: ";
				for (SDGEdge edge : sdg.getAllEdges(node, node)) {
					noInterferenceEdge |= edge.kind != Kind.INTERFERENCE
						&& edge.kind != Kind.INTERFERENCE_WRITE && edge.kind != Kind.SYNCHRONIZATION;
					msg += edge.kind + ", ";
				}

				if (noInterferenceEdge) {
					error(msg);
				}
			}

			if (checkControlFlow && !(isConst(node) || isPHI(node))) {
				if (node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXIT) {
					for (SDGEdge out : sdg.outgoingEdgesOf(node)) {
						SDGNode target = out.getTarget();
						if (out.getKind() == edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind.CONTROL_FLOW) {
						    if (target.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY) {
						        error("Bad control flow from exit to entry: " + descr(node));
						    } else {
						        error("Exit node " + descr(node) + " is not the last in the control flow of its PDG");
						    }
						}
					}
				} else {
					boolean hasControlFlow = false;
					for (SDGEdge out : sdg.outgoingEdgesOf(node)) {
						hasControlFlow |= out.kind == Kind.CONTROL_FLOW;
						if (hasControlFlow) {
							break;
						}
					}

					if (!hasControlFlow) {
						error(descr(node) + " has no outgoing control flow.");
					}
				}
			}

			if (node.kind != edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ENTRY) {
				boolean hasControlDep = false;
				for (SDGEdge out : sdg.incomingEdgesOf(node)) {
					hasControlDep |= (out.kind == Kind.CONTROL_DEP_COND)
						|| (out.kind == Kind.CONTROL_DEP_EXPR)
						|| (out.kind == Kind.CONTROL_DEP_UNCOND);
					if (hasControlDep) {
						break;
					}
				}

				if (!hasControlDep) {
					error(descr(node) + " is not control dependend on anything.");
				}

				if (checkControlFlow && !(isConst(node) || isPHI(node))) {
					// check for incoming controlflow. all nodes except entry nodes should have an incoming flow
					boolean hasControlFlow = false;
					for (SDGEdge out : sdg.incomingEdgesOf(node)) {
						hasControlFlow |= out.kind == Kind.CONTROL_FLOW;
						if (hasControlFlow) {
							break;
						}
					}

					if (!hasControlFlow) {
						error(descr(node) + " has no incoming control flow.");
					}
				}
			} else {
				// An entry node should not be control dependent on anything
				// But: There is 1 exception. We inline recursive calls to speed
				// up summary edge computation. The entry node may therefore be control
				// dependent on a call.
				for (SDGEdge out : sdg.incomingEdgesOf(node)) {
					if ((out.kind == Kind.CONTROL_DEP_COND)
							|| (out.kind == Kind.CONTROL_DEP_EXPR)
							|| (out.kind == Kind.CONTROL_DEP_UNCOND)) {
						if (out.getSource().getKind() != edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.CALL) {
							// ignore controldep from calls to entry node
							error(descr(node) + " is control dependend on: " + descr(out.getSource()));
						}
					}
				}
			}

			if (node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.ACTUAL_IN) {
				// unresolved (cut off) calls are approximated through summary edges from all act-ins to all act-out
				// but they do not have a matching form-in - as no method exists.
				// So we are happy if we find a summary edge or (the normal case) a parameter in edge to the formal-in
				boolean hasParamIn = false;
				boolean hasSummary = false;
				for (SDGEdge out : sdg.outgoingEdgesOf(node)) {
					hasParamIn |= (out.kind == Kind.PARAMETER_IN || out.kind == Kind.FORK_IN);
					hasSummary |= out.kind == Kind.SUMMARY;
					if (hasParamIn || hasSummary) {
						break;
					}
				}
				if (!hasParamIn && !hasSummary) {
					SDGNode entry = sdg.getEntry(node);
					if (!entry.getLabel().contains(".main(java.lang.String[])")
							&& !entry.getLabel().contains("java.lang.Thread.start()") ) {
						warn(descr(node) + " has no parameter-in edge.");
					}
				}
			}


			if (node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.FORMAL_OUT && node.getProc() != 0 && !clinits.contains(node.getProc())) {
				// form-out of the outer-most *Start* method do not have any connections to actual-outs as no callsite exists
				// when we have direct connected formal-node of the static initializers (old joana style), they do not need
				// to have a connected act-out node. So we skip them.
				boolean hasParamOut = false;
				for (SDGEdge out : sdg.outgoingEdgesOf(node)) {
					hasParamOut |= (out.kind == Kind.PARAMETER_OUT || out.kind == Kind.FORK_OUT);
					if (hasParamOut) {
						break;
					}
				}
				if (!hasParamOut) {
					SDGNode entry = sdg.getEntry(node);
					if (!entry.getLabel().contains(".main(java.lang.String[])")
							&& !entry.getLabel().contains(".run()")) {
						warn(descr(node) + " has no parameter-out edge.");
					}
				}
			}
		}
		System.err.println("VERIFIER DONE (total of " + error + " errors and " + warn + " warnings)");
	}

	public static boolean isPHI(SDGNode node) {
		return node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXPRESSION && node.getLabel().contains("PHI ");
	}

	public static boolean isConst(SDGNode node) {
		return node.kind == edu.kit.joana.ifc.sdg.graph.SDGNode.Kind.EXPRESSION && node.getLabel().contains("CONST ");
	}

	private String descr(SDGNode node) {
		SDGNode entry = sdg.getEntry(node);
		String entryLabel = (entry == null ? "???" : entry.getLabel());
		return "PDG(" + entryLabel + ":" + node.getProc() + ") Node " + node.getId() + " " + node.getKind() + ": '" + node.getLabel() + "'";
	}

	public static void main(String[] args) throws IOException {
	    SDG g = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/tests.Mantel00Page10.pdg");
	    SDGVerifier.verify(g, false, true);
	}
}
