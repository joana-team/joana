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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

import edu.kit.joana.util.Config;
import edu.kit.joana.util.Log;
import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetector.Result;
import edu.kit.joana.wala.util.PrettyWalaNames;

/**
 * Stores configuration and intermediate results of the optional side-effect detection.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class SideEffectDetectorConfig {

	private final List<CandidateFilter> varsToAnalyze = new LinkedList<CandidateFilter>();
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
					final CandidateFilter filter = createFieldFilter(var);
					varsToAnalyze.add(filter);
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
	
	private static CandidateFilter createFieldFilter(String var) {
		final String fieldName;
		final String className;
		
		boolean isStatic = true;
		if (var.startsWith("s_")) {
			isStatic = true;
			var = var.substring("s_".length());
		} else if (var.startsWith("o_")) {
			isStatic = false;
			var = var.substring("o_".length());
		}
		
		final int iDot = var.lastIndexOf('.');
		if (iDot > 0) {
			String tmp = var.substring(iDot + 1);
			fieldName = (tmp.isEmpty() ? null : tmp);
			tmp = var.substring(0, iDot);
			className = (tmp.isEmpty() ? null : tmp);
		} else {
			fieldName = var;
			className = null;
		}
		
		return new SingleFieldFilter(className, fieldName, isStatic);
	}
	
	public static interface CandidateFilter {
		public boolean isRelevant(ParameterField cand);
	}
	
	public static class SingleFieldFilter implements CandidateFilter {
		public final String className;
		public final String fieldName;
		public final boolean isStatic;
		
		public SingleFieldFilter(final String className, final String fieldName, final boolean isStatic) {
			this.className = className;
			this.fieldName = fieldName;
			this.isStatic = isStatic;
		}

		@Override
		public boolean isRelevant(final ParameterField f) {
			return f != null && f.isStatic() == isStatic && fieldNameMatches(f) && classNameMatches(f);
		}

		private boolean fieldNameMatches(final ParameterField f) {
			if (fieldName == null || fieldName.equals("*")) {
				return true;
			}

			return fieldName.equals(f.getName());
		}
		
		private boolean classNameMatches(final ParameterField f) {
			if (className == null || className.equals("*")) {
				return true;
			}
			
			final IClass cls = f.getField().getDeclaringClass();
			final String typeName = PrettyWalaNames.type2string(cls);
			
			return typeName.contains(className);
		}
		
		public String toString() {
			return (isStatic ? "static " : "non-static ") + (className != null ? className : "*") + "."
					+ (fieldName != null ? fieldName : "*");
		}
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
		
		for (final CandidateFilter filter : varsToAnalyze) {
			final SideEffectDetector.Result result = (isOneLevelOnly
					? SideEffectDetector.whoModifiesOneLevel(filter, modref, sideEffectsDirect, sdg, cg, progress)
					: SideEffectDetector.whoModifies(filter, modref, sideEffectsDirect, sdg, cg, progress));
			if (result != null) {
				results.add(result);
			} else {
				Log.ERROR.outln("No side-effect detector result for '" + filter + "'");
			}
		}
		
		return results;
	}
	
	public Collection<CandidateFilter> getStaticVariablesToAnalyze() {
		return Collections.unmodifiableList(varsToAnalyze);
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
