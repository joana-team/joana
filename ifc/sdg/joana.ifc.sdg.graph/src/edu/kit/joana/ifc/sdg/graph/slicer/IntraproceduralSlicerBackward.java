/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Collections;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * 
 * @author Dennis Giffhorn
 */
public class IntraproceduralSlicerBackward extends IntraproceduralSlicer implements Slicer {
	/**
	 * Creates a new instance of IntraproceduralSlicer
	 */
	public IntraproceduralSlicerBackward(SDG g) {
		super(g);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicer#adjacentEdges
	 * (edu.kit.joana.ifc.sdg.graph.SDGNode)
	 */
	@Override
	protected Collection<SDGEdge> adjacentEdges(SDGNode n) {
		return graph.incomingEdgesOf(n);
	}
	
	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicer#adjacentTarget(edu.kit.joana.ifc.sdg.graph.SDGEdge)
	 */
	@Override
	protected SDGNode adjacentNode(SDGEdge e) {
		return e.getSource();
	}

	public static void main(String[] args) throws Exception {
		SDG graph = SDG.readFrom(args[0]);
		SDGNode c = graph.getNode(Integer.parseInt(args[1]));

		IntraproceduralSlicerBackward slicer = new IntraproceduralSlicerBackward(graph);
		Collection<SDGNode> slice = slicer.slice(Collections.singleton(c));
		System.out.println(slice);

		for (SDGNode n : slice) {
			if (n.getKind() == SDGNode.Kind.ENTRY || n.getKind() == SDGNode.Kind.ACTUAL_OUT
					|| n.getKind() == SDGNode.Kind.FORMAL_IN) {

				System.out.print(n + ", ");
			}
		}
		System.out.println("\n ");
		for (SDGNode n : slice) {

			for (SDGEdge e : graph.incomingEdgesOf(n)) {
				if (e.getKind() == SDGEdge.Kind.INTERFERENCE) {
					System.out.println(n + ", ");
				}
			}
		}
	}
}
