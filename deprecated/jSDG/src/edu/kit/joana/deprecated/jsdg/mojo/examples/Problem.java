/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.mojo.examples;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class Problem {

	class A {
		B b;

		A() {
			b = new B();
		}

		B getB() {
			return b;
		}

		B newB() {
			return new B();
		}

	}

	class B {
		int i;

		B() {}

	}

	class C {

	}

	/**
	 * If b and b2 are not aliased then the modification is not visible outside this method.
	 * @return
	 */
	public B problemWithNoAlias() {
		A a = new A();
		B b = a.getB();
		b.i = 42;
		B b2 = a.getB();

		return b2;
	}

	/**
	 * a.b influences return value
	 * @param a
	 * @return
	 */
	public B okWithNoAlias(A a) {
		B b = a.getB();
		b.i = 42;
		B b2 = a.getB();

		return b2;
	}

	public B ok2WithNoAlias(A a) {
		B b = a.newB();
		b.i = 42;
		B b2 = a.newB();

		return b2;
	}

}
