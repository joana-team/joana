/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg.optimize;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class StaticFieldMerge {

	public static void mergeStaticFields(final SDG sdg) {
		StaticFieldMerge sfm = new StaticFieldMerge(sdg);
		sfm.run();
	}

	private final SDG sdg;

	private StaticFieldMerge(final SDG sdg) {
		this.sdg = sdg;
	}

	private void run() {
		final LinkedList<SDGNode> entries = new LinkedList<SDGNode>();
		final LinkedList<SDGNode> calls = new LinkedList<SDGNode>();

		for (final SDGNode n : sdg.vertexSet()) {
			if (n.kind == SDGNode.Kind.ENTRY) {
				entries.add(n);
			} else if (n.kind == SDGNode.Kind.CALL) {
				calls.add(n);
			}
		}

		for (final SDGNode entry : entries) {
			final Set<SDGNode> formIns = sdg.getFormalInsOfProcedure(entry);
			findAndMergeStaticFields(entry, formIns, false);

			final Set<SDGNode> formOuts = sdg.getFormalOutsOfProcedure(entry);
			// include formal-ins in reachablility computation, as out-nodes only exists is sth. was copied
			formOuts.addAll(formIns);
			findAndMergeStaticFields(entry, formOuts, true);
		}

		for (final SDGNode call : calls) {
			final Collection<SDGNode> acts = sdg.getParametersFor(call);
			final Set<SDGNode> actIns = new HashSet<SDGNode>();
			final Set<SDGNode> actOuts = new HashSet<SDGNode>();

			for (final SDGNode param : acts) {
				switch (param.kind) {
				case ACTUAL_IN: {
					actIns.add(param);
				} break;
				case ACTUAL_OUT: {
					actOuts.add(param);
				} break;
				default: // nothing to do here
				}
			}
			actOuts.addAll(actIns);

			findAndMergeStaticFields(call, actIns, false);
			findAndMergeStaticFields(call, actOuts, true);
		}
	}


	private void findAndMergeStaticFields(final SDGNode entry, final Set<SDGNode> paramNodes, final boolean removeIn) {
		final Set<SDGNode> reachabe = findOnlyReachableFromStatic(paramNodes);

		if (removeIn) {
			final LinkedList<SDGNode> toRemove = new LinkedList<SDGNode>();
			for (final SDGNode n : reachabe) {
				if (n.getKind() == SDGNode.Kind.ACTUAL_IN || n.getKind() == SDGNode.Kind.FORMAL_IN) {
					toRemove.add(n);
				}
			}

			reachabe.removeAll(toRemove);
		}

		if (reachabe.size() > 0) {
			final SDGNode first = reachabe.iterator().next();
			final SDGNode dummy = new SDGNode(first.getId(), first.operation, "[dummy]", first.getProc(), "[dummy]", first.getSource(), first.getSr(),
					first.getSc(), first.getEr(), first.getEc(), first.getBytecodeName(), BytecodeLocation.STATIC_FIELD);
			mergeNodes(entry, reachabe, dummy);
		}
	}

	private Set<SDGNode> findOnlyReachableFromStatic(final Set<SDGNode> nodes) {
		final Set<SDGNode> filtered = new HashSet<SDGNode>();

		for (SDGNode n : nodes) {
			if (n.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER) {
				filtered.add(n);
			}
		}

		LinkedList<SDGNode> work = new LinkedList<SDGNode>();
		work.addAll(filtered);
		while (!work.isEmpty()) {
			final SDGNode n = work.removeFirst();

			for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
				final SDGNode tgt = edge.getTarget();
				if (!filtered.contains(tgt)) {
					filtered.add(tgt);
					work.add(tgt);
				}
			}
		}

		final Set<SDGNode> result = new HashSet<SDGNode>(nodes);

		result.removeAll(filtered);

		return result;
	}

	private void mergeNodes(final SDGNode entry, final Set<SDGNode> nodes, final SDGNode merge) {
		final LinkedList<SDGEdge> out = new LinkedList<SDGEdge>();
		final LinkedList<SDGEdge> in = new LinkedList<SDGEdge>();

		for (final SDGNode n : nodes) {
			for (final SDGEdge e : sdg.outgoingEdgesOf(n)) {
				final SDGNode tgt = e.getTarget();
				if (!nodes.contains(tgt)) {
					out.add(e);
				}
			}

			for (final SDGEdge e : sdg.incomingEdgesOf(n)) {
				final SDGNode src = e.getSource();
				if (!nodes.contains(src)) {
					in.add(e);
				}
			}
		}

		sdg.removeAllEdges(out);
		sdg.removeAllEdges(in);
		sdg.removeAllVertices(nodes);

		sdg.addVertex(merge);
		sdg.addEdge(new SDGEdge(entry, merge, SDGEdge.Kind.PARAMETER_STRUCTURE));
		sdg.addEdge(new SDGEdge(entry, merge, SDGEdge.Kind.CONTROL_DEP_EXPR));

		for (final SDGEdge e : out) {
			sdg.addEdge(new SDGEdge(merge, e.getTarget(), e.getKind(), e.getLabel()));
		}

		for (final SDGEdge e : in) {
			sdg.addEdge(new SDGEdge(e.getSource(), merge, e.getKind(), e.getLabel()));
		}
	}
}
