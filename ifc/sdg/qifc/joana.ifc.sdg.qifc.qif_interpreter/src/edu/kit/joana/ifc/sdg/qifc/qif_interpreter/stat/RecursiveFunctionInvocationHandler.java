package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.types.MethodReference;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.RecursiveFuncReturnValue;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;

public class RecursiveFunctionInvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Program p, MethodReference mRef) {
		Method callee = new Method(mRef, p);
		DotGrapher.exportDotGraph(callee.getCFG());

		StaticAnalysis sa = new StaticAnalysis(p);
		RecursiveFunctionSATVisitor sv = new RecursiveFunctionSATVisitor(sa, callee);
		sa.computeSATDeps(callee, sv);

		RecursiveFuncReturnValue rv = new RecursiveFuncReturnValue(callee, sv.getRecCalls());
		callee.registerReturnValue(rv);

		return callee;
	}
}
