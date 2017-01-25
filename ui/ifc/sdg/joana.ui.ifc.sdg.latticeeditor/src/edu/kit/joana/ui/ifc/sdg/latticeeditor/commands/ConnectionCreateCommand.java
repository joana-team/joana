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

import java.util.List;

import org.eclipse.gef.commands.Command;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;

/**
 * @author Daniel Lee
 */
public class ConnectionCreateCommand extends Command {

	/** The Transistion between source and target Activities * */
	protected Transition transition;
	/** The source Activity * */
	protected Activity source;
	/** The target Activity * */
	protected Activity target;

	/**
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	public boolean canExecute() {
		if (source.equals(target))
			return false;

		// Check for existence of connection already
		List<Transition> transistions = source.getOutgoingTransitions();
		for (int i = 0; i < transistions.size(); i++) {
			if (transistions.get(i).target.equals(target))
				return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		transition = new Transition(target, source); // changed Frank Nodes
	}

	/**
	 * Returns the source Activity
	 *
	 * @return the source
	 */
	public Activity getSource() {
		return source;
	}

	/**
	 * Returns the target Activity
	 *
	 * @return the target
	 */
	public Activity getTarget() {
		return target;
	}

	/**
	 * Returns the Transistion between the source and target Activities
	 *
	 * @return the transistion
	 */
	public Transition getTransition() {
		return transition;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		source.addInput(transition);
		target.addOutput(transition);
	}

	/**
	 * Sets the source Activity
	 *
	 * @param activity
	 *            the source Activity
	 */
	public void setSource(Activity activity) {
		source = activity;
	}

	/**
	 * Sets the Transistion between the source and target Activities
	 *
	 * @param transition
	 *            the transistion
	 */
	public void setTransition(Transition transition) {
		this.transition = transition;
	}

	/**
	 * Sets the target Activity
	 *
	 * @param activity
	 *            the target
	 */
	public void setTarget(Activity activity) {
		target = activity;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		source.removeInput(transition);
		target.removeOutput(transition);
	}

}
