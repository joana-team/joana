package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * completes computation of program dependencies at phi instructions
 */
public class SimplePhiVisitor extends SSAInstruction.Visitor {

	private Method m;
	private Map<Integer, Formula[]> phiDeps;

	public SimplePhiVisitor(Method m) {
		this.m = m;
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
		Formula op1ImplicitFlow = phiBlock.preds().get(0).generateImplicitFlowFormula();
		Formula op2ImplicitFlow = phiBlock.preds().get(1).generateImplicitFlowFormula();

		Formula[] deps = new Formula[op1.length];
		IntStream.range(0, op1.length).forEach(i -> deps[i] = LogicUtil.ff
				.or(LogicUtil.ff.and(op1[i], op1ImplicitFlow), LogicUtil.ff.and(op2[i], op2ImplicitFlow)));
		phiDeps.put(instruction.getDef(), deps);
	}
}
