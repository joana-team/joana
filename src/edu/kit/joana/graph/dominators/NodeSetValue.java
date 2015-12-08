package edu.kit.joana.graph.dominators;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class NodeSetValue<T> {
	public abstract NodeSetValue<T> add(T x);
	public abstract boolean isUnset();
	public abstract NodeSetValue<T> unionWith(NodeSetValue<T> x);
	public abstract NodeSetValue<T> intersectWith(NodeSetValue<T> x);
	public abstract NodeSetValue<T> intersectWithAndAdd(NodeSetValue<T> x, T y);
	public abstract NodeSetValue<T> intersectWithUnionAndAdd(NodeSetValue<T> x1, NodeSetValue<T> x2, T x3);
	public abstract boolean contains(T x);
	public abstract boolean equals(Object o);
	public abstract int hashCode();
	public abstract String toString();
	public abstract NodeSetValue<T> clone();
	public abstract Set<T> getElements();
	public abstract int size();
	public static class NotSet<T> extends NodeSetValue<T> {

		@Override
		public boolean isUnset() {
			return true;
		}

		@Override
		public NodeSetValue<T> unionWith(NodeSetValue<T> x) {
			return new NotSet<T>();
		}

		@Override
		public NodeSetValue<T> intersectWith(NodeSetValue<T> x) {
			return x.clone();
		}

		@Override
		public NodeSetValue<T> clone() {
			return new NotSet<T>();
		}

		@Override
		public Set<T> getElements() {
			throw new UnsupportedOperationException("This method is not supposed to be called on objects of this class.");
		}

		@Override
		public NodeSetValue<T> add(T x) {
			return new SetBased<T>(Collections.singleton(x));
		}

		@Override
		public boolean contains(T x) {
			return true;
		}

		@Override
		public NodeSetValue<T> intersectWithAndAdd(NodeSetValue<T> x, T y) {
			if (x.isUnset()) {
				return new NotSet<T>();
			} else {
				Set<T> newElements = new HashSet<T>(x.getElements());
				newElements.add(y);
				return new SetBased<T>(newElements);
			}
		}

		@Override
		public NodeSetValue<T> intersectWithUnionAndAdd(NodeSetValue<T> x1, NodeSetValue<T> x2, T x3) {
			if (x1.isUnset() || x2.isUnset()) {
				return new NotSet<T>();
			} else {
				Set<T> newElements = new HashSet<T>();
				newElements.addAll(x1.getElements());
				newElements.addAll(x2.getElements());
				newElements.add(x3);
				return new SetBased<T>(newElements);
			}
		}

		@Override
		public boolean equals(Object o) {
			return o != null && o instanceof NotSet;
		}

		@Override
		public int hashCode() {
			return 42;
		}

		@Override
		public String toString() {
			return "<unset>";
		}

		@Override
		public int size() {
			return -1;
		}
	}
	public static class SetBased<T> extends NodeSetValue<T> {
		private Set<T> elements;

		public SetBased(Collection<? extends T> elements) {
			this.elements = new HashSet<T>(elements);
		}

		@Override
		public boolean isUnset() {
			return false;
		}

		@Override
		public NodeSetValue<T> unionWith(NodeSetValue<T> x) {
			if (x.isUnset()) {
				return x.unionWith(this);
			} else {
				Set<T> resultElements = new HashSet<T>();
				resultElements.addAll(getElements());
				resultElements.addAll(x.getElements());
				return new SetBased<T>(resultElements);
			}
		}

		@Override
		public NodeSetValue<T> intersectWith(NodeSetValue<T> x) {
			if (x.isUnset()) {
				return x.intersectWith(this);
			} else {
				Set<T> resultElements = new HashSet<T>();
				resultElements.addAll(getElements());
				resultElements.retainAll(x.getElements());
				return new SetBased<T>(resultElements);
			}
		}

		@Override
		public NodeSetValue<T> clone() {
			Set<T> elementsCopy = new HashSet<T>();
			elementsCopy.addAll(elements);
			SetBased<T> copy = new SetBased<T>(elementsCopy);
			return copy;
		}

		@Override
		public Set<T> getElements() {
			Set<T> elementsCopy = new HashSet<T>();
			elementsCopy.addAll(elements);
			return elementsCopy;
		}

		@Override
		public NodeSetValue<T> add(T x) {
			Set<T> resultElements = new HashSet<T>();
			resultElements.addAll(getElements());
			resultElements.add(x);
			return new SetBased<T>(resultElements);
		}

		@Override
		public boolean contains(T x) {
			return elements.contains(x);
		}

		@Override
		public NodeSetValue<T> intersectWithAndAdd(NodeSetValue<T> x, T y) {
			if (x.isUnset()) {
				return add(y);
			} else {
				Set<T> resultElements = new HashSet<T>(this.elements);
				resultElements.retainAll(x.getElements());
				resultElements.add(y);
				return new SetBased<T>(resultElements);
			}
		}

		@Override
		public NodeSetValue<T> intersectWithUnionAndAdd(NodeSetValue<T> x1,NodeSetValue<T> x2, T x3) {
			if (x1.isUnset() || x2.isUnset()) {
				return add(x3);
			} else {
				Set<T> resultElements = new HashSet<T>(x1.getElements());
				resultElements.addAll(x2.getElements());
				resultElements.retainAll(this.elements);
				resultElements.add(x3);
				return new SetBased<T>(resultElements);
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof SetBased)) return false;
			return this.elements.equals(((SetBased<T>)o).elements);
		}

		@Override
		public int hashCode() {
			return 1 + 31 * elements.hashCode();
		}

		@Override
		public String toString() {
			return elements.toString();
		}

		@Override
		public int size() {
			return elements.size();
		}
	}
}


