/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

import sensitivity.Security;

public class Recursive {
	public static void main(String[] args) {
		int val = foo(2, 5, Security.SECRET);
		Security.PUBLIC = val;
		System.out.println(val);
	}

	public static int foo (int x, int y, int z) {
		if (z > 0) {
			z--;
			return foo(y, x + x, z);
		} else {
			return x * x;
		}
	}
}
