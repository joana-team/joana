/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;


/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
class ExitNode extends FormInOutNode {

	private final boolean isVoid;

	public ExitNode(int id, boolean isPrimitive) {
		super(id, Type.RETURN, false, isPrimitive);
		isVoid = false;
	}

	public ExitNode(int id) {
		super(id, Type.RETURN, false, true);
		isVoid = true;
	}

	public boolean isVoid() {
		return isVoid;
	}

	public boolean isExit() {
		return true;
	}

	public boolean isMergingPointsToSupported() {
		return !isPrimitive();
	}

	public String getLabel() {
		return (isVoid ? "void " + super.getLabel() : super.getLabel());
	}

}
