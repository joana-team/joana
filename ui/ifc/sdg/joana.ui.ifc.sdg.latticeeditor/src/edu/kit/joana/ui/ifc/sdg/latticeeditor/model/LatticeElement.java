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
package edu.kit.joana.ui.ifc.sdg.latticeeditor.model;


/**
 * A Structured activity is an activity whose execution is determined by some
 * internal structure.
 *
 * @author hudsonr
 */
public class LatticeElement extends StructuredActivity {

	static final long serialVersionUID = 1;

	public LatticeElement() {
	}

	/**
	 * @author Frank Nodes
	 * @param name -
	 *            get Activity with Name name
	 * @return activity with name 'name'
	 *
	 *
	 */
	public Activity getChild(String name) {
		for (Activity t : children)
			if (t.getName().equals(name))
				return t;
		return null;
	}

	public boolean hasChild(String name) {
		return (this.getChild(name) != null);
	}

}
