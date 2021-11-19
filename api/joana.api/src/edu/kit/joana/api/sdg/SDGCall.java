/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;

import java.util.*;

/**
 * @author Martin Mohr
 */
public class SDGCall extends SDGInstruction implements SDGCallPart {

	private SortedMap<Integer, SDGActualParameter> actParams = new TreeMap<Integer, SDGActualParameter>();
	private SDGCallReturnNode returnNode;
	private SDGCallExceptionNode exceptionNode;
	private Set<JavaMethodSignature> possibleTargets = new HashSet<JavaMethodSignature>();

	public SDGCall(SDGMethod owner, int bcIndex, String label, String type, String op) {
		super(owner, bcIndex, label, type, op);
	}

	void addActualParameter(SDGNode act) {
		if (!(act.getKind() == Kind.ACTUAL_IN || act.getKind() == Kind.ACTUAL_OUT)) {
			throw new IllegalArgumentException("Illegal kind: " + act.getKind());
		} else {
			int index = BytecodeLocation.getRootParamIndex(act.getBytecodeName());
			if (index >= 0) {
				JavaType type = JavaType.parseSingleTypeFromString(act.getType());
				SDGActualParameter actP;
				if (!actParams.containsKey(index)) {
					actP = new SDGActualParameter(this, index, type);
					actParams.put(index, actP);
				} else {
					actP = actParams.get(index);
				}
			} else {
				if (act.getBytecodeName().equals(BytecodeLocation.RETURN_PARAM)) {
					this.returnNode = new SDGCallReturnNode(this);
				} else if (act.getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
					this.exceptionNode = new SDGCallExceptionNode(this);
				}
			}
		}
	}

	void addPossibleCallTarget(JavaMethodSignature pTgt) {
		possibleTargets.add(pTgt);
	}

	public Collection<SDGActualParameter> getActualParameters() {
		return actParams.values();
	}

	/**
	* @param i number between 1 and the number of actual parameters
	* @return the actual parameter which denotes the i'th parameter
	*/
	public SDGActualParameter getActualParameter(int i) {
		return actParams.get(i);
	}

	/**
	* @return the actual parameter which denotes the receiver object of this call
	*/
	public SDGActualParameter getThis() {
		return actParams.get(0);
	}

	public SDGCallReturnNode getReturn() {
		return returnNode;
	}

	public SDGCallExceptionNode getExceptionNode() {
		return exceptionNode;
	}

	public Collection<? extends SDGCallPart> getParts() {
		List<SDGCallPart> ret = new LinkedList<SDGCallPart>();
		ret.add(this);
		ret.addAll(getActualParameters());
		if (getReturn() != null) {
			ret.add(getReturn());
		}
		if (getExceptionNode() != null) {
			ret.add(getExceptionNode());
		}
		return ret;
	}

	/**
	 * Returns whether this instruction
	 * <p/>
	 * <ol>
	 * <li>is a call instruction</li>
	 * <li>has the method with the given signature as possible call target</li>
	 * </ol>
	 *
	 * @param target
	 * @return {@code true}, if this instruction is a call instruction having
	 *         the method with the given signature as possible target,
	 *         {@code false} otherwise
	 */
	public boolean possiblyCalls(JavaMethodSignature target) {
		return possibleTargets.contains(target);
	}

	public Set<JavaMethodSignature> getPossibleTargets() {
		return new HashSet<JavaMethodSignature>(possibleTargets);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * edu.kit.joana.api.sdg.SDGInstruction#acceptVisitor(edu.kit.joana.api.
	 * sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitCall(this, data);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.OwnedBySDGCall#getOwningCall()
	 */
	@Override
	public SDGCall getOwningCall() {
		return this;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGCallPart#acceptVisitor(edu.kit.joana.api.sdg.SDGCallPartVisitor)
	 */
	@Override
	public void acceptVisitor(SDGCallPartVisitor v) {
		v.visitCallInstruction(this);
	}

	@Override
	public boolean isCall() {
		return true;
	}
}
