/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params;

import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Special formal-out node for the return value of a method.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ExitNode extends FormOutLocalNode {

	private final boolean isVoid;

	public ExitNode(int id, boolean isPrimitive, TypeReference type, Set<PointerKey> pKey, OrdinalSet<InstanceKey> pts, boolean isVoid) {
		super(id, isPrimitive, type, pKey, pts, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE,
				BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
		this.isVoid = isVoid;
	}

	public final boolean isVoid() {
		return isVoid;
	}

	public final boolean isExit() {
		return true;
	}

	public final boolean isMergeOk() {
		return true;
	}

}
