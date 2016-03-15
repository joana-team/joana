package edu.kit.joana.graph.dominators;

import com.ibm.wala.fixpoint.UnaryOperator;

/**
 * S[v] ⊆ S[w] ∪ {v}
 * @author Martin Mohr
 *
 * @param <T> type of elements in sets
 */
public class SubsetPropagator<T> extends UnaryOperator<NodeSetVariable<T>>{

	@Override
	public byte evaluate(NodeSetVariable<T> lhs, NodeSetVariable<T> rhs) {
		NodeSetValue<T> newValue = lhs.getValue().intersectWithAndAdd(rhs.getValue(), lhs.getNode());
		byte ret = newValue.equals(lhs.getValue())?NOT_CHANGED:CHANGED;
		lhs.printValue();
		return ret;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof SubsetPropagator;
	}

	@Override
	public String toString() {
		return " x1 ⊆ x2 ∪ {x3}";
	}

}
