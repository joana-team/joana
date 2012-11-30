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
/*
 * Created on 16.12.2004
 */
package edu.kit.joana.ui.ifc.sdg.gui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

/**
 * @author naxan
 *
 */
public interface ActiveResourceChangeListener {
	public void activeResourceChanged(IResource activeResource, IProject activeProject);
}
