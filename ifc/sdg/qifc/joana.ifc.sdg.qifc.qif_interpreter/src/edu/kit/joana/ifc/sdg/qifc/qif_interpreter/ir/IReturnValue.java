package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.stream.IntStream;

public interface IReturnValue<T> {

	T getReturnValueForCallSite(SSAInvokeInstruction i, Method caller);

	boolean isArrayType();

	boolean isRecursive();

	default Substitution getCallSiteSubstitution(SSAInvokeInstruction callSite, Method caller, Method callee,
			int[] params) {

		// create variable substitution for args @ callsite
		Substitution s = new Substitution();
		int[] argValueNum = new int[callSite.getNumberOfUses()];
		IntStream.range(0, argValueNum.length).forEach(i -> argValueNum[i] = callSite.getUse(i));

		// idx 0 is this-reference --> skip
		for (int i = 1; i < callee.getParamNum(); i++) {
			if (caller.isArrayType(argValueNum[i])) {
				Formula[][] valDeps = caller.getArray(argValueNum[i]).getValueDependencies();
				int finalI = i;
				IntStream.range(0, valDeps.length).forEach(
						k -> s.addMapping(callee.getArray(params[finalI]).getValueDependencies()[k], valDeps[k]));
			} else {
				s.addMapping(callee.getDepsForValue(params[i]), caller.getDepsForValue(argValueNum[i]));
			}
		}
		return s;
	}

	void addReturnSite(SSAReturnInstruction instruction, BBlock b);

	boolean containsRecursionVar(T testValue);

	T getReturnValue();
}
