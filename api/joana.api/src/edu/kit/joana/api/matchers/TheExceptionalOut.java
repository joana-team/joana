package edu.kit.joana.api.matchers;

import edu.kit.joana.api.matchers.Entry;
import edu.kit.joana.api.matchers.EveryCall;
import edu.kit.joana.api.matchers.Matcher;
import edu.kit.joana.api.matchers.MethodMatcher;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 * Matches the exceptional out node of a method (call).
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class TheExceptionalOut implements Matcher {

	private final MethodMatcher methodMatcher;
	private final SDGNode.Kind relevantKind;

	protected TheExceptionalOut(MethodMatcher methodMatcher, SDGNode.Kind relevantKind) {
		this.methodMatcher = methodMatcher;
		this.relevantKind = relevantKind;
	}

	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		if (n.getKind() != relevantKind) return false;
		if (!BytecodeLocation.EXCEPTION_PARAM.equals(n.getBytecodeName())) return false;
		for (SDGEdge eIn : sdg.getIncomingEdgesOfKind(n, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			if (methodMatcher.matches(eIn.getSource(), sdg)) {
				return true;
			}
		}
		return false;
	}

	public static TheExceptionalOut of(EveryCall callMatcher) {
		return new TheExceptionalOut(callMatcher, SDGNode.Kind.ACTUAL_OUT);
	}

	public static TheExceptionalOut of(Entry entryMatcher) {
		return new TheExceptionalOut(entryMatcher, SDGNode.Kind.FORMAL_OUT);
	}
}