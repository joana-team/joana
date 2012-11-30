/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package tests;

public class TransitiveDeclass {
	public static void main(String[] args) {
		int high = Integer.parseInt(args[0]);
		int med = high + high;
		int low = med + med;
		System.out.println(low);
	}
//	public static void main(String[] args) {
//		String high = args[0];
//		String med = args[1] + high;
//		String low = args[2] + med;
//		System.out.println(low);
//	}
}
