package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * SAT Visitor that is used to analyse functions that contain recursive calls
 */
public class RecursiveFunctionSATVisitor extends SATVisitor {

	private final Method m;
	private final List<SSAInvokeInstruction> recCalls;

	public RecursiveFunctionSATVisitor(StaticAnalysis staticAnalysis, Method m) {
		super(staticAnalysis);
		this.m = m;
		this.recCalls = new ArrayList<>();
	}

	@Override public void visitInvoke(SSAInvokeInstruction instruction) {
		if (m.isCallRecursive(instruction)) {
			Variable[] newVars = LogicUtil.createVars(instruction.getDef(), m.getReturnType().bitwidth(), "r");

			if (!m.hasValue(instruction.getDef())) {
				edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value defVal = edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value
						.createByType(instruction.getDef(), m.getReturnType());
				m.addValue(instruction.getDef(), defVal);
			}
			m.setDepsForvalue(instruction.getDef(), newVars);
			m.addVarsToValue(instruction.getDef(), newVars);
			recCalls.add(instruction);
		} else {
			super.visitInvoke(instruction);
		}
	}

	public List<SSAInvokeInstruction> getRecCalls() {
		return this.recCalls;
	}
}
