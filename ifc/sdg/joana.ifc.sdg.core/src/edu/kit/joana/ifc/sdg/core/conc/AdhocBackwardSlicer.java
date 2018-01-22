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
import java.util.LinkedList;
import java.util.Set;
import java.util.zip.ZipError;

import com.google.common.collect.Sets;

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

	
	private static enum State { NULL, ZERO_ONE, TWO, BOTH }; 

	private static void visit_0_1_addAll(Collection<SDGNode> criteria) {
		for (SDGNode c : criteria) {
			visited_0_1_add (c); 
		}
	}
	
	private static boolean visited_0_1_add(SDGNode n) {
		switch (((State) n.customData)) {
			case TWO:
				n.customData = State.BOTH;
				return true;
			case NULL:
				n.customData = State.ZERO_ONE;
				return true;
			case BOTH:
			case ZERO_ONE:
				return false;
			default:
				assert false;
				return false;
			}
	}
	
	private static boolean visited_2_add(SDGNode n) {
		switch (((State) n.customData)) {
			case ZERO_ONE:
				n.customData = State.BOTH;
				return true;
			case NULL:
				n.customData = State.TWO;
				return true;
			case BOTH:
			case TWO:
				return false;
			default:
				assert false;
				return false;
		}
	}
	
	private static void visited_2_clear(Collection<SDGNode> criteria) {
		for (SDGNode n : criteria) {
			switch (((State) n.customData)) {
				case BOTH:
					n.customData = State.ZERO_ONE; break;
				case TWO:
					n.customData = State.NULL; break;
				case NULL:
				case ZERO_ONE:
					break;
				default:
					assert false;
			}
		}
	}
	
	@Override
	public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
		boolean assertionsEnabled = false;
		assert assertionsEnabled = true;
		
		LinkedList<SDGNode> worklist_0 = new LinkedList<SDGNode>();
		LinkedList<SDGNode> worklist_1 = new LinkedList<SDGNode>();
		LinkedList<SDGNode> worklist_2 = new LinkedList<SDGNode>();
		for (SDGNode n : g.vertexSet()) {
			n.customData = State.NULL;
		}
		
		// SDGNode.equals() seems a bit costly, so we use identityHashMaps here
		Set<SDGNode> visited_0_1 = Sets.newIdentityHashSet();
		Set<SDGNode> visited_2   = Sets.newIdentityHashSet();
		
		worklist_0.addAll(criteria);
		
		if (assertionsEnabled) {
			visited_0_1.addAll(criteria);
		} {
			visit_0_1_addAll(criteria);
		}
		
		// System.out.println("searching for data channels");

		edgeListener.init();

		// die basis bildet ein iterierter zwei-phasen slice
		while (!worklist_0.isEmpty()) {
			// init the next iteration
			if (assertionsEnabled) {
				visited_2.clear();
			} {
				visited_2_clear(g.vertexSet());
			}

			worklist_1.add(worklist_0.poll());
			if (assertionsEnabled) {
				visited_0_1.add(worklist_1.peek());
			} {
				visited_0_1_add(worklist_1.peek());
			}

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
						if (visited_0_1_add(adjacent)) {
							assert visited_0_1.add(adjacent);
							worklist_0.add(adjacent);
						}

					} else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
						// descend into called procedure in phase two!
						if (visited_2_add(adjacent)) {
							assert visited_2.add(adjacent);
							worklist_2.add(adjacent);
						}

					} else if (edge.getKind().isSDGEdge()) { // note that return
																// edges are no
																// sdg edges!

						// intra-procedural or ascending edge
						if (visited_0_1_add(adjacent)) {
							assert visited_0_1.add(adjacent);
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
						if (visited_0_1_add(adjacent)) {
							assert visited_0_1.add(adjacent);
							worklist_0.add(adjacent);
						}

					} else if (edge.getKind().isSDGEdge() && edge.getKind() != SDGEdge.Kind.CALL
							&& edge.getKind() != SDGEdge.Kind.PARAMETER_IN && edge.getKind() != SDGEdge.Kind.FORK
							&& edge.getKind() != SDGEdge.Kind.FORK_IN) {

						// intra-procedural or param-out edge
						if (visited_2_add(adjacent)) {
							assert visited_2.add(adjacent);
							worklist_2.add(adjacent);
						}
					}
				}
			}
		}

		// callers will have to promise not to re-use customData before inspecting this result;
		final Set<SDGNode> result = Sets.filter(g.vertexSet(), n -> n.customData == State.ZERO_ONE || n.customData == State.BOTH);
		
		assert visited_0_1.equals(result);
		return result;

	}

	@Override
	public Collection<SDGNode> slice(SDGNode criterion) {
		return slice(Collections.singleton(criterion));
	}

}
