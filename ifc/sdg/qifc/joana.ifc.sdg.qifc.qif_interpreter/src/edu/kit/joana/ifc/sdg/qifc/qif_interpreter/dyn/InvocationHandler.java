package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;

public class InvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Method callee, Environment env) {
		SATAnalysis sa = new SATAnalysis(env);
		sa.computeSATDeps(callee);
		callee.finishedAnalysis();
		return callee;
	}
}