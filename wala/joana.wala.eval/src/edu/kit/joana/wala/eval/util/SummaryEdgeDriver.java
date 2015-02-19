/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval.util;

import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryEdgeComputation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public abstract class SummaryEdgeDriver {

	public static class Result {
		public int numSumEdges;
		public long startTime;
		public long endTime;
	}
	
	public abstract Result compute(final SDG sdg, final IProgressMonitor progress) throws CancelException;

	public static SummaryEdgeDriver getNewVariant() {
		return new SummaryEdgeDriver() {
			@Override
			public Result compute(final SDG sdg, final IProgressMonitor progress) throws CancelException {
				final Result r = new Result();
				final Set<EntryPoint> entries = new TreeSet<EntryPoint>();
				final SDGNode root = sdg.getRoot();

				final TIntSet formalIns = new TIntHashSet();
				for (final SDGNode fIn : sdg.getFormalIns(root)) {
					formalIns.add(fIn.getId());
				}
				
				final TIntSet formalOuts = new TIntHashSet();
				for (final SDGNode fOut : sdg.getFormalOuts(root)) {
					formalOuts.add(fOut.getId());
				}
				
				final EntryPoint ep = new EntryPoint(root.getId(), formalIns, formalOuts);
				entries.add(ep);

				final WorkPackage pack = WorkPackage.create(sdg, entries, sdg.getName() + "-summaryedgedriver");

				r.startTime = System.currentTimeMillis();
				r.numSumEdges = SummaryComputation.compute(pack, progress);
				r.endTime = System.currentTimeMillis();
				
				return r;
			}
			
		};
	}
	
	public static SummaryEdgeDriver getOldVariant() {
		return new SummaryEdgeDriver() {
			@Override
			public Result compute(final SDG sdg, final IProgressMonitor progress) throws CancelException {
				final Result r = new Result();

				r.startTime = System.currentTimeMillis();
				r.numSumEdges = SummaryEdgeComputation.compute(sdg, progress).size();
				r.endTime = System.currentTimeMillis();
				
				return r;
			}
			
		};
	}
	
}
