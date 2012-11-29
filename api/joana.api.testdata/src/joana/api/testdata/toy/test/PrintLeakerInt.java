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
package joana.api.testdata.toy.test;

import java.io.PrintStream;

public class PrintLeakerInt {
	PrintStream myOut;

	public PrintLeakerInt(PrintStream out) {
		this.myOut = out;
	}

	public void leak(IntSecret sec) {
		myOut.println(sec.secretValue);
	}
}
