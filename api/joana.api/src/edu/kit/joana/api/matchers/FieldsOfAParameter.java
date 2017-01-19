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

/**
 * Matches the field of a method parameter (or return value). Exploits that
 * in JOANA's SDG implementation fields of parameters are connected with
 * root parameters by control-expression edges.
 *
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class FieldsOfAParameter implements Matcher {
	private final AParameter paramMatcher;

	/**
	 * @param aParameter
	 */
	public FieldsOfAParameter(AParameter aParameter) {
		this.paramMatcher = aParameter;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.matchers.Matcher#matches(edu.kit.joana.ifc.sdg.graph.SDGNode, edu.kit.joana.ifc.sdg.graph.SDG)
	 */
	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		for (SDGEdge eInc : sdg.incomingEdgesOf(n)) {
			if (eInc.getKind() != SDGEdge.Kind.CONTROL_DEP_EXPR) continue;
			if (paramMatcher.matches(eInc.getSource(), sdg)) return true;
		}
		return false;
	}
}
