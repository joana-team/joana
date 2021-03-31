package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.logicng.formulas.Formula;

import java.util.List;

/**
 * completes computation of program dependencies at phi instructions
 */
public class SimplePhiVisitor extends SSAInstruction.Visitor {

	// temporary
	private static final int loopUnrollingMax = 5;

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
		}
	}

	private void visitPhiFromLoop(SSAPhiInstruction instruction) {
		LoopBody l = m.getLoops().stream().filter(loop -> loop.getHead().instructions().contains(instruction)).findFirst().get();

		for (int i = 0; i <= loopUnrollingMax; i++) {
			Formula[] val = l.getRun(i).get(instruction.getDef());

			Formula iFlow = LogicUtil.ff.constant(true);
			for (int j = 0; j < i; j++) {
				iFlow = LogicUtil.ff.and(iFlow, l.substituteWithIterationOutputs(j, l.getStayInLoop()));
			}
			iFlow = LogicUtil.ff.and(iFlow, l.substituteWithIterationOutputs(i, LogicUtil.ff.not(l.getStayInLoop())));
			m.addPhiValuePossibility(instruction.getDef(), Pair.make(val, iFlow));
		}
	}
}
