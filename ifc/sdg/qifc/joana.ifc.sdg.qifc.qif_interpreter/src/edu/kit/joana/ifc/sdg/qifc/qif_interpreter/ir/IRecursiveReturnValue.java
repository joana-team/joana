package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import org.logicng.formulas.Variable;

import java.util.Set;

public interface IRecursiveReturnValue<T> {

	T getRecursiveReturnValueForCallSite(SSAInvokeInstruction instruction, Method caller);

	T getReturnValueNonRecursiveCallsite(SSAInvokeInstruction instruction, Method caller);

	T getReturnValueNoRecursion();

	T getReturnValVars();

	boolean isRecursiveCall(SSAInvokeInstruction call);

	Set<Variable> collectRecVars();

	Set<Variable> getRecVars();

	void registerRecCall(Method m, SSAInvokeInstruction recCall, T returnValVars);

}
