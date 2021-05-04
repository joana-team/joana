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

public class ReturnValue extends RecursiveReturnValue<Formula[]> {

	private final Method m;
	private final int[] paramValueNums;
	private DecisionTree<Formula[]> returnValDecision;
	private DecisionTree<Formula[]> returnValDecisionNoRecursion;
	private Formula[] returnDeps;
	private final Set<Variable> recVars;

	public ReturnValue(Method m) {
		this.m = m;
		this.paramValueNums = m.getIr().getParameterValueNumbers();
		this.returnValDecision = new DecisionTree<>(m, true);
		this.returnValDecisionNoRecursion = new DecisionTree<>(m, true);
		this.recVars = new HashSet<>();
	}

	public Formula[] getReturnValueNonRecursiveCallsite(SSAInvokeInstruction callSite, Method caller) {
		if (returnDeps == null) {
			returnDeps = returnValDecision.getDecision(DecisionTree.INT_COMBINATOR);
		}
		Substitution s = getCallSiteSubstitution(callSite, caller, m, paramValueNums);
		return LogicUtil.applySubstitution(returnDeps, s);
	}

	@Override public boolean isArrayType() {
		return false;
	}

	@Override public void addReturnSite(SSAReturnInstruction instruction, BBlock b) {
		int returnedValNum = instruction.getResult();
		Formula[] returnedValue = m.getDepsForValue(returnedValNum);
		this.returnValDecision = this.returnValDecision.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());

		if (!containsRecursionVar(returnedValue)) {
			this.returnValDecisionNoRecursion = this.returnValDecisionNoRecursion
					.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());
		}
	}

	@Override public boolean containsRecursionVar(Formula[] testValue) {
		return Arrays.stream(testValue).anyMatch(f -> LogicUtil.containsAny(f, recVars));
	}

	@Override public Formula[] getReturnValue() {
		return this.returnValDecision.getDecision(DecisionTree.INT_COMBINATOR);
	}

	@Override public Formula[] getReturnValueNoRecursion() {
		return this.returnValDecisionNoRecursion.getDecision(DecisionTree.INT_COMBINATOR);
	}

	@Override protected Formula[] substituteAll(Formula[] f, Map<Integer, Formula[]> primitiveArgsForNextCall,
			Map<Integer, Formula[][]> arrayArgsForNextCall) {
		Substitution s = new Substitution();
		primitiveArgsForNextCall.keySet()
				.forEach(k -> s.addMapping(m.getVarsForValue(k), primitiveArgsForNextCall.get(k)));
		arrayArgsForNextCall.keySet()
				.forEach(k -> s.addMapping(m.getArray(k).getArrayVars(), arrayArgsForNextCall.get(k)));
		return LogicUtil.applySubstitution(f, s);
	}

	@Override protected Formula[] substituteReturnValue(Formula[] containsRecCall, Formula[] recCallReturnValue,
			Formula[] vars) {
		Substitution s = new Substitution();
		s.addMapping(vars, recCallReturnValue);
		return LogicUtil.applySubstitution(containsRecCall, s);
	}
}
