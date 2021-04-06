package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.types.MethodReference;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.ReturnValue;

public class InvocationHandler implements IInvocationHandler {

	@Override public Method analyze(Program p, MethodReference mRef) {

		Method callee = new Method(mRef, p);
		StaticAnalysis sa = new StaticAnalysis(p);
		sa.computeSATDeps(callee);

		ReturnValue rv = new ReturnValue(callee);
		callee.registerReturnValue(rv);

		return callee;
	}
}
