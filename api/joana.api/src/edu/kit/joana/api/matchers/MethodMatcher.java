/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

/**
 * This is a matcher for nodes which have a method as a reference point, i.e. calls or entries.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public abstract class MethodMatcher implements Matcher {
	protected final String methodSignature;

	public MethodMatcher(String methodSignature) {
		this.methodSignature = methodSignature;
	}

}
