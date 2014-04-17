/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.wala.core.ParameterField;

/**
 * Takes a set of partitioned parameter fields and creates a single parameter candidate for each partition. It merges
 * all fields of a partition to a single parameter candidate. This is mainly used for fields of pruned library calls.
 * 
 * @author Martin Mohr
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class MergeByPartition implements MergeStrategy {
	
	private CandidateFactory candFact;
	// maps all fields that are part of an equivalence class (mostly fields accesses from pruned calls) to a single 
	// name that represents the equivalence class. This name is then used to create a single candidate for all fields
	// of the same equivalence class
	private final Map<ParameterField, Atom> param2eqName;
	
	public MergeByPartition(final List<Set<ParameterField>> partition) {
		param2eqName = computeParam2eqName(partition);
	}

	private static Map<ParameterField, Atom> computeParam2eqName(final List<Set<ParameterField>> partition) {
		final Map<ParameterField, Atom> ret = new HashMap<ParameterField, Atom>();
		
		int eqIndex = 0;
		for (final Set<ParameterField> part : partition) {
			final Atom eqName = Atom.findOrCreateAsciiAtom("p<" + eqIndex + ">");
			for (final ParameterField field : part) {
				ret.put(field, eqName);
			}
			eqIndex++;
		}
		
		return ret;
	}

	public void setFactory(CandidateFactory candFact) {
		this.candFact = candFact;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.MergeStrategy#doMerge(com.ibm.wala.util.intset.OrdinalSet, edu.kit.joana.wala.core.ParameterField, com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public boolean doMerge(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
			final OrdinalSet<InstanceKey> fieldPts) {
		return param2eqName.containsKey(field);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.MergeStrategy#getMergeCandidate(com.ibm.wala.util.intset.OrdinalSet, edu.kit.joana.wala.core.ParameterField, com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public UniqueParameterCandidate getMergeCandidate(final OrdinalSet<InstanceKey> basePts, final ParameterField field,
			final OrdinalSet<InstanceKey> fieldPts) {
		final Atom eqName = param2eqName.get(field);
		final UniqueMergableParameterCandidate mp = candFact.findOrCreateUniqueMergable(eqName);
		mp.merge(basePts, field, fieldPts);
		
		return mp;
	}

}
