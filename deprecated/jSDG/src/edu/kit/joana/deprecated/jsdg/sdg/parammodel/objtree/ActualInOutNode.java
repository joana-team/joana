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
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;

/**
 * Actual-in and -out nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ActualInOutNode extends ParameterNode<ActualInOutNode> {

	//TODO cleanup this constructor mess!

	private final SSAInvokeInstruction invk;

	@Override
	public ActualInOutNode createChild(ParameterField field,
			OrdinalSet<InstanceKey> pts) {

		assert (!field.isPrimitiveType());
		assert (pts != null) : "Points-to set of object field is null";
		assert (!getPointsTo().isEmpty()) : "Field nodes may only be added to a nodes whose points-to set is not null.";
			// empty pts of field nodes is ok as long as they are leafs
			//Assertions._assert(!pts.isEmpty(),
			//	"Points-to set of object field is empty");

		ActualInOutNode node =
			new ActualInOutNode(getPdgId(), isIn(), getInvoke(), field, pts, this);

		return node;
	}

	@Override
	public ActualInOutNode createPrimitiveChild(ParameterField field, Integer ssaVar) {
		assert (field.isPrimitiveType());

		ActualInOutNode node =
			new ActualInOutNode(getPdgId(), isIn(), getInvoke(), field, ssaVar, this);

		return node;
	}

	public <T extends ParameterNode<T>> ActualInOutNode(int id, T copy,
			SSAInvokeInstruction invk, OrdinalSet<InstanceKey> pts,
			ActualInOutNode parent) {
		super(id, copy.getType(), copy.isIn(), copy.isPrimitive(),
				copy.getSSAVar(), copy.getParamId(), copy.getField(),
				pts, parent);
		this.invk = invk;
	}

	/**
	 * As no points-to set is provided, the corresponding parameter is assumed
	 * to be a primitive type (E.g. as in Math.log(2)). The value and type of the
	 * constant may be inspected using the symboltable of the ir.
	 *
	 * Constants can only be actual-in nodes - No actual out nodes. So this
	 * option is missing here.
	 * @param id
	 * @param invk
	 * @param ssa
	 * @param paramId
	 */
	public ActualInOutNode(int id, boolean isIn, SSAInvokeInstruction invk,
			int ssa, Integer paramId, Type type) {
		super(id, type, isIn, true, ssa, paramId, null, null, null);
		this.invk = invk;
	}

	/**
	 * Creates a child object field node of a non-primitive type
	 */
	private ActualInOutNode(int id, boolean isIn, SSAInvokeInstruction invk,
			ParameterField field, OrdinalSet<InstanceKey> pts, ActualInOutNode parent) {
		super(id, Type.NORMAL, isIn, false, null, null, field, pts, parent);
		this.invk = invk;
	}

	/**
	 * Creates a child object field node of a primitive type
	 */
	private ActualInOutNode(int id, boolean isIn, SSAInvokeInstruction invk,
			ParameterField field, Integer ssaVar, ActualInOutNode parent) {
		super(id, Type.NORMAL, isIn, true, ssaVar, null, field, null, parent);
		this.invk = invk;
	}

	/**
	 * Creates a simple actual in node for a normal method parameter.
	 */
	public ActualInOutNode(int id, boolean isIn, SSAInvokeInstruction invk,
			int ssaVar, int paramId, OrdinalSet<InstanceKey> pts) {
		super(id, Type.NORMAL, isIn, false, ssaVar, paramId, null, pts, null);
		this.invk = invk;
	}

	/**
	 * This is used for exceptional and return nodes.
	 */
	public ActualInOutNode(int id, boolean isIn, int ssaVar, Type type,
			SSAInvokeInstruction invk, OrdinalSet<InstanceKey> pts) {
		super(id, type, isIn, false, ssaVar, null, null, pts, null);
		this.invk = invk;
	}

	public SSAInvokeInstruction getInvoke() {
		return invk;
	}

	/**
	 * Builds a list containing all nodes from this to the root parent node.
	 * Starting with this node and ending with the root parent.
	 * @return root path
	 */
	public List<ActualInOutNode> getRootPath() {
		List<ActualInOutNode> path;

		if (getParent() == null) {
			path = new ArrayList<ActualInOutNode>();
		} else {
			path = getParent().getRootPath();
		}

		path.add(0, this);

		return path;
	}

	public boolean isActual() {
		return true;
	}

	public boolean isFormal() {
		return false;
	}

}
