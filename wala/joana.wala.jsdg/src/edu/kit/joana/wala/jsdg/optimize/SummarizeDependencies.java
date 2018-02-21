/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg.optimize;

import java.util.Collection;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.SummaryMergedChopper;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges.SummaryEdge;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges.SummaryGraph;

public class SummarizeDependencies {

	private final SDG sdg;
	private final SDGNode entry;

	public static void transformToSummary(final SDG sdg, final IMethod im) {
		final SDGNode entry = findEntryNode(sdg, im);
		transformToSummary(sdg, entry);
	}

	public static void transformToSummary(final SDG sdg, final SDGNode entry) {
		SummarizeDependencies sumdep = new SummarizeDependencies(sdg, entry);

		sumdep.run();
	}

	private SummarizeDependencies(final SDG sdg, final SDGNode entry) {
		this.sdg = sdg;
		this.entry = entry;
	}

	private void run() {
		//TODO do sth intelligent
		// - do not change graph create new one -> prevents error due to inconsistent numbering
		final SummaryGraph<SDGNode> sg = IntraprocSummaryEdges.compute(sdg, entry);


		final Set<SDGNode> formIn = sdg.getFormalInsOfProcedure(entry);
		formIn.add(entry);
		final Set<SDGNode> formOut = sdg.getFormalOutsOfProcedure(entry);

		SummaryMergedChopper chopper = new SummaryMergedChopper(sdg);
		Collection<SDGNode> toRemove = chopper.chop(formIn, formOut);
		toRemove.removeAll(formIn);
		toRemove.removeAll(formOut);
		sdg.removeAllVertices(toRemove);

		for (final SummaryEdge sum : sg.edgeSet()) {
			final SDGNode from = sg.getEdgeSource(sum);
			final SDGNode to = sg.getEdgeTarget(sum);
			sdg.addEdge(from, to,  SDGEdge.Kind.DATA_DEP.newEdge(from, to));
		}
	}

	private static final SDGNode findEntryNode(final SDG sum, final IMethod im) {
		final String bcMethod = im.getSignature();
		final SDGNode root = sum.getRoot();

		for (final SDGEdge out : sum.outgoingEdgesOf(root)) {
			final SDGNode to = out.getTarget();

			if (to.kind == SDGNode.Kind.CALL) {
				for (final SDGEdge cl : sum.outgoingEdgesOf(to)) {
					if (cl.getKind() == SDGEdge.Kind.CALL) {
						final SDGNode tgt = cl.getTarget();

						if (bcMethod.equals(tgt.getBytecodeName())) {
							return tgt;
						}
					}
				}
			}
		}

		return null;
	}
}
