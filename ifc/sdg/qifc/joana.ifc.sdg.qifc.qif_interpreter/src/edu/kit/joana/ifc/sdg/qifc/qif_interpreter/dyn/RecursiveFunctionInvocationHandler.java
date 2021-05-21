package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.types.MethodReference;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;

public class RecursiveFunctionInvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Program p, MethodReference mRef) {
		Method callee = new Method(mRef, p);
		DotGrapher.exportDotGraph(callee.getCFG());

		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.StaticAnalysis sa = new StaticAnalysis(p);
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.RecursiveFunctionSATVisitor sv = new RecursiveFunctionSATVisitor(sa, callee);
		sa.computeSATDeps(callee, sv);

		return callee;
	}
}
