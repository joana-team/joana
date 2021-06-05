package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;

public class RecursiveFunctionInvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Method callee, Environment env) {
		callee.finishedAnalysis();
		SATAnalysis sa = new SATAnalysis(env);
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.RecursiveFunctionSATVisitor sv = new RecursiveFunctionSATVisitor(
				sa, callee);
		sa.computeSATDeps(callee, sv);
		return callee;
	}
}