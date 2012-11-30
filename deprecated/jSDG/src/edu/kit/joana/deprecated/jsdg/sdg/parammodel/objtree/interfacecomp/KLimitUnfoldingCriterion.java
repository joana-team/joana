/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import java.util.List;
import java.util.Set;


import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;

/**
 * k-limiting unfolding criterion. Stops unfolding at the k-th tree depth.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class KLimitUnfoldingCriterion implements ObjTreeUnfoldingCriterion {

	private final int limit;

	public KLimitUnfoldingCriterion(int limit) {
		this.limit = limit;
	}

	private <T extends ParameterNode<T>> int countFieldsInRootPath(T node, ParameterField field) {
		int num = 0;

		List<T> path = node.getRootPath();
		for (T curNode : path) {
			ParameterField curField = curNode.getField();
			if (curField != null && curField == field) {
				num++;
			}
		}

		return num;
	}

	public <T extends ParameterNode<T>> Set<T> filterNodesWithUnfoldingCriterion(
			Set<T> nodes, ParameterField field) {
		Set<T> filtered = HashSetFactory.make();

		for (T node : nodes) {
			if (nodeMatchesUnfoldingCriterion(node, field)) {
				filtered.add(node);
			}
		}

		return filtered;
	}

	public <A extends ParameterNode<A>> A findNodeMatchingUnfoldingCriterion(
			A p, ParameterField field) {
		int k = countFieldsInRootPath(p, field);

		if (k < limit) {
			return p;
		} else {
			List<A> path = p.getRootPath();
			for (A node : path) {
				ParameterField curField = node.getField();
				if (curField != null && curField == field) {
					k--;
					if (k < limit) {
						return node;
					}
				}
			}
		}

		throw new IllegalStateException("Found no node with k < " + limit +
				" in rootpath despite previous counting was > " + limit);
	}

	public <A extends ParameterNode<A>> boolean nodeMatchesUnfoldingCriterion(
			A p, ParameterField field) {
		int k = countFieldsInRootPath(p, field);

		return k < limit;
	}

	public <T extends ParameterNode<T>> boolean fieldNodesAreEqual(T node,
			ParameterField f2, OrdinalSet<InstanceKey> pts2) {
		int k = countFieldsInRootPath(node, f2);

		return k >= limit && f2 == node.getField();
	}

}
