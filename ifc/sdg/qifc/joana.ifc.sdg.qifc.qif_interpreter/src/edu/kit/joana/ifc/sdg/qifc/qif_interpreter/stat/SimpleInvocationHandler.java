package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import org.logicng.formulas.Formula;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimpleInvocationHandler {

	private final Method caller;
	private final Method callee;
	private final SSAInvokeInstruction callSite;

	public SimpleInvocationHandler(Method caller, SSAInvokeInstruction callSite) {
		this.caller = caller;
		this.callSite = callSite;
		String calleeId = callSite.getDeclaredTarget().getSignature();
		this.callee = (caller.getProg().hasMethod(calleeId)) ? caller.getProg().getMethod(calleeId) : new Method(callSite.getDeclaredTarget(), caller.getProg());
	}

	public void analyze() {
		StaticAnalysis sa = new StaticAnalysis(caller.getProg());
		sa.computeSATDeps(callee);

		// create variable substitution for args @ callsite
		Substitution s = new Substitution();
		int[] paramValueNums = callee.getIr().getParameterValueNumbers();
		int[] argValueNUm = new int[this.callSite.getNumberOfUses()];
		IntStream.range(0, argValueNUm.length).forEach(i -> argValueNUm[i] = callSite.getUse(i));

		// idx 0 is this-reference --> skip
		for (int i = 1; i < callee.getParamNum(); i++) {
			s.addMapping(callee.getDepsForValue(paramValueNums[i]), caller.getDepsForValue(argValueNUm[i]));
		}
		Formula[] returnDeps = computeReturnDeps();

		// make sure def value exists
		if (!caller.hasValue(callSite.getDef())) {
			edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value
					.createByType(callSite.getDef(), callee.getValue(callee.getReturnValue()).getType());
			caller.addValue(callSite.getDef(), defVal);
		}
		caller.setDepsForvalue(callSite.getDef(), LogicUtil.applySubstitution(returnDeps, s));
	}

	// TODO: for now assume that each function only has a single return statement
	private Formula[] computeReturnDeps() {
		List<Formula[]> returnSites = callee.getPossibleReturns().stream().map(callee::getDepsForValue)
				.collect(Collectors.toList());
		// assert(returnSites.stream().allMatch(f -> f.length == returnSites.get(0).length));
		// assert(returnSites.size() == 1);
		return returnSites.get(0);
	}
}
