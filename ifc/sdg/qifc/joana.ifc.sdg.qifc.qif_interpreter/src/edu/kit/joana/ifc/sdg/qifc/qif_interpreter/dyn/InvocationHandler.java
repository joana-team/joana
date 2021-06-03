package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;

public class InvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Method callee) {
		SATAnalysis sa = new SATAnalysis(callee.getProg());
		sa.computeSATDeps(callee);
		callee.finishedAnalysis();
		return callee;
	}
}