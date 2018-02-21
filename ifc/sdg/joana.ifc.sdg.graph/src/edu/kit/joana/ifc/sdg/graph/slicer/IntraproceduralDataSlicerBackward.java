/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;

/**
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class IntraproceduralDataSlicerBackward extends IntraproceduralSlicerBackward {
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicer#IntraproceduralSlicer
	 */
	public IntraproceduralDataSlicerBackward(SDG g) {
		super(g);
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicer#isAllowedEdge(edu.kit.joana.ifc.sdg.graph.SDGEdge)
	 */
	@Override
	protected boolean isAllowedEdge(SDGEdge e) {
		return super.isAllowedEdge(e) && SDGEdge.Kind.dataflowEdges().contains(e.getKind());
	}

	
	
	

}
