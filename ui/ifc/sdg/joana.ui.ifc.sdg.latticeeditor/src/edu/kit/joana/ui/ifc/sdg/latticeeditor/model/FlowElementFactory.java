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

import org.eclipse.gef.requests.CreationFactory;

/**
 * @author Daniel Lee
 */
public class FlowElementFactory implements CreationFactory {

	private Object template;

	/**
	 * Creates a new FlowElementFactory with the given template object
	 *
	 * @param o
	 *            the template
	 */
	public FlowElementFactory(Object o) {
		template = o;
	}

	/**
	 * @see org.eclipse.gef.requests.CreationFactory#getNewObject()
	 */
	public Object getNewObject() {
		try {
			return ((Class<?>) template).newInstance();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @see org.eclipse.gef.requests.CreationFactory#getObjectType()
	 */
	public Object getObjectType() {
		return template;
	}

}
