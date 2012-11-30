/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel;


/**
 * Generic interface for a parameter node.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public interface IParameter {

	public boolean isMayAliasing(IParameter p);

	public boolean isMustAliasing(IParameter p);

	public boolean isPrimitive();

	public boolean isStaticField();

	public boolean isRoot();

	public boolean isArray();

	public boolean isObjectField();

	public boolean isOnHeap();

	public boolean isIn();

	public boolean isOut();

	public boolean isActual();

	public boolean isFormal();

	public boolean isException();

	public boolean isExit();

	public boolean isVoid();

	/**
	 * Returns the bytecode name of the field (or parameter) this parameter node
	 * corresponds to.
	 * @return The bytecode name of the field (or parameter) this parameter node
	 * corresponds to.
	 */
	public String getBytecodeName();

}
