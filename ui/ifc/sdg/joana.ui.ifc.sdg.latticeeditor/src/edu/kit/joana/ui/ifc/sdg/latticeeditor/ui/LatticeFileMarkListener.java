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
package edu.kit.joana.ui.ifc.sdg.latticeeditor.ui;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.ifc.sdg.lattice.LatticeProblemDescription;
import edu.kit.joana.ui.ifc.sdg.latticeeditor.LatticeChangedListener;

public class LatticeFileMarkListener implements LatticeChangedListener<String> {

	public void latticeChanged(IResource res, String latSource, LatticeProblemDescription<String> problem) {
		if (res != null) {
			try {
				LatticeErrorMarker.unmark(res);
				if (problem != null)
					LatticeErrorMarker.mark(res, problem);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

}
