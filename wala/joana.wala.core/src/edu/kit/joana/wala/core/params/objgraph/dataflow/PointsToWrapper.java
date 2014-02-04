/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.dataflow;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.PDGField;

public final class PointsToWrapper {

	private final HeapModel hm;
	private final PointerAnalysis pa;

	public PointsToWrapper(final PointerAnalysis pa) {
		this.pa = pa;
		this.hm = pa.getHeapModel();
	}

	public OrdinalSet<InstanceKey> getStaticFieldPTS(final PDGField f) {
		assert f.field.isStatic();
		assert f.field.isField();

		final PointerKey pk = hm.getPointerKeyForStaticField(f.field.getField());
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

	public OrdinalSet<InstanceKey> getMethodParamPTS(final CGNode n, final int paramNum) {
		final IR ir = n.getIR();
		if (ir == null) {
			return OrdinalSet.empty();
		}

		final int valNum = ir.getParameter(paramNum);
		final PointerKey pk = hm.getPointerKeyForLocal(n, valNum);
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

	public OrdinalSet<InstanceKey> getMethodReturnPTS(final CGNode n) {
		final PointerKey pk = hm.getPointerKeyForReturnValue(n);
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

	public OrdinalSet<InstanceKey> getMethodExceptionPTS(final CGNode n) {
		final PointerKey pk = hm.getPointerKeyForExceptionalReturnValue(n);
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

	public OrdinalSet<InstanceKey> getCallParamPTS(final CGNode n, final SSAInvokeInstruction invk,
			final int paramNum) {
		final int valNum = invk.getUse(paramNum);
		final PointerKey pk = hm.getPointerKeyForLocal(n, valNum);
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

	public OrdinalSet<InstanceKey> getCallReturnPTS(final CGNode n, final SSAInvokeInstruction invk) {
		final int valNum = invk.getReturnValue(0);
		final PointerKey pk = hm.getPointerKeyForLocal(n, valNum);
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

	public OrdinalSet<InstanceKey> getCallExceptionPTS(final CGNode n, final SSAInvokeInstruction invk) {
		final int valNum = invk.getException();
		final PointerKey pk = hm.getPointerKeyForLocal(n, valNum);
    if (pk == null) {
      return OrdinalSet.empty();
    }

		return pa.getPointsToSet(pk);
	}

}
