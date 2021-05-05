package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.stream.IntStream;

public class DecisionTree<T> {

	public static final TernaryOperator<Formula[]> INT_COMBINATOR = LogicUtil::ternaryOp;

	public static final TernaryOperator<Formula[][]> ARRAY_COMBINATOR = (cond, x, y) -> {
		Formula[][] res = new Formula[x.length][x[0].length];
		IntStream.range(0, x.length).forEach(i -> res[i] = LogicUtil.ternaryOp(cond, x[i], y[i]));
		return res;
	};

	public static final TernaryOperator<Map<Integer, Formula[]>> INT_MAP_COMBINATOR = getMapOperator(INT_COMBINATOR);
	public static final TernaryOperator<Map<Integer, Formula[][]>> ARRAY_MAP_OPERATOR = getMapOperator(ARRAY_COMBINATOR);

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

	public DecisionTree<T> addInnerNode(int idx, Formula pathCondition, SortedSet<Pair<Integer, Boolean>> implicitFlows) {
		if (implicitFlows.size() == 0) {
			addInnerNode(idx, pathCondition);
			return root();
		}

		if (this.type == NodeType.LEAF) {
			DecisionTree<T> newInner = this.addInnerNode(implicitFlows.first().fst, BBlock.getBlockForIdx(m, implicitFlows.first().fst).getCondExpr());
			return newInner.addLeaf(idx, leafVal, implicitFlows);
		}

		Pair<Integer, Boolean> nextDecision = implicitFlows.first();
		implicitFlows.remove(nextDecision);
		assert(nextDecision.fst == this.nodeIdx);

		if (nextDecision.snd) {
			this.trueSubTree.addInnerNode(idx, pathCondition, implicitFlows);
		} else {
			this.falseSubTree.addInnerNode(idx, pathCondition, implicitFlows);
		}
		return this.root();
	}

	public DecisionTree<T> addLeaf(int idx, T leafVal, SortedSet<Pair<Integer, Boolean>> implicitFlows) {
		if (implicitFlows.size() == 0) {
			addLeaf(idx, leafVal);
			return root();
		}

		if (this.type == NodeType.LEAF) {
			DecisionTree<T> newInner = this.addInnerNode(implicitFlows.first().fst, BBlock.getBlockForIdx(m, implicitFlows.first().fst).getCondExpr());
			return newInner.addLeaf(idx, leafVal, implicitFlows);
		}

		Pair<Integer, Boolean> nextDecision = implicitFlows.first();
		implicitFlows.remove(nextDecision);
		assert(nextDecision.fst == this.nodeIdx);

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
	private DecisionTree<T> addInnerNode(int idx, Formula pathCondition) {
		assert(this.type == NodeType.LEAF);
		DecisionTree<T> newInner = new DecisionTree<T>(m, idx, pathCondition);
		newInner.parent = (this.isRoot()) ? newInner : this.parent;
		if (!this.isRoot()) {
			newInner.parent.replaceChild(this, newInner);
		}
		this.parent = newInner;
		newInner.trueSubTree = copyLeaf(this);
		newInner.falseSubTree = copyLeaf(this);
		return newInner;
	}

	private DecisionTree<T> copyLeaf(DecisionTree<T> original) {
		DecisionTree<T> copy = new DecisionTree<T>(original.m, original.nodeIdx, original.leafVal, original.isRoot());
		copy.parent = original.parent;
		return copy;
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

	public DecisionTree<T> root() {
		if (this.isRoot()) {
			return this;
		} else {
			return this.parent.root();
		}
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

	public static <T> TernaryOperator<Map<Integer, T>> getMapOperator(TernaryOperator<T> operator) {
		TernaryOperator<Map<Integer, T>> mapOperator = (cond, x, y) -> {
			Map<Integer, T> res = new HashMap<>();
			x.keySet().forEach(k -> res.put(k, operator.apply(cond, x.get(k), y.get(k))));
			return res;
		};
		return mapOperator;
	}

	private enum NodeType {
		INNER,
		LEAF;
	}
}
