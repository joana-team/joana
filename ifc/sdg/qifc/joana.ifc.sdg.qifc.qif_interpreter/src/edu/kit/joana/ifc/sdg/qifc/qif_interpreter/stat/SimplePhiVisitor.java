package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;

import java.util.List;

/**
 * completes computation of program dependencies at phi instructions
 */
public class SimplePhiVisitor extends SSAInstruction.Visitor {

	// temporary
	private static final int loopUnrollingMax = 2;

	private Method m;

	public SimplePhiVisitor(Method m) {
		this.m = m;
	}

	public void computePhiDeps() {

		List<? extends SSAInstruction> phis = Util.asList(m.getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			phi.visit(this);
		}
	}

	@Override public void visitPhi(SSAPhiInstruction instruction) {
		if (m.isComputedInLoop(instruction.getDef())) {
			visitPhiFromLoop(instruction);
		} else {
			visitPhiFromConditional(instruction);
		}
	}

	private void visitPhiFromConditional(SSAPhiInstruction instruction) {
		BBlock phiBlock = BBlock.getBBlockForInstruction(instruction, m.getCFG());
		for (int i = 0; i < 2; i++) {
			m.addPhiValuePossibility(instruction.getDef(), Pair.make(m.getDepsForValue(instruction.getUse(i)),
					phiBlock.preds().get(i).generateImplicitFlowFormula()));
		}

	}

	private void visitPhiFromLoop(SSAPhiInstruction instruction) {
		LoopBody l = m.getLoops().stream().filter(loop -> loop.getHead().instructions().contains(instruction)).findFirst().get();

		for (int i = 0; i <= loopUnrollingMax; i++) {
			m.addPhiValuePossibility(instruction.getDef(), Pair.make(l.getRun(i).get(instruction.getDef()), l.getImplicitFlowForIter(i)));
		}
	}
}
