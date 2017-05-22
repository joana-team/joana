/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

/**
 * A matcher for call sites. Leaves open whether the given method signature refers
 * to the caller or the callee.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public abstract class CallMatcher extends MethodMatcher {

	public CallMatcher(String methodSignature) {
		super(methodSignature);
	}

}
