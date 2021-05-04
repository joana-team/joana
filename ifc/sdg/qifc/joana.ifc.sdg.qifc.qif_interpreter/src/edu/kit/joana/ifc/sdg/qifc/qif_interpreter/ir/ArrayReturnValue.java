package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.DecisionTree;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class ArrayReturnValue extends RecursiveReturnValue<Formula[][]> {

	private final Method m;
	private final int[] paramValueNums;
	private DecisionTree<Formula[][]> returnValDecision;
	private DecisionTree<Formula[][]> returnValDecisionNoRecursion;
	private Formula[][] returnDeps;
	private final Set<Variable> recVars;

	public ArrayReturnValue(Method m) {
		this.m = m;
		this.paramValueNums = m.getIr().getParameterValueNumbers();
		this.returnValDecision = new DecisionTree<>(m, true);
		this.returnValDecisionNoRecursion = new DecisionTree<>(m, true);
		this.recVars = new HashSet<>();
	}

	@Override public Formula[][] getReturnValueForCallSite(SSAInvokeInstruction callSite, Method caller) {
		if (returnDeps == null) {
			this.returnDeps = returnValDecision.getDecision(DecisionTree.ARRAY_COMBINATOR);
		}

		Substitution s = getCallSiteSubstitution(callSite, caller, m, paramValueNums);
		Formula[][] res = new Formula[returnDeps.length][returnDeps[0].length];
		IntStream.range(0, res.length).forEach(i -> res[i] = LogicUtil.applySubstitution(returnDeps[i], s));
		return res;
	}

	@Override protected Formula[][] substituteAll(Formula[][] returnValueNoRecursion,
			Map<Integer, Formula[]> primitiveArgsForNextCall, Map<Integer, Formula[][]> arrayArgsForNextCall) {
		return new Formula[0][];
	}

	@Override protected Formula[][] substituteReturnValue(Formula[][] containsRecCall, Formula[][] recCallReturnValue,
			Formula[][] vars) {
		return new Formula[0][];
	}

	@Override public boolean isArrayType() {
		return true;
	}

	@Override public void addReturnSite(SSAReturnInstruction instruction, BBlock b) {
		int returnedValNum = instruction.getResult();
		Formula[][] returnedValue = m.getArray(returnedValNum).getValueDependencies();
		this.returnValDecision = this.returnValDecision.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());

		if (!containsRecursionVar(returnedValue)) {
			this.returnValDecisionNoRecursion = this.returnValDecisionNoRecursion
					.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());
		}
	}

	@Override public boolean containsRecursionVar(Formula[][] testValue) {
		return Arrays.stream(testValue)
				.anyMatch(arr -> Arrays.stream(arr).anyMatch(f -> LogicUtil.containsAny(f, recVars)));
	}

	@Override public Formula[][] getReturnValue() {
		return this.returnValDecision.getDecision(DecisionTree.ARRAY_COMBINATOR);
	}

	@Override public Formula[][] getReturnValueNonRecursiveCallsite(SSAInvokeInstruction instruction, Method caller) {
		if (returnDeps == null) {
			returnDeps = getReturnValue();
		}
		Substitution s = getCallSiteSubstitution(instruction, caller, m, paramValueNums);
		return LogicUtil.applySubstitution(returnDeps, s);
	}

	@Override public Formula[][] getReturnValueNoRecursion() {
		return this.returnValDecisionNoRecursion.getDecision(DecisionTree.ARRAY_COMBINATOR);
	}
}
