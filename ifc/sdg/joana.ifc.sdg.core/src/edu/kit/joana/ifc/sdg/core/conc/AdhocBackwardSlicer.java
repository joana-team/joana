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
import java.util.Set;
import java.util.stream.Collectors;

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

	
	//private static enum StateKind { NULL, ZERO_ONE, TWO, BOTH };
	private static class State { }
	
	private static final State NULL = new State();
	private static final State ZERO_ONE = new State();

	private State TWO = new State();
	private State BOTH = new State();
	private Set<State> oldTWO = new HashSet<>();
	private Set<State> oldBOTH = new HashSet<>();

	private void visit_0_1_addAll(Collection<SDGNode> criteria) {
		for (SDGNode c : criteria) {
			visited_0_1_add(c); 
		}
	}
	
	private boolean visited_0_1_add(SDGNode n) {
		State state = (State) n.customData; 
		if (state == TWO) {
			n.customData = BOTH;
			return true;
		}
		if (state == NULL) {
			n.customData = ZERO_ONE;
			return true;
		}
		if (state == BOTH) {
			return false;
		}
		if (state == ZERO_ONE) {
			return false;
		}
		if (oldBOTH.contains(state)) {
			n.customData = ZERO_ONE;
			return false;
		}
		assert oldTWO.contains(state);
		n.customData = ZERO_ONE;
		return true;
	}
	
	private boolean visited_2_add(SDGNode n) {
		State state = (State) n.customData; 
		if (state == ZERO_ONE) {
			n.customData = BOTH;
			return true;
		}
		if (state == NULL) {
			n.customData = TWO;
			return true;
		}
		if (state == BOTH) {
			return false;
		}
		if (state == TWO) {
			return false;
		}
		if (oldBOTH.contains(state)) {
			n.customData = BOTH;
			return true;
		}
		assert oldTWO.contains(state);
		n.customData = TWO;
		return true;
	}
	
	private void visited_2_clear() {
		oldTWO.add(TWO);
		oldBOTH.add(BOTH);
		TWO = new State();
		BOTH = new State();
	}
	
	@Override
	public Collection<SDGNode> slice(Collection<SDGNode> criteria) {
		boolean assertionsEnabled = false;
		assert assertionsEnabled = true;
		
		LinkedList<SDGNode> worklist_0 = new LinkedList<SDGNode>();
		LinkedList<SDGNode> worklist_1 = new LinkedList<SDGNode>();
		LinkedList<SDGNode> worklist_2 = new LinkedList<SDGNode>();
		for (SDGNode n : g.vertexSet()) {
			n.customData = NULL;
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
				visited_2_clear();
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
						} else {
							assert visited_0_1.contains(adjacent);
						}

					} else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
						// descend into called procedure in phase two!
						if (visited_2_add(adjacent)) {
							assert visited_2.add(adjacent);
							worklist_2.add(adjacent);
						} else {
							assert visited_2.contains(adjacent);
						}

					} else if (edge.getKind().isSDGEdge()) { // note that return
																// edges are no
																// sdg edges!

						// intra-procedural or ascending edge
						if (visited_0_1_add(adjacent)) {
							assert visited_0_1.add(adjacent);
							worklist_1.add(adjacent);
						} else {
							assert visited_0_1.contains(adjacent);
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
						} else {
							assert visited_0_1.contains(adjacent);
						}

					} else if (edge.getKind().isSDGEdge() && edge.getKind() != SDGEdge.Kind.CALL
							&& edge.getKind() != SDGEdge.Kind.PARAMETER_IN && edge.getKind() != SDGEdge.Kind.FORK
							&& edge.getKind() != SDGEdge.Kind.FORK_IN) {

						// intra-procedural or param-out edge
						if (visited_2_add(adjacent)) {
							assert visited_2.add(adjacent);
							worklist_2.add(adjacent);
						} else {
							assert visited_2.contains(adjacent);
						}
					}
				}
			}
		}

		final Set<SDGNode> result =
			g.vertexSet()
			 .stream()
			 .filter(n -> n.customData == ZERO_ONE || n.customData == BOTH || oldBOTH.contains(n.customData))
			 .collect(Collectors.toSet());
		
		assert visited_0_1.equals(result);
		return result;

	}

	@Override
	public Collection<SDGNode> slice(SDGNode criterion) {
		return slice(Collections.singleton(criterion));
	}

}
