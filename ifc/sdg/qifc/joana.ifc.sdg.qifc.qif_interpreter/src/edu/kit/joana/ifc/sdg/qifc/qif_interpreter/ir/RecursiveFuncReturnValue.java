package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecursiveFuncReturnValue implements IReturnValue {

	private final Method m;
	private final SSAInvokeInstruction recCall;
	private final Pair<Integer, Variable[]> recCallDef;
	private final Formula[] returnDeps;
	private final Formula[] noRecCallreturnDeps;
	final int[] paramValueNums;
	final int[] argValNums;
	private Map<Integer, Formula[]> argsForNextCall;

	public RecursiveFuncReturnValue(Method m, List<SSAInvokeInstruction> recCalls) {
		this.m = m;
		this.paramValueNums = Arrays.copyOfRange(m.getIr().getParameterValueNumbers(), 1, m.getParamNum());
		this.recCall = recCalls.get(0);
		this.returnDeps = computeReturnDeps(m);
		this.noRecCallreturnDeps = this.returnValRecPathNotTaken();
		this.argsForNextCall = new HashMap<>();
		this.recCallDef = Pair.make(recCall.getDef(), m.getVarsForValue(recCall.getDef()));
		this.argValNums = new int[recCall.getNumberOfUses() - 1];

		for (int i = 1; i < recCall.getNumberOfUses(); i++) {
			argValNums[i - 1] = recCall.getUse(i);
		}
	}

	public Formula[] simulateLastRun() {
		return substituteAll(noRecCallreturnDeps, argsForNextCall);
	}

	@Override public Formula[] getReturnValueForCallSite(SSAInvokeInstruction i, Method caller) {

		// args w/ which the recursive function was called
		int[] argValnums = new int[i.getNumberOfUses() - 1];
		for (int j = 0; j < argValnums.length; j++) {
			argsForNextCall.put(paramValueNums[j], caller.getDepsForValue(i.getUse(j + 1)));
		}

		Formula[] singleCall = substituteAll(returnDeps, argsForNextCall);

		for (int j = 0; j < m.getProg().getConfig().recDepthMax(); j++) {
			computeNextArgs();
			Formula[] nextCall = substituteAll(returnDeps, argsForNextCall);

			Substitution s = new Substitution();
			s.addMapping(m.getVarsForValue(recCall.getDef()), nextCall);
			singleCall = LogicUtil.applySubstitution(singleCall, s);
		}

		computeNextArgs();
		Formula[] lastRun = simulateLastRun();
		Substitution s = new Substitution();
		s.addMapping(m.getVarsForValue(recCall.getDef()), lastRun);
		return LogicUtil.applySubstitution(singleCall, s);
	}

	private Formula[] returnValRecPathNotTaken() {
		return returnValRecPathNotTakenRec(m.getCFG().entry()).snd;
	}

	/*
	computes the SAT formula for an execution where no rec call happens.
	First element of the pair signals whether we encountered a recursive call on the current path. If it is true, we do not take into account, the possible values returned on this path.
	 */
	private Pair<Boolean, Formula[]> returnValRecPathNotTakenRec(BBlock b) {
		if (containsRecursiveCall(b)) {
			return Pair.make(true, null);
		} else if (b.isReturnBlock()) {
			return Pair.make(false, m.getDepsForValue(b.getReturn().getUse(0)));
		} else if (b.isCondHeader()) {
			SSAConditionalBranchInstruction condJmp = (SSAConditionalBranchInstruction) b.getWalaBasicBlock()
					.getLastInstruction();
			int blockIfTrue = condJmp.getTarget();
			BBlock trueTarget = b.succs().stream().map(dummy -> dummy.succs().get(0))
					.filter(s -> s.getWalaBasicBlock().getFirstInstructionIndex() == blockIfTrue).findFirst().get();
			BBlock falseTarget = b.succs().stream().map(dummy -> dummy.succs().get(0))
					.filter(s -> s.getWalaBasicBlock().getFirstInstructionIndex() != blockIfTrue).findFirst().get();

			Pair<Boolean, Formula[]> trueF = returnValRecPathNotTakenRec(trueTarget);
			Pair<Boolean, Formula[]> falseF = returnValRecPathNotTakenRec(falseTarget);

			if (trueF.fst && falseF.fst) {
				return Pair.make(true, null);
			} else if (trueF.fst) {
				return falseF;
			} else if (falseF.fst) {
				return trueF;
			} else {
				return Pair.make(false, LogicUtil.ternaryOp(b.getCondExpr(), trueF.snd, falseF.snd));
			}
		} else {
			assert (b.succs().stream().filter(s -> !s.getWalaBasicBlock().isCatchBlock()).count() == 1);
			return returnValRecPathNotTakenRec(
					b.succs().stream().filter(s -> !s.getWalaBasicBlock().isCatchBlock()).findFirst().get());
		}
	}

	private boolean containsRecursiveCall(BBlock b) {
		return b.instructions().stream().filter(i -> i instanceof SSAInvokeInstruction)
				.anyMatch(i -> ((SSAInvokeInstruction) i).getDeclaredTarget().getSignature().equals(m.identifier()));
	}

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

			assert (b.succs().stream().filter(s -> !s.getWalaBasicBlock().isExitBlock()).count() == 1);
			return computeReturnDepsRec(callee,
					b.succs().stream().filter(s -> !s.getWalaBasicBlock().isCatchBlock()).findFirst().get());
		}
	}

	private void computeNextArgs() {
		HashMap<Integer, Formula[]> newArgs = new HashMap<>();
		for (int i = 0; i < paramValueNums.length; i++) {
			Formula[] arg = m.getDepsForValue(argValNums[i]);
			newArgs.put(paramValueNums[i], substituteAll(arg, argsForNextCall));
		}
		argsForNextCall = newArgs;
	}

	private Formula[] substituteAll(Formula[] f, Map<Integer, Formula[]> args) {
		Substitution s = new Substitution();
		for (int i : args.keySet()) {
			s.addMapping(m.getVarsForValue(i), args.get(i));
		}
		return LogicUtil.applySubstitution(f, s);
	}
}
