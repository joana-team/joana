package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import org.logicng.formulas.Formula;

public interface IReturnValue {

	Formula[] getReturnValueForCallSite(SSAInvokeInstruction i, Method caller);

}
