/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Matches the entry of a given method.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class Entry extends MethodMatcher {

	public Entry(String methodSignature) {
		super(methodSignature);
	}

	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		if (n.getKind() != SDGNode.Kind.ENTRY) return false;
		return n.getBytecodeMethod().equals(methodSignature);
	}

}
