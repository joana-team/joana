/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.ContextSensitiveStateMachine;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.demandpa.alg.statemachine.StateMachineFactory;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFABuilder;
import com.ibm.wala.ipa.callgraph.pruned.CallGraphPruning;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ipa.slicer.Slicer.DataDependenceOptions;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

import edu.kit.joana.api.sdg.SDGBuildPreparation;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.ObjTreeType;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.PointsToType;
import edu.kit.joana.deprecated.jsdg.exceptions.ExceptionPrunedCFGAnalysis;
import edu.kit.joana.deprecated.jsdg.exceptions.nullpointer.NullPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.immutables.ExtractImmutables;
import edu.kit.joana.deprecated.jsdg.nontermination.NonTerminationSensitive;
import edu.kit.joana.deprecated.jsdg.output.JoanaCFGSanitizer;
import edu.kit.joana.deprecated.jsdg.output.JoanaStyleSDG;
import edu.kit.joana.deprecated.jsdg.output.WalaConverter;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryEdgeComputation;
import edu.kit.joana.deprecated.jsdg.sdg.interference.CSDGPreprocessor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.parammodel.IParamComputation;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.ObjSensZeroXCFABuilder;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Log.LogLevel;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.InstanceAndPointerKeyFactoryAdapter;
import edu.kit.joana.ifc.sdg.graph.SDGVerifier;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.util.pointsto.ExtendedAnalysisOptions;
import edu.kit.joana.wala.util.pointsto.WalaPointsToUtil;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SDGFactory {

	public static class Config implements Cloneable, Serializable {

		private static final long serialVersionUID = 1L;

		private static enum Options { CLASSPATH("classpath:"), MAIN_CLASS("main class:"),
			SCOPE("scope:"), LOG_FILE("log file:"), POINTSTO("points-to:"),
			LOG_LEVEL("log level:"), EXCLUDE("exclude:"), MAX_THREADS("max number of threads:"),
			INVERT_EXCLUSION("invert exclusion:"), EXCEPTION_STUBS("use stubs for exceptions:"),
			OPTIMIZE_IMMUTABLES("optimize immutables:"), IGNORE_EXCEPTIONS("ignore exceptions:"),
			OPTIMIZE_EXCEPTIONS("optimize exceptions:"),
			NON_TERMINATION("detect non termination:"), SIMPLE_DATA_DEP("simple data dependency:"),
			CONTROL_FLOW("add control flow to sdg:"), INTERFERENCE("compute interference:"),
			SUMMARY_EDGES("compute summary edges:"),
			INTERFERENCE_OPT_THIS("optimize interference (ignore this pointer access in constructor):"),
			INTERFERENCE_NO_CLINITS("optimize interference (no clinits):"),
			INTERFERENCE_USE_ESCAPE("optimize interference (escape analysis):"),
			OUTPUT_DIR("output directory:"), SDG_FILE("output sdg file:"), OBJ_TREE("object tree:"),
			WALA_SDG("use wala sdg:"), NATIVES_XML("native methods xml file:"),
			SLICING_PATTERN("edu.kit.joana.deprecated.jsdg.slicing pattern:"), JOANA_COMPILER("use joana compiler:"),
			DEMAND_PTS("use demand pts:"), SUMMARY_OPT("use summary optimization for recursive calls:"),
			VARIABLE_MAPS("create vraiable maps"), OPTIMIZE_CG("optimize callgraph:");

			private Options(String str) {
				this.str = str;
			}

			private final String str;

			public String toString() {
				return str;
			}

		}


		public Config clone() throws CloneNotSupportedException {
			Config cfg = (Config) super.clone();

			cfg.addControlFlow = addControlFlow;
			cfg.classpath = classpath;
			cfg.computeInterference = computeInterference;
			cfg.computeSummaryEdges = computeSummaryEdges;
			cfg.useSummaryOpt = useSummaryOpt;
			cfg.exceptionStubs = exceptionStubs;
			if (exclusions != null) {
				cfg.exclusions = new ArrayList<String>(exclusions);
			}
			cfg.nonTermination = nonTermination;
			cfg.ignoreExceptions = ignoreExceptions;
			cfg.optimizeExceptions = optimizeExceptions;
			if (immutables != null) {
				cfg.immutables = immutables.clone();
			}
			cfg.interferenceNoClinits = interferenceNoClinits;
			cfg.interferenceOptimizeThisAccess = interferenceOptimizeThisAccess;
			cfg.interferenceUseEscape = interferenceUseEscape;
			cfg.invertExclusion = invertExclusion;
			cfg.logFile = logFile;
			cfg.logLevel = logLevel;
			cfg.mainClass = mainClass;
			cfg.maxNumberOfThreads = maxNumberOfThreads;
			cfg.nativesXML = nativesXML;
			cfg.objTree = objTree;
			cfg.outputDir = outputDir;
			cfg.outputSDGfile = outputSDGfile;
			cfg.pointsTo = pointsTo;
			if (scopeData != null) {
				cfg.scopeData = new ArrayList<String>(scopeData);
			}
			cfg.simpleDataDependency = simpleDataDependency;
			cfg.slicingPattern = slicingPattern;
			cfg.useDemandPts = useDemandPts;
			cfg.useJoanaCompiler = useJoanaCompiler;
			cfg.createVariableMaps = createVariableMaps;
			cfg.useWalaSdg = useWalaSdg;
			cfg.optimizeCg = optimizeCg;

			return cfg;
		}

		public void writeTo(OutputStream out) {
			PrintWriter pw = new PrintWriter(out);
			pw.write(this.toString());
			pw.flush();
		}


		public static Config readFrom(InputStream in) throws IOException {
			Config conf = new Config();

			BufferedReader br = new BufferedReader(new InputStreamReader(in));

			String line = br.readLine();
			while (line != null) {
				line = line.trim();
				if (line.startsWith(Options.CLASSPATH.str)) {
					conf.classpath = getLine(line, Options.CLASSPATH.str);
				} else if (line.startsWith(Options.MAIN_CLASS.str)) {
					conf.mainClass = getLine(line, Options.MAIN_CLASS.str);
				} else if (line.startsWith(Options.SCOPE.str)) {
					if (conf.scopeData == null) {
						conf.scopeData = new LinkedList<String>();
					}
					conf.scopeData.add(getLine(line, Options.SCOPE.str));
				} else if (line.startsWith(Options.LOG_FILE.str)) {
					conf.logFile = getLine(line, Options.LOG_FILE.str);
				} else if (line.startsWith(Options.POINTSTO.str)) {
					conf.pointsTo = PointsToType.valueOf(getLine(line, Options.POINTSTO.str));
				} else if (line.startsWith(Options.LOG_LEVEL.str)) {
					conf.logLevel = LogLevel.valueOf(getLine(line, Options.LOG_LEVEL.str));
				} else if (line.startsWith(Options.EXCLUDE.str)) {
					if (conf.exclusions == null) {
						conf.exclusions = new LinkedList<String>();
					}
					conf.exclusions.add(getLine(line, Options.EXCLUDE.str));
				} else if (line.startsWith(Options.MAX_THREADS.str)) {
					conf.maxNumberOfThreads = getInt(line, Options.MAX_THREADS.str);
				} else if (line.startsWith(Options.INVERT_EXCLUSION.str)) {
					conf.invertExclusion = getBoolean(line, Options.INVERT_EXCLUSION.str);
				} else if (line.startsWith(Options.OPTIMIZE_IMMUTABLES.str)) {
					boolean opt = getBoolean(line, Options.OPTIMIZE_IMMUTABLES.str);
					if (opt) {
						conf.immutables = SDG.stdImmutables;
					} else {
						conf.immutables = null;
					}
				} else if (line.startsWith(Options.EXCEPTION_STUBS.str)) {
					conf.exceptionStubs = getBoolean(line, Options.EXCEPTION_STUBS.str);
				} else if (line.startsWith(Options.IGNORE_EXCEPTIONS.str)) {
					conf.ignoreExceptions = getBoolean(line, Options.IGNORE_EXCEPTIONS.str);
				} else if (line.startsWith(Options.OPTIMIZE_EXCEPTIONS.str)) {
					conf.optimizeExceptions = getBoolean(line, Options.OPTIMIZE_EXCEPTIONS.str);
				} else if (line.startsWith(Options.NON_TERMINATION.str)) {
					conf.nonTermination = getBoolean(line, Options.NON_TERMINATION.str);
				} else if (line.startsWith(Options.JOANA_COMPILER.str)) {
					conf.useJoanaCompiler = getBoolean(line, Options.JOANA_COMPILER.str);
				} else if (line.startsWith(Options.VARIABLE_MAPS.str)) {
					conf.createVariableMaps = getBoolean(line, Options.VARIABLE_MAPS.str);
				} else if (line.startsWith(Options.SIMPLE_DATA_DEP.str)) {
					conf.simpleDataDependency = getBoolean(line, Options.SIMPLE_DATA_DEP.str);
				} else if (line.startsWith(Options.WALA_SDG.str)) {
					conf.useWalaSdg = getBoolean(line, Options.WALA_SDG.str);
					if (conf.useWalaSdg && conf.objTree != ObjTreeType.WALA) {
						conf.objTree = ObjTreeType.WALA;
					}
				} else if (line.startsWith(Options.DEMAND_PTS.str)) {
					conf.useDemandPts = getBoolean(line, Options.DEMAND_PTS.str);
				} else if (line.startsWith(Options.CONTROL_FLOW.str)) {
					conf.addControlFlow = getBoolean(line, Options.CONTROL_FLOW.str);
				} else if (line.startsWith(Options.SUMMARY_OPT.str)) {
					conf.useSummaryOpt = getBoolean(line, Options.SUMMARY_OPT.str);
				} else if (line.startsWith(Options.SUMMARY_EDGES.str)) {
					conf.computeSummaryEdges = getBoolean(line, Options.SUMMARY_EDGES.str);
				} else if (line.startsWith(Options.INTERFERENCE.str)) {
					conf.computeInterference = getBoolean(line, Options.INTERFERENCE.str);
				} else if (line.startsWith(Options.INTERFERENCE_OPT_THIS.str)) {
					conf.interferenceOptimizeThisAccess = getBoolean(line, Options.INTERFERENCE_OPT_THIS.str);
				} else if (line.startsWith(Options.INTERFERENCE_NO_CLINITS.str)) {
					conf.interferenceNoClinits = getBoolean(line, Options.INTERFERENCE_NO_CLINITS.str);
				} else if (line.startsWith(Options.INTERFERENCE_USE_ESCAPE.str)) {
					conf.interferenceUseEscape = getBoolean(line, Options.INTERFERENCE_USE_ESCAPE.str);
				} else if (line.startsWith(Options.NATIVES_XML.str)) {
					conf.nativesXML = getLine(line, Options.NATIVES_XML.str);
				} else if (line.startsWith(Options.OUTPUT_DIR.str)) {
					conf.outputDir = getLine(line, Options.OUTPUT_DIR.str);
				} else if (line.startsWith(Options.SDG_FILE.str)) {
					conf.outputSDGfile = getLine(line, Options.SDG_FILE.str);
				} else if (line.startsWith(Options.SLICING_PATTERN.str)) {
					conf.slicingPattern = getLine(line, Options.SLICING_PATTERN.str);
				} else if (line.startsWith(Options.OBJ_TREE.str)) {
					conf.objTree = ObjTreeType.valueOf(getLine(line, Options.OBJ_TREE.str));
					if (conf.objTree == ObjTreeType.WALA) {
						conf.useWalaSdg = true;
					}
				} else if (line.startsWith(Options.OPTIMIZE_CG.str)) {
					conf.optimizeCg = getInt(line, Options.OPTIMIZE_CG.str);
				}

				line = br.readLine();
			}

			return conf;
		}

		private static Integer getInt(String line, String prefix) {
			String value = getLine(line, prefix);
			return Integer.valueOf(value);
		}

		private static Boolean getBoolean(String line, String prefix) {
			String value = getLine(line, prefix);
			return Boolean.valueOf(value);
		}

		private static String getLine(String line, String prefix) {
			String result = line.substring(prefix.length());
			result = result.trim();
			return result;
		}
		/*
		 * e.g. ".../JoanaTestProject/bin" or ".../joanaTest.jar"
		 */
		public String classpath = null;

		/*
		 * e.g. "Lheapmodel/A"
		 * @see "The JavaTM Virtual Machine Specification -> Descriptors" for
		 * explanations of the naming scheme.
		 * A short example: class XYZ in package pkg1.pkg2
		 * would be written as "Lpkg1/pkg2/XYZ".
		 * 'L' <package_name> ('/' <package_name>)* '/' <class_name>
		 * or for a class without a package
		 * 'L' <class_name>
		 */
		public String mainClass = null;

		/*
		 * this is a file where debugging/logging information is written to
		 * may be null
		 */
		public String logFile = null;

		public LogLevel logLevel = null;

		public static enum PointsToType
			{ZERO_CFA, ZERO_ONE_CFA, VANILLA_ZERO_ONE_CFA, VANILLA_ZERO_ONE_CONTAINER_CFA,
			OBJ_SENS, n0CFA, n1CFA, n2CFA, n3CFA};

		public PointsToType pointsTo = PointsToType.ZERO_CFA;

		public static enum ObjTreeType
			{PTS_GRAPH, PTS_GRAPH_NO_FIELD, PTS_GRAPH_NO_FIELD_NO_REFINE,
			PTS_GRAPH_NO_REFINE, PTS_LIMIT, K1_LIMIT, K2_LIMIT, K3_LIMIT, ZERO,
			DIRECT_CONNECTIONS, WALA};

		public ObjTreeType objTree = ObjTreeType.PTS_GRAPH;

		/*
		 * takes a list of regular expressions of class names that should be
		 * excluded from the analysis
		 */
		public List<String> exclusions = null;

		public String[] immutables = null;

		public boolean invertExclusion = false;

		public List<String> scopeData = null;

		public String nativesXML = "../jSDG/lib/natives_empty.xml";

		public String outputDir = null;

		public String outputSDGfile = null;

		public String slicingPattern = null;

		public boolean requiredFieldsSet() {
			if (useWalaSdg != (objTree == ObjTreeType.WALA)) {
				Log.warn("use wala sdg setting is not in sync with objtree type. Fix: adapted to objtree type.");
				useWalaSdg = (objTree == ObjTreeType.WALA);
			}

			return classpath != null && mainClass != null && pointsTo != null
				&& maxNumberOfThreads >= 1
				&& ((objTree == ObjTreeType.WALA && useWalaSdg) || (objTree != ObjTreeType.WALA && !useWalaSdg))
				&& scopeData != null && (logFile == null || logFile.length() > 0);
		}

		public boolean exceptionStubs = false;

		public boolean ignoreExceptions = false;

		/**
		 * Do an intraprocedural analysis for exceptions that may not occur. E.g. null-pointer on this access...
		 * This should enhance precision, when exceptions are turned on.
		 */
		public boolean optimizeExceptions = false;

		public boolean nonTermination = false;

		public boolean simpleDataDependency = false;

		public boolean useWalaSdg = false;

		public boolean useDemandPts = false;

		public boolean addControlFlow = true;

		public boolean computeInterference = true;

		public boolean computeSummaryEdges = true;

		public boolean useSummaryOpt = false;

		public boolean interferenceOptimizeThisAccess = false;

		public boolean interferenceNoClinits = false;

		public boolean interferenceUseEscape = true;

		public boolean useJoanaCompiler = false;

		public boolean createVariableMaps = false;

		public int maxNumberOfThreads = 1;

		public int optimizeCg = -1;

		public String toString() {
			StringBuilder str = new StringBuilder();
			str.append(Options.CLASSPATH).append(' ').append(classpath);
			str.append('\n').append(Options.MAIN_CLASS).append(' ').append(mainClass);
			if (scopeData != null) {
				for (String scope : scopeData) {
					str.append('\n').append(Options.SCOPE).append(' ').append(scope);
				}
			}
			if (nativesXML != null) {
				str.append('\n').append(Options.NATIVES_XML).append(' ').append(nativesXML);
			}
			str.append('\n').append(Options.LOG_FILE).append(' ').append(logFile);
			str.append('\n').append(Options.POINTSTO).append(' ').append(pointsTo);
			str.append('\n').append(Options.OBJ_TREE).append(' ').append(objTree);
			if (exclusions != null) {
				for (String excl : exclusions) {
					str.append('\n').append(Options.EXCLUDE).append(' ').append(excl);
				}
			}
			str.append('\n').append(Options.MAX_THREADS).append(' ').append(maxNumberOfThreads);
			str.append('\n').append(Options.INVERT_EXCLUSION).append(' ').append(invertExclusion);
			str.append('\n').append(Options.OPTIMIZE_IMMUTABLES).append(' ').append(!(immutables == null || immutables.length == 0));
			str.append('\n').append(Options.EXCEPTION_STUBS).append(' ').append(exceptionStubs);
			str.append('\n').append(Options.IGNORE_EXCEPTIONS).append(' ').append(ignoreExceptions);
			str.append('\n').append(Options.OPTIMIZE_EXCEPTIONS).append(' ').append(optimizeExceptions);
			str.append('\n').append(Options.NON_TERMINATION).append(' ').append(nonTermination);
			str.append('\n').append(Options.SIMPLE_DATA_DEP).append(' ').append(simpleDataDependency);
			str.append('\n').append(Options.CONTROL_FLOW).append(' ').append(addControlFlow);
			str.append('\n').append(Options.SUMMARY_EDGES).append(' ').append(computeSummaryEdges);
			str.append('\n').append(Options.SUMMARY_OPT).append(' ').append(useSummaryOpt);
			str.append('\n').append(Options.INTERFERENCE).append(' ').append(computeInterference);
			str.append('\n').append(Options.INTERFERENCE_OPT_THIS).append(' ').append(interferenceOptimizeThisAccess);
			str.append('\n').append(Options.INTERFERENCE_NO_CLINITS).append(' ').append(interferenceNoClinits);
			str.append('\n').append(Options.INTERFERENCE_USE_ESCAPE).append(' ').append(interferenceUseEscape);
			str.append('\n').append(Options.WALA_SDG).append(' ').append(useWalaSdg);
			str.append('\n').append(Options.DEMAND_PTS).append(' ').append(useDemandPts);
			str.append('\n').append(Options.OUTPUT_DIR).append(' ').append(outputDir);
			str.append('\n').append(Options.SDG_FILE).append(' ').append(outputSDGfile);
			if (slicingPattern != null) {
				str.append('\n').append(Options.SLICING_PATTERN).append(' ').append(slicingPattern);
			}
			str.append('\n').append(Options.JOANA_COMPILER).append(' ').append(useJoanaCompiler);
			str.append('\n').append(Options.LOG_LEVEL).append(' ').append(logLevel);
			str.append('\n').append(Options.OPTIMIZE_CG).append(' ').append(optimizeCg);

			return str.toString();
		}
	}

	@SuppressWarnings("all")
	private Date initSDGcomputation(Config cfg) throws FileNotFoundException {
		if (cfg == null || !cfg.requiredFieldsSet()) {
			throw new IllegalArgumentException("Configuration is not valid: " + cfg);
		}

		File outputDir = new File(cfg.outputDir);
		if (!outputDir.exists()) {
			outputDir.mkdir();
		}

		Log.setLogFile(cfg.logFile);
		if (cfg.logLevel != null) {
			Log.setMinLogLevel(cfg.logLevel);
		}

		Date date = new Date();
		Log.info("Starting Analysis at " + date);

		boolean assertions = false;
		assert (assertions = true);
		Log.info("Assertions are turned " + (assertions ? "ON" : "OFF"));
		Log.info("Java Datamodel: " + System.getProperty("sun.arch.data.model") + "bit");
		Runtime run = Runtime.getRuntime();
		Log.info("Avaliable Processors: " + run.availableProcessors());
		Log.info("Free Memory: " + run.freeMemory());
		Log.info("Total Memory: " + run.totalMemory());
		Log.info("Maximum Memory: " + run.maxMemory());
		Log.info(Debug.getSettings());

		Log.info("SDGFactory.getSDG started with: \n" + cfg);

		return date;
	}

	/* ****************************** */
	private SDG sdg;
	public SDG getRawSDG() { return sdg; }
	/* ****************************** */

	@SuppressWarnings("unused")
	private static boolean assertVerify(edu.kit.joana.ifc.sdg.graph.SDG joanaSdg, boolean directConnectedClinits, boolean hasControlFlow) {
        Log.appendInfo("begin verification ... ");
    	SDGVerifier.verify(joanaSdg, directConnectedClinits, hasControlFlow);
        Log.appendInfo("done\n");

		return true;
	}

	public final edu.kit.joana.ifc.sdg.graph.SDG getJoanaSDG(Config cfg, final IProgressMonitor progress)
	throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		final Date start = initSDGcomputation(cfg);

		edu.kit.joana.ifc.sdg.graph.SDG joanaSdg = null;

		if (cfg.useWalaSdg) {
			com.ibm.wala.ipa.slicer.SDG<InstanceKey> walaSDG = computeWalaSDG(cfg, progress);

			WalaConverter conf = new WalaConverter(walaSDG, cfg, k2o);
			joanaSdg = conf.convertToJoanaSDG(progress);
		} else {
			sdg = getOrigSDG(cfg, progress);

            joanaSdg = JoanaStyleSDG.createJoanaSDG(sdg, cfg.addControlFlow, cfg.nonTermination, cfg.useSummaryOpt, progress);
   		}

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

        // as long as we can not cope with nodes that do not belong to the control flow we do this...
        JoanaCFGSanitizer.sanitizeCFG(joanaSdg);

//        assert assertVerify(joanaSdg, !cfg.useWalaSdg, cfg.addControlFlow);

        final Date beforeThreadAllocation = new Date();

        if (cfg.computeInterference) {
			progress.beginTask("Creating cSDG from SDG " + cfg.outputSDGfile, -1);

			progress.subTask("Running Thread Allocation Analysis");
			Log.info("Running Thread Allocation Analysis");

			joanaSdg = CSDGPreprocessor.createCSDG(joanaSdg, progress);

			Log.info("Thread Allocation done.");
	        progress.done();
        }

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

        final Date beforeSummaryEdge = new Date();

        if (cfg.computeSummaryEdges) {
            progress.subTask("Compute Summary Edges");
            Log.info("Compute Summary Edges");
            SummaryEdgeComputation.compute(joanaSdg, progress);

            Log.info("Summary Edges done.");
            progress.done();
        }

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

        final Date end = new Date();

        long start2end = end.getTime() - start.getTime();
        long summary2end = end.getTime() - beforeSummaryEdge.getTime();
        long start2thread = beforeThreadAllocation.getTime() - start.getTime();
        long threadAlloc = beforeSummaryEdge.getTime() - beforeThreadAllocation.getTime();

        Log.info("Start 2 End: " + start2end / 1000 + "s (" + start2end + "ms)");
        Log.info("Create: " + start2thread / 1000 + "s (" + start2thread + "ms)");
        Log.info("Summary: " + summary2end / 1000 + "s (" + summary2end + "ms)" + (cfg.computeSummaryEdges ? "" : " [deactivated]"));
        Log.info("Thread: " + threadAlloc / 1000 + "s (" + threadAlloc + "ms)" + (cfg.computeInterference ? "" : " [deactivated]"));

        return joanaSdg;
	}

	public final SDG getJSDG(Config cfg, IProgressMonitor progress)
	throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		initSDGcomputation(cfg);

		if (cfg.useWalaSdg) {
			throw new IllegalArgumentException("Configuration is set to create WALA sdg: " + cfg);
		}


		SDG sdg = getOrigSDG(cfg, progress);

	    if (cfg.nonTermination) {
	    	progress.beginTask("Compute interprocedural nontermination sensitive control dependencies", -1);
	    	NonTerminationSensitive.run(sdg, progress);
	    	progress.done();
	    }

		return sdg;
	}

	public final com.ibm.wala.ipa.slicer.SDG<InstanceKey> getWalaSDG(Config cfg, IProgressMonitor progress)
	throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		initSDGcomputation(cfg);

		if (!cfg.useWalaSdg) {
			throw new IllegalArgumentException("Configuration is not set to create WALA sdg: " + cfg);
		}

		if (cfg.nonTermination) {
			throw new IllegalArgumentException("Nontermination sensitive control dependencies are not supported in the WALA sdg");
		}

		return computeWalaSDG(cfg, progress);
	}

	private SDGBuildPreparation.Config translateConfig(Config cfg) {
		final SDGBuildPreparation.Config ncfg = new SDGBuildPreparation.Config("tranlated from jsdg of " + cfg.mainClass);
		ncfg.accessPath = false;
		ncfg.cgConsumer = null;
		ncfg.classpath = cfg.classpath;
		ncfg.computeAllocationSites = false;
		ncfg.computeInterference = cfg.computeInterference;
		ncfg.computeSummaryEdges = cfg.computeSummaryEdges;
		ncfg.ctxSelector = null;
		ncfg.ddisp = null;
		ncfg.debugManyGraphsDotOutput = false;
		ncfg.defaultExceptionMethodState = null;
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(cfg.mainClass.substring(1));
		ncfg.entryMethod = mainMethod.toBCString();
		ncfg.exceptions = (cfg.ignoreExceptions ? ExceptionAnalysis.IGNORE_ALL : 
			(cfg.optimizeExceptions ? ExceptionAnalysis.INTRAPROC : ExceptionAnalysis.ALL_NO_ANALYSIS));
		
		final StringBuilder excl = new StringBuilder();
		if (cfg.exclusions != null) {
			for (final String e : cfg.exclusions) {
				excl.append(e + "\n");
			}
		}
		ncfg.exclusions = excl.toString();
		
		ncfg.extern = null;
		switch (cfg.pointsTo) {
		case n0CFA:
			ncfg.pts = PointsToPrecision.TYPE_BASED;
			break;
		case n1CFA:
			ncfg.pts = PointsToPrecision.N1_CALL_STACK;
			break;
		case n2CFA:
			ncfg.pts = PointsToPrecision.N2_CALL_STACK;
			break;
		case n3CFA:
			ncfg.pts = PointsToPrecision.N3_CALL_STACK;
			break;
		case OBJ_SENS:
			ncfg.pts = PointsToPrecision.OBJECT_SENSITIVE;
			break;
		case VANILLA_ZERO_ONE_CFA:
			ncfg.pts = PointsToPrecision.INSTANCE_BASED;
			break;
		case ZERO_ONE_CFA:
			ncfg.pts = PointsToPrecision.INSTANCE_BASED;
			break;
		case VANILLA_ZERO_ONE_CONTAINER_CFA:
			ncfg.pts = PointsToPrecision.INSTANCE_BASED;
			break;
		case ZERO_CFA:
			ncfg.pts = PointsToPrecision.TYPE_BASED;
			break;
		}
		
//		ncfg.fieldPropagation =
		ncfg.nativesXML = cfg.nativesXML;
//		ncfg.objSensFilter =
		ncfg.sideEffects = null;

		ncfg.stubs = new String[cfg.scopeData.size()];
		int index = 0;
		for (final String sc : cfg.scopeData) {
			final String stub = sc.substring(sc.lastIndexOf(",") + 1);
			ncfg.stubs[index] = stub;
			index++;
		}
		//ncfg.stubs = 
		
		ncfg.thirdPartyLibPath = null;
		
		return ncfg;
	}

	private SSAPropagationCallGraphBuilder makeBuilder(final SDGBuildPreparation.Config cfg) throws ClassHierarchyException, IOException, CancelException {
		System.out.println();
		System.out.println("----- build config deprecated jsdg -----");
		System.out.println(cfg);
		System.out.println("===== build config deprecated jsdg =====");
		System.out.println();
		final Pair<Long, SDGBuilder.SDGBuilderConfig> prep = SDGBuildPreparation.prepareBuild(System.out, cfg, NullProgressMonitor.INSTANCE);
		final SDGBuilder sdgb = SDGBuilder.onlyCreate(prep.snd);
		final CallGraphBuilder<InstanceKey> cgb = sdgb.createCallgraphBuilder(NullProgressMonitor.INSTANCE);
		return (SSAPropagationCallGraphBuilder) cgb;
	}
	
	private SDG getOrigSDG(Config cfg, IProgressMonitor progress)
	throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		final SDGBuildPreparation.Config pcfg = translateConfig(cfg);
		
