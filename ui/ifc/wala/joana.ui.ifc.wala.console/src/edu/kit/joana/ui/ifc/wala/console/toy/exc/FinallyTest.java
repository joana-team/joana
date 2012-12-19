/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.toy.exc;

public class FinallyTest {

	public static void main(String[] args) {
		int i = 0;
		while (i < 2) {
			try {
				if (i == 1) {
					throw new Exception("");
				}
			} catch (Exception e) {
				System.out.println("Ist Ende jetzt.");
			} finally {
				System.out.println("i = " + i);
				i++;
			}
		}
	}
}
