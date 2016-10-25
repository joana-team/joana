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