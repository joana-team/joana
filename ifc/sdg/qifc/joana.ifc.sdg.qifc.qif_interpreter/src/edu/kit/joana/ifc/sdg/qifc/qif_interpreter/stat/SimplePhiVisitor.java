package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Util;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.util.List;
import java.util.stream.IntStream;

/**
 * completes computation of program dependencies at phi instructions
 */
public class SimplePhiVisitor extends SSAInstruction.Visitor {

	private Method m;
	private FormulaFactory f;
	private Formula[] deps;

	public SimplePhiVisitor(Method m, FormulaFactory f) {
		this.m = m;
		this.f = f;
	}

	public Formula[] computePhiDeps() {

		List<? extends SSAInstruction> phis = Util.asList(m.getIr().iteratePhis());
		phis.forEach(phi -> phi.visit(this));
		return deps;
	}

	@Override public void visitPhi(SSAPhiInstruction instruction) {
		Formula[] op1 = m.getDepsForValue(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValue(instruction.getUse(1));
		Formula op1Block = BBlock.getBBlockForInstruction(instruction, m.getCFG()).generateImplicitFlowFormula(f);
		Formula op2Block = BBlock.getBBlockForInstruction(instruction, m.getCFG()).generateImplicitFlowFormula(f);

		deps = new Formula[op1.length];
		IntStream.range(0, op1.length).forEach(i -> deps[i] = f.or(f.and(op1[i], op1Block), f.and(op2[i], op2Block)));
	}
}
