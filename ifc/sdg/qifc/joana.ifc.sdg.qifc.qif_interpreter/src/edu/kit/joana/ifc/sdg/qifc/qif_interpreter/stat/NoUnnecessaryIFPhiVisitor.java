package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil.ff;

public class NoUnnecessaryIFPhiVisitor extends SSAInstruction.Visitor {

	private Method m;
	private Map<Integer, Formula[]> phiDeps;

	public NoUnnecessaryIFPhiVisitor(Method m) {
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
		Value op1Val = m.getValue(instruction.getUse(0));
		Value op2Val = m.getValue(instruction.getUse(1));

		assert(op1Val.assigned() || op2Val.assigned());
		assert(!(op1Val.assigned() && op2Val.assigned()));

		Formula[] op1 = op1Val.getDeps();
		Formula[] op2 = op2Val.getDeps();

		boolean[] identical = iFIdentical(op1, op2);
		boolean[] exclusive = iFExculsive(op1, op2);

		BBlock phiBlock = BBlock.getBBlockForInstruction(instruction, m.getCFG());
		Formula op1ImplicitFlow = phiBlock.preds().get(0).generateImplicitFlowFormula();
		Formula op2ImplicitFlow = phiBlock.preds().get(1).generateImplicitFlowFormula();

		Formula[] deps = new Formula[op1.length];

		for (int i = 0; i < deps.length; i++) {
			if (identical[i]) {
				deps[i] = op1[i];
			} else {
				deps[i] = ff.or(ff.and(op1[i], op1ImplicitFlow), ff.and(op2[i], op2ImplicitFlow));
			}
		}
		phiDeps.put(instruction.getDef(), deps);

		Formula[] oldDeps = m.getDepsForValue(instruction.getDef());
		Formula[] newDeps = new Formula[oldDeps.length];
		IntStream.range(0, oldDeps.length).forEach(i -> newDeps[i] = ff.equivalence(oldDeps[i], deps[i]));
		m.setDepsForvalue(instruction.getDef(), newDeps);
	}

	private boolean[] iFExculsive(Formula[] op1, Formula[] op2) {
		assert(op1.length == op2.length);
		boolean[] res = new boolean[op1.length];

		for (int i = 0; i < op1.length; i++) {
			res[i] = iFExclusive(op1[i], op2[i]);
		}

		return res;
	}

	private boolean iFExclusive(Formula op1, Formula op2) {
		Formula f = ff.not(ff.equivalence(op1, op2));
		Tristate res = LogicUtil.isSat(f);
		return res.equals(Tristate.FALSE);
	}

	// check if both branches compute the same value. If yes, we dont need to record the implicit information flow from the conditional jump.
	private boolean[] iFIdentical(Formula[] op1, Formula[] op2) {
		assert(op1.length == op2.length);
		boolean[] res = new boolean[op1.length];

		for (int i = 0; i < op1.length; i++) {
			res[i] = iFIdentical(op1[i], op2[i]);
		}

		return res;
	}

	public boolean iFIdentical(Formula op1, Formula op2) {
		Formula f = ff.equivalence(op1, ff.not(op2));
		Tristate res = LogicUtil.isSat(f);
		return res.equals(Tristate.FALSE);
	}

}
