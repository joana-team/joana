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
public class AParameter implements Matcher {

	private final MethodMatcher methodMatcher;
	private final int index;
	private final SDGNode.Kind relevantKind;

	private AParameter(MethodMatcher methodMatcher, int index, SDGNode.Kind relevantKind) {
		this.methodMatcher = methodMatcher;
		this.index = index;
		this.relevantKind = relevantKind;
	}

	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		boolean rightMethod = false;
		for (SDGEdge eIn : sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			if (methodMatcher.matches(eIn.getSource(), sdg)) {
				rightMethod = true;
				break;
			}
		}
		if (!rightMethod) return false;
		if (n.getKind() != relevantKind) return false;
		if (BytecodeLocation.getRootParamIndex(n.getBytecodeName()) != index) return false;
		return true;
	}

	public Matcher includeWrittenFields() {
		return this.andAlso(new WrittenFieldsOfAParameter(this));
	}

	public static AParameter of(CallMatcher callMatcher, int index) {
		return new AParameter(callMatcher, index, SDGNode.Kind.ACTUAL_IN);
	}

	public static AParameter of(Entry entryMatcher, int index) {
		return new AParameter(entryMatcher, index, SDGNode.Kind.FORMAL_IN);
	}
}
