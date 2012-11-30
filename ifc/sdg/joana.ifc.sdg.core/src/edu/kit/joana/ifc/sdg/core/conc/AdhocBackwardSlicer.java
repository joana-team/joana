/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core.conc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.EdgeListener;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.NandaI2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.TrivialEdgeListener;



/**
 * This is a modified iterated 2-phase backward slicer which is used in {@link ProbabilisticNISlicer} to detect
 * data conflicts. Essentially, it works like {@link NandaI2PBackward}, but does not consider any information from
 * thread invocation analysis. Additionally, not its result, the slice, is used for data conflict detection, but the
 * data conflict edges encountered during traversal. It has been outsourced to make it replaceable by another slicer,
 * for example one with better precision.
 * This slicer can be configured with an {@link EdgeListener edge listener}, which is called at each edge traversed
 * during a slice.
 *
 * @author Dennis Giffhorn, Martin Mohr
 *
 */
public class AdhocBackwardSlicer implements Slicer {

	private SDG g;
	private EdgeListener edgeListener;

	/**
	 * Initializes this slicer. The new slicer does nothing special when encountering an edge.
	 * @param g the sdg to slice
	 */
	public AdhocBackwardSlicer(SDG g) {
		this(g, new TrivialEdgeListener());
	}

	/**
	 * Initializes this slicer and configures its behavior when encountering an edge.
	 * @param g sdg to slice
	 * @param edgeListener implements the behavior of this slicer when it encounters each edge during a slice
	 */
	public AdhocBackwardSlicer(SDG g, EdgeListener edgeListener) {
		if (g == null) {
			throw new IllegalArgumentException("sdg to be sliced must not be null!");
		}

		if (edgeListener == null) {
			throw new IllegalArgumentException("provided edge listener must not be null!");
		}

		this.g = g;
		this.edgeListener = edgeListener;
	}

	@Override
	public void setGraph(SDG graph) {
		this.g = graph;
	}

	@Override
	public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
		LinkedList<SDGNode> worklist_0 = new LinkedList<SDGNode>();
		LinkedList<SDGNode> worklist_1 = new LinkedList<SDGNode>();
		LinkedList<SDGNode> worklist_2 = new LinkedList<SDGNode>();
		HashSet<SDGNode> visited_0_1 = new HashSet<SDGNode>();
		HashSet<SDGNode> visited_2 = new HashSet<SDGNode>();
		worklist_0.addAll(criteria);
		visited_0_1.addAll(criteria);
		// System.out.println("searching for data channels");

		edgeListener.init();

		// die basis bildet ein iterierter zwei-phasen slice
		while (!worklist_0.isEmpty()) {
			// init the next iteration
			visited_2.clear();

			worklist_1.add(worklist_0.poll());
			visited_0_1.add(worklist_1.peek());

			// === phase 1 ===
			// only ascend to calling procedures
			while (!worklist_1.isEmpty()) {
				SDGNode next = worklist_1.poll();

				for (SDGEdge edge : g.incomingEdgesOf(next)) {
					edgeListener.edgeEncountered(edge);
					SDGNode adjacent = edge.getSource();


					if (edge.getKind() == SDGEdge.Kind.INTERFERENCE || edge.getKind() == SDGEdge.Kind.FORK_IN
							|| edge.getKind() == SDGEdge.Kind.FORK_OUT || edge.getKind() == SDGEdge.Kind.FORK) {
						// handle thread edges - concurrency edges have to be
						// traversed in the next iteration
						// TODO: what about join edges? Why are thread numbers
						// not taken into account?
						// I suspect that this is not critical for correctness
						// but rather for precision...
						if (visited_0_1.add(adjacent)) {
							worklist_0.add(adjacent);
						}

					} else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
						// descend into called procedure in phase two!
						if (visited_2.add(adjacent)) {
							worklist_2.add(adjacent);
						}

					} else if (edge.getKind().isSDGEdge()) { // note that return
																// edges are no
																// sdg edges!

						// intra-procedural or ascending edge
						if (visited_0_1.add(adjacent)) {
							worklist_1.add(adjacent);
						}
					}
				}
			}

			// === phase 2 ===
			// visit all procedures called underway
			while (!worklist_2.isEmpty()) {
				SDGNode next = worklist_2.poll();

				for (SDGEdge edge : g.incomingEdgesOf(next)) {
					edgeListener.edgeEncountered(edge);
					SDGNode adjacent = edge.getSource();

					if (edge.getKind() == SDGEdge.Kind.INTERFERENCE) {
						// handle interference edges: create new elements for
						// worklist_0
						if (visited_0_1.add(adjacent)) {
							worklist_0.add(adjacent);
						}

					} else if (edge.getKind().isSDGEdge() && edge.getKind() != SDGEdge.Kind.CALL
							&& edge.getKind() != SDGEdge.Kind.PARAMETER_IN && edge.getKind() != SDGEdge.Kind.FORK
							&& edge.getKind() != SDGEdge.Kind.FORK_IN) {

						// intra-procedural or param-out edge
						if (visited_2.add(adjacent)) {
							worklist_2.add(adjacent);
						}
					}
				}
			}
		}

		return visited_0_1;

	}

	@Override
	public Collection<SDGNode> slice(SDGNode criterion) {
		return slice(Collections.singleton(criterion));
	}

}
