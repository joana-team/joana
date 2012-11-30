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

/**
 * Actual-out nodes passed through the stack.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ActOutLocalNode extends ActOutNode {

	private final int paramNum;
	private final int displayParamNum;

	public ActOutLocalNode(final int id, final boolean isPrimitive, final TypeReference type,
			final Set<PointerKey> pKey, final OrdinalSet<InstanceKey> pts, final int callId, final int paramNum,
			 final int displayParamNum) {
		super(id, type, isPrimitive, pKey, pts, callId);
		this.paramNum = paramNum;
		this.displayParamNum = displayParamNum;
	}

	public final boolean isOnHeap() {
		return false;
	}

	public final int getParameterNumber() {
		return paramNum;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params.ObjGraphParameter#getDisplayParameterNumber()
	 */
	@Override
	public int getDisplayParameterNumber() {
		return displayParamNum;
	}

}
