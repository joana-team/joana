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

/**
 * AddCommand
 *
 * @author Daniel Lee
 */
public class AddCommand extends Command {

	private Activity child;
	private LatticeElement parent;

	/**
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	public void execute() {
		parent.addChild(child);
	}

	/**
	 * Returns the LatticeElement that is the parent
	 *
	 * @return the parent
	 */
	public LatticeElement getParent() {
		return parent;
	}

	/**
	 * Sets the child to the passed Activity
	 *
	 * @param subpart
	 *            the child
	 */
	public void setChild(Activity newChild) {
		child = newChild;
	}

	/**
	 * Sets the parent to the passed StructuredActiivty
	 *
	 * @param newParent
	 *            the parent
	 */
	public void setParent(LatticeElement newParent) {
		parent = newParent;
	}

	/**
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	public void undo() {
		parent.removeChild(child);
	}

}
