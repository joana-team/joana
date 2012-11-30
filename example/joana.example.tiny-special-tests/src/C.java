/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

public class C {

	private static class D extends C {}

	int f = 4;
	static int sF = 5;
	int[] a = new int[34];
	C c;
	C[] ca = new C[3];

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		C c = new C();
		c.c = new C();
		c.c.c = c.c;
		C a = nullC();

		a = more(c, a);

		System.out.println(a.f);
	}

	public static C more(C c, C a) {
		a.f = 23;
		a = bar(c, a);

		for (int i = 0; i < 10; i++) {
			a.f = 13 + i;
			a.f = a.f + 1;
			c.a[i] = 3 + c.a[i];
			c.c.c.f = c.ca[i].f;
		}

		a = foo(c);

		return a;
	}

	public static C foo(C c) {
		c.f = 32;
		return c.c;
	}

	public static C bar(C c, C c2) {
		c.f = 32;
		c2.f = 12;
		return c2.c;
	}

	public static C newC() {
		return new C();
	}

	public static C nullC() {
		C c = new D();
		c.c = c;

		return c;
	}

}
