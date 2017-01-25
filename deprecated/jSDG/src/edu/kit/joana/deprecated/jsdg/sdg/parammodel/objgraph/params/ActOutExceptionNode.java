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
 * Special actual-out node for exceptional return value.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ActOutExceptionNode extends ActOutLocalNode {

	public ActOutExceptionNode(int id, TypeReference type, Set<PointerKey> pKey,
			OrdinalSet<InstanceKey> pts, int callId) {
		super(id, false, type, pKey, pts, callId, BytecodeLocation.UNDEFINED_POS_IN_BYTECODE,
				BytecodeLocation.UNDEFINED_POS_IN_BYTECODE);
	}

	public final boolean isException() {
		return true;
	}

}
