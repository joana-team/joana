/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.Set;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public abstract class EscapeAnalysis {


	/**
	 * Returns whether the access in the given method m to the given set of instance keys may be visible outside one of the methods from the given set of methods
	 * @return {@code true} if the effect of the access to one of the instance keys is visible outside the given set of methods (modeled by {@code CGNode}s).
	 */
	public abstract boolean mayEscape(CGNode m, Iterable<InstanceKey> pts, Set<CGNode> eSet);
}
