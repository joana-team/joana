package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Util;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * completes computation of program dependencies at phi instructions
 */
public class SimplePhiVisitor extends SSAInstruction.Visitor {

	private Method m;
	private FormulaFactory f;
	private Map<Integer, Formula[]> phiDeps;

	public SimplePhiVisitor(Method m, FormulaFactory f) {
		this.m = m;
		this.f = f;
		this.phiDeps = new HashMap<>();
	}

	public Map<Integer, Formula[]> computePhiDeps() {

		List<? extends SSAInstruction> phis = Util.asList(m.getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			phi.visit(this);
		}
		return phiDeps;
	}

	public Formula[] getPhiDeps(int valNum) {
		return phiDeps.get(valNum);
	}

	@Override public void visitPhi(SSAPhiInstruction instruction) {
		Formula[] op1 = m.getDepsForValue(instruction.getUse(0));
		Formula[] op2 = m.getDepsForValue(instruction.getUse(1));

		BBlock phiBlock = BBlock.getBBlockForInstruction(instruction, m.getCFG());
		Formula op1ImplicitFlow = phiBlock.preds().get(0).generateImplicitFlowFormula(f);
		Formula op2ImplicitFlow = phiBlock.preds().get(1).generateImplicitFlowFormula(f);

		Formula[] deps = new Formula[op1.length];
		IntStream.range(0, op1.length)
				.forEach(i -> deps[i] = f.or(f.and(op1[i], op1ImplicitFlow), f.and(op2[i], op2ImplicitFlow)));
		phiDeps.put(instruction.getDef(), deps);

		Formula[] oldDeps = m.getDepsForValue(instruction.getDef());
		Formula[] newDeps = new Formula[oldDeps.length];
		IntStream.range(0, oldDeps.length).forEach(i -> newDeps[i] = f.equivalence(oldDeps[i], deps[i]));
		m.setDepsForvalue(instruction.getDef(), newDeps);
	}
}
