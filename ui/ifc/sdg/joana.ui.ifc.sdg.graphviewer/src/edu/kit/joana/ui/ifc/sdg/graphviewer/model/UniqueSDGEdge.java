/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import org.jgrapht.EdgeFactory;

import edu.kit.joana.ifc.sdg.graph.LabeledSDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class UniqueSDGEdge extends LabeledSDGEdge {
	public static final UniqueSDGEdgeFactory FACTORY = new UniqueSDGEdgeFactory();

	public UniqueSDGEdge(SDGNode source, SDGNode sink) {
        super(source, sink, SDGEdge.Kind.HELP, null);
    }

	public UniqueSDGEdge(SDGNode source, SDGNode sink, Kind kind) {
        super(source, sink, kind, null);
    }

	public UniqueSDGEdge(SDGNode source, SDGNode sink, Kind kind, String label) {
		super(source, sink, kind, label);
	}

	public boolean equals(Object o) {
		return this == o;
	}

	public static class UniqueSDGEdgeFactory implements EdgeFactory<SDGNode, SDGEdge> {
		public SDGEdge createEdge(SDGNode sourceVertex, SDGNode targetVertex) {
			return new UniqueSDGEdge(sourceVertex, targetVertex);
		}
	}
}
