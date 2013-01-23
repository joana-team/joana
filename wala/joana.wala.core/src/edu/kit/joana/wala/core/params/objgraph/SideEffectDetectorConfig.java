/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.util.Config;
import edu.kit.joana.util.Log;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetector.Result;

/**
 * Stores configuration and intermediate results of the optional side-effect detection.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class SideEffectDetectorConfig {

	private final List<String> staticVarsToAnalyze = new LinkedList<String>();
	private final boolean isOneLevelOnly;
	private HashMap<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect;
	
	private SideEffectDetectorConfig() {
		if (!isActivated()) {
			throw new IllegalStateException("Side-effect detector is not activated, no need to instance this class.");
		}
		
		final String option = Config.getString(Config.C_SIDEEFFECT_DETECTOR);
		if (option != null && option.equals("all")) {
			isOneLevelOnly = false;
		} else {
			isOneLevelOnly = true;
		}
		
		if (Config.isDefined(Config.C_SIDEEFFECT_DETECTOR_VAR)) {
			final String vars = Config.getString(Config.C_SIDEEFFECT_DETECTOR_VAR);
			if (vars != null) {
				final String[] varArr = vars.split(",");
				for (final String var : varArr) {
					staticVarsToAnalyze.add(var);
				}
			} else {
				Log.ERROR.outln("No variables defined for side-effect detector. Use '-D"
						+ Config.C_SIDEEFFECT_DETECTOR_VAR + "=<varname>[,<varname>]' to specify variables.");
			}
		} else {
			Log.ERROR.outln("No variables defined for side-effect detector. Use '-D"
					+ Config.C_SIDEEFFECT_DETECTOR_VAR + "=<varname>[,<varname>]' to specify variables.");
		}
	}
	
	public static SideEffectDetectorConfig maybeCreateInstance() {
		return (isActivated() ? new SideEffectDetectorConfig() : null);
	}
	
	public void copyIntraprocState(final ModRefCandidates modref) {
		if (sideEffectsDirect != null) {
			throw new IllegalStateException("Intraproc state has already been copied.");
		}
		
		final Map<CGNode, Collection<ModRefFieldCandidate>> intraMap = modref.getCandidateMap();
		sideEffectsDirect = new HashMap<CGNode, Collection<ModRefFieldCandidate>>();
		for (final CGNode n : intraMap.keySet()) {
			final Collection<ModRefFieldCandidate> intraCands = intraMap.get(n);
			sideEffectsDirect.put(n, new HashSet<ModRefFieldCandidate>(intraCands));
		}
	}
	
	public List<SideEffectDetector.Result> runAnalysis(final SDGBuilder sdg, final CallGraph cg,
			final ModRefCandidates modref, final IProgressMonitor progress) throws CancelException {
		if (sideEffectsDirect == null) {
			throw new IllegalStateException("Intraproc side-effect have not been registered.");
		}
		
		final List<SideEffectDetector.Result> results = new LinkedList<SideEffectDetector.Result>();
		
		for (final String varname : staticVarsToAnalyze) {
			final SideEffectDetector.Result result = (isOneLevelOnly
					? SideEffectDetector.whoModifiesOneLevel(varname, modref, sideEffectsDirect, sdg, cg, progress)
					: SideEffectDetector.whoModifies(varname, modref, sideEffectsDirect, sdg, cg, progress));
			if (result != null) {
				results.add(result);
			} else {
				Log.ERROR.outln("No side-effect detector result for '" + varname + "'");
			}
		}
		
		return results;
	}
	
	public Collection<String> getStaticVariablesToAnalyze() {
		return Collections.unmodifiableList(staticVarsToAnalyze);
	}

	public static boolean isActivated() {
		return Config.isDefined(Config.C_SIDEEFFECT_DETECTOR);
	}

	public void printOut(final List<Result> results) {
		System.out.println("\n>>>>> side-effect detector results (" + (isOneLevelOnly ? "one level" : "all reachable")
				+ ") >>>>>");
		for (final Result result : results) {
			System.out.println(result);
		}
		System.out.println("<<<<< side-effect detector results <<<<<");
	}
}
