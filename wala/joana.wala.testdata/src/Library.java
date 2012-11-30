/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class Library {
	static class A {
		int i;
	}

	public static int call(A a, A b, A c, int x) {
		c.i = a.i;
		a.i = x;

		return b.i;
	}
}
