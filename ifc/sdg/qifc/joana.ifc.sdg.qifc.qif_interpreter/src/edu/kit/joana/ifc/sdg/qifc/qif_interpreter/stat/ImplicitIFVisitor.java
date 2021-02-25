package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.CFG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.IBBlockVisitor;

import java.util.Set;
import java.util.stream.Collectors;

public class ImplicitIFVisitor implements IBBlockVisitor {

	private CFG g;

	public void compute(CFG g) {
		this.g = g;
		Set<Integer> unvisited = g.getBlocks().stream().map(BBlock::idx).collect(Collectors.toSet());
		BBlock curr = g.entry();

		unvisited = computeRec(g.entry(), unvisited);
		assert (unvisited.isEmpty());
	}

	private Set<Integer> computeRec(BBlock toVisit, Set<Integer> unvisited) {
		if (unvisited.isEmpty()) {
			return unvisited;
		}

		unvisited.remove(toVisit.idx());
		toVisit.acceptVisitor(this);
		for (BBlock b: toVisit.succs()) {
			if (unvisited.contains(b.idx())) {
				unvisited = this.computeRec(b, unvisited);
			}
		}
		return unvisited;
	}

	@Override public void visitStartNode(BBlock node) {
		// nothing to do
	}

	@Override public void visitExitNode(BBlock node) {
		visitStandardNode(node);
	}

	@Override public void visitStandardNode(BBlock node) {
		// copy the implicit flows from our immediate dominator
		BBlock immDom = g.getImmDom(node);
		assert (immDom != null);
		node.copyImplicitFlowsFrom(immDom.idx());
	}

	@Override public void visitDecisionNode(BBlock node) {
		// no special handling necessary for this node
		visitStandardNode(node);

		// add the new implicit information that we gain by splitting the CF to our successors
		SSAInstruction condInstr = node.getWalaBasicBLock().getLastInstruction();
		assert(condInstr instanceof SSAConditionalBranchInstruction);

		int trueTarget = g.getMethod().getBlockStartingAt(((SSAConditionalBranchInstruction) condInstr).getTarget()).idx();
		assert(node.succs().size() == 2);
		for (BBlock succ: node.succs()) {
			BBlock nonDummySucc = succ.succs().get(0);
			succ.addImplicitFlow(node.idx(), nonDummySucc.idx() == trueTarget);
		}
	}

	@Override public void visitDummyNode(BBlock node) {
		// no special handling necessary
		visitStandardNode(node);
	}
}
