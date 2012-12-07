/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.simp;

import static edu.kit.joana.api.annotations.Annotations.*;

public class Nested {
	public class Layer1 {
		public class Layer2 {
			public class Layer3 {
				public int x = 0;

				public Layer3(int x) {
					this.x = toggle(x);
					throw new Error("Foo.");
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