//		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);

		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		//AnalysisScope scope = SDGBuildPreparation.setUpAnalysisScope(System.out, pcfg);
//		ClassLoader loader = getClass().getClassLoader();
//		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
			//AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile, cfg.classpath, null);
		progress.done();

//		ClassHierarchy cha = ClassHierarchy.make(scope, progress);
//
//		Iterable<Entrypoint> entrypoints =
//			com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, cfg.mainClass);
//		ExtendedAnalysisOptions options = new ExtendedAnalysisOptions(scope, entrypoints);
//	    AnalysisCache cache = new AnalysisCache();

	    progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
	    
//		SSAPropagationCallGraphBuilder builder = getCallGraphBuilder(cfg.pointsTo, options, cache, cha, scope);
	    final SSAPropagationCallGraphBuilder builder = makeBuilder(pcfg);
	    final AnalysisOptions options = builder.getOptions();
	    final AnalysisScope scope = options.getAnalysisScope();
	    IAnalysisCacheView cache = builder.getAnalysisCache();
	    final List<Entrypoint> eps = new LinkedList<>();
	    for (final Entrypoint ep : options.getEntrypoints()) {
	    	eps.add(ep);
	    }
	    final Iterable<Entrypoint> entrypoints = eps;

		/**
		 * Change the wala internal pointer and instancekeyfactory of the
		 * callgraph builder to our adapter. So we can keep track of the created
		 * InstanceKeys and PointerKeys. This information is used later on
		 * when creating subobject trees for accessed field variables.
		 */
		InstanceAndPointerKeyFactoryAdapter adapter = null;
		InstanceKeyFactory ikFact = builder.getInstanceKeys();
		PointerKeyFactory pkFact = builder.getPointerKeyFactory();
		adapter = new InstanceAndPointerKeyFactoryAdapter(ikFact, pkFact);

		k2o = adapter;

		builder.setInstanceKeys(adapter);
		builder.setPointerKeyFactory(adapter);

		progress.done();

		progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph")); //$NON-NLS-1$
		CallGraph cg = builder.makeCallGraph(options, progress);

		if (cfg.optimizeCg >= 0) {
			CallGraphPruning opt = new CallGraphPruning(cg);
			System.out.println("Call Graph has " + cg.getNumberOfNodes() + " Nodes.");
			Set<CGNode> sopt = opt.findApplicationNodes(cfg.optimizeCg);
			cg = new PrunedCallGraph(cg, sopt);
			System.out.println("Optimized Call Graph has " + cg.getNumberOfNodes() + " Nodes.");
		}
		System.out.println("\ncall graph has " + cg.getNumberOfNodes() + " nodes.");
		
		if (Debug.Var.DUMP_CALLGRAPH.isSet()) {
			Util.dumpCallGraph(cg, cfg.mainClass.replace('/','.').substring(1), progress);
		}

		if (Debug.Var.DUMP_HEAP_GRAPH.isSet()) {
			PointerAnalysis<InstanceKey> pta = builder.getPointerAnalysis();
			HeapGraph<InstanceKey> hg = pta.getHeapGraph();
			Util.dumpHeapGraph(cfg.mainClass.replace('/','.').substring(1) +
				"." + cfg.pointsTo, hg, null);
		}

		PointerAnalysis<InstanceKey> pta = builder.getPointerAnalysis();
		progress.done();

		DemandRefinementPointsTo demandPts = null;
		if (cfg.useDemandPts) {
			throw new UnsupportedOperationException();
//		    MemoryAccessMap mam = new PABasedMemoryAccessMap(cg, builder.getPointerAnalysis());
//			demandPts = new DemandRefinementPointsTo(cg,
//				new ThisFilteringHeapModel(builder,cha), mam, cha, options, getStateMachineFactory());
		}

		IPointerAnalysis pts = new PointsToWrapper(demandPts, pta);

		progress.subTask(Messages.getString("Analyzer.SubTask_Search_Main")); //$NON-NLS-1$
		IMethod main = edu.kit.joana.deprecated.jsdg.util.Util.searchMethod(entrypoints, "main([Ljava/lang/String;)V"); //$NON-NLS-1$
		progress.done();

		progress.done();

		SDG sdg = SDG.create(main, cg, cache, adapter, pts, cfg,  progress);
		sdg.setAnalysisScope(scope);
		sdg.setPointerAnalysis(pta);

		progress.done();


		if (Debug.Var.PRINT_FIELD_PTS_INFO.isSet()) {
			Log.info("search for field allocs called " + PDG.searchFieldAllocs + " times.");
		}

		if (Debug.Var.PRINT_UNRESOLVED_CLASSES.isSet()) {
			for (TypeReference tRef : cg.getClassHierarchy().getUnresolvedClasses()) {
				Log.warn("Could not resolve: " + Util.typeName(tRef.getName()));
			}
		}

		return sdg;
	}

	protected StateMachineFactory<IFlowLabel> getStateMachineFactory() {
		return new ContextSensitiveStateMachine.Factory();
	}

	private IKey2Origin k2o;

	private com.ibm.wala.ipa.slicer.SDG<InstanceKey> computeWalaSDG(Config cfg, IProgressMonitor progress)
	throws ClassHierarchyException, IllegalArgumentException, CancelException, PDGFormatException, IOException, InvalidClassFileException {
		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);


		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		ClassLoader loader = getClass().getClassLoader();
		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
			//AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile, cfg.classpath, null);
		progress.done();

		ClassHierarchy cha = ClassHierarchyFactory.make(scope, progress);

		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.
			Util.makeMainEntrypoints(scope, cha, cfg.mainClass);
		ExtendedAnalysisOptions options = new ExtendedAnalysisOptions(scope, entrypoints);
	    AnalysisCache cache = new AnalysisCacheImpl();

	    progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
		SSAPropagationCallGraphBuilder builder =
			getCallGraphBuilder(cfg.pointsTo, options, cache, cha, scope);

		/**
		 * Change the wala internal pointer and instancekeyfactory of the
		 * callgraph builder to our adapter. So we can keep track of the created
		 * InstanceKeys and PointerKeys. This information is used later on
		 * when creating subobject trees for accessed field variables.
		 */
		InstanceAndPointerKeyFactoryAdapter adapter = null;
		InstanceKeyFactory ikFact = builder.getInstanceKeys();
		PointerKeyFactory pkFact = builder.getPointerKeyFactory();
		adapter = new InstanceAndPointerKeyFactoryAdapter(ikFact, pkFact);

		k2o = adapter;

		builder.setInstanceKeys(adapter);
		builder.setPointerKeyFactory(adapter);

		progress.done();

		progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph")); //$NON-NLS-1$
		CallGraph cg = builder.makeCallGraph(options, progress);
		PointerAnalysis<InstanceKey> pta = builder.getPointerAnalysis();
		progress.done();

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

		progress.done();

		progress.beginTask(Messages.getString("SDG.Task_Create_WALA_SDG"), -1); //$NON-NLS-1$

		DataDependenceOptions dOpt;
		ControlDependenceOptions cOpt;
		if (cfg.objTree == ObjTreeType.ZERO) {
			if (cfg.ignoreExceptions) {
				dOpt = DataDependenceOptions.NO_HEAP_NO_EXCEPTIONS;
				cOpt = ControlDependenceOptions.NO_EXCEPTIONAL_EDGES;
			} else {
				dOpt = DataDependenceOptions.NO_HEAP;
				cOpt = ControlDependenceOptions.FULL;
			}
		} else {
			if (cfg.ignoreExceptions) {
				dOpt = DataDependenceOptions.FULL;
				cOpt = ControlDependenceOptions.NO_EXCEPTIONAL_EDGES;
			} else {
				dOpt = DataDependenceOptions.FULL;
				cOpt = ControlDependenceOptions.FULL;
			}
		}

		Log.info("Wala SDG using cDeps: " + cOpt + " dDeps: " + dOpt);

		com.ibm.wala.ipa.slicer.SDG<InstanceKey> sdg =
			new com.ibm.wala.ipa.slicer.SDG<>(cg, pta, dOpt, cOpt);

		progress.done();

		return sdg;
	}

	@SuppressWarnings("all")
	public final PDG getPDG(Config cfg, String methodName, IProgressMonitor progress)
	throws ClassHierarchyException, IllegalArgumentException, CancelException, PDGFormatException, IOException, InvalidClassFileException {
		if (cfg == null || !cfg.requiredFieldsSet()) {
			throw new IllegalArgumentException("Configuration is not valid: " + cfg);
		}


		Log.setLogFile(cfg.logFile);
		if (cfg.logLevel != null) {
			Log.setMinLogLevel(cfg.logLevel);
		}

		Date date = new Date();
		Log.info("Starting Analysis at " + date);

		boolean assertions = false;
		assert (assertions = true);
		Log.info("Assertions are turned " + (assertions ? "ON" : "OFF"));
		Log.info("Java Datamodel: " + System.getProperty("sun.arch.data.model") + "bit");
		Runtime run = Runtime.getRuntime();
		Log.info("Avaliable Processors: " + run.availableProcessors());
		Log.info("Free Memory: " + run.freeMemory());
		Log.info("Total Memory: " + run.totalMemory());
		Log.info("Maximum Memory: " + run.maxMemory());
		Log.info(Debug.getSettings());
		Log.info("SDGFactory.getPDG(" + methodName + ") started with: \n" + cfg);

		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);

		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		ClassLoader loader = getClass().getClassLoader();
		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
			//AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile, cfg.classpath, null);

		ClassHierarchy cha = ClassHierarchyFactory.make(scope, progress);

		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.
			Util.makeMainEntrypoints(scope, cha, cfg.mainClass);
		ExtendedAnalysisOptions options = new ExtendedAnalysisOptions(scope, entrypoints);
	    AnalysisCache cache = new AnalysisCacheImpl();

	    progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
		CallGraphBuilder builder =
			getCallGraphBuilder(cfg.pointsTo, options, cache, cha, scope);

		/**
		 * Change the wala internal pointer and instancekeyfactory of the
		 * callgraph builder to our adapter. So we can keep track of the created
		 * InstanceKeys and PointerKeys. This information is used later on
		 * when creating subobject trees for accessed field variables.
		 */
		InstanceAndPointerKeyFactoryAdapter adapter = null;
		if (builder instanceof PropagationCallGraphBuilder) {
			PropagationCallGraphBuilder pb = (PropagationCallGraphBuilder) builder;
			InstanceKeyFactory ikFact = pb.getInstanceKeys();
			PointerKeyFactory pkFact = pb.getPointerKeyFactory();
			adapter = new InstanceAndPointerKeyFactoryAdapter(ikFact, pkFact);

			pb.setInstanceKeys(adapter);
			pb.setPointerKeyFactory(adapter);
		}

		progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph")); //$NON-NLS-1$
		CallGraph cg = builder.makeCallGraph(options, progress);
		PointerAnalysis pta = builder.getPointerAnalysis();

	    MethodReference mRef = StringStuff.makeMethodReference(methodName);

		/**
		 * Create PDGs for all matching nodes
		 */
	    Set<CGNode> nodes = cg.getNodes(mRef);
		if (nodes == null || nodes.isEmpty()) {
			Log.warn("Could not find a method named " + methodName);
			throw new IllegalStateException("Could not find a method named " + methodName);
		}

		progress.subTask(Messages.getString("SDG.SubTask_Create_PDG") +  //$NON-NLS-1$
				mRef);

		Log.info("Creating PDG " + mRef); //$NON-NLS-1$

		IParamComputation pcomp = Util.getParamComputation(cfg);

		IPointerAnalysis pts = new PointsToWrapper(null, pta);

		CGNode cgnode = nodes.iterator().next();
		if (nodes.size() > 1) {
			System.err.println("Multiple instances found for method " + mRef);
			System.err.println("Using first one found...: " + cgnode);
		}

		ExceptionPrunedCFGAnalysis<SSAInstruction, IExplodedBasicBlock> epa = null;
		if (cfg.optimizeExceptions && !cfg.ignoreExceptions) {
//			System.err.println("Exception analysis removed for distribution.");
			epa = NullPointerAnalysis.createIntraproceduralExplodedCFGAnalysis();
		}

		PDG pdg = PDG.create(progress, cgnode, 1, pts, adapter, cg, cfg.ignoreExceptions, epa, pcomp);

		progress.done();

		return pdg;
	}

	public final static SSAPropagationCallGraphBuilder getCallGraphBuilder(PointsToType type,
			ExtendedAnalysisOptions options, AnalysisCache cache, IClassHierarchy cha,
			AnalysisScope scope) {
		SSAPropagationCallGraphBuilder cg = null;

		switch (type) {
		/*
		case RTA:
			cg = com.ibm.wala.ipa.callgraph.impl.
				PrettyWalaNames.makeRTABuilder(options, cache, cha, scope);
			break; */
		case ZERO_CFA:
			cg = (SSAPropagationCallGraphBuilder) WalaPointsToUtil.makeContextFreeType(options, cache, cha, scope); 
//				com.ibm.wala.ipa.callgraph.impl.
//				Util.makeZeroCFABuilder(options, cache, cha, scope);
			break;
		case ZERO_ONE_CFA:
			cg = (SSAPropagationCallGraphBuilder) WalaPointsToUtil.makeContextSensSite(options, cache, cha, scope); 
//			
//			com.ibm.wala.ipa.callgraph.impl.
//				Util.makeZeroOneCFABuilder(options, cache, cha, scope);
			break;
		case VANILLA_ZERO_ONE_CFA:
			cg = com.ibm.wala.ipa.callgraph.impl.
				Util.makeVanillaZeroOneCFABuilder(options, cache, cha, scope);
			break;
		case OBJ_SENS:
			cg = (SSAPropagationCallGraphBuilder) WalaPointsToUtil.makeObjectSens(options, cache, cha, scope);
			
			//makeObjSensZeroXCFABuilder(options, cache, cha, scope,
				//	new ObjSensContextSelector(), new DefaultSSAInterpreter(options, cache));
			break;
		case VANILLA_ZERO_ONE_CONTAINER_CFA:
			cg = com.ibm.wala.ipa.callgraph.impl.
				Util.makeVanillaZeroOneContainerCFABuilder(options, cache, cha, scope);
			break;

		case n0CFA:
			cg = makeNCFA(0, options, cache, cha, scope);
			break;
		case n1CFA:
			cg = makeNCFA(1, options, cache, cha, scope);
			break;
		case n2CFA:
			cg = makeNCFA(2, options, cache, cha, scope);
			break;
		case n3CFA:
			cg = makeNCFA(3, options, cache, cha, scope);
			break;
		}

		return cg;
	}

	  /**
		 * @param options
		 *            options that govern call graph construction
		 * @param cha
		 *            governing class hierarchy
		 * @param scope
		 *            representation of the analysis scope
		 * @param customSelector
		 *            user-defined context selector, or null if none
		 * @param customInterpreter
		 *            user-defined context interpreter, or null if none
		 * @return a 0-1-CFA Call Graph Builder.
		 * @throws IllegalArgumentException
		 *             if options is null
		 */
	public static SSAPropagationCallGraphBuilder makeObjSensZeroXCFABuilder(
			AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha,
			AnalysisScope scope, ContextSelector customSelector,
			SSAContextInterpreter customInterpreter) {

		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		addDefaultSelectors(options, cha);
		addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

		return ObjSensZeroXCFABuilder.make(cha, options, cache, customSelector, customInterpreter,
				ZeroXInstanceKeys.ALLOCATIONS |
                ZeroXInstanceKeys.CONSTANT_SPECIFIC |
                ZeroXInstanceKeys.SMUSH_MANY |
                ZeroXInstanceKeys.SMUSH_THROWABLES);
	}


	private static final nCFABuilder makeNCFA(int n, AnalysisOptions options,
			AnalysisCache cache, IClassHierarchy cha, AnalysisScope scope) {
		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		addDefaultSelectors(options, cha);
		addDefaultBypassLogic(options, scope, Util.class.getClassLoader(), cha);

		ContextSelector appSelector = null;
		SSAContextInterpreter appInterpreter = null;

		return new nCFABuilder(n, cha, options, cache, appSelector, appInterpreter);
	}

	/**
	 * Set up an AnalysisOptions object with default selectors, corresponding to
	 * class hierarchy lookup
	 *
	 * @throws IllegalArgumentException
	 *             if options is null
	 */
	public static void addDefaultSelectors(AnalysisOptions options,
			IClassHierarchy cha) {
		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		options.setSelector(new ClassHierarchyMethodTargetSelector(cha));
		options.setSelector(new ClassHierarchyClassTargetSelector(cha));
	}

	public static void addDefaultBypassLogic(AnalysisOptions options,
			AnalysisScope scope, ClassLoader cl, IClassHierarchy cha) {
		final String nativeSpec = com.ibm.wala.ipa.callgraph.impl.Util
				.getNativeSpec();
		if (cl.getResourceAsStream(nativeSpec) != null) {
			addBypassLogic(options, scope, cl, nativeSpec, cha);
		} else {
			// try to load from filesystem
			try {
				BufferedInputStream bIn = new BufferedInputStream(
						new FileInputStream(nativeSpec));
				XMLMethodSummaryReader reader = new XMLMethodSummaryReader(bIn,
						scope);
				addBypassLogic(options, scope, cl, reader, cha);
			} catch (FileNotFoundException e) {
				System.err.println("Could not load natives xml file from: "
						+ nativeSpec);
				e.printStackTrace();
			}
		}
	}

	/**
	 * Modify an options object to include bypass logic as specified by a an XML
	 * file.
	 *
	 * @throws IllegalArgumentException
	 *             if scope is null
	 * @throws IllegalArgumentException
	 *             if cl is null
	 * @throws IllegalArgumentException
	 *             if options is null
	 * @throws IllegalArgumentException
	 *             if scope is null
	 */
	public static void addBypassLogic(AnalysisOptions options,
			AnalysisScope scope, ClassLoader cl, String xmlFile,
			IClassHierarchy cha) throws IllegalArgumentException {
		if (scope == null) {
			throw new IllegalArgumentException("scope is null");
		}
		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		if (cl == null) {
			throw new IllegalArgumentException("cl is null");
		}
		if (cha == null) {
			throw new IllegalArgumentException("cha cannot be null");
		}

		InputStream s = cl.getResourceAsStream(xmlFile);
		XMLMethodSummaryReader summary = new XMLMethodSummaryReader(s, scope);

		addBypassLogic(options, scope, cl, summary, cha);
	}

	public static void addBypassLogic(AnalysisOptions options,
			AnalysisScope scope, ClassLoader cl,
			XMLMethodSummaryReader summary, IClassHierarchy cha)
			throws IllegalArgumentException {
		if (scope == null) {
			throw new IllegalArgumentException("scope is null");
		}
		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		if (cl == null) {
			throw new IllegalArgumentException("cl is null");
		}
		if (cha == null) {
			throw new IllegalArgumentException("cha cannot be null");
		}

		MethodTargetSelector ms = new BypassMethodTargetSelector(options
				.getMethodTargetSelector(), summary.getSummaries(), summary
				.getIgnoredPackages(), cha);
		options.setSelector(ms);

		ClassTargetSelector cs = new BypassClassTargetSelector(options
				.getClassTargetSelector(), summary.getAllocatableClasses(),
				cha, cha.getLoader(scope.getLoader(Atom
						.findOrCreateUnicodeAtom("Synthetic"))));
		options.setSelector(cs);
	}

	public final void runExtractImmutables(Config cfg, IProgressMonitor progress)
	throws IOException, ClassHierarchyException, IllegalArgumentException, CallGraphBuilderCancelException, InvalidClassFileException {
		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);

		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		ClassLoader loader = getClass().getClassLoader();
		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
			//AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile, cfg.classpath, null);
		progress.done();

		ClassHierarchy cha = ClassHierarchyFactory.make(scope, progress);

		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.
			Util.makeMainEntrypoints(scope, cha, cfg.mainClass);
		ExtendedAnalysisOptions options = new ExtendedAnalysisOptions(scope, entrypoints);
	    AnalysisCache cache = new AnalysisCacheImpl();

	    progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
		SSAPropagationCallGraphBuilder builder =
			SDGFactory.getCallGraphBuilder(cfg.pointsTo, options, cache, cha, scope);

		progress.done();

		progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph")); //$NON-NLS-1$
		CallGraph cg = builder.makeCallGraph(options, progress);
		PointerAnalysis<InstanceKey> pta = builder.getPointerAnalysis();
		progress.done();

		ExtractImmutables.getImmutables(cg, cha, pta);
	}


}
