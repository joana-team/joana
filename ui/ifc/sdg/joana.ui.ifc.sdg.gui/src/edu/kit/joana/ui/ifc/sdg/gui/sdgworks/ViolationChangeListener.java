/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 27.02.2005
 *
 */
package edu.kit.joana.ui.ifc.sdg.gui.sdgworks;

import java.util.Collection;

import org.eclipse.core.resources.IProject;

import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;

/**
 * Listens for changes in found violations
 * mainly used by ViolationView for getting informed of new
 * Violation Check being available
 *
 * @author naxan
 */
public interface ViolationChangeListener {
	void violationsChanged(IProject p, Collection<ClassifiedViolation> violations);
}
