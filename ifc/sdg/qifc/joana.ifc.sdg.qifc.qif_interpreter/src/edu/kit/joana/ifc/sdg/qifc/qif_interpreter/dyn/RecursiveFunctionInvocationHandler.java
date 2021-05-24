package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

public class RecursiveFunctionInvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Method callee) {
		callee.finishedAnalysis();
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.StaticAnalysis sa = new StaticAnalysis(callee.getProg());
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.RecursiveFunctionSATVisitor sv = new RecursiveFunctionSATVisitor(
				sa, callee);
		sa.computeSATDeps(callee, sv);
		return callee;
	}
}