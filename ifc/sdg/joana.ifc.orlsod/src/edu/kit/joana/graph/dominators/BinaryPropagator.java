package edu.kit.joana.graph.dominators;

import com.ibm.wala.fixpoint.AbstractOperator;

/**
 * S[v] ⊆ (S[w1] ∪ S[w2]) ∪ {v}
 * @author Martin Mohr
 *
 * @param <T> type of elements in node sets
 */
public class BinaryPropagator<T> extends AbstractOperator<NodeSetVariable<T>>{

	@Override
	public byte evaluate(NodeSetVariable<T> lhs, NodeSetVariable<T>[] rhs) {
		assert rhs.length == 2;
		NodeSetVariable<T> rhs1 = rhs[0];
		NodeSetVariable<T> rhs2 = rhs[1];
		NodeSetValue<T> oldValue = lhs.getValue();
		NodeSetValue<T> newValue = lhs.getValue().intersectWithUnionAndAdd(rhs1.getValue(), rhs2.getValue(), lhs.getNode());
		if (!newValue.equals(oldValue)) {
			lhs.setValue(newValue);
			return CHANGED;
		} else {
			return NOT_CHANGED;
		}
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}

}
