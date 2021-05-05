package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.CFG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.IBBlockVisitor;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.IFTreeNode;

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
			Set<Integer> finalUnvisited = unvisited;
			if (unvisited.contains(b.idx()) && b.preds().stream().filter(pred -> !pred.getCFG().isDominatedBy(pred, b))
					.noneMatch(pred -> finalUnvisited.contains(pred.idx()))) {
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
		IFTreeNode if_ = node.preds().get(0).getIfTree();
		if (node.preds().get(0).isCondHeader()) {
			if_ = addCFSplit(if_, node.preds().get(0), node);
		}

		for (int i = 1; i < node.preds().size(); i++) {
			IFTreeNode predIF = node.preds().get(i).getIfTree();
			if (node.preds().get(i).isCondHeader()) {
				predIF = addCFSplit(predIF, node.preds().get(i), node);
			}
			if_ = new IFTreeNode.OrNode(if_, predIF);
		}
		node.setIfTree(if_);
	}

	private IFTreeNode addCFSplit(IFTreeNode old, BBlock pred, BBlock curr) {
		SSAInstruction condInstr = pred.getWalaBasicBlock().getLastInstruction();
		assert (condInstr instanceof SSAConditionalBranchInstruction);
		return new IFTreeNode.AndNode(old,
				new IFTreeNode.LeafNode(pred.getCFG().getMethod(), pred.idx(), pred.getTrueTarget() == curr.idx()));
	}

	@Override public void visitDecisionNode(BBlock node) {
		// no special handling necessary for this node
		visitStandardNode(node);
	}

	@Override public void visitDummyNode(BBlock node) {
		// no special handling necessary
		visitStandardNode(node);
	}
}
