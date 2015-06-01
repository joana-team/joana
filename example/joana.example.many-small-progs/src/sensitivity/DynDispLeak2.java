/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package sensitivity;


/**
 * This is an example where information flows because the same method is called on different objects
 * depending on the secret, propagating different values to the public channel.
 * The purpose of this example is to demonstrate that this kind of information flow is captured even
 * if dynamic dispatch is handled with the PRECISE setting.
 * @author Martin Mohr
 */
public class DynDispLeak2 {
	static class A {
		int x;
		void foo() {
			Security.leak(x);
		}
	}
	static class B extends A {
		void foo() {
			
		}
	}
	public static void main(String[] args) {
		A a1 = new A(); a1.x = 0;
		A a2 = new A(); a2.x = 1;
		A a0;
		if (Security.SECRET == 17) {
			a0 = a1;
		} else {
			a0 = a2;
		}
		/**
		 * The secret decides whether a1 or a2 is called and hence which value is copied to public.
		 * Despite the fact that foo may be called on different instances, a precise points-to analysis
		 * finds out that there is only one possible call target and precise handling of dynamic dispatches
		 * also accounts for this fact by not letting all code inside foo be control-dependent on the
		 * receiver object.
		 * However, the secret decides which field is read by foo if 'this.x' is read there, so the information
		 * flow between secret and public is still captured.
		 */
		a0.foo();
	}
}
