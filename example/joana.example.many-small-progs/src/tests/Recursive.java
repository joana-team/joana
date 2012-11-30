/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class Recursive {
	public static void main(String[] args) {
		System.out.println(foo(2, 5, 4));
	}

	public static int foo (int x, int y, int z) {
		if (z > 0) {
			z--;
			return foo(y, x, z);
		} else {
			return x * x;
		}
	}
}
