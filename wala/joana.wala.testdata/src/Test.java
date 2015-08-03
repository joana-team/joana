
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class Test {


	static class A {
		A2 f;
		int x;
	}

	static class A2 {
		int f2;
	}

	static class A3 {
		A3 f1;
		A3 f2;
		int f3;
	}

	//@ifc: !{a,b} => a.x -!> b.x
	public void exceptionalFlow1(A a, A b) {
		if (a.x == 1) {
			((A) null).x = 3;//throw new IllegalArgumentException();
		} 

		b.x = 2;
	}
	
	//
	//@ifc: ? => b -!> \result
	public static A2 foo1(A a, A b) {
		return a.f;
	}

	//@ifc: ? => b -!> \result
	public static int impossibleAlias(A2 a, A2 b, A3 c, A3 d) {
		return a.f2 + c.f3;
	}

	//@ifc: ? => b -!> \result
	public static A2 indirectFoo1(A a, A b) {
		return foo1(a, b);
	}

	//@ifc: ? => b -!> \result
	//@ifc: ? => a -!> \result
	public static A2 indirectRevFoo1(A a, A b) {
		return foo1(b, a);
	}

	//@ifc: ? => b -!> \result
	//@ifc: ? => a -!> \result
	public static A2 indirectMultipleFoo1(A a, A b) {
		foo1(a, b);
		foo1(b, a);
		return foo1(a, b);
	}

	//@ifc: ? => b -!> \result
	//@ifc: ? => a -!> \result
	public static A2 indirectSameFoo1(A a, A b) {
		return foo1(a, a);
	}

	//@ifc: ? => b -!> \result
	//@ifc: ? => a -!> \result
	public static int foo2(A a, A b) {
		A2 c = b.f;
		c.f2 = 3;

		if (a.f.f2 > 2) {
			return 44;
		} else {
			return 42;
		}
	}

	//@ifc: ? => b -!> \result
	//@ifc: ? => a -!> \result
	public static int foo3(A a, A b) {
		int tmp = 42;

		if (a.f.f2 > 2) {
			tmp = 44;
		}

		A2 c = b.f;
		c.f2 = 3;

		return tmp;
	}

	
	//@ifc: !{a.*, b.*} & !{a.*, c.*} & !{a.*, d.*} & !{b.*, c.*} & !{b.*, d.*} & !{c.*, d.*} => a -!> \result
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	//@ifc: ? => c -!> \result
	//@ifc: ? => d -!> \result
	public static int foo4(A a, A b, A c, A d) {
//		foo2(a, b);
//		foo3(c, d);

		return d.f.f2;
	}

	//@ifc: !{a.*, b.*} & !{a.*, c.*} & !{a.*, d.*} & !{b.*, c.*} & !{b.*, d.*} & !{c.*, d.*} => a -!> \result
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	//@ifc: ? => c -!> \result
	//@ifc: ? => d -!> \result
	public static int foo5(A a, A b, A c, A d) {
		return foo4(a, b, c, d);
	}

	//@ifc: ? => a -!> b
	//@ifc: !{a.*, b.*} => a -!> b
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	public static int foo6(A a, A b) {
		if (a.f.f2 > 3) {
			return 42;
		}

		return b.f.f2;
	}

	//@ifc: ? => a -!> b
	//@ifc: !{a.*, b.*} => a -!> b
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	public static int foo7(A3 a, A3 b) {
		a.f1 = a;
		a.f2 = a;
		a.f1.f2.f2.f1.f2.f1.f3 = 4;

		return b.f3;
	}

	//@ifc: ? => a -!> b
	//@ifc: !{a.*, b.*} => a -!> b
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	public static int foo8(A3 a, A3 b) {
		a.f1.f2.f2.f1.f2.f1.f3 = 4;

		return b.f3;
	}

	//@ifc: ? => a -!> b
	//@ifc: !{a.*, b.*} => a -!> b
	//@ifc: ? => b -!> a
	//@ifc: !{a.*, b.*} => b -!> a
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	public static int foo9(A2 a, A2 b) {
		foo10(a, b);

		return b.f2;
	}

	//@ifc: ? => a -!> b
	//@ifc: !{a.*, b.*} => a -!> b
	//@ifc: ? => b -!> a
	//@ifc: !{a.*, b.*} => b -!> a
	public static void foo10(A2 a, A2 b) {
		b.f2 = a.f2;
	}

	//ifc: ? => a -!> b
	//ifc: !{a.*, b.*} => a -!> b
	//ifc: ? => b -!> a
	//@ifc: !{a.f.*, b.*} => b -!> a
	//ifc: ? => a -!> \result
	//ifc: ? => b -!> \result
	public static int foo9b(A a, A2 b) {
		foo10(a.f, b);

		return b.f2;
	}

	public static A3 foo11() {
		A3 a3 = new A3();
		a3.f1 = new A3();
		a3.f2 = new A3();

		A3 ret = getField1(a3, a3);

		return ret;
	}

	//
	//@ifc: !{a.*, b.*} => a -!> b
	public static void foo12(A3 a, A3 b) {
		if (a.f1.f2.f3 < 3) {
			return;
		}

		b.f2.f3--;

		foo13(b, a);
	}

	//@ifc: !{a.*, b.*} => a -!> b
	public static void foo13(A3 a, A3 b) {
		b.f2 = a.f1;

		foo12(a, b);
	}

	//@ifc: => a -!> b
	//@ifc: ? => a -!> \result
	//@ifc: ? => b -!> \result
	private static A3 getField1(A3 a, A3 b) {
		return a.f1;
	}

	private static void setField(A3 p, int i) {
		p.f2.f3 = i;
	}

	private static int singleParamAlias(A3 a) {
		return a.f1.f3;
	}

	//
	//@ifc: !{a, b} => a -!> b
	//@ifc: ? => a -!> b
	//@ifc: => a -!> b
	public int aliasExcTest(A3 a, A3 b) {
		int i = 0;
		i = a.f2.f3;
		b.f2.f3 = 17;
		return i;
	}

	// 1. ok, 2. leak, 3. ok
	//@ifc: !{a, b} => a -!> b
	//@ifc: !{a, a} => a -!> b
	//@ifc: ? => a -!> b
	public static int aliasTest(A3 a, A3 b) {
		int i = 0;
		try {
			i = a.f2.f3;
		} catch (Exception e) {}
		int num = 0;
		try {
		if (a.f3 > 10) {
			num = 17;
		}} catch (Exception e) {}
		num = 23;
		b.f2.f3 = num;
		setField(b, b.f3);
		return i;
	}

	//
	//@ifc: !{a, b} => a -!> b
	//@ifc: !{a, a} => a -!> b
	//@ifc: ? => a -!> b
	public static int indirectAliasTest(A3 a, A3 b) {
		return aliasTest(a, b);
	}

	//
	//@ifc: => a -!> \result
	//@ifc: => a.f2 -!> \result
	//@ifc: !{a,a} => a.f2.f3 -!> \result
	//@ifc: !{a,a} => a.f1.f3 -!> \result
	public static int invokeSingleParamAlias(A3 a) {
		try {
			a.f2.f3 = 42;
		} catch (Exception e) {}
		return singleParamAlias(a);
	}

	//
	//@ifc: ? => d.f2 -!> \result
	//@ifc: !{b, a} & !{d, a} & !{a, c} => d.f2 -!> \result
	//@ifc: !{b, a} & !{d, a} => d.f2 -!> \result
	//@ifc: !{b, a} & !{d, a} & !{a, c} & !{d, c} => d.f2 -!> \result
	//@ifc: !{b, a} & !{d, a} & !{a, c} & !{a, a} => a.f2.f3 -!> \result
	public static int invokeMultipleParamAlias(A3 a, A3 b, A3 c, A3 d, int i) {
		try {
			a.f2.f3 = 42;
		} catch (Exception e) {}
		b.f2.f3 = d.f2.f3;

		return singleParamAlias(a);
	}

	//@ifc: ? => a -!> \result
	//@ifc: => s1 -!> \result
	public static int invokeStringAndPrintln(String s1, String s2, A a) {
		System.out.println(s1);

		if (s2.contains("foo")) {
			a.f.f2 = 12;
		}

		return a.f.f2 + 10;
	}
	
	//BEGIN compute example - breaks previous access paths computation
	public static class A1 {
		B1 f;
	}
	
	public static class B1 {
		int i;
	}
	
	//@ifc: => d -!> \result
	//@ifc: !{a.*, b.*, c.*, d.*, e.*, g.*} => d-!>\result
	//@ifc: !{a.*, b.*, c.*, d.*, e.*} => d-!>\result
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

	//@ifc: => d -!> \result
	//@ifc: !{a.*, b.*, c.*, d.*} => d-!>\result
	public static int callToCompute(final A1 a, final A1 b, final A1 c, final A1 d) {
		return compute(a, b, c, d, a, b);
	}

	//END compute example - breaks previous access paths computation

}
