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

import static edu.kit.joana.api.annotations.Annotations.*;

class StaticConst {
	public static int modulus = toggle(SECRET);
}

public class Sick2 {

	private int count = SECRET;

	public int foo(int a, int b) {
		this.count += 1;
		return (a + b + toggle(count)) % StaticConst.modulus;
	}


	public static void main(String[] args) {
		Sick2 s = new Sick2();
		leak(s.foo(17, 23));
	}
}
