/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package sensitivity;


/**
 * This example demonstrates how information flow can be caused by dynamic dispatch.
 * @author Martin Mohr
 */
public class DynDispLeak {
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
		A a = new A();
		B b = new B();
		A a0;
		if (Security.SECRET == 17) {
			a0 = a;
		} else {
			a0 = b;
		}
		// The secret value decides whether A::foo or B::foo is called
		// B::foo contains an assignment to PUBLIC. So, the secret value
		// controls whether this assignment to PUBLIC is executed or not.
		a0.foo();
	}
}
