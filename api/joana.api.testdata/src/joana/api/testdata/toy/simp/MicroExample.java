/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

public class MicroExample {

	public static int foo(int x, int y) {
		int result;
		if (x < 0) {
			result = x - y;
		} else {
			result = x + y;
		}
		return result;
	}

	public static void main(String[] args) {
		foo(2, 3);
	}
}
