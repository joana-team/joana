/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

/**
 * Three valued logic + unknown.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class TVL {

	private TVL() {}

	public enum V { UNKNOWN, YES, MAYBE, NO }

	public static boolean isTrue(V a) {
		return a == V.YES;
	}

	public static boolean isFalse(V a) {
		return a == V.NO;
	}

	public static boolean mayBe(V a) {
		return a == V.MAYBE;
	}

	/**
	 * <pre>
	 * or | ? | + | - | M |
	 * --------------------
	 *  ? | ? | + | - | M |
	 *  + | + | + | M | M |
	 *  - | - | M | - | M |
	 *  M | M | M | M | M |
	 * --------------------
	 * </pre>
	 */
	public static V or(V a, V b) {
		switch (a) {
		case UNKNOWN:
			return b;
		case YES:
			return (b == V.YES ? V.YES : V.MAYBE);
		case NO:
			return (b == V.NO ? V.NO : V.MAYBE);
		case MAYBE:
			return V.MAYBE;
		}

		throw new IllegalStateException();
	}

	/**
	 * <pre>
	 * and| ? | + | - | M |
	 * --------------------
	 *  ? | ? | + | - | M |
	 *  + | + | + | - | M |
	 *  - | - | - | - | - |
	 *  M | M | M | - | M |
	 * --------------------
	 * </pre>
	 */
	public static V and(V a, V b) {
		switch (a) {
		case UNKNOWN:
			return b;
		case YES:
			if (b == V.NO) {
				return V.NO;
			} else if (b == V.YES) {
				return V.YES;
			} else {
				return V.MAYBE;
			}
		case NO:
			return V.NO;
		case MAYBE:
			return (b == V.NO ? V.NO : V.MAYBE);
		}

		throw new IllegalStateException();
	}

	/**
	 * <pre>
	 *   not
	 * -------
	 *  ? | ?
	 *  + | -
	 *  - | +
	 *  M | M
	 * -------
	 * </pre>
	 */
	public static V not(V a) {
		switch (a) {
		case MAYBE:
			return V.MAYBE;
		case NO:
			return V.YES;
		case YES:
			return V.NO;
		case UNKNOWN:
			return V.UNKNOWN;
		}

		throw new IllegalStateException();
	}
}
