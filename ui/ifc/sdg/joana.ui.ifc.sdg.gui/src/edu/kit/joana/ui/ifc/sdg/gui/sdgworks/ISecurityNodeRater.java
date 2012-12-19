/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.sdgworks;

import java.util.HashMap;
import java.util.List;


import org.eclipse.core.resources.IMarker;

import edu.kit.joana.ifc.sdg.core.SecurityNode;

public interface ISecurityNodeRater {

	/***
	 * Returns a rating that identifies how good SDGNode m probably matches to IMarker im
	 * @param im
	 * @param m
	 * @return
	 */
//	int getRating(IMarker im, SecurityNode m);

	HashMap<SecurityNode, Integer> getRating(IMarker im, List<SecurityNode> ms);
}
