/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import java.io.IOException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.wala.util.Log;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

public abstract class SDGCreator {

	public abstract JoanaSDGResult buildSDG(String classPath, String mainClass, String runtimeLib, String outputSDGFile, IProgressMonitor progress) throws IllegalArgumentException, CancelException, IOException, WalaException, InvalidClassFileException;

	protected MHPAnalysis doMHP(String outputSDGFile, SDG joanaSdg, boolean computeInterference, IProgressMonitor progress) throws CancelException {
		final MHPAnalysis mhp;

		if (computeInterference) {
			progress.beginTask("Creating cSDG from SDG " + outputSDGFile, -1);

			progress.subTask("Running Thread Allocation Analysis");
			Log.info("Running Thread Allocation Analysis");

			mhp = runMHP(joanaSdg, progress);

			Log.info("Thread Allocation done.");
			progress.done();
		} else {
			mhp = null;
		}

		return mhp;
	}

	protected abstract MHPAnalysis runMHP(SDG joanaSdg, IProgressMonitor progress) throws CancelException;
}
