package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.DecisionTree;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

public class ReturnValue implements IReturnValue<Formula> {

	private final Method m;
	int[] paramValueNums;
	DecisionTree<Formula[]> returnValDecision;
	Formula[] returnDeps;

	public ReturnValue(Method m) {
		this.m = m;
		this.paramValueNums = m.getIr().getParameterValueNumbers();
		this.returnValDecision = new DecisionTree<>(m, true);
	}

	/**
	 * computes formulas that represents the return value of the called method {@code m} @ callsite {@code callsite} by {@code caller}.
	 *
	 * Result is computed by obtaining the used arguments from {@code caller} and substituting those values in for the parameter variables of {@code returnDeps}
	 *
	 * @param callSite invokeInstruction where the return value is used
	 * @param caller Method that calls the function. Has to contain value dependencies for the arguments used in {@code callSite}
	 * @return formulas that represents the return value of the called method
	 */
	@Override public Formula[] getReturnValueForCallSite(SSAInvokeInstruction callSite, Method caller) {
		if (returnDeps == null) {
			returnDeps = returnValDecision.getDecision(DecisionTree.INT_COMBINATOR);
		}
		Substitution s = getCallSiteSubstitution(callSite, caller, m, paramValueNums);
		return LogicUtil.applySubstitution(returnDeps, s);
	}

	@Override public boolean isArrayType() {
		return false;
	}

	@Override
	public void addReturnSite(SSAReturnInstruction instruction, BBlock b) {
		int returnedValNum = instruction.getResult();
		Formula[] returnedValue = m.getDepsForValue(returnedValNum);
		this.returnValDecision = this.returnValDecision.addLeaf(b.idx(), returnedValue, b.getImplicitFlows());
	}
}
