/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.summary;

import java.util.Set;
import java.util.TreeSet;


import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.joana.JoanaConverter;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

@Deprecated
public final class SummaryComputationWrapper {

	private SummaryComputationWrapper() {}

	public static void computeDataDepSummary(final SDGBuilder builder, final IProgressMonitor progress) throws CancelException {
		final SDG sdg = JoanaConverter.convert(builder, progress);

		final Set<EntryPoint> entries = new TreeSet<EntryPoint>();
		final PDG pdg = builder.getMainPDG();
		final TIntSet formIns = new TIntHashSet();
		for (final PDGNode p : pdg.params) {
			formIns.add(p.getId());
		}
		final TIntSet formOuts = new TIntHashSet();
		formOuts.add(pdg.exception.getId());
		formOuts.add(pdg.exit.getId());
		final EntryPoint ep = new EntryPoint(pdg.entry.getId(), formIns, formOuts);
		entries.add(ep);
		final WorkPackage pack = WorkPackage.create(sdg, entries, sdg.getName());
		SummaryComputation.computePureDataDep(pack, progress);

		// write back to this sdg
		for (final SDGNode call : sdg.vertexSet()) {
			if (call.kind == SDGNode.Kind.CALL) {
				final PDG pdgCall = builder.getPDGforId(call.getProc());

				for (SDGNode param : sdg.getParametersFor(call)) {
					if (param.kind == SDGNode.Kind.ACTUAL_IN) {
						PDGNode pIn = pdgCall.getNodeWithId(param.getId());

						for (SDGEdge out : sdg.outgoingEdgesOf(param)) {
							if (out.getKind() == SDGEdge.Kind.SUMMARY_DATA) {
								PDGNode pOut = pdgCall.getNodeWithId(out.getTarget().getId());
								pdgCall.addEdge(pIn, pOut, PDGEdge.Kind.SUMMARY_DATA);
							}
						}
					}
				}
			}
		}

	}

}
