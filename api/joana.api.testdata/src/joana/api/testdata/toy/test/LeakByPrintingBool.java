/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.test;
//TODO: Add proper annotations for use in ToyTests.java? Or Extend Annotations.java with PrintStreams?
public class LeakByPrintingBool {

	public static void main(String[] args) {

		PrintLeakerBool leaker = new PrintLeakerBool(System.out);
		BooleanSecret sec = new BooleanSecret(true);
		leaker.leak(sec);

	}
}
