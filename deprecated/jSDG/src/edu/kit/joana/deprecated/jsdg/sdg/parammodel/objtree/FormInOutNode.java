/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.ArrayList;
import java.util.List;


import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.util.Util;

/**
 * Formal-in and -out nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class FormInOutNode extends ParameterNode<FormInOutNode> {

//TODO cleanup this constructor mess!

	@Override
	public FormInOutNode createChild(ParameterField field, OrdinalSet<InstanceKey> pts) {
		assert (!field.isPrimitiveType());
		assert (pts != null) : "Points-to set of object field is null: " + Util.fieldName(field);
		assert (!getPointsTo().isEmpty()) : "Field nodes may only be added to a nodes whose points-to set is not null.";
			// empty pts of field nodes is ok as long as they are leafs
//			Assertions._assert(!pts.isEmpty(),
//				"Points-to set of object field is empty: " + Util.fieldName(field));

		FormInOutNode node =
			new FormInOutNode(getPdgId(), isIn(), field, pts, this);

		return node;
	}

	@Override
	public FormInOutNode createPrimitiveChild(ParameterField field, Integer ssaVar) {
		assert (field.isPrimitiveType());

		FormInOutNode node =
			new FormInOutNode(getPdgId(), isIn(), field, ssaVar, this);

		return  node;
	}

	/**
	 * Used to create an ExitNode or ExceptionExitNode
	 */
	public FormInOutNode(int id, Type type, boolean isIn, boolean isPrimitive) {
		super(id, type, isIn, isPrimitive, null, null, null,
				(isPrimitive ? null : OrdinalSet.<InstanceKey>empty()), null);
	}

	/**
	 * Creates a static field node
	 */
	public FormInOutNode(int id, boolean isIn, ParameterField field,
			OrdinalSet<InstanceKey> pts) {
		super(id, Type.NORMAL, isIn, false, null, null, field, pts, null);
	}

	/**
	 * Creates a primitive static field node
	 */
	public FormInOutNode(int id, boolean isIn, ParameterField field) {
		super(id, Type.NORMAL, isIn, true, null, null, field, null, null);
	}

	/**
	 * Creates a child node
	 */
	private FormInOutNode(int id, boolean isIn, ParameterField field,
			OrdinalSet<InstanceKey> pts, FormInOutNode parent) {
		super(id, Type.NORMAL, isIn, false, null, null, field, pts, parent);
	}

	/**
	 * Creates a primitive child node
	 */
	private FormInOutNode(int id, boolean isIn, ParameterField field,
			Integer ssaVar, FormInOutNode parent) {
		super(id, Type.NORMAL, isIn, true, ssaVar, null, field, null, parent);
	}

	/**
	 * Creates a form in node for a primitive parameter.
	 */
	public FormInOutNode(int id, int paramNum, int ssaVar) {
		super(id, Type.NORMAL, true, true, ssaVar, paramNum, null, null, null);
	}

	/**
	 * Used to create the root formal in parameter nodes (this, param 0-n)
	 */
	public FormInOutNode(int id, boolean isIn, int paramNum, int ssaVar,
			OrdinalSet<InstanceKey> pts) {
		super(id, Type.NORMAL, isIn, false, ssaVar, paramNum, null, pts, null);
	}

	/**
	 * Used to create exceptional and return formal out nodes
	 */
	public FormInOutNode(int id, int ssaVar, Type type,
			OrdinalSet<InstanceKey> pts) {
		super(id, type, false, false, ssaVar, null, null, pts, null);
	}

	/**
	 * Creates a copy of a root parameter node. Its parent has to be null;
	 */
	public <T extends ParameterNode<T>> FormInOutNode(int id, T copy,
			OrdinalSet<InstanceKey> pts) {
		super(id, copy.getType(), copy.isIn(), copy.isPrimitive(),
				copy.getSSAVar(), copy.getParamId(), copy.getField(), pts, null);

		assert (copy.getParent() == null) : "Copying a non-root parameter: " + copy;
	}

	/**
	 * Create a copy of a primitive root parameter node
	 * @param <T>
	 * @param id
	 * @param copy
	 * @param pts
	 */
	public <T extends ParameterNode<T>> FormInOutNode(int id, T copy) {
		super(id, copy.getType(), copy.isIn(), copy.isPrimitive(),
				copy.getSSAVar(), copy.getParamId(), copy.getField(), null, null);

		assert (copy.getParent() == null) : "Copying a non-root parameter: " + copy;
		assert (copy.isPrimitive());
		assert (isPrimitive());
	}

	/**
	 * Creates a copy of a root parameter node with a given in or out direction.
	 * Its parent has to be null;
	 */
	public <T extends ParameterNode<T>> FormInOutNode(int id, T copy,
			boolean isIn, OrdinalSet<InstanceKey> pts) {
		super(id, copy.getType(), isIn, copy.isPrimitive(),
				copy.getSSAVar(), copy.getParamId(), copy.getField(), pts, null);

		assert (copy.getParent() == null) : "Copying a non-root parameter: " + copy;
	}

	/**
	 * Builds a list containing all nodes from this to the root parent node.
	 * Starting with this node and ending with the root parent.
	 * @return root path
	 */
	public List<FormInOutNode> getRootPath() {
		List<FormInOutNode> path;

		if (getParent() == null) {
			path = new ArrayList<FormInOutNode>();
		} else {
			path = getParent().getRootPath();
		}

		assert (getLabel() != null);

		path.add(0, this);

		return path;
	}

	/**
	 * Checks if this node is the matching form-in node of the provided
	 * formal-out node. Those nodes match iff they correspond to each other
	 * and they refer to the same ssa variable. This is special for the
	 * form-in/out case as the actual/formal parameter buddies do not have a
	 * shared ssa variable in most cases.
	 * @param formOut
	 * @return true iff this node is the matching form-out node
	 */
	public final boolean isMatchingFormOut(FormInOutNode formOut) {
		assert (formOut.isOut()) : "Parameter " + formOut + " is not a form-out node.";
		assert (isIn()) : "Parameter " + this +	" is not a form-in node.";

		return getSSAVar() == formOut.getSSAVar() && isCorrespondingNode(formOut, false);
	}

	public boolean isExit() {
		return false;
	}

	public boolean isActual() {
		return false;
	}

	public boolean isFormal() {
		return true;
	}

}
