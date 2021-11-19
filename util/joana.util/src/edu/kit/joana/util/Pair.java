/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

public class Pair<S, T> {

  public static enum Component {
		/** for first component */
		FIRST,

		/** for second component */
		SECOND;

		/**
		 * Returns whether the given pair matches the given object in this component
		 * @param encInst pair to check
		 * @param o object to compare this component of the given pair with
		 * @return {@code true}, if o equals this component of the given pair
		 */
		<S,T> boolean matches(Pair<S,T> encInst, Object o) {
			switch (this) {
			case FIRST:
				return (encInst.fst == null && o == null) || (encInst.fst != null && encInst.fst.equals(o));
			case SECOND:
				return (encInst.snd == null && o == null) || (encInst.snd != null && encInst.snd.equals(o));
			default:
				throw new IllegalStateException();
			}
		}
	}

	/** first component of pair */
	private final S fst;

	/** second component of pair */
	private final T snd;

	/**
	 * Constructs a new pair from the given objects.
	 * @param fst first component of the new pair
	 * @param snd second component of the new pair
	 */
	protected Pair(S fst, T snd) {
		this.fst = fst;
		this.snd = snd;
	}

	/**
	 * Returns the first component of this pair.
	 * @return the first component of this pair
	 */
	public final S getFirst() {
		return fst;
	}

	/**
	 * Returns the second component of this pair.
	 * @return the second component of this pair
	 */
	public final T getSecond() {
		return snd;
	}

	/**
	 * Returns whether the given component of this pair matches the given object
	 * @param c component of this pair to check
	 * @param o object to compare with
	 * @return {@code true} if o equals the c-component of this pair, {@code false} otherwise
	 */
	public boolean componentEquals(Component c, Object o) {
		return c.matches(this, o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fst == null) ? 0 : fst.hashCode());
		result = prime * result + ((snd == null) ? 0 : snd.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("rawtypes")
		Pair other = (Pair) obj;
		if (fst == null) {
			if (other.fst != null)
				return false;
		} else if (!fst.equals(other.fst))
			return false;
		if (snd == null) {
			if (other.snd != null)
				return false;
		} else if (!snd.equals(other.snd))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "(" + fst + ", " + snd + ")";
	}

	/**
	 * Static factory method pairs
	 * @param fst first component of new pair
	 * @param snd second component of new pair
	 * @return new pair consisting of the given objects
	 */
	public static <S, T> Pair<S, T> pair(S fst, T snd) {
		return new Pair<S, T>(fst, snd);
	}

	/**
	 * Static factory method for non-null pairs
	 * @param fst first component of new pair
	 * @param snd second component of new pair
	 * @return new pair consisting of the given objects
	 * @throws NullPointerException if one of the given objects is {@code null}
	 */
	public static <S, T> Pair<S, T> nonNullPairs(S fst, T snd) {
		if (fst == null || snd == null) {
			throw new NullPointerException();
		}
		return new Pair<S, T>(fst, snd);
	}

	public static <U,V> Pair<U, V> make(U x, V y) {
		return new Pair<>(x,y);
	}

	public <U, V, E extends Throwable> Pair<U, V> map(ThrowingFunction<S, U, ? extends E> firstMap,
			ThrowingFunction<T, V, ? extends E> secondMap) throws E {
		return new Pair<>(firstMap.apply(fst), secondMap.apply(snd));
	}

	public <U, E extends Throwable> Pair<U, T> mapFirst(ThrowingFunction<S, U, E> firstMap) throws E {
		return new Pair<>(firstMap.apply(fst), snd);
	}

	public <V, E extends Throwable> Pair<S, V> mapSecond(ThrowingFunction<T, V, E> secondMap) throws E {
		return new Pair<>(fst, secondMap.apply(snd));
	}

	public S component1() {
		return fst;
	}

	public T component2() {
		return snd;
	}
}
