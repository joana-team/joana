/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
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
 * SplitTransitionCommand
 *
 * @author Daniel Lee
 */
public class SplitTransitionCommand extends Command {

	private LatticeElement parent;
	private Activity oldSource;
	private Activity oldTarget;
	private Transition transition;

	private Activity newActivity;
	private Transition newIncomingTransition;
	private Transition newOutgoingTransition;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		oldSource.removeOutput(transition);
		oldTarget.removeInput(transition);
		parent.addChild(newActivity);
		newIncomingTransition = new Transition(oldSource, newActivity);
		newOutgoingTransition = new Transition(newActivity, oldTarget);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		oldSource.addOutput(newIncomingTransition);
		oldTarget.addInput(newOutgoingTransition);
		newActivity.addInput(newIncomingTransition);
		newActivity.addOutput(newOutgoingTransition);
		parent.addChild(newActivity);
		oldSource.removeOutput(transition);
		oldTarget.removeInput(transition);
	}

	/**
	 * Sets the parent Activity. The new Activity will be added as a child to
	 * the parent.
	 *
	 * @param activity
	 *            the parent
	 */
	public void setParent(LatticeElement activity) {
		parent = activity;
	}

	/**
	 * Sets the transition to be "split".
	 *
	 * @param transition
	 *            the transition to be "split".
	 */
	public void setTransition(Transition transition) {
		this.transition = transition;
		oldSource = transition.source;
		oldTarget = transition.target;
	}

	/**
	 * Sets the Activity to be added.
	 *
	 * @param activity
	 *            the new activity
	 */
	public void setNewActivity(Activity activity) {
		newActivity = activity;
		newActivity.setName("a " + (parent.getChildren().size() + 1));
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		oldSource.removeOutput(newIncomingTransition);
		oldTarget.removeInput(newOutgoingTransition);
		newActivity.removeInput(newIncomingTransition);
		newActivity.removeOutput(newOutgoingTransition);
		parent.removeChild(newActivity);
		oldSource.addOutput(transition);
		oldTarget.addInput(transition);
	}

}
