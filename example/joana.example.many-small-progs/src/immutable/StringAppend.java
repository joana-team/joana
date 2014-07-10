/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package immutable;

public class StringAppend {
	
	static String high = "42";
	static int low;
	
	public static void main(String[] args) {
		low = (high + "17").hashCode();
	}
}
