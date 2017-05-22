package edu.kit.joana.api.matchers;

import edu.kit.joana.api.matchers.Matcher;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
* This matcher matches nothing.
* @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
*/
public final class NothingMatcher implements Matcher {
	public static NothingMatcher INSTANCE = new NothingMatcher();
	private NothingMatcher() {
	}
	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		return false;
	}
}
