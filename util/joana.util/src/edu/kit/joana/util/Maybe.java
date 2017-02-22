/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

import java.util.Objects;

/**
 * This class provides a typesafe Maybe type, which is known from functional
 * languages such as Haskell. A Maybe value is either 'Nothing' or of the form
 * 'Just t'. The 't' can be extracted from a 'Just t' value.<br>
 * Whether a Maybe value is 'Nothing' or of the form 'Just t' is returned by the
 * method {@link isNothing} and {@link isJust}, respectively.<br>
 * <em>Note:</em>
 * <ul>
 * <li>Although it is not syntactically forbidden to extend this class, don't do
 * it!</li>
 * <li>'Nothing' is considered to be not equal to 'Just null'.</li>
 * </ul>
 * 
 * @param <T>
 *            Type of objects to be wrapped
 * @author Martin Mohr
 */
public abstract class Maybe<T> {
	
	private Maybe() {
		
	}
	/**
	 * Returns whether this Maybe value is of the form 'Just t'.
	 * 
	 * @return {@code true} if this Maybe value is of the form 'Just t',
	 *         {@code false} otherwise.
	 */
	public boolean isJust() {
		return !isNothing();
	}

	/**
	 * Returns whether this Maybe value is of the form 'Nothing'.
	 * 
	 * @return {@code true} if this Maybe value is of the form 'Nothing',
	 *         {@code false} otherwise.
	 */
	public abstract boolean isNothing();

	/**
	 * If this Maybe value is of the form 'Just t', returns t. Otherwise, an
	 * {@link UnsupportedOperationException} is thrown.
	 * 
	 * @return t, if this Maybe value is of the form (Just t)
	 * @throws UnsupportedOperationException
	 *             if this Maybe value is of the form 'Nothing'
	 */
	public abstract T extract() throws UnsupportedOperationException;

	/**
	 * Two Maybe instances m and n are considered equal iff one of the following
	 * conditions are met:
	 * <ol>
	 * <li>Both m and n are 'Nothing'.</li>
	 * <li>m is 'Just t1', n is 'Just t2' and t1 is equal to t2.</li>
	 * </ol>
	 */
	@SuppressWarnings("rawtypes")
	public boolean equals(Object o) {
		if (!(o instanceof Maybe)) {
			return false;
		}

		Maybe m = (Maybe) o;
		if (isNothing()) {
			return m.isNothing();
		}

		if (m.isNothing()) {
			return false;
		}

		// now we know that both this and m are of the form 'Just t'
		return Objects.equals(extract(), m.extract());
	}

	@Override
	public int hashCode() {
		if (isNothing()) {
			return 0;
		} else {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((extract() == null) ? 0 : extract().hashCode());
			return result;
		}
	}

	/**
	 * Returns a 'Nothing'.
	 * 
	 * @return 'Nothing'
	 */
	public static <T> Maybe<T> nothing() {
		return new Nothing<T>();
	}

	/**
	 * Given an object t, returns a 'Just t'.
	 * 
	 * @param t
	 *            object to be packed into a 'Just'.
	 * @return 'Just t'
	 */
	public static <T> Maybe<T> just(T t) {
		return new Just<T>(t);
	}

	private static class Nothing<T> extends Maybe<T> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.util.Maybe#isNothing()
		 */
		@Override
		public boolean isNothing() {
			return true;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.util.Maybe#extract()
		 */
		@Override
		public T extract() throws UnsupportedOperationException {
			throw new UnsupportedOperationException("Cannot extract the value from nothing!");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Nothing";
		}

	}

	private static class Just<T> extends Maybe<T> {

		private final T t;

		Just(T t) {
			this.t = t;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.util.Maybe#isJust()
		 */
		@Override
		public boolean isNothing() {
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see edu.kit.joana.util.Maybe#extract()
		 */
		@Override
		public T extract() throws UnsupportedOperationException {
			return t;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "Just " + ((t == null) ? "null" : t.toString());
		}
	}
}
