package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import org.logicng.formulas.Formula;

public class RecursiveFuncReturnValue implements IReturnValue {

	@Override public Formula[] getReturnValueForCallSite(SSAInvokeInstruction i, Method caller) {
		return new Formula[0];
	}
}
