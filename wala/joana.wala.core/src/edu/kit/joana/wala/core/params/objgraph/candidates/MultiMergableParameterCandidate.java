/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

import com.ibm.wala.util.intset.OrdinalSet;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface MultiMergableParameterCandidate extends ParameterCandidate {

	void merge(OrdinalSet<UniqueParameterCandidate> toMerge);

}
