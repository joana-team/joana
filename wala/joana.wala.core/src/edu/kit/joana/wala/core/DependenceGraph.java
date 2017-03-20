/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.graph.AbstractJoanaGraph;

public class DependenceGraph extends AbstractJoanaGraph<PDGNode, PDGEdge> {

	public static final EdgeFactory<PDGNode, PDGEdge> DEFAULT_EDGE_FACTORY = new PDGEdgeFactory();

	private static final class PDGEdgeFactory implements EdgeFactory<PDGNode, PDGEdge> {

		@Override
		public PDGEdge createEdge(PDGNode arg0, PDGNode arg1) {
			throw new UnsupportedOperationException("Please specify the edge kind.");
		}

	}

	public DependenceGraph() {
		super(DEFAULT_EDGE_FACTORY);
	}

	public boolean removeEdge(PDGNode from, PDGNode to, PDGEdge.Kind kind) {
		return super.removeEdge(new PDGEdge(from, to, kind));
	}

	public boolean removeNode(PDGNode node) {
		if (containsVertex(node)) {
			for (PDGEdge in : incomingEdgesOf(node)) {
				for (PDGEdge out : outgoingEdgesOf(node)) {
					if (isKindCompatible(in.kind, out.kind) && in.from != out.to) {
						addEdge(in.from, out.to, mergeKind(in.kind, out.kind));
					}
				}
			}

			return removeVertex(node);
		}

		return false;
	}

	private static boolean isKindCompatible(PDGEdge.Kind k1, PDGEdge.Kind k2) {
		return k1 == k2 || (k1.isControl() && k2.isControl()) || (k1.isData() && k2.isData()) || (k1.isFlow() && k2.isFlow())
			|| (k1.isData() && k2 == PDGEdge.Kind.PARAM_STRUCT) || (k2.isData() && k1 == PDGEdge.Kind.PARAM_STRUCT);
	}

	private static PDGEdge.Kind mergeKind(PDGEdge.Kind k1, PDGEdge.Kind k2) {
		assert isKindCompatible(k1, k2) : "I can only merge compatible edges";

		if (k1 == k2) {
			return k1;
		} else if ((k1 == PDGEdge.Kind.PARAM_STRUCT && k2.isData()) || (k2 == PDGEdge.Kind.PARAM_STRUCT && k1.isData())) {
			return PDGEdge.Kind.PARAM_STRUCT;
		} else if (k1.isControl()) {
			return PDGEdge.Kind.CONTROL_DEP;
		} else if (k1.isData()) {
			return PDGEdge.Kind.DATA_HEAP;
		} else if (k1.isFlow()) {
			return PDGEdge.Kind.CONTROL_FLOW_EXC;
		}

		throw new IllegalStateException();
	}

	public final PDGEdge addEdge(PDGNode from, PDGNode to, PDGEdge.Kind kind) {
		PDGEdge edge = new PDGEdge(from, to, kind);
		addEdge(from, to, edge);

		return edge;
	}
	
	@Override
	public boolean addVertex(PDGNode arg0) {
		return super.addVertex(arg0);
	};

	public String toString() {
		return "Generic Dependence Graph";
	}

}
