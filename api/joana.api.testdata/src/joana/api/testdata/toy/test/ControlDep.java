/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.test;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.*;

/**
 * This example demonstrates some imprecision of our analysis in connection
 * with constant propagation and invariant detection.
 * @author Martin Mohr
 *
 */
public class ControlDep {

    int sec1;
    int pub1;

    int sec2;
    int pub2;



    void foo(int x) {
	/**
	 * A data dependency from y to z is introduced, since x might be zero -
	 * regardless of the fact that this method is called with a non-zero
	 * value only.
	 */
	if (x == 0) {
	    pub1 = SECRET;
	}
    }

    void foo2(int x) {
	/**
	 * The condition is semantically always false, so need not be
	 * a data dependency from a to b
	 * Nevertheless it is introduced, since the analysis is not able to
	 * detect this.
	 */
	if (45 * x - 32 *x != 13 * x) {
	    pub2 = SECRET;
	}
    }

    public static void main(String[] args) {
	ControlDep cd = new ControlDep();
	cd.foo(11); // illegal flow from SECRET to PUBLIC
	leak(toggle(cd.pub1));
	cd.foo2(42); // illegal flow from SECRET to PUBLIC
	leak(toggle(cd.pub2));
    }
}
