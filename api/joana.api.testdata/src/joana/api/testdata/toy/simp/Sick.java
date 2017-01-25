/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

public class Sick {

	private static double someDamnNumber = 42.23;
	private short salt = 5;

	public void bar(A a) {
		// use manual rounding here since JOANA thinks Math.round will throw an exception
		a.b.c.x = (5 - toggle((int) (SECRET + 0.5))) << salt;
	}

	public int bar2(A a, B b) {
		if (b.c.x == 0)
			a = null;
		else
			a = new A();

		return a.b.c.x + toggle(SECRET);
	}

	public static void main(String[] args) {
		Sick s = new Sick();
		A a = new A();
		B b = new B();
		s.bar(a);
		int i = s.bar2(a, b);
		leak(i);
	}
}
