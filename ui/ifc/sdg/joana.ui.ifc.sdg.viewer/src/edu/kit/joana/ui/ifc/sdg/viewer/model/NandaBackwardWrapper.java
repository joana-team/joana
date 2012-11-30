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
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaFactory;


public class NandaBackwardWrapper implements edu.kit.joana.ifc.sdg.graph.slicer.Slicer {
	private Nanda nanda;

	public NandaBackwardWrapper(SDG g) {
		nanda = NandaFactory.createNandaBackward(g);
	}

	@Override
	public void setGraph(SDG graph) {
		nanda.setGraph(graph);
	}

	@Override
	public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
		return nanda.slice(criteria);
	}

	@Override
	public Collection<SDGNode> slice(SDGNode criterion) {
		return nanda.slice(criterion);
	}
}
