/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import java.util.Collection;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.wala.core.ParameterField;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface CandidateFactory {

	UniqueParameterCandidate findOrCreateUnique(OrdinalSet<InstanceKey> basePts, ParameterField field,
			OrdinalSet<InstanceKey> fieldPts);

	UniqueMergableParameterCandidate findOrCreateUniqueMergable(Atom id);

	MetaMergableParameterCandidate createMerge(ParameterCandidate a, ParameterCandidate b);

	MultiMergableParameterCandidate createMerge(OrdinalSet<UniqueParameterCandidate> cands);

	OrdinalSet<UniqueParameterCandidate> createSet(Collection<UniqueParameterCandidate> cands);

	OrdinalSet<UniqueParameterCandidate> findUniqueSet(Collection<ParameterCandidate> cands);

	Iterable<UniqueParameterCandidate> getUniqueCandidates();

}
