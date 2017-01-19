/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 *
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public interface Matcher {
	/**
	 * @param n a SDG node
	 * @param sdg a system dependence graph
	 * @return whether the given node matches the criterion described by this matcher object
	 */
	boolean matches(SDGNode n, SDG sdg);

	/**
	 * Returns a matcher which matches all nodes which are matched by this matcher and also all nodes
	 * matched by the given matcher
	 * @param m2 matcher to combine this matcher with
	 * @return a matcher which matches all nodes which are matched by this matcher and also all nodes
	 * matched by the given matcher
	 */
	default Matcher andAlso(Matcher m2) {
		Matcher m1 = this;
		return new Matcher() {
			@Override
			public boolean matches(SDGNode n, SDG sdg) {
				return m1.matches(n, sdg) || m2.matches(n, sdg);
			}
		};
	}

	public static class Do {
		public static Set<SDGNode> collect(SDG sdg, Matcher m) {
			Set<SDGNode> ret = new HashSet<SDGNode>();
			for (SDGNode n : sdg.vertexSet()) {
				if (m.matches(n, sdg)) {
					ret.add(n);
				}
			}
			return ret;
		}
	}
}