/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;


import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET_BOOL;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

/**
 * @author Martin Hecker
 */


class A {
	int f;
}

public class FieldAccess {
	public static void main(String[] args) {

		A a1 = new A();
		A a2 = new A();

		A a;

		a1.f = 42;
		a2.f = 17;

		A v3;
		if (SECRET_BOOL) {
			v3 = a1;
		} else {
			v3 = a2;
		}
		v3.f = 0;
		
		int v4 = toggle(v3.f);
		leak(v4);
	}
}
