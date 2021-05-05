package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.DecisionTree;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.stream.IntStream;

public class ArrayReturnValue implements IReturnValue<Formula[][]> {

	private final Method m;
	private final int[] paramValueNums;
	private DecisionTree<Formula[][]> returnValDecision;
	private Formula[][] returnDeps;

	public ArrayReturnValue(Method m) {
		this.m = m;
		this.paramValueNums = m.getIr().getParameterValueNumbers();
		this.returnValDecision = new DecisionTree<>(m, true);
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

	@Override public boolean isArrayType() {
		return true;
	}

	@Override public void addReturnSite(SSAReturnInstruction instruction, BBlock b) {
		int returnedValNum = instruction.getResult();
		Formula[][] returnedValue = m.getArray(returnedValNum).getValueDependencies();
		// TODO let returnvalues handle iftrees
		this. returnValDecision = this.returnValDecision.addLeaf(b.idx(), returnedValue, null);
	}
}
