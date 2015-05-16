/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package sensitivity;


/**
 * This example is a variant of {@link DynDispLeak} in which there is no leak.
 * A precise information flow analysis should be able to verify this.
 * @author Martin Mohr
 */
public class DynDispValid {
	static class A {
		void foo() {
		}
	}
	static class B extends A {
		void foo() {
			Security.leak(42);
		}
	}
	public static void main(String[] args) {
		A a1 = new B();
		A a2 = new B();
		A a;
		if (Security.SECRET == 17) {
			a = a1;
		} else {
			a = a2;
		}
		// The secret decides on which object foo is called.
		// However, both a1 and a2 are instances of B, so
		// B::foo is called no matter what a is.
		// Hence, the assignment to PUBLIC is executed, regardless
		// of the secret value.
		a.foo();
	}
}
