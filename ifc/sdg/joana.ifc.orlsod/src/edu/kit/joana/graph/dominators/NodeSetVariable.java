package edu.kit.joana.graph.dominators;

import com.ibm.wala.fixpoint.AbstractVariable;

public class NodeSetVariable<T> extends AbstractVariable<NodeSetVariable<T>> {
	protected final T id;
	protected NodeSetValue<T> value;

	public NodeSetVariable(T id, NodeSetValue<T> initialValue) {
		this.id = id;
		this.value = initialValue;
	}

	public T getNode() {
		return id;
	}

	public void setValue(NodeSetValue<T> newValue) {
		this.value = newValue;
	}

	public NodeSetValue<T> getValue() {
		return value;
	}

	@Override
	public void copyState(NodeSetVariable<T> v) {
		this.value = v.value;
	}

	@Override
	public String toString() {
		return String.format("S[%s]", id);
	}

	public void printValue() {

	}
}
