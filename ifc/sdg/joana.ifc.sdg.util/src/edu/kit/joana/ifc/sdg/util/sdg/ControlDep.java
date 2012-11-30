/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.sdg;

import java.util.HashMap;

public class ControlDep {

	public static int foo(int x) {
		int y = 4 * x * x - 2 * x + 5;
		if (y < 0) {
			return y + 5;
		} else {
			return y - 7;
		}
	}

	public static void main(String[] args) {
		Object o;
		int x = Integer.parseInt(args[0]);
		if (x > 42) {
			o = new HashMap<String, String>();
		} else {
			o = new String();
		}

		System.out.println(o);

		if (foo(x) == -7) {
			System.out.println(x);
		}
	}
}
