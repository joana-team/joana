/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

import edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks;

/**
 * TODO: @author Add your name here.
 */
public class ControlDependenceDueToDubiousPhiPlacement {

	public static void main(String[] argv) throws InterruptedException {
		int y = ToyTestsDefaultSourcesAndSinks.SECRET;
		int x = 0;
		for (int i = 31; i > 0; i--) {
			int bit = y & (1 << 30);
			if (bit != 0) {
				int n = 1;
				for (int k=1; k < 111; k++) {
					for (int j=1; j < 999; j++) {
						n = n * k;
					}
				}
			}
			y = y << 1;
			ToyTestsDefaultSourcesAndSinks.leak(x);
		}
		ToyTestsDefaultSourcesAndSinks.leak(ToyTestsDefaultSourcesAndSinks.toggle(y));
	}

}