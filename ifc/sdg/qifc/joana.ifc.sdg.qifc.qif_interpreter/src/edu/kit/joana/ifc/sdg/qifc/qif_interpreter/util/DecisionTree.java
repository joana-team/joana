package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import org.logicng.formulas.Formula;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;


public class DecisionTree<T> {

	public static final TernaryOperator<Formula[]> INT_COMBINATOR = LogicUtil::ternaryOp;

	public static final TernaryOperator<Formula[][]> ARRAY_COMBINATOR = (cond, x, y) -> {
		Formula[][] res = new Formula[x.length][x[0].length];
		IntStream.range(0, x.length).forEach(i -> res[i] = LogicUtil.ternaryOp(cond, x[i], y[i]));
		return res;
	};

	private final Method m;
	private int nodeIdx;
	private final NodeType type;
	private Formula pathCondition;
	private DecisionTree<T> parent;
	private DecisionTree<T> trueSubTree;
	private DecisionTree<T> falseSubTree;
	private T leafVal;

	private DecisionTree(Method m, int idx, NodeType type) {
		this.m = m;
		this.nodeIdx = idx;
		this.type = type;
	}

	private DecisionTree(Method m, int idx, Formula pathCondition) {
		this(m, idx, NodeType.INNER);
		this.pathCondition = pathCondition;
	}

	public DecisionTree(Method m, int idx, T leafVal, boolean isRoot) {
		this(m, idx, NodeType.LEAF);
		this.leafVal = leafVal;

		if (isRoot) {
			this.parent = this;
		}
	}

	public DecisionTree(Method m, boolean isRoot) {
		this(m, -1, NodeType.LEAF);

		if (isRoot) {
			this.parent = this;
		}
	}

	public T getDecision(TernaryOperator<T> operator) {
		if (this.type == NodeType.LEAF) {
			return leafVal;
		} else {
			return operator.apply(this.pathCondition, trueSubTree.getDecision(operator), falseSubTree.getDecision(operator));
		}
	}

	public DecisionTree<T> addInnerNode(int idx, Formula pathCondition, List<Pair<Integer, Boolean>> implicitFlows) {
		if (implicitFlows.size() == 0) {
			addInnerNode(idx, pathCondition);
		}

		Pair<Integer, Boolean> nextDecision = implicitFlows.remove(0);
		assert(nextDecision.fst == this.nodeIdx);

		if (this.type == NodeType.LEAF) {
			this.addInnerNode(nextDecision.fst, BBlock.getBlockForIdx(m, nextDecision.fst).getCondExpr());
		}

		if (nextDecision.snd) {
			this.trueSubTree.addInnerNode(idx, pathCondition, implicitFlows);
		} else {
			this.falseSubTree.addInnerNode(idx, pathCondition, implicitFlows);
		}
		return this.root();
	}

	public DecisionTree<T> addLeaf(int idx, T leafVal, List<Pair<Integer, Boolean>> implicitFlows) {
		if (implicitFlows.size() == 0) {
			addLeaf(idx, leafVal);
		}

		Pair<Integer, Boolean> nextDecision = implicitFlows.remove(0);
		assert(nextDecision.fst == this.nodeIdx);

		if (this.type == NodeType.LEAF) {
			this.addInnerNode(nextDecision.fst, BBlock.getBlockForIdx(m, nextDecision.fst).getCondExpr());
		}

		if (nextDecision.snd) {
			this.trueSubTree.addLeaf(idx, leafVal, implicitFlows);
		} else {
			this.falseSubTree.addLeaf(idx, leafVal, implicitFlows);
		}
		return this.root();
	}

	/**
	 * replaces a Leaf with an inner node. The leaf is added as subtrees to the new inner node
	 * @param idx blockIdx corresponding to the new node
	 * @param pathCondition conditional jmp condition to execute either trueSubtree oder FalseSubTree
	 */
	private void addInnerNode(int idx, Formula pathCondition) {
		assert(this.type == NodeType.LEAF);
		DecisionTree<T> newInner = new DecisionTree<T>(m, idx, pathCondition);
		newInner.trueSubTree = this;
		newInner.falseSubTree = this;
		this.parent = newInner.parent;
		newInner.parent.replaceChild(this, newInner);
	}

	/**
	 * replaces an old leaf value by the new one and updates the idx
	 * @param idx blockIdx corresponding to the new node
	 * @param leafVal new leafValue to replace the old one
	 */
	private void addLeaf(int idx, T leafVal) {
		assert(this.type == NodeType.LEAF);
		this.nodeIdx = idx;
		this.leafVal = leafVal;
	}

	public boolean isRoot() {
		return this.parent.equals(this);
	}

	public void replaceChild(DecisionTree<T> oldChild, DecisionTree<T> newChild) {
		if (this.trueSubTree.equals(oldChild)) {
			this.trueSubTree = newChild;
		} else {
			this.falseSubTree = newChild;
		}
	}

	public void setTrueSubTree(DecisionTree<T> trueSubTree) {
		this.trueSubTree = trueSubTree;
	}

	public void setFalseSubTree(DecisionTree<T> falseSubTree) {
		this.falseSubTree = falseSubTree;
	}

	public void setLeafVal(T leafVal) {
		this.leafVal = leafVal;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		DecisionTree<?> that = (DecisionTree<?>) o;
		return nodeIdx == that.nodeIdx;
	}

	@Override public int hashCode() {
		return Objects.hash(nodeIdx, type, pathCondition, parent, trueSubTree, falseSubTree, leafVal);
	}

	private enum NodeType {
		INNER,
		LEAF;
	}

	public DecisionTree<T> root() {
		if (this.isRoot()) {
			return this;
		} else {
			return this.parent.root();
		}
	}
}
