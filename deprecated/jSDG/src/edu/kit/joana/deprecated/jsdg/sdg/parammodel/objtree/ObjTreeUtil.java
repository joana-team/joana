/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.Set;
import java.util.Stack;

/**
 * Utility class for object tree computation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ObjTreeUtil {

	private ObjTreeUtil() {}

	/**
	 * Searches a node matching the node searchFor in the objecttrees of all
	 * nodes that are part of the nodeSet. This methode matches In-Params to
	 * In-Params. To match In-Params to Out-Params (or vice versa) one has to
	 * the methode with the same name and the extended signature.
	 * @param <X> Type of the set of parameter nodes where a matching node
	 * is searched in
	 * @param <Y> Type of the node a matching node is searched for in the list
	 * @param nodeSet Set of parameter nodes where a matching node
	 * is searched in. The nodes in this set are expected to be root nodes.
	 * @param searchFor Node a matching node is searched for in the set
	 * @return A node that is part of the nodeSet or a child of one of those nodes
	 * that matches the node searchFor. Iff no match was found null is returned.
	 */
	public static <X extends ParameterNode<X>, Y extends ParameterNode<Y>>
	X searchForMatchingNode(Set<X> nodeSet, Y searchFor) {
		return searchForMatchingNode(nodeSet, searchFor, true);
	}

	/**
	 * Searches a node matching the node searchFor in the objecttrees of all
	 * nodes that are part of the nodeSet. This methode matches In-Params to
	 * In-Params or In-Params to Out-Params (and vice versa) depending on the
	 * value of sameInOut.
	 * @param <X> Type of the set of parameter nodes where a matching node
	 * is searched in
	 * @param <Y> Type of the node a matching node is searched for in the list
	 * @param nodeSet Set of parameter nodes where a matching node
	 * is searched in. The nodes in this set are expected to be root nodes.
	 * @param searchFor Node a matching node is searched for in the set
	 * @param sameInOut If true out params are matched to out params and in to in.
	 * 	If false out params are matched to in params and in to out.
	 * @return A node that is part of the nodeSet or a child of one of those nodes
	 * that matches the node searchFor. Iff no match was found null is returned.
	 */
	public static <X extends ParameterNode<X>, Y extends ParameterNode<Y>>
	X searchForMatchingNode(Set<X> nodeSet, Y node, boolean sameInOut) {
		for (X actNode : nodeSet) {
			X match = searchForMatchingNode(actNode, node, sameInOut);

			if (match != null) {
				return match;
			}
		}

		return null;
	}

	/**
	 * Searches for a node in the object field tree of root node that matches
	 * the given field node
	 * @param root Root node of the parameter node tree
	 * @param field Field node of a parameter node tree.
	 * @return
	 */
	public static <X extends ParameterNode<X>, Y extends ParameterNode<Y>>
	X searchForMatchingNode(X root, Y field, boolean sameInOut) {
		if (!field.hasParent()) {
			if (root.isCorrespondingNode(field, sameInOut)) {
				return root;
			} else {
				return null;
			}
		}
		Y fieldRoot = field;
		Stack<Y> nodeStack = new Stack<Y>();
		do {
			nodeStack.push(fieldRoot);
			fieldRoot = fieldRoot.getParent();
		} while (fieldRoot != null);

		X original = root;
		Y compareTo = nodeStack.pop();

		boolean finished = false;
		while (!finished) {
			if (compareTo.isCorrespondingNode(original, sameInOut)) {
				finished = nodeStack.isEmpty();
				if (!finished) {
					compareTo = nodeStack.pop();
					X child = original.getChildForField(compareTo.getField());

					// Quit iff no child has been found
					if (child == null) {
						break;
					}

					original = child;
				}
			} else {
				return null;
			}
		}

		return original;
	}



}
