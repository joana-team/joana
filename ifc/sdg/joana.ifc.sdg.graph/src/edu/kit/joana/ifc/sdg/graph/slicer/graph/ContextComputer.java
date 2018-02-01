/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.util.Pair;


/** The ContextComputer computes contexts for given nodes.
 * It employs a folded call graph for that purpose.
 *
 * @author giffhorn
 */
public class ContextComputer {
	private final JoanaGraph g;
	private final CallGraph call;
	private final FoldedCallGraph folded; // a folded call graph

	public ContextComputer(JoanaGraph g, CallGraph call, FoldedCallGraph folded) {
		this.g = g;
		this.call = call;
		this.folded = folded;
	}

	public ContextComputer(SDG sdg, CallGraph call) {
		this.g = sdg;
		this.call = call;
		this.folded = GraphFolder.foldCallGraph(call);
	}

	public ContextComputer(SDG sdg) {
		this.g = sdg;
		this.call = CallGraphBuilder.buildCallGraph(sdg);
		this.folded = GraphFolder.foldCallGraph(call);
	}

	/** Returns all contexts of the given node.
	 *
	 * @param node    The node whose contexts are requested.
	 * @return        A set with the found contexts or an empty set.
	 * @see           DynamicContext
	 */
	public Set<DynamicContext> allPossibleContextsForNode(SDGNode node) {
		HashSet<DynamicContext> cs = new HashSet<>();

		for (int thread : node.getThreadNumbers()) {
			cs.addAll(allPossibleContextsForNode(node, thread));
		}

		return cs;
	}

	/** Returns all thread-local contexts of the given node inside the given thread.
	 *
	 * @param node    The node whose contexts are requested.
	 * @param thread  A thread ID.
	 * @return        A set with the found contexts or an empty set.
	 * @see           DynamicContext
	 */
	public Set<DynamicContext> allPossibleContextsForNode(SDGNode node, int thread) {
		Set<LinkedList<SDGNode>> temp = buildContextsFor(node, thread);
		HashSet<DynamicContext> cs = new HashSet<>();

		for (LinkedList<SDGNode> tmp : temp) {
			cs.add(new DynamicContext(tmp, node, thread));
		}

		return cs;
	}

	/** Returns all thread-local contexts of the given node.
	 *
	 * @param node    The node whose contexts are requested.
	 * @return        A collection with the found contexts or an empty set.
	 * @see           Context
	 */
	public Collection<DynamicContext> getAllContextsOf(SDGNode node) {
		HashSet<DynamicContext> cs = new HashSet<>();

		for (int thread : node.getThreadNumbers()) {
			cs.addAll(allPossibleContextsForNode(node, thread));
		}

		return cs;
	}

	/** Returns all inter-thread contexts of the given node.
	 * These contexts do not have a thread ID and should be used with caution.
	 *
	 * @param node    The node whose contexts are requested.
	 * @return        A collection with the found contexts or an empty set.
	 * @see           Context
	 */
	public Collection<Pair<SDGNode,DynamicContext>> getExtendedContextsOf(SDGNode node) {
		Set<Pair<SDGNode, LinkedList<SDGNode>>> pairs = buildExtendedContextsFor(node);
		HashSet<Pair<SDGNode,DynamicContext>> cs = new HashSet<>();

		for (Pair<SDGNode,LinkedList<SDGNode>> pair : pairs) {
			final SDGNode forkNode = pair.getFirst();
			final LinkedList<SDGNode> tmp = pair.getSecond();
			cs.add(Pair.pair(forkNode, new DynamicContext(tmp, node, -1)));
		}

		return cs;
	}

	/** Returns all contexts of the given node inside the given thread.
	 *
	 * @param node    The node whose contexts are requested.
	 * @param thread  A thread ID.
	 * @return        A collection with the found contexts or an empty set.
	 * @see           DynamicContext
	 */
	public Collection<DynamicContext> getContextsOf(SDGNode node, int thread) {
		Set<LinkedList<SDGNode>> temp = buildContextsFor(node, thread);
		HashSet<DynamicContext> cs = new HashSet<>();

		for (LinkedList<SDGNode> tmp : temp) {
			cs.add(new DynamicContext(tmp, node, thread));
		}

		return cs;
	}

