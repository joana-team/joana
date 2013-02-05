/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

public class Pair<S, T> {

	/** first component of pair */
	private final S fst;
	
	/** second component of pair */
	private final T snd;

	/**
	 * Constructs a new pair from the given objects.
	 * @param fst first component of the new pair
	 * @param snd second component of the new pair
	 */
	private Pair(S fst, T snd) {
		this.fst = fst;
		this.snd = snd;
	}

	/**
	 * Returns the first component of this pair.
	 * @return the first component of this pair
	 */
	public S getFirst() {
		return fst;
	}

	/**
	 * Returns the second component of this pair.
	 * @return the second component of this pair
	 */
	public T getSecond() {
		return snd;
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

}
