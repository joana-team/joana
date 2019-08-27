/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.util.Objects;

public class Triple<S, T, R> {

	private final S left;

	private final T middle;

	private final R right;

	public Triple(S left, T middle, R right) {
		this.left = left;
		this.middle = middle;
		this.right = right;
	}

	public S getLeft() {
		return left;
	}

	public T getMiddle() {
		return middle;
	}

	public R getRight() {
		return right;
	}

	@Override
	public int hashCode() {
		return Objects.hash(left, middle, right);
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;

		if (!left.equals(triple.left))
			return false;
		if (!middle.equals(triple.middle))
			return false;
		return right.equals(triple.right);

	}

	@Override
	public String toString() {
		return "(" + left + ", " + middle + ", " + right + ")";
	}


	public static <S, T, R> Triple<S, T, R> triple(S left, T middle, R right) {
		return new Triple<>(left, middle, right);
	}

	public static <S, T, R> Triple<S, T, R> nonNullTriple(S left, T middle, R right) {
		Objects.requireNonNull(left);
		Objects.requireNonNull(middle);
		Objects.requireNonNull(right);
		return triple(left, middle, right);
	}
}
