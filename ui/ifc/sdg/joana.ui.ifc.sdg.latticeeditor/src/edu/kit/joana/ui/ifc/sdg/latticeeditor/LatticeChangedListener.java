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
 * Created on 06.12.2004
 *
 */
package edu.kit.joana.ui.ifc.sdg.latticeeditor;


import org.eclipse.core.resources.IResource;

import edu.kit.joana.ifc.sdg.lattice.LatticeProblemDescription;

/**
 * @author naxan
 *
 * Classes implementing this interface may register iwth LatticeEditor and get
 * notified when lattice changes
 */
public interface LatticeChangedListener<ElementType> {
	/**
	 * Called when a observed lattice changes
	 *
	 * @param res
	 *            the resource providing the lattice source, or
	 *            <code>null</code> if none
	 * @param latSource
	 *            the source of the lattice
	 * @param problem
	 *            a <code>LatticeProblemDescription</code> containing a
	 *            description of the problem, or <code>null</code> if the
	 *            lattice passed validation.
	 */
	public void latticeChanged(IResource res, String latSource, LatticeProblemDescription<ElementType> problem);
}
