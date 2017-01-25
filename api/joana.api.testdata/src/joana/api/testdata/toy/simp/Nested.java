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

public class Nested {
	public class Layer1 {
		public class Layer2 {
			public class Layer3 {
				public int x = 0;

				public Layer3(int x) {
					this.x = toggle(x);
					// if uncommented, this would cause the execution to end, independent of secret
					// therefore, the secret could not be leaked
					//throw new Error("Foo.");
				}
			}
		}
	}

	public static void main(String[] main) {
		Nested n = new Nested();
		Nested.Layer1.Layer2.Layer3 l = new Nested().new Layer1().new Layer2().new Layer3(SECRET);
		leak(l.x);
	}
}
