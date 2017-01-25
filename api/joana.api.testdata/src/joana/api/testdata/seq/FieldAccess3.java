/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;


import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

/**
 * @author Martin Hecker
 */




public class FieldAccess3 {
	private static class C {
		int f;
	}
	public static void main(String[] args) {

		C c = new C();
		
		c.f = SECRET;         // The secret input we are interested in
		c.f = toggle(SECRET); // if this secret influences is absent, 
		                      // c.f is overwritten with 0 instead,
		                      // which ought to be inferred by local-killing-defs analysis
		int result = c.f;
		
		leak(result);
	}
}
