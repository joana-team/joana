/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel;

import java.util.Iterator;

import edu.kit.joana.deprecated.jsdg.sdg.nodes.AbstractParameterNode;


/**
 * Generic set of parameter nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IParamSet<T extends AbstractParameterNode> extends Iterable<T> {

	public boolean isEmpty();

	public void merge(IParamSet<T> set);

	public boolean equals(IParamSet<T> set);

	public Iterator<T> iterator();

	public boolean contains(IParameter param);

}
