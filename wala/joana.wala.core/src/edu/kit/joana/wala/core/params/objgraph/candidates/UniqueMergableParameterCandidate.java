/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public abstract class UniqueMergableParameterCandidate extends UniqueParameterCandidate {

	public abstract void merge(OrdinalSet<InstanceKey> basePts, ParameterField field, OrdinalSet<InstanceKey> fieldPts);

	@Override
	public final boolean isMerged() {
		return true;
	}

	@Override
	public final boolean isMustAliased(final ParameterCandidate other) {
		return false;
	}

}
