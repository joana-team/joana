/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui;

import org.eclipse.jface.action.Action;

/**
 * Extends Action with two Methods setContext and getContext,
 * which my be set to an Object from outside thats needed inside the action
 *
 * @author naxan
 *
 */
public class CAction extends Action {
	public Object context = null;

	public void setContext(Object nc) {
		context = nc;
	}

	public Object getContext() {
		return context;
	}
}
