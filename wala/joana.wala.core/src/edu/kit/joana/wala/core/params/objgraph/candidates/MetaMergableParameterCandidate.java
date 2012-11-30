/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph.candidates;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface MetaMergableParameterCandidate extends ParameterCandidate {

	void merge(ParameterCandidate toMerge);

}
