package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.logicng.formulas.Formula;

public abstract class IFTreeNode {

	public abstract NodeType getType();

	public abstract Formula getImplicitFlowFormula();

	public enum NodeType {
		AND, OR, LEAF
	}

	public static class AndNode extends IFTreeNode {

		IFTreeNode lhs;
		IFTreeNode rhs;

		public AndNode(IFTreeNode lhs, IFTreeNode rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override public NodeType getType() {
			return NodeType.AND;
		}

		@Override public Formula getImplicitFlowFormula() {
			return LogicUtil.ff.and(lhs.getImplicitFlowFormula(), rhs.getImplicitFlowFormula());
		}
	}

	public static class OrNode extends IFTreeNode {

		IFTreeNode lhs;
		IFTreeNode rhs;

		public OrNode(IFTreeNode lhs, IFTreeNode rhs) {
			this.lhs = lhs;
			this.rhs = rhs;
		}

		@Override public NodeType getType() {
			return NodeType.OR;
		}

		@Override public Formula getImplicitFlowFormula() {
			return LogicUtil.ff.or(lhs.getImplicitFlowFormula(), rhs.getImplicitFlowFormula());
		}
	}

	public static class LeafNode extends IFTreeNode {

		int blockIdx;
		boolean evalTo;
		Method m;

		public LeafNode(Method m, int blockIdx, boolean evalTo) {
			this.m = m;
			this.blockIdx = blockIdx;
			this.evalTo = evalTo;
		}

		@Override public NodeType getType() {
			return NodeType.LEAF;
		}

		@Override public Formula getImplicitFlowFormula() {
			Formula condExpr = BasicBlock.getBlockForIdx(m, blockIdx).getCondExpr();
			return (evalTo) ? condExpr : LogicUtil.ff.not(condExpr);
		}
	}

	public static class NoIFLeaf extends IFTreeNode {

		public static final NoIFLeaf SINGLETON = new NoIFLeaf();

		@Override public NodeType getType() {
			return NodeType.LEAF;
		}

		@Override public Formula getImplicitFlowFormula() {
			return LogicUtil.ff.constant(true);
		}
	}
}