/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public abstract class UniqueParameterCandidate implements ParameterCandidate {

	public final boolean isUnique() {
		return true;
	}

//	public abstract Set<ParameterField> getFields();
	public abstract int hashCode();
	public abstract boolean equals(Object obj);
	public abstract String toString();

	public String toDebugString() {
		return toString();
	}

	public final OrdinalSet<UniqueParameterCandidate> getUniques() {
		throw new UnsupportedOperationException("I am unique myself!");
	}

	public final int getBytecodeIndex() {
		if (isStatic() == V.YES) {
			return BytecodeLocation.STATIC_FIELD;
		} else if (isRoot() == V.YES) {
			return BytecodeLocation.ROOT_PARAMETER;
		} else if (isArray() == V.YES) {
			return BytecodeLocation.ARRAY_FIELD;
		} else {
			return BytecodeLocation.OBJECT_FIELD;
		}
	}

}
