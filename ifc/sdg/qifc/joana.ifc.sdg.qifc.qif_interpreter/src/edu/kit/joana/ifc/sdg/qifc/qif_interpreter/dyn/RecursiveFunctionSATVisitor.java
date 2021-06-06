package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Variable;

/**
 * SAT Visitor that is used to analyse functions that contain recursive calls
 */
public class RecursiveFunctionSATVisitor extends SATVisitor {

	private final Method m;

	public RecursiveFunctionSATVisitor(SATAnalysis SATAnalysis, Method m, Environment env) {
		super(SATAnalysis, env);
		this.m = m;
	}

	@Override public void visitInvoke(SSAInvokeInstruction instruction) {
		if (m.isCallRecursive(instruction)) {
			if (m.getReturnType().equals(Type.ARRAY)) {
				try {
					Array arr = Array.newArray(m.getReturnElementType(), instruction.getDef(), true, "r");
					m.addValue(instruction.getDef(), arr);
					((ArrayReturnValue) m.getReturn()).registerRecCall(m, instruction, arr.getArrayVars());
				} catch (UnexpectedTypeException e) {
					e.printStackTrace();
				}

			} else {
				Variable[] newVars = LogicUtil.createVars(instruction.getDef(), m.getReturnType().bitwidth(), "r");

				if (!m.hasValue(instruction.getDef())) {
					edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value
							.createPrimitiveByType(instruction.getDef(), m.getReturnType());
					m.addValue(instruction.getDef(), defVal);
				}
				m.setDepsForvalue(instruction.getDef(), newVars);
				m.addVarsToValue(instruction.getDef(), newVars);
				((ReturnValue) m.getReturn()).registerRecCall(m, instruction, newVars);
			}
		} else {
			super.visitInvoke(instruction);
		}
	}
}