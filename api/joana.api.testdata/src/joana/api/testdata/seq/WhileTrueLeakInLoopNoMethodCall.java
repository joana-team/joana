/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.PUBLIC;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;
/**
 * @author Martin Hecker
 */
public class WhileTrueLeakInLoopNoMethodCall {
	public static void main(String[] args) {
		foo();
	}
	
	static void foo() {
		int i = 0;
		int secret = toggle(SECRET);

		while (true) {
			i++;
			if(secret > 0) {
				PUBLIC = 0;
			} else {
				secret = 42;
			}
			
		}
	}
}
