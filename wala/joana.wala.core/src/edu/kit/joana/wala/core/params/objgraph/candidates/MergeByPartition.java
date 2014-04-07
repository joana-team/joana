/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.ParameterField;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * @author Martin Mohr
 */
public class MergeByPartition implements MergeStrategy {
	
	private CandidateFactory candFact;
	private final Map<ParameterField, Set<ParameterField>> eqClasses;
	private final TObjectIntMap<Set<ParameterField>> eqClass2Index;
	
	public MergeByPartition(LinkedList<Set<ParameterField>> partition) {
		eqClasses = computeEquivalenceClasses(partition);
		eqClass2Index = computeEqClass2Index(partition);
	}

	private Map<ParameterField, Set<ParameterField>> computeEquivalenceClasses(LinkedList<Set<ParameterField>> partition) {
		Map<ParameterField, Set<ParameterField>> eqClasses = new HashMap<ParameterField, Set<ParameterField>>();
		for (Set<ParameterField> s : partition) {
			for (ParameterField p : s) {
				eqClasses.put(p, s);
			}
		}
		return eqClasses;
	}
	
	private TObjectIntMap<Set<ParameterField>> computeEqClass2Index(LinkedList<Set<ParameterField>> partition) {
		TObjectIntMap<Set<ParameterField>> ret = new TObjectIntHashMap<Set<ParameterField>>();
		int counter = 0;
		for (Set<ParameterField> p : partition) {
			ret.put(p, counter);
			counter++;
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
	public boolean doMerge(OrdinalSet<InstanceKey> basePts, ParameterField field, OrdinalSet<InstanceKey> fieldPts) {
		return eqClasses.keySet().contains(field);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.wala.core.params.objgraph.candidates.MergeStrategy#getMergeCandidate(com.ibm.wala.util.intset.OrdinalSet, edu.kit.joana.wala.core.ParameterField, com.ibm.wala.util.intset.OrdinalSet)
	 */
	@Override
	public UniqueParameterCandidate getMergeCandidate(OrdinalSet<InstanceKey> basePts, ParameterField field,
			OrdinalSet<InstanceKey> fieldPts) {
		final Atom f = Atom.findOrCreate(IOFactory.createUTF8Bytes(Integer.toString(eqClass2Index.get(eqClasses.get(field)))));
		final UniqueMergableParameterCandidate mp = candFact.findOrCreateUniqueMergable(f);
		mp.merge(basePts, field, fieldPts);
		return mp;
	}

}
