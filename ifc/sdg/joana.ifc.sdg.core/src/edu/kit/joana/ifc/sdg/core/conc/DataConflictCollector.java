/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.EdgeListener;


public class DataConflictCollector implements EdgeListener {

	List<SDGEdge> dataConflicts = new LinkedList<SDGEdge>();

	@Override
	public void init() {
		dataConflicts.clear();
	}

	@Override
	public void edgeEncountered(SDGEdge e) {
		if (e.getKind() == SDGEdge.Kind.CONFLICT_DATA) {
			dataConflicts.add(e);
		}
	}

	public List<SDGEdge> getDataConflicts() {
		List<SDGEdge> ret = new LinkedList<SDGEdge>();
		ret.addAll(dataConflicts);
		return ret;
	}

}
