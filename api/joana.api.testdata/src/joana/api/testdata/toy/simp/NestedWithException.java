/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class NestedWithException {
	public class Layer1 {
		public class Layer2 {
			public class Layer3 {
				public int x = 0;

				public Layer3(int x) {
					this.x = toggle(x);
					// just that a leak is found if toggle passes on its parameter
					leak(this.x);
					throw new Error("Foo.");
				}
			}
		}
	}

	public static void main(String[] main) {
		NestedWithException.Layer1.Layer2.Layer3 l
		    = new NestedWithException().new Layer1().new Layer2().new Layer3(SECRET);
		// the exception thrown in the constructor of Layer3 prevents the leakage,
		// so this program should be considered secure.
		// However, JOANA currently does not recognize this
		leak(SECRET);
	}
}

