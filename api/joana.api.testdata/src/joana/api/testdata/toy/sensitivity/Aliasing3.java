/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.toy.sensitivity;

import edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks;

/**
 * This program was extracted from the SecuriBench micro suite. 
 * The author expects a leak here but actually there is
 * no leak. Joana is able to detect this.
 *
 * The name of the example might be confusing but it stems from the author's
 * assumptions of a leak due to aliasing. I decided to keep the name so that
 * the original example can be easily located.
 * @author Martin Mohr
 */
public class Aliasing3 {
	public static void main(String[] args) {
		String name = ToyTestsDefaultSourcesAndSinks.SECRET_STRING;
		String[] a = new String[10];
		String str = a[5];
		a[5] = name;
		name = str;
		ToyTestsDefaultSourcesAndSinks.leak(str);
	}
}
