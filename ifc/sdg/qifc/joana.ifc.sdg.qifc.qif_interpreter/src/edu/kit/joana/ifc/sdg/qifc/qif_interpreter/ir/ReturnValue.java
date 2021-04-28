package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.stream.IntStream;

public class ReturnValue implements IReturnValue {

	private final Method m;
	int[] paramValueNums;
	Formula[] returnDeps;

	public ReturnValue(Method m) {
		this.m = m;
		this.paramValueNums = m.getIr().getParameterValueNumbers();
		this.returnDeps = computeReturnDeps(m);
	}

	@Override public Formula[] getReturnValueForCallSite(SSAInvokeInstruction callSite, Method caller) {
		// create variable substitution for args @ callsite
		Substitution s = new Substitution();
		int[] argValueNum = new int[callSite.getNumberOfUses()];
		IntStream.range(0, argValueNum.length).forEach(i -> argValueNum[i] = callSite.getUse(i));

		// idx 0 is this-reference --> skip
		for (int i = 1; i < m.getParamNum(); i++) {
			if (caller.isArrayType(argValueNum[i])) {
				Formula[][] valDeps = caller.getArray(argValueNum[i]).getValueDependencies();
				int finalI = i;
				IntStream.range(0, valDeps.length).forEach(k -> s.addMapping(m.getArray(paramValueNums[finalI]).getValueDependencies()[k], valDeps[k]));
			} else {
				s.addMapping(m.getDepsForValue(paramValueNums[i]), caller.getDepsForValue(argValueNum[i]));
			}
		}
		return LogicUtil.applySubstitution(returnDeps, s);
	}

	// creates a Formula that describes the return value of the function
	private Formula[] computeReturnDeps(Method callee) {
		return computeReturnDepsRec(callee, callee.getCFG().entry());
	}

	private Formula[] computeReturnDepsRec(Method callee, BBlock b) {
		if (b.isReturnBlock()) {
			return callee.getDepsForValue(b.getReturn().getUse(0));
		} else if (b.isCondHeader()) {
			SSAConditionalBranchInstruction condJmp = (SSAConditionalBranchInstruction) b.getWalaBasicBlock()
					.getLastInstruction();
			int blockIfTrue = condJmp.getTarget();
			BBlock trueTarget = b.succs().stream().map(dummy -> dummy.succs().get(0))
					.filter(s -> s.getWalaBasicBlock().getFirstInstructionIndex() == blockIfTrue).findFirst().get();
			BBlock falseTarget = b.succs().stream().map(dummy -> dummy.succs().get(0))
					.filter(s -> s.getWalaBasicBlock().getFirstInstructionIndex() != blockIfTrue).findFirst().get();
			return LogicUtil.ternaryOp(b.getCondExpr(), computeReturnDepsRec(callee, trueTarget),
					computeReturnDepsRec(callee, falseTarget));
		} else {
			assert (b.succs().stream()
					.filter(s -> !s.getWalaBasicBlock().isCatchBlock())
					.filter(s -> !s.getWalaBasicBlock().isExitBlock())
					.count() == 1);
			return computeReturnDepsRec(callee,
					b.succs().stream()
							.filter(s -> !s.getWalaBasicBlock().isCatchBlock())
							.filter(s -> !s.getWalaBasicBlock().isExitBlock())
							.findFirst().get());
		}
	}
}
