/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * The de-facto standard of the unfolding criteria. Abort when points-to sets
 * are equal.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class PtsLimitUnfoldingCriterion implements ObjTreeUnfoldingCriterion {

	/**
	 * Takes a set of parameter nodes and removes the ones not matching the
	 * unfolding criterion. The result should be a subset of the provided set.
	 * @param <T> Type of the parameter nodes to filter (form or act in)
	 * @param nodes Set of nodes to filter
	 * @param field the field that is going to be added to the nodes matching
	 * the unfolding criterion
	 * @return Subset containing only the nodes matching the unfolding criterion
	 */
	public <T extends ParameterNode<T>> Set<T>
	filterNodesWithUnfoldingCriterion(Set<T> nodes, ParameterField field) {
		Set<T> filtered = new HashSet<T>();

		for (T node : nodes) {
			T filteredNode = findNodeMatchingUnfoldingCriterion(node, field);
			filtered.add(filteredNode);
		}

		assert (nodes.containsAll(filtered)) : "The filtered nodes are not a subset of the nodes given.";

		return filtered;
	}

	/**
	 * This methods checks for the unfolding criterion presented in the paper:
	 * An Improved Slicer for Java, Christian Hammer and Gregor Snelting,
	 * PASTE 2004
	 *
	 * Let pt(x) be the points-to set for an object reference x. A node for field
	 * f need not to be added to a parent node p in the object tree, iff the
	 * path from the root r to p contains another node p' with p != p' where
	 * pt(p') == pt(p), and p' already has a child node for field f. If
	 * pt(p) == pt(p'), but p' does not yet have a child for f, f is added to p'.
	 *
	 * @param <A>
	 * @param p The starting parameter node where the field may be added to later on
	 * @return A parent node for the object field that may differ from the starting
	 * parameter node p. But it must be part of the root path of p.
	 */
	public <A extends ParameterNode<A>> A findNodeMatchingUnfoldingCriterion(A p, ParameterField field) {
		A newParent = p;

		OrdinalSet<InstanceKey> fromPts = p.getPointsTo();

		List<A> rootPath = p.getRootPath();
		for (A node : rootPath) {
			if (node != p) {
				OrdinalSet<InstanceKey> toPts = node.getPointsTo();

				if (Util.setsEqual(fromPts, toPts)) {
					newParent = node;
					//break;
				}

			}
		}

		return newParent;
	}

	public <A extends ParameterNode<A>> boolean nodeMatchesUnfoldingCriterion(A p, ParameterField field) {
		return findNodeMatchingUnfoldingCriterion(p, field) == p;
	}

	public <T extends ParameterNode<T>> boolean fieldNodesAreEqual(T node,
			ParameterField f2, OrdinalSet<InstanceKey> pts2) {
		ParameterField f1 = node.getField();
		OrdinalSet<InstanceKey> pts1 = node.getPointsTo();

		return f1 == f2 && Util.setsEqual(pts1, pts2);
	}



}
