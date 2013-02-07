/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

/**
 * @author Martin Mohr
 */
public class LocalKillDefRegression {

	public static void main(String[] args) {
		if (Float.floatToRawIntBits(42.0f) < 17) {
			return;
		}
	}
}
