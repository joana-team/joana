/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;

/**
 * @author Martin Mohr
 */
public class SDGCall extends SDGInstruction implements SDGCallPart {

	private SortedMap<Integer, SDGActualParameter> actParams = new TreeMap<Integer, SDGActualParameter>();
	private SDGCallReturnNode returnNode;
	private SDGCallExceptionNode exceptionNode;
	private Set<JavaMethodSignature> possibleTargets = new HashSet<JavaMethodSignature>();

	public SDGCall(SDGMethod owner, SDGNode callNode, int index) {
		super(owner, callNode, index);
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
				if (act.getKind() == Kind.ACTUAL_IN) {
					actP.setInRoot(act);
				} else {
					actP.setOutRoot(act);
				}
			} else {
				if (act.getBytecodeName().equals(BytecodeLocation.RETURN_PARAM)) {
					this.returnNode = new SDGCallReturnNode(act, this);
				} else if (act.getBytecodeName().equals(BytecodeLocation.EXCEPTION_PARAM)) {
					this.exceptionNode = new SDGCallExceptionNode(act, this);
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
	
	public SDGCallReturnNode getReturn() {
		return returnNode;
	}
	
	public SDGCallExceptionNode getExceptionNode() {
		return exceptionNode;
	}
	
	public Collection<? extends SDGCallPart> getParts() {
		List<SDGCallPart> ret = new LinkedList<SDGCallPart>();
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
}
