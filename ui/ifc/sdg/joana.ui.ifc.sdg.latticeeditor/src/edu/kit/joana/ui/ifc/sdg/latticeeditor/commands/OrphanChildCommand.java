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

import java.util.List;

import org.eclipse.gef.commands.Command;

import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.Activity;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.model.LatticeElement;

/**
 * OrphanChildCommand
 *
 * @author Daniel Lee
 */
public class OrphanChildCommand extends Command {

	private LatticeElement parent;
	private Activity child;
	private int index;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		List<Activity> children = parent.getChildren();
		index = children.indexOf(child);
		parent.removeChild(child);
	}

	/**
	 * Sets the child to the passed Activity
	 *
	 * @param child
	 *            the child
	 */
	public void setChild(Activity child) {
		this.child = child;
	}

	/**
	 * Sets the parent to the passed LatticeElement
	 *
	 * @param parent
	 *            the parent
	 */
	public void setParent(LatticeElement parent) {
		this.parent = parent;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		parent.addChild(child, index);
	}

}
