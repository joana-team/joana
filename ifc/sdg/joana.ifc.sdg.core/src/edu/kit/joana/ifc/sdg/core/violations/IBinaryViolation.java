/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.violations;

/**
 * TODO: @author Add your name here.
 */
public interface IBinaryViolation<T, L> extends IViolation<T> {
	public T getNode();
	public T getInfluencedBy();
	public L getAttackerLevel();
}
