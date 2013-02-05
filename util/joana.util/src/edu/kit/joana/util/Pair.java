/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

public class Pair<S, T> {

	private S fst;
	private T snd;

	public Pair(S fst, T snd) {
		super();
		this.fst = fst;
		this.snd = snd;
	}

	public S getFirst() {
		return fst;
	}

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

	public String toString() {
		return "(" + fst + ", " + snd;
	}

	public static <S, T> Pair<S, T> pair(S fst, T snd) {
		return new Pair<S, T>(fst, snd);
	}

}
