/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary.accesspath;

import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;

/**
 * Match FlowLess parameter variables and SDG nodes.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public interface Matcher {

	/**
	 * Returns the root node of the matched parameter
	 */
	SDGNode getMatch(Parameter param);

	/**
	 * Returns the input field node of the matched parameter.
	 */
	SDGNode getFineMatchIN(Parameter param);

	/**
	 * Returns the output field node of the matched parameter.
	 */
	SDGNode getFineMatchOUT(Parameter param);

	Set<SDGNode> getFineReachIN(Parameter param);

	Set<SDGNode> getFineReachOUT(Parameter param);

	/**
	 * Returns true if a corresponding SDGNode for the given parameter has been found.
	 */
	boolean hasMatchFor(Parameter param);

	/**
	 * Returns all resolved parameters.
	 */
	Set<Parameter> getResolvedParams();

}
