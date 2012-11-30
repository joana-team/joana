/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

public class FieldPropagation {

	public static class A {
		public B f;
	}

	public static class B {
		public int i;
	}

	public static int out;

	public static void entry() {
		A a = new A();
		a.f = createB();
		foo(a);
		out = a.f.i;
	}

	public static void foo(A p) {
		nop(p.f);
		bar();
	}

	public static void nop(Object o) {
	}

	public static void bar() {
		B b = createB();
		b.i = 10;
	}

	public static B createB() {
		return new B();
	}

}
