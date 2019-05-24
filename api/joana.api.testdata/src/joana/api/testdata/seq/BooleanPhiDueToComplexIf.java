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
 * 
 * This program does *not* leak, but we currently do not determine this!!!
 * @author Martin Hecker
 */




public class BooleanPhiDueToComplexIf {
	public static boolean invisibleUse;
	
	public static void main(String[] args) {

		int h = toggle(SECRET) + args.length;
		int l =                  args.length;
		boolean fooLow  = l > 42;
		boolean fooHigh = h > 17;
		
		boolean baz = false;
		boolean bar = false;
		if (fooLow) {
			bar = true;
			if (fooHigh) {
				baz = true;
			}
		}
		leak(bar);
		invisibleUse = baz;
	}
}
