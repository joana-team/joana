/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.strings.Atom;

import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.ParameterField;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class MergeAllFieldsOfClass implements MergeStrategy {

	private final Set<IClass> classTypes;
	private final CandidateFactory fact;

	public MergeAllFieldsOfClass(final Set<IClass> classTypes, final CandidateFactory fact) {
		this.classTypes = classTypes;
		this.fact = fact;
	}

	@Override
	public boolean doMerge(OrdinalSet<InstanceKey> basePts, ParameterField field, OrdinalSet<InstanceKey> fieldPts) {
		return classTypes.contains(field.getField().getDeclaringClass());
	}

	@Override
	public UniqueParameterCandidate getMergeCandidate(OrdinalSet<InstanceKey> basePts, ParameterField field,
			OrdinalSet<InstanceKey> fieldPts) {
		final IClass cls = field.getField().getDeclaringClass();
		if (!classTypes.contains(cls)) {
			throw new IllegalArgumentException();
		}

		final Atom f = Atom.findOrCreate(IOFactory.createUTF8Bytes(cls.toString()));
		final UniqueMergableParameterCandidate mp = fact.findOrCreateUniqueMergable(f);

		mp.merge(basePts, field, fieldPts);

		return mp;
	}

}
