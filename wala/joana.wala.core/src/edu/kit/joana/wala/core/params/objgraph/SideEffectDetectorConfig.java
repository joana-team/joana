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
 * Stores configuration and intermediate results of the optional side-effect detection. The side-effect detection
 * searches methods that may modify a given set of static of class attributes. It can also detect if a method modifies
 * a field reachable from one of the attributes.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public final class SideEffectDetectorConfig {

	private final List<CandidateFilter> varsToAnalyze = new LinkedList<CandidateFilter>();
	private final boolean isOneLevelOnly;
	private HashMap<CGNode, Collection<ModRefFieldCandidate>> sideEffectsDirect;
	private final ResultConsumer resultConsumer;
	
	/**
	 * The side-effect detector uses a ResultComsumer class to return the results of the analysis.
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 */
	public static interface ResultConsumer {
		public void consume(List<CandidateFilter> usedFilters, boolean isOneLevelOnly, List<Result> results);
		public void consumeModRef(ModRefCandidates modref);
	}
	
	/**
	 * The default result consumer prints a summary of the results to the console. 
	 */
	public static class DefaultResultConsumer implements ResultConsumer {

		@Override
		public void consume(final List<CandidateFilter> usedFilters, final boolean isOneLevelOnly,
				final List<Result> results) {
			System.out.println("\n>>>>> side-effect detector results ("
				+ (isOneLevelOnly ? "one level" : "all reachable") + ") >>>>>");
			for (final Result result : results) {
				System.out.println(result);
			}
			System.out.println("<<<<< side-effect detector results <<<<<");
		}

		@Override
		public void consumeModRef(ModRefCandidates modref) {

		}
	}
	
	/**
	 * Creates a configuration for the side-effect detector. The relevant options are:
	 * <ul>
	 * <li>isOneLevelOnly: When set only modifications to the relevant attributes and their fields are detected.
	 *  Otherwise all modification to the relevant attributes and all transitively reachable fields are detected.</li>
	 *  <li>resultConsumer: The result consumer is notified when computation is done.</li>
	 *  <li>varsToAnalyze: A list of candidate filters that specifies the set of relevant attributes.</li>
	 * </ul>
	 * @param isOneLevelOnly When set only modifications to the relevant attributes and their fields are detected.
	 *  Otherwise all modification to the relevant attributes and all transitively reachable fields are detected.
	 * @param resultConsumer The result consumer is notified when computation is done.
	 * @param varsToAnalyze A list of candidate filters that specifies the set of relevant attributes.
	 */
	public SideEffectDetectorConfig(final boolean isOneLevelOnly, final ResultConsumer resultConsumer,
			final List<CandidateFilter> varsToAnalyze) {
		if (varsToAnalyze == null || varsToAnalyze.isEmpty()) {
			throw new IllegalArgumentException("You need to provide a non-empty list of variables to analyze.");
		}
		
		this.isOneLevelOnly = isOneLevelOnly;
		this.resultConsumer = (resultConsumer == null ? new DefaultResultConsumer() : resultConsumer);
		for (final CandidateFilter cf : varsToAnalyze) {
			if (cf != null) {
				this.varsToAnalyze.add(cf);
			} else {
				throw new IllegalArgumentException("List of candidate filters contained a null entry.");
			}
		}
	}
	
	private SideEffectDetectorConfig() {
		if (!isActivated()) {
			throw new IllegalStateException("Side-effect detector is not activated, no need to instance this class.");
		}
		this.resultConsumer = new DefaultResultConsumer();
		
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
	
	/**
	 * Creates an instance of the SideEffectDetectorConfig with default options (output to console) if the
	 * corresponding system property has been set through a command line option or a property file. See
	 * <tt>edu.kit.joana.util.Config</tt> for details. 
	 * @return An instance of SideEffectDetectorConfig or <tt>null</tt>
	 */
	public static SideEffectDetectorConfig maybeCreateInstance() {
		return (isActivated() ? new SideEffectDetectorConfig() : null);
	}
	
	/**
	 * Creates a candidate filter for a given "magic" variable specification string.
	 * <pre>('s_' | 'o_')? &lt;className&gt '.' &lt;fieldName&gt;</pre>
	 * The prefix 's_' selects static attributes, 'o_' class attributes. If no prefix is provided, static attributes
	 * are selected. E.g.
	 * <pre>
	 * 's_my.package.MyClass.f4' => static, &lt;className&gt='my.package.MyClass', &lt;fieldName&gt;='f4'
	 * 'o_A.B.C.i'               => non-static, &lt;className&gt='A.B.C', &lt;fieldName&gt;='i'
	 * 'MyClass.some'            => static, &lt;className&gt='MyClass', &lt;fieldName&gt;='some'
	 * </pre>
	 * @param var The magic variable specification.
	 * @return A candidate filter that matches all attributes corresponding to the "magic" string.
	 */
	public static CandidateFilter createFieldFilter(String var) {
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
	
	/**
	 * A candidate filter decides which calss and static attributes are relevant for the side-effect analysis.
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 */
	public static interface CandidateFilter {
		/**
		 * Returns true iff the given parameter is relevant for the side-effect computation. 
		 */
		public boolean isRelevant(ParameterField cand);
	}
	
	/**
	 * A filter that matches class and static attributes given a single field name and a part of the class name.
	 * The fieldname is checked for equality, the class name is interpreted a a part of the actual class name.
	 * The fully qualified attribute name is matched against: <pre>.*&lt;className&gt;.*\.&lt;fieldName&gt;</pre>
	 * E.g. given a filter with <tt>[className="Voter", fieldName="fA", isStatic=false]</tt>
	 * <pre>
	 * matched: Voter.fa
	 * matched: some.package.Voter.fa
	 * matched: SuperVoterClass.fa
	 * not-matched: Voter.faa
	 * not-matched: static Voter.fa
	 * </pre>
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 */
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
			
			if (f.isField()) {
				final IClass cls = f.getField().getDeclaringClass();
				final String typeName = PrettyWalaNames.type2string(cls);
				
				return typeName.contains(className);
			}
			
			return false;
		}
		
		public String toString() {
			return (isStatic ? "static " : "non-static ") + (className != null ? className : "*") + "."
					+ (fieldName != null ? fieldName : "*");
		}
	}
	
	/**
	 * Copies the state of the mod/ref computation after the intraprocedural phase, but before interprocedural
	 * propagation. This allows us to see which side-effect originate directly from each method.
	 * This methods needs to be called before <tt>runAnalysis</tt> can be run.
	 * @param modref Intraprocedural state of the mod/ref computation.
	 */
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
	
	/**
	 * Runs the side-effect detection analysis. The intraprocedural mod/ref state has to be copied through a call to
	 * <tt>copyIntraprocState</tt> before this method can be run.
	 * When the analysis is finished the previously configured ResultConsumer gets triggered. The default
	 * ResultComsumer writes the results to the console.
	 */
	public void runAnalysis(final SDGBuilder sdg, final CallGraph cg,
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
		resultConsumer.consume(varsToAnalyze, isOneLevelOnly, results);
		resultConsumer.consumeModRef(modref);
		// cleanup
		sideEffectsDirect = null;
	}
	
	/**
	 * Returns a list of candidate filters. A candidate filter is a method that decides which class or static
	 * attributes are relevant for the side-effect analysis.
	 * @return A list of candidate filters.
	 */
	public Collection<CandidateFilter> getVariablesToAnalyze() {
		return Collections.unmodifiableList(varsToAnalyze);
	}

	/**
	 * Check if side-effect detection has been triggered through a runtime property.
	 */
	public static boolean isActivated() {
		return Config.isDefined(Config.C_SIDEEFFECT_DETECTOR);
	}

}
