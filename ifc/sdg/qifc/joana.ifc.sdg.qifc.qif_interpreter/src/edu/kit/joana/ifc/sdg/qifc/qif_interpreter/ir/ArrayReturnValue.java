package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.DecisionTree;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.TernaryOperator;
import edu.kit.joana.util.Triple;
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


	public ArrayReturnValue(Method m) {
		this.m = m;
		this.paramValueNums = m.getIr().getParameterValueNumbers();
		this.returnValDecision = new DecisionTree<>(m, true);
		this.returnValDecisionNoRecursion = new DecisionTree<>(m, true);
	}

	@Override protected Formula[][] substituteAll(Formula[][] returnValueNoRecursion,
			Map<Integer, Formula[]> primitiveArgsForNextCall, Map<Integer, Formula[][]> arrayArgsForNextCall) {
		Formula[][] res = new Formula[returnValueNoRecursion.length][returnValueNoRecursion[0].length];
		Substitution s = new Substitution();

		primitiveArgsForNextCall.keySet()
				.forEach(k -> s.addMapping(m.getVarsForValue(k), primitiveArgsForNextCall.get(k)));
		arrayArgsForNextCall.keySet()
				.forEach(k -> s.addMapping(m.getArray(k).getArrayVars(), arrayArgsForNextCall.get(k)));

		IntStream.range(0, res.length).forEach(i -> res[i] = LogicUtil.applySubstitution(returnValueNoRecursion[i], s));
		return res;
	}

	@Override protected Formula[][] substituteReturnValue(Formula[][] containsRecCall, Formula[][] recCallReturnValue,
			Formula[][] vars) {
		Formula[][] res = new Formula[containsRecCall.length][containsRecCall[0].length];
		Substitution s = new Substitution();

		s.addMapping(vars, recCallReturnValue);
		IntStream.range(0, res.length).forEach(i -> res[i] = LogicUtil.applySubstitution(containsRecCall[i], s));
		return res;
	}

	@Override public boolean isArrayType() {
		return true;
	}

	@Override public TernaryOperator<Formula[][]> getOperator() {
		return DecisionTree.ARRAY_COMBINATOR;
	}

	@Override public void addReturnSite(SSAReturnInstruction instruction, BasicBlock b) {
		this.returnDeps = m.getArray(instruction.getResult()).getValueDependencies();
		this.addReturn(Triple.triple(b.idx(), b.generateImplicitFlowFormula(), this.returnDeps));

		/*
		int returnedValNum = instruction.getResult();
		Formula[][] returnedValue = m.getArray(returnedValNum).getValueDependencies();
		this.returnValDecision = this.returnValDecision.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());

		if (!containsRecursionVar(returnedValue)) {
			this.returnValDecisionNoRecursion = this.returnValDecisionNoRecursion
					.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());
		}

		 */
	}

	@Override public boolean containsRecursionVar(Formula[][] testValue) {
		if (!this.isRecursive()) {
			return false;
		}
		return Arrays.stream(testValue)
				.anyMatch(arr -> Arrays.stream(arr).anyMatch(f -> LogicUtil.containsAny(f, getRecVars())));
	}

	/*
	@Override public Formula[][] getReturnValue() {
		return this.returnDeps;
	}
	 */

	@Override public Formula[][] getReturnValueNonRecursiveCallsite(SSAInvokeInstruction instruction, Method caller) {
		returnDeps = getReturnValue();
		Substitution s = getCallSiteSubstitution(instruction, caller, m, paramValueNums);
		return LogicUtil.applySubstitution(returnDeps, s);
	}

	@Override public Formula[][] getReturnValueNoRecursion() {
		return this.returnDeps;
	}

	@Override public Set<Variable> collectRecVars() {
		Set<Variable> recVars = new HashSet<>();
		for (int i = 0; i < this.getReturnValVars().length; i++) {
			Arrays.stream(this.getReturnValVars()[i]).map(f -> (Variable) f).forEach(recVars::add);
		}
		return recVars;
	}
}