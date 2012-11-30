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
public interface MergeStrategy {

	boolean doMerge(OrdinalSet<InstanceKey> basePts, ParameterField field, OrdinalSet<InstanceKey> fieldPts);

	UniqueParameterCandidate getMergeCandidate(OrdinalSet<InstanceKey> basePts, ParameterField field,
			OrdinalSet<InstanceKey> fieldPts);

	public static MergeStrategy NO_INITIAL_MERGE = new MergeStrategy() {

		@Override
		public boolean doMerge(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
				final OrdinalSet<InstanceKey> fieldPts) {
			return false;
		}

		@Override
		public UniqueParameterCandidate getMergeCandidate(final OrdinalSet<InstanceKey> basePts,
				final ParameterField field,	final OrdinalSet<InstanceKey> fieldPts) {
			throw new UnsupportedOperationException();
		}
	};
}
