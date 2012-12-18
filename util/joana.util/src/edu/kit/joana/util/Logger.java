/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public interface Logger {

	boolean isEnabled();
	
	void out(Object obj);
	void outln(Object obj);
	void outln(Object obj, Throwable t);
	void out(String str);
	void outln(String str);
	void outln(String str, Throwable t);
	
}
