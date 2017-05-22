/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 *
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class TheReturnValue implements Matcher {

	private final MethodMatcher methodMatcher;
	private final SDGNode.Kind relevantKind;

	protected TheReturnValue(MethodMatcher methodMatcher, SDGNode.Kind relevantKind) {
		this.methodMatcher = methodMatcher;
		this.relevantKind = relevantKind;
	}

	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		if (n.getKind() != relevantKind) return false;
		if (!BytecodeLocation.RETURN_PARAM.equals(n.getBytecodeName())) return false;
		for (SDGEdge eIn : sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			if (methodMatcher.matches(eIn.getSource(), sdg)) {
				return true;
			}
		}
		return false;
	}

	public static TheReturnValue of(CallMatcher callMatcher) {
		return new TheReturnValue(callMatcher, SDGNode.Kind.ACTUAL_OUT);
	}

	public static TheReturnValue of(Entry entryMatcher) {
		return new TheReturnValue(entryMatcher, SDGNode.Kind.FORMAL_OUT);
	}
}
