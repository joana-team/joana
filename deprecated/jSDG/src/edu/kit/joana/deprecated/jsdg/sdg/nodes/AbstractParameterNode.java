/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;

import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParameter;

/**
 * Base class for all parameter nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class AbstractParameterNode extends AbstractPDGNode implements
		IParameter {

	protected AbstractParameterNode(int id) {
		super(id);
	}

	public boolean isException() {
		return false;
	}

	public boolean isExit() {
		return false;
	}

	public boolean isVoid() {
		return false;
	}

	public final boolean isParameterNode() {
		return true;
	}

	public void accept(IPDGNodeVisitor visitor) {
		visitor.visitParameter(this);
	}

}
