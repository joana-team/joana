/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.util.Objects;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Models the conditional that a SDGNode directly control depends on
 * and the branch that the node is part of
 */
public class AffectingConditional {
	
	public final SDGNode conditional;
	
	/**
	 * Value of the conditional node, that yields
	 * to the execution of a specific node
	 */
	public final boolean value;

	public AffectingConditional(SDGNode conditional, boolean value) {
		this.conditional = conditional;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.format("%s[%s]", value ? "" : "!", conditional.getLabel());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(conditional, value);
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof AffectingConditional && 
				conditional.equals(((AffectingConditional)obj).conditional) &&
				value == ((AffectingConditional)obj).value;
	}
}
