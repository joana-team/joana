package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.types.MethodReference;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;

public class InvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Program p, MethodReference mRef) {

		Method callee = new Method(mRef, p);
		edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.StaticAnalysis sa = new StaticAnalysis(p);
		sa.computeSATDeps(callee);

		return callee;
	}
}
