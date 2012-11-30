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

import java.util.ArrayList;
import java.util.List;


import org.eclipse.gef.commands.Command;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.LatticeElement;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Transition;

/**
 * Handles the deletion of Activities.
 *
 * @author Daniel Lee
 */
public class DeleteCommand extends Command {

	private Activity child;
	private LatticeElement parent;
	private int index = -1;
	private List<Transition> sourceConnections = new ArrayList<Transition>();
	private List<Transition> targetConnections = new ArrayList<Transition>();

	@SuppressWarnings("unchecked")
	private void deleteConnections(Activity a) {
		if (a instanceof LatticeElement) {
			List children = ((LatticeElement) a).getChildren();
			for (int i = 0; i < children.size(); i++)
				deleteConnections((Activity) children.get(i));
		}
		sourceConnections.addAll(a.getIncomingTransitions());
		for (int i = 0; i < sourceConnections.size(); i++) {
			Transition t = sourceConnections.get(i);
			t.source.removeOutput(t);
			a.removeInput(t);
		}
		targetConnections.addAll(a.getOutgoingTransitions());
		for (int i = 0; i < targetConnections.size(); i++) {
			Transition t = targetConnections.get(i);
			t.target.removeInput(t);
			a.removeOutput(t);
		}
	}

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		primExecute();
	}

	/**
	 * Invokes the execution of this command.
	 */
	protected void primExecute() {
		deleteConnections(child);
		index = parent.getChildren().indexOf(child);
		parent.removeChild(child);
	}

	/**
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	public void redo() {
		primExecute();
	}

	private void restoreConnections() {
		for (int i = 0; i < sourceConnections.size(); i++) {
			Transition t = sourceConnections.get(i);
			t.target.addInput(t);
			t.source.addOutput(t);
		}
		sourceConnections.clear();
		for (int i = 0; i < targetConnections.size(); i++) {
			Transition t = targetConnections.get(i);
			t.source.addOutput(t);
			t.target.addInput(t);
		}
		targetConnections.clear();
	}

	/**
	 * Sets the child to the passed Activity
	 *
	 * @param a
	 *            the child
	 */
	public void setChild(Activity a) {
		child = a;
	}

	/**
	 * Sets the parent to the passed LatticeElement
	 *
	 * @param sa
	 *            the parent
	 */
	public void setParent(LatticeElement sa) {
		parent = sa;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		parent.addChild(child, index);
		restoreConnections();
	}

}
