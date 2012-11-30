/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.interfacecomp;

import java.util.Set;


import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;

/**
 * Unfolding criterion that is always false. Can be used to create object trees
 * without child nodes. This is no safe approximation anymore, but its fast
 * and may be intresting for evaluation.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ZeroUnfoldingCriterion implements ObjTreeUnfoldingCriterion {

	public <T extends ParameterNode<T>> Set<T> filterNodesWithUnfoldingCriterion(
			Set<T> nodes, ParameterField field) {
		return HashSetFactory.make();
	}

	public <A extends ParameterNode<A>> A findNodeMatchingUnfoldingCriterion(
			A p, ParameterField field) {
		return null;
	}

	public <A extends ParameterNode<A>> boolean nodeMatchesUnfoldingCriterion(
			A p, ParameterField field) {
		return false;
	}

	public <T extends ParameterNode<T>> boolean fieldNodesAreEqual(T node,
			ParameterField f2, OrdinalSet<InstanceKey> pts2) {
		return node.getField() == f2;
	}

}
