/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.violations.IConflict;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;

/**
 * Common interface for all algorithms scanning for conflicts leading to 
 * probabilistic security leaks. 
 * Provides a method which is called to check a program for possible probabilisitic
 * security leaks and a method to retrieve all conflicts leading to such leaks.
 * Note, that {@link #getConflicts()} is supposed to be called after {@link #check()}.
 * @author Martin Mohr
 */
public interface ConflictScanner {
	Collection<ClassifiedViolation> check();
	Collection<? extends IConflict> getConflicts();
	
}
