/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.HashSet;
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
public class SDGCall extends SDGInstruction {

	private SortedMap<Integer, SDGActualParameter> actParams = new TreeMap<Integer, SDGActualParameter>();
	private Set<JavaMethodSignature> possibleTargets = new HashSet<JavaMethodSignature>();
	
	public SDGCall(SDGMethod owner, SDGNode callNode, int index) {
		super(owner, callNode, index);
	}

	void addActualParameter(SDGNode act) {
		if (!(act.getKind() == Kind.ACTUAL_IN || act.getKind() == Kind.ACTUAL_OUT)) {
			throw new IllegalArgumentException("Illegal kind: " + act.getKind());
		} else {
			int index = BytecodeLocation.getRootParamIndex(act.getBytecodeName());
			JavaType type = JavaType.parseSingleTypeFromString(act.getType());
			SDGActualParameter actP;
			if (!actParams.containsKey(index)) {
				actP = new SDGActualParameter(this, index, type);
			} else {
				actP = actParams.get(index);
				if (act.getKind() == Kind.ACTUAL_IN) {
					actP.setInRoot(act);
				} else {
					actP.setOutRoot(act);
				}
			}
		}
	}
	
	void addPossibleCallTarget(JavaMethodSignature pTgt) {
		possibleTargets.add(pTgt);
	}
	
	/**
	 * Returns whether this instruction <p/>
	 * <ol>
	 * <li>is a call instruction </li>
	 * <li>has the method with the given signature as possible call target</li>
	 * </ol>
	 * @param target 
	 * @return {@code true}, if this instruction is a call instruction having the method with the given signature as possible target, {@code false} otherwise
	 */
	public boolean possiblyCalls(JavaMethodSignature target) {
		return possibleTargets.contains(target);
	}
}
