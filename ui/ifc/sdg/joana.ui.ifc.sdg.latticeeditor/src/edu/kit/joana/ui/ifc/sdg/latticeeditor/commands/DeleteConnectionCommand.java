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
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;

/**
 * Handles the deletion of connections between Activities.
 *
 * @author Daniel Lee
 */
public class DeleteConnectionCommand extends Command {

	private Activity source;
	private Activity target;
	private Transition transition;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		source.removeOutput(transition);
		target.removeInput(transition);
		transition.source = null;
		transition.target = null;
	}

	/**
	 * Sets the source activity
	 *
	 * @param activity
	 *            the source
	 */
	public void setSource(Activity activity) {
		source = activity;
	}

	/**
	 * Sets the target activity
	 *
	 * @param activity
	 *            the target
	 */
	public void setTarget(Activity activity) {
		target = activity;
	}

	/**
	 * Sets the transition
	 *
	 * @param transition
	 *            the transition
	 */
	public void setTransition(Transition transition) {
		this.transition = transition;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		transition.source = source;
		transition.target = target;
		source.addOutput(transition);
		target.addInput(transition);
	}

}
