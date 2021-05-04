package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;

public interface IRecursiveReturnValue<T> {

	T getRecursiveReturnValueForCallSite(SSAInvokeInstruction instruction, Method caller);

	T getReturnValueNonRecursiveCallsite(SSAInvokeInstruction instruction, Method caller);

	T getReturnValueNoRecursion();

	boolean isRecursiveCall(SSAInvokeInstruction call);
}
