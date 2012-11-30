/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params;

import java.util.Set;


import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ObjectField;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.ParameterField;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Generic parameter node of the object graph.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class ObjGraphParameter extends AbstractParameterNode {

	private final boolean primitive;

	private Set<PointerKey> pKeys;
	private OrdinalSet<InstanceKey> pts;
	private final TypeReference type;

	ObjGraphParameter(int id, TypeReference type, boolean primitive, PointerKey pKey, OrdinalSet<InstanceKey> pts) {
		super(id);
		this.type = type;
		this.primitive = primitive;
		this.pKeys = HashSetFactory.make();
		pKeys.add(pKey);
		this.pts = pts;
	}

	ObjGraphParameter(int id, TypeReference type, boolean primitive,  Set<PointerKey> pKeys, OrdinalSet<InstanceKey> pts) {
		super(id);
		this.type = type;
		this.primitive = primitive;
		this.pKeys = pKeys;
		this.pts = pts;
	}

	public abstract boolean isActual();
	public abstract boolean isIn();
	public abstract boolean isOnHeap();

	public boolean isMergeOk() {
		return false;
	}

	public boolean mayBeParentOf(final IClassHierarchy cha, final ObjGraphParameter child) {
		if (isPrimitive() || isActual() != child.isActual()) {
			// || isOut() != child.isOut()) { // not true, as non-modified parent nodes are not part of the interface.
			return false;
		}

		final OrdinalSet<InstanceKey> parentPts = pts;
		final OrdinalSet<InstanceKey> basePts = child.getBasePointsTo();

		if (parentPts != null && basePts != null && !parentPts.isEmpty() && !basePts.isEmpty()) {
			return parentPts.containsAny(basePts);
		} else {
			return checkTypesAreCompatible(this, child, cha);
		}
	}

	private static final boolean checkTypesAreCompatible(ObjGraphParameter parent, ObjGraphParameter child, IClassHierarchy cha) {
		// safe approximate with class info...?
		final TypeReference pType = parent.getType();
		final ParameterField field = child.getBaseField();

		if (field.isArray()) {
			final boolean isArray = (parent.getBaseField() != null ? parent.getBaseField().isArray() : false);
			if (isArray || !pType.isArrayType()) {
				// no array field may be parent of another array field, as we approximate multidimensional arrays
				// with a single array field...
				return false;
			} else {
				// check if a reference to a pType object may point to an object of the
				// class that contains a field pointing to an array of the arrField type
				final TypeReference arrField = child.getType();
				final TypeReference elemType = pType.getArrayElementType();

				if (arrField.isPrimitiveType() && elemType.isPrimitiveType()) {
					return arrField.equals(elemType);
				} else if (arrField.isClassType() && elemType.isClassType()) {
					// parent is a normal field node of type array
					// child is a array field node
					final IClass parentCls = cha.lookupClass(pType);
					final IClass declCls = cha.lookupClass(arrField);

					return parentCls != null && declCls != null && cha.isSubclassOf(parentCls, declCls);
				} else {
					return false;
				}
			}
		} else {
			// check if a reference to a pType object may point to an object of the
			// class that contains the field of the child parameter
			final ObjectField objField = (ObjectField) field;
			final IField ifield =  objField.getField();
			final IClass declCls = ifield.getDeclaringClass();
			final IClass parentCls = cha.lookupClass(pType);

			if (parentCls != null && declCls != null
					&& (parentCls == declCls || cha.isSubclassOf(declCls, parentCls) || cha.isSubclassOf(parentCls, declCls))) {
				// ugly return true/false stmts for debugging (breakpoints...)
				return true;
			} else {
				return false;
			}
		}
	}

	public final TypeReference getType() {
		return type;
	}

	public OrdinalSet<InstanceKey> getBasePointsTo() {
		if (isOnHeap() && !getBaseField().isStatic()) {
			throw new IllegalStateException("A heap located non-static parameter must have a base points-to set.");
		}

		return null;
	}

	public ParameterField getBaseField() {
		if (isOnHeap()) {
			throw new IllegalStateException("A heap located parameter must have a base field.");
		}

		return null;
	}

	@Override
	public final boolean isArray() {
		ParameterField baseField = getBaseField();
		return baseField != null && baseField.isArray();
	}

	@Override
	public final boolean isObjectField() {
		ParameterField baseField = getBaseField();
		return baseField != null && baseField.isField() && !baseField.isStatic();
	}

	@Override
	public final boolean isRoot() {
		ParameterField baseField = getBaseField();
		return baseField == null || baseField.isStatic();
	}

	@Override
	public final boolean isStaticField() {
		ParameterField baseField = getBaseField();
		return baseField != null && baseField.isField() && baseField.isStatic();
	}

	public final String getBytecodeName() {
		if (isOnHeap()) {
			return getBaseField().getBytecodeName();
		} else if (isException()) {
			return BytecodeLocation.EXCEPTION_PARAM;
		} else if (isExit()) {
			return BytecodeLocation.RETURN_PARAM;
		} else if (isStatic()) {
			return getBaseField().getBytecodeName();
		} else if (isRoot()) {
			return BytecodeLocation.ROOT_PARAM_PREFIX + getDisplayParameterNumber();
		}

		return BytecodeLocation.UNKNOWN_PARAM;
	}

	/**
	 * For normal root method parameters this corresponds to the array index of the IMethod parameter.
	 * Beware: The display number is +1 for static methods, as by convention all normal params start at 1 and 0 is
	 * reserved for the this pointer.
	 */
	public int getParameterNumber() {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	 * For normal parameters the count always starts at 1, 0 is reserved for the this pointer. This number is used
	 * for the label and the bytecode id of this node.
	 * 
	 * This method is overwritten by all local act-/formal- in-/out- nodes
	 */
	public int getDisplayParameterNumber() {
		return getParameterNumber();
	}

	public final boolean isFormal() {
		return !isActual();
	}

	public final boolean isOut() {
		return !isIn();
	}

	public final boolean isPrimitive() {
		return primitive;
	}

	public final boolean isStatic() {
		return isOnHeap() && getBaseField().isStatic();
	}

	public final Set<PointerKey> getPointerKeys() {
		return pKeys;
	}

	public final OrdinalSet<InstanceKey> getPointsTo() {
		return pts;
	}

	public final void mergePointsTo(OrdinalSet<InstanceKey> newPts) {
		if (!isMergeOk()) {
			throw new UnsupportedOperationException("No merge suporrted for " + getClass().getCanonicalName());
		}

		assert (newPts != null) : "Trying to merge non-existent points-to set.";

		if (pts == null || pts.isEmpty()) {
			pts = newPts;
		} else {
			pts = OrdinalSet.unify(pts, newPts);
		}
	}

	public final void mergePointerKeys(Set<PointerKey> newKeys) {
		if (!isMergeOk()) {
			throw new UnsupportedOperationException("No merge suporrted for " + getClass().getCanonicalName());
		}

		Set<PointerKey> tmp = HashSetFactory.make();
		if (pKeys != null) {
			tmp.addAll(pKeys);
		}
		tmp.addAll(newKeys);
		pKeys = tmp;
	}

	public final boolean isMayAliasing(IParameter p) {
		ObjGraphParameter wp = (ObjGraphParameter) p;

		if (isStatic() && wp.isStatic()) {
			return pKeys.equals(wp.pKeys);
		} else if (pts != null && wp.pts != null) {
			return pts.containsAny(wp.pts);
		} else {
			return false;
		}
	}

	public final boolean isMustAliasing(IParameter p) {
		ObjGraphParameter wp = (ObjGraphParameter) p;

		if (isStatic() && wp.isStatic()) {
			return pKeys.equals(wp.pKeys);
		} else if (pts != null && wp.pts != null) {
			return pts.size() == 1 && wp.pts.size() == 1 && pts.containsAny(wp.pts);
		} else {
			return false;
		}
	}
/*
	public boolean equals(Object obj) {
		boolean isOk = obj != null && obj instanceof WalaParameter;
		if (isOk) {
			WalaParameter wp = (WalaParameter) obj;
			isOk &= primitive == wp.primitive && isStatic() == wp.isStatic();
			isOk &= isOnHeap() == wp.isOnHeap() && isFormal() == wp.isFormal() && isIn() == wp.isIn();
			if (pKeys != null) {
				isOk &= wp.pKeys != null && pKeys.equals(wp.pKeys);
			} else {
				isOk &= wp.pKeys == null;
			}
			if (pts != null) {
				isOk &= wp.pts != null && ((pts.isEmpty() && wp.pts.isEmpty())
						|| pts.getBackingSet().sameValue(wp.pts.getBackingSet()));
			} else {
				isOk &= wp.pts == null;
			}
			if (getBasePointsTo() != null) {
				final OrdinalSet<InstanceKey> p1 = getBasePointsTo();
				final OrdinalSet<InstanceKey> p2 = wp.getBasePointsTo();
				isOk &= p2 != null && ((p1.isEmpty() && p2.isEmpty()) || p1.getBackingSet().sameValue(p2.getBackingSet()));
			} else {
				isOk &= wp.getBasePointsTo() == null;
			}
			if (getBaseField() != null) {
				isOk &= wp.getBaseField() != null && getBaseField().equals(wp.getBaseField());
			} else {
				isOk &= wp.getBaseField() == null;
			}
		}

		return isOk;
	}*/
	/*
	public int hashCode() {
		int hc = getClass().hashCode();

		if (isMergeOk()) {
			return hc + getLabel().hashCode();
		}

		if (pKeys != null) {
			for (PointerKey pk : pKeys) {
				hc += pk.hashCode();
			}
		}

		if (pts != null) {
			for (InstanceKey ik : pts) {
				hc += ik.hashCode();
			}
		}

		if (getBasePointsTo() != null) {
			for (InstanceKey ik : getBasePointsTo()) {
				hc += (2 * ik.hashCode());
			}
		}

		if (getBaseField() != null) {
			hc += 2 * getBaseField().hashCode();
		}

		return hc;
	}*/
}
