/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 * Provides utility code used for parameters.
 * @author Martin Mohr
 */
public class SDGParameterUtils {
	private SDGParameterUtils() {}

	/**
	 * Checks if the given inRoot or outRoot node can reach the given node using only {@link SDGEdge.Kind#PARAMETER_STRUCTURE} edges.
	 * This includes the case 'zero edges', i.e. this method also returns {@code true} if 'node' equals either 'inRoot' or 'outRoot'.
	 * The given node must not be {@code null} and either all three nodes must be formal nodes or all three nodes
	 * must be actual nodes. 
	 * @param node node to check
	 * @param inRoot possible root of a *-in parameter (may be {@code null})
	 * @param outRoot possible root of a *-out parameter (may be {@code null})
	 * @param sdg sdg in which to search
	 * @return whether inRoot or outRoot can reach the given node using only {@link SDGEdge.Kind#PARAMETER_STRUCTURE} edges
	 */
	public static boolean psBackwardsReachable(SDGNode node, SDGNode inRoot, SDGNode outRoot, SDG sdg) {
		LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
		Set<SDGNode> visited = new HashSet<SDGNode>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			SDGNode next = worklist.poll();
			visited.add(next);
			if (next.equals(inRoot) || next.equals(inRoot)) {
				return true;
			} else {
				for (SDGEdge out : sdg.incomingEdgesOf(next)) {
					if (out.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE && !visited.contains(out.getSource())) {
						worklist.add(out.getSource());
					}
				}
			}
		}
		return false;
	}

	/**
	 * Computes all the root parameter nodes or static field nodes from which the given node may be reached using only
	 * {@link SDGEdge.PARAMETER_STRUCTURE} edges
	 * @param node node to check - should be of kind ACT_IN or ACT_OUT to get a reasonable result
	 * @param sdg sdg in which to search
	 * @return all root parameter nodes or static field nodes from which the given node may be reached using only {@link SDGEdge.PARAMETER_STRUCTURE} edges
	 */
	public static List<SDGNode> collectPsBWReachableRootParameters(SDGNode node, SDG sdg) {
		LinkedList<SDGNode> ret = new LinkedList<SDGNode>();
		Stack<SDGNode> worklist = new Stack<SDGNode>();
		Set<SDGNode> visited = new HashSet<SDGNode>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			SDGNode next = worklist.pop();
			if (visited.contains(next)) {
				continue;
			}
			visited.add(next);
			if (next.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER) {
				ret.add(next);
			} else {
				for (SDGEdge out : sdg.incomingEdgesOf(next)) {
					if (out.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
						worklist.push(out.getSource());
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Locates one root parameter or static field node from which the given node may be reached using only
	 * {@link SDGEdge.PARAMETER_STRUCTURE} edges
	 * @param node node to check - should be of kind ACT_IN or ACT_OUT to get a reasonable result
	 * @param sdg sdg in which to search
	 * @return one root parameter or static field node from which the given node may be reached using only
	 * {@link SDGEdge.PARAMETER_STRUCTURE} edges, or {@code null} if none is found
	 */
	public static SDGNode findOnePsBWReachableRootParameter(SDGNode node, SDG sdg) {
		Stack<SDGNode> worklist = new Stack<SDGNode>();
		Set<SDGNode> visited = new HashSet<SDGNode>();
		worklist.add(node);
		while (!worklist.isEmpty()) {
			SDGNode next = worklist.pop();
			if (visited.contains(next)) {
				continue;
			}
			visited.add(next);
			if (next.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER) {
				return next;
			} else {
				for (SDGEdge out : sdg.incomingEdgesOf(next)) {
					if (out.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
						worklist.push(out.getSource());
					}
				}
			}
		}
		return null;
	}

	/**
	 * Locates the method call node to which the given node belongs. The given node must be of kind
	 * {@link SDGNode.Kind#ACTUAL_IN} or {@link SDGNode.Kind#ACTUAL_OUT}.
	 * @param node node to find corresponding call of
	 * @param sdg sdg in which to search
	 * @return the method call node to which the given node belongs
	 */
	public static SDGNode locateCall(SDGNode node, SDG sdg) {
		if (node.getKind() != SDGNode.Kind.ACTUAL_IN && node.getKind() != SDGNode.Kind.ACTUAL_OUT) {
			throw new IllegalArgumentException(String.format("Given node must be of kind %s or %s!", SDGNode.Kind.ACTUAL_IN.toString(), SDGNode.Kind.ACTUAL_OUT.toString()));
		}
		for (SDGEdge eIn : sdg.incomingEdgesOf(node)) {
			if (eIn.getKind() == SDGEdge.Kind.CONTROL_DEP_EXPR && eIn.getSource().getKind() == Kind.CALL) {
				return eIn.getSource();
			}
		}
		throw new IllegalStateException("This should not happen!");
	}

}
