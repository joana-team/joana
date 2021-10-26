/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.ifc.sdg.graph.SDG;

public enum SummaryComputationType {
	JOANA_CLASSIC(new SummaryComputer()),
	JOANA_CLASSIC_SCC(new SummaryComputer3()),
	SIMON_SCC(new SummaryComputer2()),
	@Deprecated
	SIMON_PARALLEL_SCC(new AlwaysParallel(new SummaryComputer2()));


	public static final SummaryComputationType DEFAULT = JOANA_CLASSIC_SCC;
	private final ISummaryComputer summaryComputer;

	private SummaryComputationType(ISummaryComputer summaryComputer) {
		this.summaryComputer = summaryComputer;
	}

	public ISummaryComputer getSummaryComputer() {
		new SummaryComputer2() {
			/* (non-Javadoc)
			 * @see edu.kit.joana.wala.summary.SummaryComputer2#compute(edu.kit.joana.wala.summary.WorkPackage, boolean, com.ibm.wala.util.MonitorUtil.IProgressMonitor)
			 */
			@Override
			public int compute(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
					throws CancelException {
				// TODO Auto-generated method stub
				return super.compute(pack, parallel, progress);
			}
		};
		return summaryComputer;
	}

	private static class AlwaysParallel implements ISummaryComputer {
		private final ISummaryComputer other;
		AlwaysParallel(ISummaryComputer other) {
			this.other = other;
		}

		@Override
		public int compute(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException {
			return other.compute(pack, true, progress);
		}

		@Override
		public int computeAdjustedAliasDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
				throws CancelException {
			return other.computeAdjustedAliasDep(pack, true, progress);
		}

		@Override
		public int computeFullAliasDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
				throws CancelException {
			return other.computeFullAliasDataDep(pack, true, progress);
		}

		@Override
		public int computeHeapDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
				throws CancelException {
			return other.computeHeapDataDep(pack, true, progress);
		}

		@Override
		public int computeNoAliasDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
				throws CancelException {
			return other.computeNoAliasDataDep(pack, true, progress);
		}

		@Override
		public int computePureDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress)
				throws CancelException {
			return other.computePureDataDep(pack, true, progress);
		}
	}
}
