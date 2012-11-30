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
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree.ParameterNode;

/**
 * Interface to encapsulate several different objecttree unfolding criterions.
 * 3 are implemented at the moment including k-limiting, no-objecttrees and
 * points-to limiting.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface ObjTreeUnfoldingCriterion {

	/**
	 * Looks in a set of parameter nodes for all nodes where the field field may
	 * legally (with respect to the current unfolding criterion) be added to.
	 *
	 * Takes a set of parameter nodes and creates a nodes set including only nodes
	 * matching the unfolding criterion. The parameterfield object field is
	 * the field that will be added to the nodes matching the unfolding criterion.
	 * @param <T> Type of the parameter node
	 * @param nodes Set of nodes to search
	 * @param field Parameter field a new child will be created for
	 * @return set of nodes where the child may be legally added
	 */
	public <T extends ParameterNode<T>> Set<T>
	filterNodesWithUnfoldingCriterion(Set<T> nodes, ParameterField field);

	/**
	 * Search the root path of a node p for the node where the provided field
	 * field is allowed be added (or is already present).
	 * @param <A> Type of the parameter node
	 * @param p Parameter node whose root path is searched
	 * @param field the parameter field thats going to be added to the node
	 * returned
	 * @return The node in the root path of node p where the field field is
	 * allowed to be added with respect to the unfolding criterion.
	 */
	public <A extends ParameterNode<A>> A
	findNodeMatchingUnfoldingCriterion(A p, ParameterField field);

	/**
	 * Check if the provided node matches the unfolding criterion and therefore
	 * the field field may be added as child.
	 * @param <A> type of the parameter node
	 * @param p Parameter node to test
	 * @param field Field that will be added
	 * @return true if the node matches the unfolding criterion
	 */
	public <A extends ParameterNode<A>> boolean
	nodeMatchesUnfoldingCriterion(A p, ParameterField field);

	/**
	 * Tests if two parameter nodes are considered as equal. One is provided as
	 * existing node the other node (that has not been created yet) is represented
	 * by its identifiers the points-to set of the field and the field name. In k-limiting
	 * nodes are equal as long as the fields are equal - whereas in the points-to
	 * limiting criterion the points-to sets have also to be equal.
	 * @param <T> type of the parameter node
	 * @param node parameter node
	 * @param f2 field of the other parameter node
	 * @param pts2 points-to set of the other parameter node
	 * @return
	 */
	public <T extends ParameterNode<T>> boolean
	fieldNodesAreEqual(T node, ParameterField f2, OrdinalSet<InstanceKey> pts2);

}
