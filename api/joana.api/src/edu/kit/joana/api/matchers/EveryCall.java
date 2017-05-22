/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.matchers;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class EveryCall extends CallMatcher {

	public EveryCall(String methodSignature) {
		super(methodSignature);
	}

	@Override
	public boolean matches(SDGNode n, SDG sdg) {
		if (n.getKind() != SDGNode.Kind.CALL) return false;
		Collection<SDGNode> possTgts = sdg.getPossibleTargets(n);
		if (possTgts.isEmpty()) {
			return n.getUnresolvedCallTarget() != null && n.getUnresolvedCallTarget().equals(methodSignature);
		} else {
			for (SDGNode entry : possTgts) {
				String tgtSignature = entry.getBytecodeName();
				if (tgtSignature.equals(methodSignature)) {
					return true;
				}
			}
			return false;
		}
	}
	public static EveryCall of(String methodSignature) {
		return new EveryCall(methodSignature);
	}
}
