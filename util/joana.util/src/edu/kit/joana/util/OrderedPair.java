/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

/**
 * TODO: @author Add your name here.
 */
public class OrderedPair<S extends Comparable<? super S>, T extends Comparable<? super T>> extends Pair<S,T> implements Comparable<OrderedPair<S,T>> {
	
	public OrderedPair(S s, T t) {
		super(s, t);
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(OrderedPair<S, T> o) {
		int cmpFirst = this.getFirst().compareTo(o.getFirst());
		if (cmpFirst != 0) {
			return cmpFirst;
		} else {
			return this.getSecond().compareTo(o.getSecond());
		}
	}
	
	/**
	 * Static factory method for ordered pairs
	 * @param fst first component of new pair
	 * @param snd second component of new pair
	 * @return new pair consisting of the given objects
	 */
	public static <S extends Comparable<? super S>, T extends Comparable<? super T>> OrderedPair<S, T> make(S fst, T snd) {
		return new OrderedPair<S, T>(fst, snd);
	}
	
	/**
	 * Static factory method for non-null ordered pairs
	 * @param fst first component of new pair
	 * @param snd second component of new pair
	 * @return new pair consisting of the given objects
	 * @throws NullPointerException if one of the given objects is {@code null}
	 */
	public static <S extends Comparable<? super S>, T extends Comparable<? super T>> Pair<S, T> makeNonNull(S fst, T snd) {
		if (fst == null || snd == null) {
			throw new NullPointerException();
		}
		return new Pair<S, T>(fst, snd);
	}
}
