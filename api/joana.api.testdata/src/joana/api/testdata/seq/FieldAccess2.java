/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;


import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;

/**
 * @author Martin Hecker
 */


class B {
	int f;
}

public class FieldAccess2 {
	public static void main(String[] args) {

		B b1 = new B();
		B b2 = new B();

		B a;

		b1.f = 42;
		b2.f = 17;

		B b3;
		if (SECRET_BOOL) {
			b3 = b1;
		} else {
			b3 = b2;
		}
		
		int v4 = toggle(b3.f);
		leak(v4);
	}
}
