/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.ui.ifc.sdg.latticeeditor.commands;


import org.eclipse.gef.commands.Command;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.LatticeElement;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;

/**
 * @author Daniel Lee
 */
public class CreateAndAssignSourceCommand extends Command {

	private LatticeElement parent;
	private Activity child;
	private Activity source;
	private Transition transition;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		parent.addChild(child);
		transition = new Transition(source, child);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		source.addOutput(transition);
		child.addInput(transition);
		parent.addChild(child);
	}

	/**
	 * Sets the parent ActivityDiagram
	 *
	 * @param sa
	 *            the parent
	 */
	public void setParent(LatticeElement sa) {
		parent = sa;
	}

	/**
	 * Sets the Activity to create
	 *
	 * @param activity
	 *            the Activity to create
	 */
	public void setChild(Activity activity) {
		child = activity;
		child.setName("a " + (parent.getChildren().size() + 1));
	}

	/**
	 * Sets the source to the passed activity
	 *
	 * @param activity
	 *            the source
	 */
	public void setSource(Activity activity) {
		source = activity;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		source.removeOutput(transition);
		child.removeInput(transition);
		parent.removeChild(child);
	}

}
