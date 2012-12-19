/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.simp;

public class Sick {

	private static double someDamnNumber = 42.23;
	private short salt = 5;

	public void bar(A a) {
		a.b.c.x = (5 - (int) Math.round(someDamnNumber)) << salt;
	}

	public int bar2(A a, B b) {
		if (b.c.x == 0)
			a = null;
		else
			a = new A();

		return a.b.c.x + (int) someDamnNumber;
	}

	public static void main(String[] args) {
		Sick s = new Sick();
		A a = new A();
		B b = new B();
		s.bar(a);
		int i = s.bar2(a, b);
		System.out.println(i);
	}
}
