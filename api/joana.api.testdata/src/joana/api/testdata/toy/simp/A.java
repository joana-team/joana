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

class B {
	C c;

	B() {
		if (c == null) {
			c = new C();
		}
	}
}

class C {
	C() {
	}

	int x;
}

public class A {
	B b;
	static double foo = 17.0;
	static double bar;
	static boolean[] troodles;

	A() {
		b = new B();
		b.c.x = 42;
	}

	public static void main(String[] args) {
		A a = new A();
		a.b.hashCode();
		foo = foo + 1;
		bar = foo;
		troodles = new boolean[42];
		troodles[17] = troodles[5] || troodles[23];
	}
}
