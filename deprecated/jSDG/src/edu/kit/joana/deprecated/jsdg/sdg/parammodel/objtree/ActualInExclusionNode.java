/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;


import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * This node is used to model non-primitive method parameters that do not have
 * a points-to set associated with, because wala excluded them from the
 * analysis.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ActualInExclusionNode extends ActualInOutNode {

	public ActualInExclusionNode(int id, SSAInvokeInstruction invk,
			int ssaVar, int paramId, OrdinalSet<InstanceKey> pts) {
		super(id, true, invk, ssaVar, paramId, pts);

		assert (pts.isEmpty());
	}

}
