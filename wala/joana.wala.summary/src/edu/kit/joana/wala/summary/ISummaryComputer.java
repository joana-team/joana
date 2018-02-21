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

public interface ISummaryComputer {
	public int compute                (WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException;
	public int computeAdjustedAliasDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException;
	public int computePureDataDep     (WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException;
	public int computeFullAliasDataDep(WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException;
	public int computeNoAliasDataDep  (WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException;
	public int computeHeapDataDep     (WorkPackage<SDG> pack, boolean parallel, IProgressMonitor progress) throws CancelException;
}