	/** Traverses the call graph backwards to the root of a thread, starting at a given node.
	 * All visited call and entry sites are are packed into a stack.
	 * The stacks of every possible path root -> node are built and returned in a list.
	 *
	 * @param node    The node.
	 * @param root    The root of the thread to handle.
	 * @param thread  The thread's ID.
	 */
	private Set<LinkedList<SDGNode>> buildContextsFor(SDGNode node, int thread) {
		// the result list
		HashSet<LinkedList<SDGNode>> result = new HashSet<LinkedList<SDGNode>>();

		// 1. Find corresponding entry node of 'node'
		SDGNode entry = g.getEntry(node);

		// 2. build all possible contexts
		if (entry == null || entry.getProc() == 0) {
			LinkedList<SDGNode> trivial = new LinkedList<SDGNode>();
			result.add(trivial);

		} else {
			LinkedList<LinkedList<SDGNode>> worklist = new LinkedList<LinkedList<SDGNode>>();

			// initialize worklist with the direct calls of entry
			for (SDGEdge e : g.incomingEdgesOf(entry)) {
				if (e.getKind() == SDGEdge.Kind.CALL) {
					SDGNode f = folded.map(e.getSource());
					LinkedList<SDGNode> p = new LinkedList<SDGNode>();
					p.addLast(f);
					worklist.add(p);

				} else if (e.getKind() == SDGEdge.Kind.FORK) {
					LinkedList<SDGNode> trivial = new LinkedList<SDGNode>();
					result.add(trivial);
				}
			}

			while(!worklist.isEmpty()){
				LinkedList<SDGNode> next = worklist.poll();
				Set<SDGEdge> incEdges = folded.incomingEdgesOf(next.getLast());

				if (incEdges.isEmpty()) {
					// we've reached an end - context found
					result.add(next);

				} else {
					// traverse incoming edges of next.n
					for(SDGEdge e : incEdges){
						if (!next.contains(e.getSource())) {
							if (e.getKind() == SDGEdge.Kind.FORK) {
								// we've reached the root of this thread - context found
								result.add(next);

							} else if (e.getKind() == SDGEdge.Kind.CALL) {
								SDGNode source = e.getSource();
								@SuppressWarnings("unchecked")
								final LinkedList<SDGNode> p = (LinkedList<SDGNode>) next.clone();
								p.addLast(source);
								worklist.add(p);
							}
						}
					}
				}
			}
		}

		return result;
	}

	/** Traverses the call graph backwards to its root, starting at a given node.
	 * All visited call and entry sites are are packed into a stack.
	 * The stacks of every possible path root -> node are built and returned in a list, together with
	 * the threads fork node.
	 *
	 * @param node    The node.
	 * @param root    The root of the thread to handle.
	 * @param thread  The thread's ID.
	 */
	@SuppressWarnings("unchecked")
	private Set<Pair<SDGNode, LinkedList<SDGNode>>> buildExtendedContextsFor(SDGNode node) {
		// the result list
		HashSet<Pair<SDGNode, LinkedList<SDGNode>>> result = new HashSet<>();

		// 1. Find corresponding entry node of 'node'
		SDGNode entry = g.getEntry(node);

		assert entry == null || entry.kind == SDGNode.Kind.ENTRY;

		// 2. build all possible contexts
		if (entry == null) {
			LinkedList<SDGNode> trivial = new LinkedList<SDGNode>();
			result.add(Pair.pair(null, trivial));
		} else {
			LinkedList<Pair<SDGNode, LinkedList<SDGNode>>> worklist = new LinkedList<>();

			// initialize worklist with the direct calls of entry
			for (SDGEdge e : g.incomingEdgesOf(entry)) {
				if (e.getKind() == SDGEdge.Kind.FORK) {
					SDGNode f = folded.map(e.getSource());
					LinkedList<SDGNode> p = new LinkedList<SDGNode>();
					p.addLast(f);

					assert e.getSource().getKind() == SDGNode.Kind.CALL;
					worklist.add(Pair.pair(e.getSource(), p));
				}
			}

			while(!worklist.isEmpty()){
				final Pair<SDGNode, LinkedList<SDGNode>> nextPair = worklist.poll();
				final SDGNode forkNode = nextPair.getFirst();
				final LinkedList<SDGNode> next = nextPair.getSecond();
				Set<SDGEdge> incEdges = folded.incomingEdgesOf(next.getLast());

				if (incEdges.isEmpty()) {
					// we've reached an end - context found
					result.add(nextPair);

				} else {
					// traverse incoming edges of next.n
					for(SDGEdge e : incEdges){
						if (e.getKind() == SDGEdge.Kind.FORK || e.getKind() == SDGEdge.Kind.CALL) {
							SDGNode source = e.getSource();

							if (!next.contains(source)) {
								LinkedList<SDGNode> p = (LinkedList<SDGNode>) next.clone();
								p.addLast(source);
								worklist.add(Pair.pair(forkNode, p));
							}
						}
					}
				}
			}
		}

		return result;
	}
}
