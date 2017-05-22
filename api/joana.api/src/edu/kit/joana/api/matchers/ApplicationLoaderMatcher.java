package edu.kit.joana.api.matchers;

import edu.kit.joana.api.matchers.Matcher;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
* This matcher matches all nodes in application scope.
* @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
*/
public final class ApplicationLoaderMatcher implements Matcher {
	public static ApplicationLoaderMatcher INSTANCE = new ApplicationLoaderMatcher();
	private ApplicationLoaderMatcher() {
	}
	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		return sdg.getEntry(n).getClassLoader().equals("Application");
	}

}
