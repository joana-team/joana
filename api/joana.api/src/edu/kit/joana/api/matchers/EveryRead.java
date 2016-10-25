/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Matcher for read operations of a given field
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class EveryRead extends EveryAccess {
	public EveryRead(String fieldName) {
		super(fieldName, SDGNode.Operation.REFERENCE);
	}
	public static EveryRead of(String fieldName) {
		return new EveryRead(fieldName);
	}
}
