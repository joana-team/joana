/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval.util;


/**
 * Exceptions that occur during eval phase.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class EvalException extends Exception {

	private static final long serialVersionUID = 1025170722668008042L;

	public EvalException(final Throwable t) {
		super(t);
	}
	
	public EvalException(final String msg) {
		super(msg);
	}
	
	public EvalException(final String msg, final Throwable t) {
		super(msg, t);
	}
	
}
