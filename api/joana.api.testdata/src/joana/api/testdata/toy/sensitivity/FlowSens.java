/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.sensitivity;

import static edu.kit.joana.api.annotations.Annotations.*;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class FlowSens {
	
	public static class A {
		public int i;
		
		public A(int i) {
			this.i = i;
		}
	}

	public static void main(String[] args) {
		A a = new A(5);
		leak(a.i);				// ok
		a.i = toggle(SECRET);
		leak(a.i);				// illegal
	}

}
