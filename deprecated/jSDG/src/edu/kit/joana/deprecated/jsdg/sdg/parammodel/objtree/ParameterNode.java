/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 * @param <A> type of the nodes that may be childs of this node
 */
public abstract class ParameterNode<A extends ParameterNode<A>> extends AbstractParameterNode {

	private final Type type;
	private final boolean isIn;
	private final boolean primitive;
	private final Integer ssaVar;
	// this stores ssa var number of the parameter
	// not the number of the param in the method signature
	private final Integer paramId;

	/* a param in/out node is either represented by an field access or an
	 * ssa variable (e.g. for return statements) */
	private final ParameterField field;

	private OrdinalSet<InstanceKey> pointsTo;

	private final A parent;
	private Set<A> childs;

	public abstract A createChild(ParameterField field, OrdinalSet<InstanceKey> pts);

	public abstract A createPrimitiveChild(ParameterField field, Integer ssaVar);

	public boolean isMergingPointsToSupported() {
		return false;
	}

	public final void mergePointsTo(OrdinalSet<InstanceKey> pts) {
		if (isMergingPointsToSupported()) {
			assert (!(pointsTo == null && pts != null && pts.isEmpty())) : "Trying to set nonexisting pts to empty pts.";
			assert (pts != null) : "Trying to merge non-existent points-to set.";

			if (pointsTo == null || pointsTo.isEmpty()) {
				pointsTo = pts;
			} else {
				pointsTo = OrdinalSet.unify(pointsTo, pts);
			}
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@SuppressWarnings("unchecked")
	ParameterNode(int id, Type type, boolean isIn, boolean primitive,
			Integer ssaVar, Integer paramId, ParameterField field,
			OrdinalSet<InstanceKey> pointsTo, A parent) {
		super(id);
		this.type = type;
		this.isIn = isIn;
		this.primitive = primitive;
		this.ssaVar = ssaVar;
		this.paramId = paramId;
		this.field = field;
		this.pointsTo = pointsTo;
		this.parent = parent;

		assert (primitive || this instanceof ExitNode || this instanceof ActualInExclusionNode || (!primitive && pointsTo != null))
			: "A non-primitive parameter must have a points-to set: " + this;
		assert (!primitive || (primitive && pointsTo == null)) : "A primitive parameter must have an empty points-to set: " + this;
		assert (field == null || field.isStatic() || (field != null && parent != null))
			: "A parameter with a field should have a parent: " + this;
		assert (field == null || !field.isStatic() || (field != null && parent == null && field.isStatic()))
			: "A parameter with a static field should not have a parent: " + this;
		assert (field != null || (field == null && parent == null)) : "A parameter without a field should not have a parent: " + this;
		assert (parent == null || !parent.isPrimitive()) : "A primitive type parameter (" + parent
			+ ") may not have an object field: " + this;

		if (parent != null) {
			parent.addChild((A) this);
		}
	}

	public final A getParent() {
		return parent;
	}

	public final boolean hasParent() {
		return parent != null;
	}

	protected void addChild(A node) {
		if (childs == null) {
			childs = new HashSet<A>();
		}

		childs.add(node);
	}

	public final Set<A> getChilds() {
		return childs;
	}

	public final boolean hasChilds() {
		return childs != null;
	}

	public final boolean hasChild(ParameterField field) {
		return getChildForField(field) != null;
	}

	public final A getChildForField(ParameterField field) {
		if (childs == null || field == null) {
			return null;
		}

		for (A child : childs) {
			if (field.equals(child.getField())) {
				return child;
			}
		}

		return null;
	}

	/**
	 * Builds a list containing all nodes from this to the root parent node.
	 * Starting with this node and ending with the root parent.
	 * @return root path
	 */
	public abstract List<A> getRootPath();

	public static enum Type {NORMAL, EXCEPTION, RETURN};

	public final Type getType() {
		return type;
	}

	@Override
	public final String getBytecodeName() {
		if (isOnHeap()) {
			return field.getBytecodeName();
		} else if (isException()) {
			return BytecodeLocation.EXCEPTION_PARAM;
		} else if (isExit()) {
			return BytecodeLocation.RETURN_PARAM;
		} else if (isRoot()) {
			return getLabel();
		}

		return BytecodeLocation.UNKNOWN_PARAM;
	}

	@Override
	public final boolean isArray() {
		ParameterField baseField = field;
		return baseField != null && baseField.isArray();
	}

	@Override
	public final boolean isObjectField() {
		ParameterField baseField = field;
		return baseField != null && baseField.isField() && !baseField.isStatic();
	}

	@Override
	public final boolean isRoot() {
		return parent == null;
	}

	@Override
	public final boolean isStaticField() {
		ParameterField baseField = field;
		return baseField != null && baseField.isField() && baseField.isStatic();
	}

	/**
	 * Checks if this parameter node corresponds to a given parameter node.
	 * This is used to match formal-in/out params to actual-in/out params
	 * and vice versa.
	 * The possible child and parent nodes of those params is not considered.
	 * @param node
	 * @param sameInOut
	 * @return
	 */
	public final boolean isCorrespondingNode(ParameterNode<?> node, boolean sameInOut) {
		// this param-in/out node corresponds to the provided param-in/out if
		// they are of the same type and are both in/out params or not
		// because we try to match act-nodes with form-node the ssavar is likely
		// to differ because of the different context.
		// if they relate to normal parameters (not return or exceptions) they
		// must relate to the same parameter id
		boolean corresponds;

		if (sameInOut) {
			corresponds = (isIn() == node.isIn());
		} else {
			corresponds = (isIn() == !node.isIn());
		}

		corresponds &= (getType() == node.getType());

		if (getType() == Type.NORMAL) {
			if (getParamId() != null) {
				corresponds &= getParamId().equals(node.getParamId());
			} else {
				corresponds &= (node.getParamId() == null);
			}

			if (getField() != null) {
				corresponds &= getField().equals(node.getField());
			} else {
				corresponds &= node.getField() == null;
			}
		}

		corresponds &= (isPrimitive() == node.isPrimitive());
		corresponds &= (isParameter() == node.isParameter());

		return corresponds;
	}

	public final boolean isIn() {
		return isIn;
	}

	public final boolean isOut() {
		return !isIn;
	}

	public final Integer getParamId() {
		return paramId;
	}

	public final ParameterField getField() {
		return field;
	}

	public final boolean isStatic() {
		return field != null && field.isStatic();
	}

	public final OrdinalSet<InstanceKey> getPointsTo() {
		return pointsTo;
	}

	public final boolean isPrimitive() {
		assert (pointsTo != null || primitive);

		return primitive;
	}

	public final Integer getSSAVar() {
		return ssaVar;
	}

	public final boolean isFieldAccess() {
		return field != null;
	}

	public final boolean isParameter() {
		return type != Type.EXCEPTION && field == null;
	}

	public String toString() {
		String str = (isIn ? "|in|" : "|out|") + super.toString();
		/*
		if (hasChilds()) {
			str += "[";
			for (ParameterNode<?> child : getChilds()) {
				str += child.toString();
			}
			str += "]";
		}
		*/
		return str;
	}

	private boolean isMayAliasing2(ParameterNode<?> p) {
		if (isStatic() && p.isStatic()) {
			return field.equals(p.getField());
		} else if (pointsTo != null && p.pointsTo != null) {
			return pointsTo.containsAny(p.pointsTo);
		} else {
			return false;
		}
	}

	public boolean isMayAliasing(IParameter p) {
		return (p != null) && (p instanceof ParameterNode<?>) && isMayAliasing2((ParameterNode<?>) p);
	}

	private boolean isMustAliasing2(ParameterNode<?> p) {
		if (isStatic() && p.isStatic()) {
			return field.equals(p.getField());
		} else if (pointsTo != null && p.pointsTo != null) {
			return pointsTo.size() == 1 && pointsTo.getBackingSet().sameValue(p.pointsTo.getBackingSet());
		} else {
			return false;
		}
	}

	public boolean isMustAliasing(IParameter p) {
		return (p != null) && (p instanceof ParameterNode<?>) && isMustAliasing2((ParameterNode<?>) p);
	}

	public boolean isOnHeap() {
		return isStatic() || !isRoot();
	}


}
