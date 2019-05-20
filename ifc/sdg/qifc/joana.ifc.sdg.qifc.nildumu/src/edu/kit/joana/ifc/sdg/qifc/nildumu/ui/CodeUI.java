/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu.ui;

public class CodeUI {
	
	/**
	 * Leaks the argument to an attacker of the given level
	 * 
	 * @param o information to leak
	 * @param level attacker level
	 */
	@OutputMethod
	public static void output(int o, String level) {
		
	}
	
	/**
	 * Leaks the argument to an attacker of the lowest level
	 * 
	 * @param o information to leak
	 */
	@OutputMethod
	public static void leak(int o) {
		
	}
	
	/**
	 * Leaks the argument to an attacker of the lowest level
	 * 
	 * @param o information to leak
	 */
	@OutputMethod
	public static void leak(boolean o) {
		
	}
}
