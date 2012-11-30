/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objgraph.params;

import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Special formal-out node for the exceptional return value of a method.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ExceptionExitNode extends ExitNode {

	public ExceptionExitNode(int id, TypeReference type, Set<PointerKey> key, OrdinalSet<InstanceKey> pts) {
		super(id, false, type, key, pts, false);
	}

	public final boolean isException() {
		return true;
	}

}
