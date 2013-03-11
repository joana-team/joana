/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg.optimize;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;
import edu.kit.joana.wala.summary.ForwardReachablilitySlicer;


public class RemoveLibraryClinits {

	public static void removeLibraryClinits(final SDG sdg) {
		RemoveLibraryClinits rlc = new RemoveLibraryClinits(sdg);
		rlc.run();
	}

	private final SDG sdg;

	private RemoveLibraryClinits(final SDG sdg) {
		this.sdg = sdg;
	}

	private void run() {
//		final LinkedList<SDGNode> clEntries = new LinkedList<SDGNode>();
//		for (final SDGNode n : sdg.vertexSet()) {
//			if (n.getKind() == SDGNode.Kind.ENTRY && n.getBytecodeMethod() != null && n.getBytecodeMethod().contains("<clinit>")) {
//				clEntries.add(n);
//			}
//		}
//
		// find all called methods that are only reachable from those entry nodes
		final CFG cg = CallGraphBuilder.buildEntryGraph(sdg);
		final Set<SDGNode> mainEntries = new HashSet<SDGNode>();
		for (final SDGEdge call : cg.outgoingEdgesOf(sdg.getRoot())) {
			final int procID = call.getTarget().getProc();
			if (procID == 3) {
				// 0 == start
				// 1 == Thread.start
				// 2 == Thread.run
				// 3 == Entrypoint
				mainEntries.add(call.getTarget());
			}
//			final String label = call.getTarget().getBytecodeMethod();
//
//			if (label != null && !label.contains("<clinit>")) {
//				mainEntries.add(call.getTarget());
//			}
		}

		final Set<SDGNode> reachFromMain = ForwardReachablilitySlicer.slice(cg, mainEntries);
		reachFromMain.add(sdg.getRoot());

		final LinkedList<SDGNode> toRemove = new LinkedList<SDGNode>();

		for (final SDGNode entry : sdg.vertexSet()) {
			if (entry.kind == SDGNode.Kind.ENTRY && !reachFromMain.contains(entry)) {
				toRemove.add(entry);
			}
		}

		for (final SDGNode entry : toRemove) {
			removeMethod(entry);
		}
	}

	private void removeMethod(final SDGNode entry) {
		final List<SDGNode> proc = sdg.getNodesOfProcedure(entry);

//		final Collection<SDGNode> calls = sdg.getCallers(entry);
//
//		for (final SDGNode call : calls) {
//			// merge param nodes
//		}

//		final LinkedList<SDGEdge> toRemove = new LinkedList<SDGEdge>();
//
//		for (final SDGNode p : proc) {
//			final Set<SDGEdge> in = sdg.incomingEdgesOf(p);
//			toRemove.addAll(in);
//			final Set<SDGEdge> out = sdg.outgoingEdgesOf(p);
//			toRemove.addAll(out);
//		}
//
//		sdg.removeAllEdges(toRemove);
		sdg.removeAllVertices(proc);
	}
}
