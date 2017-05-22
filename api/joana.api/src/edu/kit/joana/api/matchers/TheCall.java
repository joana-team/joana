package edu.kit.joana.api.matchers;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * A matcher for one specific call site. Does not restrict the callee.
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class TheCall extends CallMatcher {
	private int bcIndex;

	public TheCall(String methodSignature, int bcIndex) {
		super(methodSignature);
		this.bcIndex = bcIndex;
	}
	
	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		if (n.getKind() != SDGNode.Kind.CALL) return false;
		return n.getBytecodeIndex() == bcIndex;
	}
	public static TheCall at(String callerSignature, int bcIndex) {
		return new TheCall(callerSignature, bcIndex);
	}
}
