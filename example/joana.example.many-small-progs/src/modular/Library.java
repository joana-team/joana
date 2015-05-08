/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package modular;

/**
 * Examples for modular SDG computation. 
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class Library {

	public static class A {
		int i;
		int f;
	}

	int i;

	public static int call(A a, A b, A c, int x) {
		c.i = a.i;
		a.i = x;
		return b.i;
	}

	public static int callIndirect(A a, A b, A c, int x) {
		c.i = a.i;
		if (x > 4) {
			a.i = 2;
		}
		return b.i;
	}

	public static int callIndirect2(A a, A b, A c, int x) {
		if (x > 4) {
			c.i = 2;
		}

		a.i = c.i;

		return b.i;
	}

	public static int call2(A a, A b) {
		A c = new A();
		c.i = a.i;
		return return2(c, b);
	}

	public static int return2(A a, A b) {
		return b.i;
	}

	public static A call3(A a, A b) {
		if (a == null) {
			return b;
		}

		A ret = (true ? new A() : a);
		ret.i = b.i;

		return ret;
	}

	// compute example - breaks previous access paths computation
	
	public static class A1 {
		B1 f;
	}
	
	public static class B1 {
		int i;
	}
	
	public static int compute(A1 a, A1 b, A1 c, A1 d, A1 e, A1 g) {
		B1 x = c.f;
		a.f = x;
		B1 y = b.f;
		
		B1 v = d.f;
		e.f = v;
		B1 z = g.f;
		
		z.i = 3;
		int w = y.i;
		return w;
	}

	// merge op example
	
	public static class C {
		D d1;
		D d2;
	}
	
	public static class D {
		int i;
	}

	public static class E {
		C c1;
		C c2;
	}
	
	public static int mergeOp(E e0, E e1) {
		C x = e0.c2;
		C y = e1.c1;
		x.d1 = y.d2;
		return e0.c2.d1.i;
	}
	
	public static int phiTest(E e0, E e1, boolean b) {
		C x;
		if (b) {
			x = e0.c2;
		} else {
			x = e1.c2;
		}
		C y = e1.c1;
		x.d1 = y.d2;
		return e0.c2.d1.i;
	}
}
