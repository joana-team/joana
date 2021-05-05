package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Array;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.DecisionTree;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.Map;

/**
 * when analysing loops we use special placeholder array values
 *
 * this class re-defines all array-specific visit methods to make sure we use the right reference objects
 */
public class LoopSATVisitor extends SATVisitor {

	private LoopBody l;
	private DecisionTree<Map<Integer, Formula[]>> currentLeaf;

	public void setLoop(LoopBody l) {
		this.l = l;
	}

	public LoopSATVisitor(StaticAnalysis staticAnalysis, LoopBody l) {
		super(staticAnalysis);
		this.l = l;
		Map<Integer, Formula[]> initals = new HashMap<>();
		l.getOwner().getProgramValues().forEach((i, v) -> initals.put(i, v.getDeps()));
		this.currentLeaf = new DecisionTree<>(l.getOwner(), l.getHead().idx(), initals, true);
	}

	@Override public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		Array<? extends Value> placeHolder = l.getPlaceholderArray(instruction.getArrayRef());
		this.visitArrayLoad(instruction, placeHolder);
	}


	@Override public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		Array<? extends Value> placeHolder = l.getPlaceholderArray(instruction.getArrayRef());
		this.visitArrayStore(instruction, placeHolder, this.getCurrentBlock().generateImplicitFlowFormula());
	}

	@Override public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
		super.visitConditionalBranch(instruction);

		if (this.getCurrentBlock().succs().stream().anyMatch(succ -> !l.hasBlock(succ.idx()))) {
			addPossibleBreakValue();
		}
	}

	private void addPossibleBreakValue() {
		BBlock outsideLoop = this.getCurrentBlock().succs().stream().filter(succ -> !l.hasBlock(succ.idx())).findFirst().get();
		Formula takeBreak = (this.getCurrentBlock().getTrueTarget() == outsideLoop.idx()) ? this.getCurrentBlock().getCondExpr() :
				LogicUtil.ff.not(this.getCurrentBlock().getCondExpr());

		Map<Integer, Formula[]> currentVals = new HashMap<>();
		for (int i: l.getOwner().getProgramValues().keySet()) {
			currentVals.put(i, l.getOwner().getDepsForValue(i).clone());
		}

		DecisionTree<Map<Integer, Formula[]>> newLeaf = new DecisionTree<>(l.getOwner(), outsideLoop.idx(), currentVals,
				false);
		this.currentLeaf = this.currentLeaf.replaceLeaf(this.getCurrentBlock().idx(), LogicUtil.ff.and(this.getCurrentBlock().generateImplicitFlowFormula(), takeBreak), newLeaf, this.currentLeaf);
		this.currentLeaf = currentLeaf.getFalseSubTree();
	}

	public DecisionTree<Map<Integer, Formula[]>> getCurrentLeaf() {
		return this.currentLeaf;
	}
}
