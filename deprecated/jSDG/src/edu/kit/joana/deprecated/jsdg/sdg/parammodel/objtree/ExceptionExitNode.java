/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.parammodel.objtree;



class ExceptionExitNode extends FormInOutNode {

	public ExceptionExitNode(int id) {
		super(id, Type.EXCEPTION, false, false);
	}

	public boolean isVoid() {
		return false;
	}

	public boolean isExit() {
		return true;
	}

	public boolean isMergingPointsToSupported() {
		return true;
	}

	public boolean isException() {
		return true;
	}

}
