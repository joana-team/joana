/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.ddisp;

import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * @author Martin Mohr
 */
public class DynamicDispatch2 {
	public static void main(String[] args) {
		Object o = realMain(1, 2, 3);
		System.out.println(o.toString().equals("bogus"));
	}

	private static Object realMain(int x, int y, int z) {
		switch (x * x - 2 * y + x * y * z) {
		case 0:
			return new HashMap<String, Object>();
		case 1:
			return BigInteger.valueOf(42);
		case 2:
			return new BufferedImage(640, 480, BufferedImage.TYPE_INT_ARGB);
		default:
			return new Object();
		}
	}
}
