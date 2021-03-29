package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Substitution;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.logicng.formulas.Formula;

import java.util.List;

/**
 * completes computation of program dependencies at phi instructions
 */
public class SimplePhiVisitor extends SSAInstruction.Visitor {

	// temporary
	private static final int loopUnrollingMax = 6;

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
			LoopBody.Run r = l.getRun(i);
			Formula[] val = r.getAfter().get(instruction.getDef());
			BBlock outsideLoopSuccessor = l.getHead().succs().stream().filter(b -> !l.getBlocks().contains(b))
					.findFirst().get();
			BBlock insideLoopSuccessor = l.getHead().succs().stream().filter(b -> l.getBlocks().contains(b)).findFirst()
					.get();
			Formula implicitFlowAfter = outsideLoopSuccessor.generateImplicitFlowFormula();
			Formula implicitFlowIn = insideLoopSuccessor.generateImplicitFlowFormula();

			Formula iFlow = LogicUtil.ff.constant(true);

			for (int j = 0; j < i; j++) {
				Substitution s = new Substitution();
				for (int k : l.getIn().keySet()) {
					s.addMapping(l.getIn().get(k), l.getRun(j).getAfter().get(k));
				}
				iFlow = LogicUtil.ff.and(iFlow, implicitFlowIn.substitute(s.toLogicNGSubstitution()));
			}
			Substitution s = new Substitution();
			for (int k : l.getIn().keySet()) {
				s.addMapping(l.getIn().get(k), r.getAfter().get(k));
			}
			iFlow = LogicUtil.ff.and(iFlow, implicitFlowAfter.substitute(s.toLogicNGSubstitution()));

			// add iteration deps
			iFlow = l.getIn().keySet().stream().map(k -> r.getRunDeps().get(k)).reduce(iFlow, LogicUtil.ff::and);
			m.addPhiValuePossibility(instruction.getDef(), Pair.make(val, iFlow));
		}
	}
}
