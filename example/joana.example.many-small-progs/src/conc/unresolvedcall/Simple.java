/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package conc.unresolvedcall;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */

interface I {
	void foo();
}
class A implements I {
	private I i;
	A(I i) {
		this.i = i;
	};
	
	public void foo() {
		i.foo();
	}
}

class B implements I {
	private I i;
	B(I i) {
		this.i = i;
	};
	public void foo() {
		i.foo();
	}
}

class C implements I {
	public void foo() {
	}
}
public class Simple {
	public static void main(String[] args) {
		I i1 = new A(new B(new C()));
		I i2 = new A(      new C() );
		i1.foo();
	}

}
