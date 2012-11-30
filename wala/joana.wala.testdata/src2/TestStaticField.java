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
public class TestStaticField {


	static class A {
		A2 f;
	}

	static class A2 {
		int f2;
	}

	static class A3 {
		A3 f1;
		A3 f2;
		int f3;
	}

	public static A staticField = new A();
	static {
		staticField.f = new A2();
	}

	//
	//ifc: => TestStaticField.staticField -!> \result
	//ifc: !{TestStaticField.staticField, a} => TestStaticField.staticField -!> \result
	//ifc: => a -!> TestStaticField.staticField
	public static A2 referenceA(A a, A b) {
		return a.f;
	}

	//
	//@ifc: ? => TestStaticField.staticField.f -!> \result
	//@ifc: !{TestStaticField.staticField, a} => TestStaticField.staticField -!> \result
	//@ifc: => a -!> TestStaticField.staticField
	public static A2 modifyA(A a, A b) {
		b.f = a.f;
		if (staticField != null && staticField.f != null && a != null && a.f != null) {
		a.f.f2 = staticField.f.f2;
		}
//		b.f = staticField.f;
		return b.f;
	}

}
