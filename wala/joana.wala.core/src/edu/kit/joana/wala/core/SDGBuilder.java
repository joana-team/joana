/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.

 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.exc.ExceptionPruningAnalysis;
import com.ibm.wala.cfg.exc.InterprocAnalysisResult;
import com.ibm.wala.cfg.exc.NullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.escape.TrivialMethodEscape;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.SubtypesEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.callgraph.pruned.CallGraphPruning;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.callgraph.pruned.PruningPolicy;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.analysis.ExplodedControlFlowGraph;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.SDGConstants;
import edu.kit.joana.util.Config;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.LogUtil;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.CallGraph.CallGraphFilter;
import edu.kit.joana.wala.core.CallGraph.Edge;
import edu.kit.joana.wala.core.CallGraph.Node;
import edu.kit.joana.wala.core.accesspath.APResult;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.core.clinit.StaticInitializers;
import edu.kit.joana.wala.core.interference.Call2ForkConverter;
import edu.kit.joana.wala.core.interference.InterferenceComputation;
import edu.kit.joana.wala.core.interference.InterferenceEdge;
import edu.kit.joana.wala.core.interference.ThreadInformationProvider;
import edu.kit.joana.wala.core.joana.DumpSDG;
import edu.kit.joana.wala.core.joana.JoanaConverter;
import edu.kit.joana.wala.core.killdef.IFieldsMayMod;
import edu.kit.joana.wala.core.killdef.LocalKillingDefs;
import edu.kit.joana.wala.core.killdef.impl.FieldsMayModComputation;
import edu.kit.joana.wala.core.killdef.impl.SimpleFieldsMayMod;
import edu.kit.joana.wala.core.params.FlatHeapParams;
import edu.kit.joana.wala.core.params.SearchFieldsOfPrunedCalls;
import edu.kit.joana.wala.core.params.StaticFieldParams;
import edu.kit.joana.wala.core.params.objgraph.ModRefCandidates;
import edu.kit.joana.wala.core.params.objgraph.ObjGraphParams;
import edu.kit.joana.wala.core.params.objgraph.SideEffectDetectorConfig;
import edu.kit.joana.wala.flowless.util.Util;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import edu.kit.joana.wala.util.EdgeFilter;
import edu.kit.joana.wala.util.WriteGraphToDot;
import edu.kit.joana.wala.util.pointsto.CallGraphBuilderFactory;
import edu.kit.joana.wala.util.pointsto.ExtendedAnalysisOptions;
import edu.kit.joana.wala.util.pointsto.ObjSensZeroXCFABuilder;
import edu.kit.joana.wala.util.pointsto.WalaPointsToUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class SDGBuilder implements CallGraphFilter {

	private final static Logger debug = Log.getLogger(Log.L_WALA_CORE_DEBUG);
	private final static boolean IS_DEBUG = debug.isEnabled();

	public final static int PDG_FAKEROOT_ID = 0;
	public final static int PDG_THREAD_START_ID = 1;
	public final static int PDG_THREAD_RUN_ID = 2;
	// start at 2+1 == 3 because other ids are reserved.
	public final static int PDG_START_ID = 3;
	public final static int NO_PDG_ID = -1;
	public final static boolean DATA_FLOW_FOR_GET_FROM_FIELD_NODE =
			Config.getBool(Config.C_SDG_DATAFLOW_FOR_GET_FROM_FIELD, false);


	public static final TypeReference[] DEFAULT_IGNORE_EXCEPTIONS = {
	    TypeReference.JavaLangOutOfMemoryError,
	    TypeReference.JavaLangExceptionInInitializerError
	};

	public final SDGBuilderConfig cfg;

	public LinkedList<Set<ParameterField>> partitions;

	public static enum ExceptionAnalysis {
		/*
		 * Act as if exceptions can never occur.
		 */
		IGNORE_ALL(true, "ignore all exceptions (may miss information flow)"),
		/*
		 * Assume each instruction that potentially can throw an exception may throw one. 
		 */
		ALL_NO_ANALYSIS(true, "integrate all exceptions without optimization"),
		/*
		 * Like ALL_NO_ANALYSIS but apply an intraprocedural analysis that detects instructions that definitely can not
		 * throw an exception. E.g. access to this pointer, subsequent accesses to the same unchanged object,
		 * if-guarded accesses, etc. 
		 */
		INTRAPROC(true, "integrate all exceptions, optimize intraprocedurally"),
		/*
		 * Like INTRAPROC but extended to an interprocedural analysis.
		 */
		INTERPROC(true, "integrate all exceptions, optimize interprocedurally");

		public final String desc;		  // short textual description of the option - can be used for gui
		public final boolean recommended; // option can make sense aside for academic evaluation

		private ExceptionAnalysis(final boolean recommended, final String desc) {
			this.recommended = recommended;
			this.desc = desc;
		}
	}

	public static enum PointsToPrecision {
		/*
		 * Rapid Type Analysis
		 * Maybe UNSOUND - WALAs implementation looks suspicious.
		 * Its also less precise and slower (blows up dynamic calls) as TYPE (0-CFA).
		 * Its just here for academic purposes.
		 */
		RTA(false, "rapid type analysis"),
		/*
		 * 0-CFA
		 * Fastest option. Use this in case everything else is too slow aka the callgraph is getting too big.
		 */
		TYPE_BASED(true, "type-based (0-CFA)"),
		/* DEFAULT
		 * 0-1-CFA
		 * Best bang for buck. Use this in case you are not sure what to pick.
		 */
		INSTANCE_BASED(true, "instance-based (0-1-CFA)"),
		/*
		 * Object-sensitive (unlimited receiver object context for application code)
		 * Very precise for OO heavy code - best option for really precise analysis.
		 * Unlimited receiver context for application code, 1-level receiver context for library code. 
		 * Uses n-CFA as fallback for static methods. Customizable: Provide objSensFilter to specify 'n' for fallback
		 * n-CFA and filter for methods where object-sensitivity should be engaged. Default 'n = 1'.
		 */
		OBJECT_SENSITIVE(true, "object-sensitive + 1-level call-stack"),
		/*
		 * 1-level object-sensitive (1-level receiver object context)
		 * Receiver context is limited to 1-level. 
		 * Uses 1-CFA as fallback for static methods.
		 */
		N1_OBJECT_SENSITIVE(true, "1-level object-sensitive + 1-level call-stack"),
		/*
		 * Object-sensitive (unlimited receiver object context)
		 * Very precise for OO heavy code, but also very slow.
		 * Unlimited receiver context for the whole code - application as well as library. 
		 * Uses 1-CFA as fallback for static methods.
		 */
		UNLIMITED_OBJECT_SENSITIVE(true, "unlimited object-sensitive + 1-level call-stack"),
		/*
		 * 1-CFA
		 * Slower as 0-1-CFA, yet few precision improvements
		 */
		N1_CALL_STACK(true, "1-level call-stack (1-CFA)"),
		/*
		 * 2-CFA
 		 * Slow, but precise
		 */
		N2_CALL_STACK(true, "2-level call-stack (2-CFA)"),
		/*
		 * 3-CFA
		 * Very slow with little increased precision. Not much improvement over 2-CFA.
		 */
		N3_CALL_STACK(true, "3-level call-stack (3-CFA)"),
		/**
		 * custom call graph builder for testing out experimental features such as IFC-driven
		 * pointer analysis
		 */
		CUSTOM(false, "custom call graph builder - use only if you know what you're doing!");

		public final String desc;		  // short textual description of the option - can be used for gui
		public final boolean recommended; // option can make sense aside for academic evaluation

		private PointsToPrecision(final boolean recommended, final String desc) {
			this.recommended = recommended;
			this.desc = desc;
		}

		public boolean isObjSens() {
			return this == OBJECT_SENSITIVE || this == UNLIMITED_OBJECT_SENSITIVE || this == N1_OBJECT_SENSITIVE;
		}
	}

	public static enum StaticInitializationTreatment {
		/*
		 * Ignore static initializers
		 */
		NONE(true, "ignore static initialization"),
		/* DEFAULT
		 * Assume all static initializers are called once before the program starts. 
		 */
		SIMPLE(true, "simple approximation of static intialization"),
		/*
		 * NOT YET WORKING.
		 * Place calls to static initializers where they may in fact occur.
		 */
		ACCURATE(false, "(unfinished - do not use for now) simple approximation of static intialization");

		public final String desc;		  // short textual description of the option - can be used for gui
		public final boolean recommended; // option can make sense aside for academic evaluation

		private StaticInitializationTreatment(final boolean recommended, final String desc) {
			this.recommended = recommended;
			this.desc = desc;
		}
	}

	public static enum FieldPropagation {
		/*
		 * Very imprecise side-effect computation. Merges all effects/heap locations reachable through a methods
		 * parameter to a single node.
		 * Slow (could be optimized through caching) and imprecise. Do not use unless for academic evaluation purposes.
		 */
		FLAT(true, "flat - merge all reachable locations in single root nodes"),
		/* DEFAULT
		 * A fine-grained side-effect computation. Scales well with precise and imprecise points-to analysis.
		 * This is your best bet in case you don't know what to choose.
		 */
		OBJ_GRAPH(true, "object-graph - precise and fast (default)"),
		/*
		 * Object graph algorithm. Does not merge nodes, when their number is getting
		 * large. Does not merge accesses to the same field.
		 */
		OBJ_GRAPH_NO_FIELD_MERGE(false, "object-graph - fixpoint propagation with no field merging (internal use only)"),
		/*
		 * Object graph algorithm with no merging at all.
		 */
		OBJ_GRAPH_NO_MERGE_AT_ALL(false, "object-graph - no merge at all (internal use only)"),
		/*
		 * Object graph algorithm without speed/space optimization. Does not merge nodes, when their number is getting
		 * large. Does not merge accesses to the same field.
		 */
		OBJ_GRAPH_FIXP_NO_OPT(false, "object-graph - fixpoint propagation with no additional optimizations (internal use only)"),
		/*
		 * Object graph algorithm with simple propagation with no additional optimization.
		 */
		OBJ_GRAPH_SIMPLE_NO_OPT(false, "object-graph - simple propagation with no additional optimization (internal use only)"),
		/*
		 * Object graph algorithm with simple propagation, no optimizations and no additional separate escape analysis.
		 * Do not choose if you don't know what it does. It exists for academic evaluation purposes.
		 */
		OBJ_GRAPH_SIMPLE_NO_OPT_NO_ESCAPE(false, "object-graph - with simple propagation and no additional escape analysis (internal use only)"),
		/*
		 * Object graph algorithm with fixpoint propagation. Do not choose if you don't know what it does. It
		 * exists for academic evaluation purposes.
		 */
		OBJ_GRAPH_FIXPOINT_PROPAGATION(false, "object-graph - with fixpoint propagation (internal use only)"),
		/*
		 * Use object graph algorithm, but do not apply an escape analysis. This increases runtime and space needed,
		 * while the precision is decreased. No sane person would choose this option. It exists only to evaluate the
		 * effect of the integrated escape analysis.
		 */
		OBJ_GRAPH_NO_ESCAPE(false, "object-graph - without escape analysis (internal use only)"),
		/*
		 * Run object graph algorithm without any optimizations. Again do not choose if you don't know what this means.
		 * Only for evaluation.
		 */
		OBJ_GRAPH_NO_OPTIMIZATION(false, "object-graph - without optimization (internal use only)"),
		/*
		 * Object graph algorithm with a simple propagation. Should be faster (and less precise) as the fixpoint
		 * propagation. Do not choose if you don't know what it does. It exists for academic evaluation purposes.
		 */
		OBJ_GRAPH_SIMPLE_PROPAGATION(false, "object-graph - with simple propagation (internal use only)"),
		/*
		 * Old deprecated object tree algorithm. The predecessor of the object graph. Scales less well for imprecise
		 * points-to analysis, Is overall slower. Kept around for evaluation purposes. 
		 */
		OBJ_TREE(false, "object-tree - old tree-based propagation (internal use only)"),
		/*
		 * Special variant of object-tree for access paths. 
		 */
		OBJ_TREE_AP(false, "object-tree - old tree-based propagation for access paths (internal use only)"),
		/*
		 * A special variant of the object tree algorithm that allows multiple nodes for a single field. A little bit
		 * more precise and even slower. Again kept around for evaluation purposes.
		 */
		OBJ_TREE_NO_FIELD_MERGE(false, "object-tree - old tree-based propagation without field merge (internal use only)");

		public final String desc;		  // short textual description of the option - can be used for gui
		public final boolean recommended; // option can make sense aside for academic evaluation

		private FieldPropagation(final boolean recommended, final String desc) {
			this.recommended = recommended;
			this.desc = desc;
		}
	}

	/**
	 * This controls dynamic dispatch is handled in terms of adding control dependencies for
	 * virtual calls.
	 * @author Martin Mohr
	 */
	public static enum DynamicDispatchHandling {
		/**
		 * always add control dependencies for virtual calls
		 */
		SIMPLE,
		/**
		 * add control dependency for a virtual call only if its target
		 * is not unique.
		 */
		PRECISE,
		/**
		 * never add control dependencies for virtual calls
		 */
		IGNORE;
	};

	public static SDGBuilder onlyCreate(final SDGBuilderConfig cfg) throws CancelException {
		SDGBuilder builder = new SDGBuilder(cfg);
		return builder;
	}
	public static SDGBuilder create(final SDGBuilderConfig cfg) throws UnsoundGraphException, CancelException {
		IProgressMonitor progress = NullProgressMonitor.INSTANCE;

		SDGBuilder builder = new SDGBuilder(cfg);
		builder.run(progress);

		return builder;
	}

	public static SDGBuilder create(final SDGBuilderConfig cfg, final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		SDGBuilder builder = new SDGBuilder(cfg);
		builder.run(progress);

		return builder;
	}


	public static SDGBuilder create(final SDGBuilderConfig cfg, final com.ibm.wala.ipa.callgraph.CallGraph walaCG,
			final PointerAnalysis<InstanceKey> pts) throws UnsoundGraphException, CancelException {
		IProgressMonitor progress = NullProgressMonitor.INSTANCE;

		SDGBuilder builder = new SDGBuilder(cfg);
		builder.run(walaCG, pts, progress);

		return builder;
	}

	public static SDG build(final SDGBuilderConfig cfg, final com.ibm.wala.ipa.callgraph.CallGraph walaCG,
			final PointerAnalysis<InstanceKey> pts) throws UnsoundGraphException, CancelException {
		SDG sdg = null;
		WorkPackage pack = null;
		IProgressMonitor progress = NullProgressMonitor.INSTANCE;

		/* additional scope so SDGBuilder object can be garbage collected */{
			SDGBuilder builder = new SDGBuilder(cfg);
			builder.run(walaCG, pts, progress);
			sdg = convertToJoana(cfg.out, builder, progress);

			if (cfg.computeSummary) {
				pack = createSummaryWorkPackage(cfg.out, builder, sdg, progress);
			}
		}

		if (cfg.computeSummary) {
			if (cfg.accessPath) {
				computeDataAndAliasSummaryEdges(cfg.out, pack, sdg, progress);
			} else {
				computeSummaryEdges(cfg.out, pack, sdg, progress);
			}
		}

		return sdg;
	}

	public static SDGBuilder create(final SDGBuilderConfig cfg, final com.ibm.wala.ipa.callgraph.CallGraph walaCG,
			final PointerAnalysis<InstanceKey> pts, IProgressMonitor progress) throws UnsoundGraphException, CancelException {

		SDGBuilder builder = new SDGBuilder(cfg);
		builder.run(walaCG, pts, progress);

		return builder;
	}


	public static SDG build(final SDGBuilderConfig cfg, IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		SDG sdg = null;
		WorkPackage pack = null;

		/* additional scope so SDGBuilder object can be garbage collected */{
			SDGBuilder builder = new SDGBuilder(cfg);
			builder.run(progress);
			if (cfg.abortAfterCG) return null;
			sdg = convertToJoana(cfg.out, builder, progress);

			if (cfg.computeSummary) {
				pack = createSummaryWorkPackage(cfg.out, builder, sdg, progress);
			}
		}

		if (cfg.computeSummary) {
			if (cfg.accessPath) {
				computeDataAndAliasSummaryEdges(cfg.out, pack, sdg, progress);
			} else {
				computeSummaryEdges(cfg.out, pack, sdg, progress);
			}
		}

		return sdg;
	}

	public static Pair<SDG, SDGBuilder> buildAndKeepBuilder(final SDGBuilderConfig cfg, IProgressMonitor progress)
			throws UnsoundGraphException, CancelException {
		SDG sdg = null;
		WorkPackage pack = null;

		SDGBuilder builder = new SDGBuilder(cfg);
		builder.run(progress);
		sdg = convertToJoana(cfg.out, builder, progress);

		if (cfg.computeSummary) {
			pack = createSummaryWorkPackage(cfg.out, builder, sdg, progress);
		}

		if (cfg.computeSummary) {
			if (cfg.accessPath) {
				computeDataAndAliasSummaryEdges(cfg.out, pack, sdg, progress);
			} else {
				computeSummaryEdges(cfg.out, pack, sdg, progress);
			}
		}

		return Pair.make(sdg, builder);
	}

	public static SDG build(final SDGBuilderConfig cfg) throws UnsoundGraphException, CancelException {
		IProgressMonitor progress = NullProgressMonitor.INSTANCE;
		return build(cfg, progress);
	}

	public static SDG convertToJoana(PrintStream out, SDGBuilder builder, IProgressMonitor progress)
			throws CancelException {
		out.print("convert");
		final SDG sdg = JoanaConverter.convert(builder, progress);
		out.print(".");

		return sdg;
	}

	private static WorkPackage createSummaryWorkPackage(PrintStream out, SDGBuilder builder, SDG sdg,
			IProgressMonitor progress) {
		out.print("summary");
		Set<EntryPoint> entries = new TreeSet<EntryPoint>();
		PDG pdg = builder.getMainPDG();
		TIntSet formIns = new TIntHashSet();
		for (PDGNode p : pdg.params) {
			formIns.add(p.getId());
		}
		TIntSet formOuts = new TIntHashSet();
		formOuts.add(pdg.exception.getId());
		formOuts.add(pdg.exit.getId());
		EntryPoint ep = new EntryPoint(pdg.entry.getId(), formIns, formOuts);
		entries.add(ep);
		WorkPackage pack = WorkPackage.create(sdg, entries, sdg.getName());
		out.print(".");

		return pack;
	}

	private static void computeSummaryEdges(PrintStream out, WorkPackage pack, SDG sdg, IProgressMonitor progress)
			throws CancelException {
		SummaryComputation.compute(pack, progress);
		out.print(".");
	}

	private static void computeDataAndAliasSummaryEdges(PrintStream out, WorkPackage pack, SDG sdg,
			IProgressMonitor progress) throws CancelException {
		SummaryComputation.computeNoAliasDataDep(pack, progress);
		out.print(".");
		SummaryComputation.computeFullAliasDataDep(pack, progress);
		out.print(".");
	}

	private APResult apResult = null;
	private final ParameterFieldFactory params = new ParameterFieldFactory();
	private int currentNodeId = 1;
	private int pdgId = getMainId();
	private final List<PDG> pdgs = new LinkedList<PDG>();
	/**
	 * currently unused - could later be used to append static initializer calls
	 * to it
	 */
	// private final DependenceGraph startPDG = new DependenceGraph();

	private CallGraph cg = null;
	private com.ibm.wala.ipa.callgraph.CallGraph nonPrunedCG = null;
	private Map<PDGNode, TIntSet> call2alloc = null;
	private InterprocAnalysisResult<SSAInstruction, IExplodedBasicBlock> interprocExceptionResult = null;

	private SDGBuilder(final SDGBuilderConfig cfg) {
		this.cfg = cfg;
	}

	private void run(final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		if (debug.isEnabled()) {
			debug.outln("Running sdg computation with configuration:");
			debug.outln(LogUtil.attributesToString(cfg));
		}
		cfg.out.print("\n\tcallgraph: ");
		progress.beginTask("building call graph...", IProgressMonitor.UNKNOWN);
		final CGResult walaCG = buildCallgraph(progress);
		progress.done();
		if (cfg.cgConsumer != null) {
			cfg.cgConsumer.consume(walaCG.cg, walaCG.pts);
		}
		if (!cfg.abortAfterCG) {
			run(walaCG, progress);
		}
	}

	private void run(final com.ibm.wala.ipa.callgraph.CallGraph walaCG, final PointerAnalysis<InstanceKey> pts,
			final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		cfg.out.print("\n\tcallgraph: ");

		final CGResult cgresult = new CGResult(walaCG, pts);

		run(cgresult, progress);
	}

	private void run(final CGResult initalCG, final IProgressMonitor progress) throws UnsoundGraphException,
			CancelException {
		nonPrunedCG = initalCG.cg;
		progress.beginTask("pruning call graph...", IProgressMonitor.UNKNOWN);
		cg = convertAndPruneCallGraph(cfg.prunecg, initalCG, progress);
		progress.done();
		if (cfg.debugCallGraphDotOutput) {
			debugDumpGraph(cg, "callgraph.dot");
		}

		cfg.out.println(cg.vertexSet().size() + " nodes and " + cg.edgeSet().size() + " edges");

		if (cfg.exceptions == ExceptionAnalysis.INTERPROC) {
			cfg.out.print("\tinterproc exception analysis... ");
			progress.beginTask("interproc exception analysis... ", IProgressMonitor.UNKNOWN);

			try {
				interprocExceptionResult = NullPointerAnalysis.computeInterprocAnalysis(
						DEFAULT_IGNORE_EXCEPTIONS, nonPrunedCG,	cfg.defaultExceptionMethodState,
						progress, cfg.pruneDDEdgesToDanglingExceptionNodes);
			} catch (WalaException e) {
				throw new CancelException(e);
			}

			progress.done();
			if (IS_DEBUG) debug.outln(interprocExceptionResult.toString());
		}

		pdgId = getMainId();
		{
			// create main pdg
			final CGNode cgm = cg.getRoot().node;
			final PDG pdg = createAndAddPDG(cgm, progress);
			MonitorUtil.throwExceptionIfCanceled(progress);

			if (cfg.debugManyGraphsDotOutput) {
				debugOutput(pdg);
			}
		}

		cfg.out.print("\tintraproc: ");
		progress.beginTask("computing intraprocedural flow", cg.vertexSet().size());
		int currentNum = 1;

		for (CallGraph.Node node : cg.vertexSet()) {
			if (node.node == cg.getRoot().node) {
				continue;
			}

			final CGNode cgm = node.node;
			final PDG pdg = createAndAddPDG(cgm, progress);

			progress.worked(currentNum++);

			MonitorUtil.throwExceptionIfCanceled(progress);

			if (cfg.debugManyGraphsDotOutput) {
				debugOutput(pdg);
			}
		}
		progress.done();

		cfg.out.print("calls");
		progress.beginTask("interproc: connect call sites", pdgs.size());
		currentNum = 0;
		// connect call sites
		for (PDG pdg : pdgs) {
			if (isImmutableStub(pdg.getMethod().getDeclaringClass().getReference())) {
				continue;
			}

			for (PDGNode call : pdg.getCalls()) {
				Set<PDG> tgts = findPossibleTargets(cg, pdg, call);
				pdg.connectCall(call, tgts);
				if (!tgts.isEmpty()) {
					// we only need to record the signature of the call target
					// if it is a native method, or if there is no PDG
					// to jump to, respectively
					call.setUnresolvedCallTarget(null);
				}
			}

			progress.worked(currentNum++);
		}

		cfg.out.print(".");
		progress.worked(1);


		if (cfg.mergeFieldsOfPrunedCalls) {
			cfg.out.print("mergeable");

			partitions = SearchFieldsOfPrunedCalls.compute(this, progress);

			cfg.out.print(".");
			progress.worked(1);
		} else {
			partitions = null;
		}

		cfg.out.print(".");
		progress.done();
		
		if (cfg.staticInitializers != StaticInitializationTreatment.NONE) {
			progress.beginTask("interproc: handling static initializers (clinit)...", IProgressMonitor.UNKNOWN);
			cfg.out.print("clinit");
			switch (cfg.staticInitializers) {
			case SIMPLE:
				// nothing to do, this is handled though fakeWorldClinit of wala
				// callgraph
				break;
			case ACCURATE:
				StaticInitializers.compute(this, progress);
				break;
			default:
				throw new IllegalStateException("Unknown option: " + cfg.staticInitializers);
			}
			cfg.out.print(".");

		}
		progress.done();
		cfg.out.print("statics");
		// propagate static root nodes and add dataflow
		progress.beginTask("interproc: adding data flow for static fields...", IProgressMonitor.UNKNOWN);
		addDataFlowForStaticFields(progress);
		progress.done();
		cfg.out.print(".");

		cfg.out.print("heap");
		// compute dataflow through heap/fields (no-alias)
		addDataFlowForHeapFields(progress);
		cfg.out.print(".");

		cfg.out.print("misc");
		// compute dummy connections for unresolved calls
		progress.beginTask("interproc: adding dummy data flow to unresolved calls...", IProgressMonitor.UNKNOWN);
		addDummyDataFlowToUnresolvedCalls();
		progress.done();
		cfg.out.print(".");

		if (cfg.localKillingDefs) {
			cfg.out.print("killdef");
			progress.beginTask("interproc: computing local killing defintions...", IProgressMonitor.UNKNOWN);
			LocalKillingDefs.run(this, progress);
			progress.done();
			cfg.out.print(".");
		}

		if (cfg.accessPath) {
			DumpSDG.dumpIfEnabled(this, Log.L_SDG_DUMP_PRE_AP);
			cfg.out.print("accesspath");
			progress.beginTask("interproc: computing access path information...", IProgressMonitor.UNKNOWN);
			// compute access path info
			this.apResult = AccessPath.compute(this, getMainPDG());
			progress.done();
			cfg.out.print(".");
		}

		addReturnEdges();

		progress.worked(1);
		if (cfg.computeAllocationSites) {
			call2alloc = new AllCallsAllocationSiteFinder(this).getAllocationSites();
		}

		progress.worked(1);

		if (cfg.computeInterference) {
			cfg.out.print("interference");
			ThreadInformationProvider tiProvider = new ThreadInformationProvider(this);

			if (!cfg.computeAllocationSites) {
				call2alloc = tiProvider.getAllocationSitesForThreadStartCalls();
			}

			progress.beginTask("adding interference edges...", IProgressMonitor.UNKNOWN);
			addInterferenceEdges(tiProvider, progress);
			progress.subTask("introducing fork edges...");
			introduceForkEdges(tiProvider);
			cfg.out.print(".");
		}

		progress.done();
		addEntryExitCFEdges();

		if (cfg.pruneDDEdgesToDanglingExceptionNodes) {
			pruneDDEdgesToDanglingExceptionNodes();
		}

		final Logger l = Log.getLogger(Log.L_WALA_UNRESOLVED_CLASSES);
		if (l.isEnabled()) {
			final Set<TypeReference> unresolved = cfg.cha.getUnresolvedClasses();
			if (unresolved != null) {
				for (final TypeReference tref : unresolved) {
					l.outln("unresolved: " + tref);
				}
			}
		}
	}

	private void addEntryExitCFEdges() {
		for (PDG pdg : pdgs) {
			pdg.addEdge(pdg.entry, pdg.exit, PDGEdge.Kind.CONTROL_FLOW);
		}
	}

	public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> createIntraExceptionAnalyzedCFG(final CGNode n,
			final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> npa = NullPointerAnalysis
				.createIntraproceduralExplodedCFGAnalysis(DEFAULT_IGNORE_EXCEPTIONS, n.getIR(),
						null, cfg.defaultExceptionMethodState, cfg.pruneDDEdgesToDanglingExceptionNodes);

		npa.compute(progress);

		return npa.getCFG();
	}

	public ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> createExceptionAnalyzedCFG(final CGNode n,
			final IProgressMonitor progress) throws UnsoundGraphException, CancelException {
		ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> ecfg = null;

		switch (cfg.exceptions) {
		case ALL_NO_ANALYSIS: {
			ecfg = ExplodedControlFlowGraph.make(n.getIR());
		}
			break;
		case INTRAPROC: {
			ecfg = createIntraExceptionAnalyzedCFG(n, progress);
		}
			break;
		case INTERPROC: {
			final ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> npa =
					(interprocExceptionResult != null ? interprocExceptionResult.getResult(n) : null);

			if (npa != null) {
				npa.compute(progress);
				ecfg = npa.getCFG();
			} else {
				// No result for this method or called at the wrong time. We do not keep the interprocedural analysis
				// result during the whole computation due to memory usage. -> fallback intraproc analysis

				ecfg = createIntraExceptionAnalyzedCFG(n, progress);
			}
		}
			break;
		case IGNORE_ALL: {
			ecfg = ExceptionPrunedCFG.make(ExplodedControlFlowGraph.make(n.getIR()));
		}
			break;
		}

		return ecfg;
	}

	private void pruneDDEdgesToDanglingExceptionNodes() {
		for (PDG pdg : getAllPDGs()) {
			for (PDGNode call : pdg.getCalls()) {
				PDGNode excOut = pdg.getExceptionOut(call);
				boolean calleeMayThrowException = getPossibleTargets(call).isEmpty();
				for (PDG possibleTarget : getPossibleTargets(call)) {
					if (mayThrowException(possibleTarget.cgNode)) {
						calleeMayThrowException = true;
					}
				}
				if (!calleeMayThrowException) {
					pdg.removeEdge(excOut, pdg.exception, PDGEdge.Kind.DATA_DEP);
				}
			}
		}
	}

	public boolean mayThrowException(CGNode n) {
		switch (cfg.exceptions) {
		case ALL_NO_ANALYSIS:
			return true;
		case IGNORE_ALL:
			return false;
		case INTRAPROC:
			if (n.getIR() == null) {
				return true;
			}
			ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> npa = NullPointerAnalysis
			.createIntraproceduralExplodedCFGAnalysis(DEFAULT_IGNORE_EXCEPTIONS, n.getIR(),
					null, cfg.defaultExceptionMethodState);
			try {
				npa.compute(NullProgressMonitor.INSTANCE);
			} catch (UnsoundGraphException e) {
				return true;
			} catch (CancelException e) {
				return true;
			}
			return npa.hasExceptions();
		case INTERPROC:
		}
		if (interprocExceptionResult == null) {
			return true;
		} else {
				ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> result = interprocExceptionResult.getResult(n);
				if (result == null) {
					return true;
				} else {
					return result.hasExceptions();
				}
		}
	}

	private void addReturnEdges() {
		for (PDG pdg : getAllPDGs()) {
			for (PDGNode call : pdg.getCalls()) {
				for (PDG tgt : getPossibleTargets(call)) {
					// add return edge from callee to unique successor of call
					// node
					// if successor is not unique, add dummy node
					PDGNode exitOfCallee = tgt.exit;
					List<PDGNode> callSucc = new LinkedList<PDGNode>();
					for (PDGEdge callOut : pdg.outgoingEdgesOf(call)) {
						if (call.equals(callOut.from) && callOut.kind == PDGEdge.Kind.CONTROL_FLOW) {
							callSucc.add(callOut.to);
						}
					}

					PDGNode uniqueSucc;

					if (callSucc.size() == 1 && callSucc.get(0).getBytecodeIndex() == BytecodeLocation.CALL_RET) {
						// already added a dummy node
						uniqueSucc = callSucc.get(0);

					} else {
						uniqueSucc = pdg.createCallReturnNode(call);

						// unique successor inherits all outgoing control flow
						// edges of the call node
						for (PDGNode succOfCall : callSucc) {
							pdg.removeEdge(call, succOfCall, PDGEdge.Kind.CONTROL_FLOW);
							pdg.addEdge(uniqueSucc, succOfCall, PDGEdge.Kind.CONTROL_FLOW);
						}

						// As a potential occurring exception controls iff the call exists normally,
						// we add a control dependence to the call return node.
						// This is a more natural and precise solution to the bug reported by benedikt (bug #16)
						// We also move all control dependencies from call node to call return
						final PDGNode excRet = pdg.getExceptionOut(call);
						pdg.addEdge(excRet, uniqueSucc, PDGEdge.Kind.CONTROL_DEP_EXPR);

						final List<PDGEdge> callControlDeps = new LinkedList<PDGEdge>();
						for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
							if (e.kind == PDGEdge.Kind.CONTROL_DEP) {
								callControlDeps.add(e);
								pdg.addEdge(uniqueSucc, e.to, PDGEdge.Kind.CONTROL_DEP);
							}
						}
						pdg.removeAllEdges(callControlDeps);

						// add edge from call node to unique successor
						pdg.addEdge(call, uniqueSucc, PDGEdge.Kind.CONTROL_FLOW);
						pdg.addEdge(call, uniqueSucc, PDGEdge.Kind.CONTROL_DEP_EXPR);
					}

					// add return edge
					tgt.addVertex(uniqueSucc);
					tgt.addEdge(exitOfCallee, uniqueSucc, PDGEdge.Kind.RETURN);
					if (IS_DEBUG) debug.outln("Added return edge between " + exitOfCallee + " and " + uniqueSucc + ".");
				}
			}
		}
	}

	private void addInterferenceEdges(ThreadInformationProvider tiProvider, IProgressMonitor progress)
			throws CancelException {
		if (tiProvider.hasThreadStartMethod()) {
			EscapeAnalysis escapeAnalysis = new MethodEscapeAnalysis(new TrivialMethodEscape(
					getNonPrunedWalaCallGraph(), getPointerAnalysis().getHeapGraph()));
			Set<InterferenceEdge> interferences = InterferenceComputation.computeInterference(this, tiProvider, true,
					false, escapeAnalysis, progress);
			assert interferences != null;
			for (InterferenceEdge iEdge : interferences) {
				iEdge.addToPDG();
			}
		}
	}

	private void introduceForkEdges(ThreadInformationProvider tiProvider) {
		new Call2ForkConverter(this, tiProvider).run();
	}

	public static final int DO_NOT_PRUNE = -1;

	public static class CGResult {
		public final com.ibm.wala.ipa.callgraph.CallGraph cg;
		public final PointerAnalysis<InstanceKey> pts;

		private CGResult(com.ibm.wala.ipa.callgraph.CallGraph cg, PointerAnalysis<InstanceKey> pts) {
			this.cg = cg;
			this.pts = pts;
		}
	}

	public CallGraphBuilder createCallgraphBuilder(final IProgressMonitor progress) {
		final List<Entrypoint> entries = new LinkedList<Entrypoint>();
		final Entrypoint ep = new SubtypesEntrypoint(cfg.entry, cfg.cha);
		entries.add(ep);
		final ExtendedAnalysisOptions options = new ExtendedAnalysisOptions(cfg.objSensFilter, cfg.scope, entries);
		if (cfg.ext.resolveReflection()) {
			options.setReflectionOptions(ReflectionOptions.NO_STRING_CONSTANTS);
		} else {
			options.setReflectionOptions(ReflectionOptions.NONE);
		}
		if (cfg.methodTargetSelector != null) {
			options.setSelector(cfg.methodTargetSelector);
		}

		CallGraphBuilder cgb = null;
		switch (cfg.pts) {
		case RTA: // Rapid Type Analysis
			// Maybe UNSOUND - WALAs implementation looks suspicious.
			// Its also less precise and slower (blows up dynamic calls) as TYPE (0-CFA).
			// Its just here for academic purposes.
			cgb = WalaPointsToUtil.makeRTA(options, cfg.cache, cfg.cha, cfg.scope);
			break;
		case TYPE_BASED: // 0-CFA
			// Fastest option.
			cgb = WalaPointsToUtil.makeContextFreeType(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case INSTANCE_BASED: // 0-1-CFA
			// Best bang for buck
			cgb = WalaPointsToUtil.makeContextSensSite(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N1_OBJECT_SENSITIVE:
			// Receiver context is limited to 1-level. 
			// Uses 1-CFA as fallback for static methods.
			options.filter = new ObjSensZeroXCFABuilder.DefaultMethodFilter() {
				@Override
				public boolean restrictToOneLevelObjectSensitivity(final IMethod m) {
					return true;
				}
			};
			cgb = WalaPointsToUtil.makeObjectSens(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case OBJECT_SENSITIVE:
			// Very precise for OO heavy code - best option for really precise analysis.
			// Unlimited receiver context for application code, 1-level receiver context for library code. 
			// Uses n-CFA as fallback for static methods. Customizable: Provide objSensFilter to specify 'n' for fallback
			// n-CFA and filter for methods where object-sensitivity should be engaged. Default 'n = 1'.
			cgb = WalaPointsToUtil.makeObjectSens(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case UNLIMITED_OBJECT_SENSITIVE:
			// Very precise for OO heavy code, but also very slow.
			// Unlimited receiver context for the whole code - application as well as library. 
			// Uses 1-CFA as fallback for static methods.
			options.filter = new ObjSensZeroXCFABuilder.DefaultMethodFilter() {
				@Override
				public boolean restrictToOneLevelObjectSensitivity(final IMethod m) {
					return false;
				}
			};
			cgb = WalaPointsToUtil.makeObjectSens(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N1_CALL_STACK: // 1-CFA
			// Slower as 0-1-CFA, yet few precision improvements
			cgb = WalaPointsToUtil.makeNCallStackSens(1, options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N2_CALL_STACK: // 2-CFA
			// Slow, but precise
			cgb = WalaPointsToUtil.makeNCallStackSens(2, options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N3_CALL_STACK: // 3-CFA
			// Very slow and little bit more precise. Not much improvement over 2-CFA.
			cgb = WalaPointsToUtil.makeNCallStackSens(3, options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case CUSTOM:
			cgb = cfg.customCGBFactory.createCallGraphBuilder(options, cfg.cache, cfg.cha, cfg.scope, cfg.additionalContextSelector, cfg.additionalContextInterpreter);
		}
		
		return cgb;
	}
	
	public CGResult buildCallgraph(final IProgressMonitor progress) throws IllegalArgumentException,
			CallGraphBuilderCancelException {
		final List<Entrypoint> entries = new LinkedList<Entrypoint>();
		final Entrypoint ep = new SubtypesEntrypoint(cfg.entry, cfg.cha);
		entries.add(ep);
		final ExtendedAnalysisOptions options = new ExtendedAnalysisOptions(cfg.objSensFilter, cfg.scope, entries);
		if (cfg.ext.resolveReflection()) {
			options.setReflectionOptions(ReflectionOptions.NO_STRING_CONSTANTS);
		} else {
			options.setReflectionOptions(ReflectionOptions.NONE);
		}
		if (cfg.methodTargetSelector != null) {
			options.setSelector(cfg.methodTargetSelector);
		}

		CallGraphBuilder cgb = null;
		switch (cfg.pts) {
		case RTA: // Rapid Type Analysis
			// Maybe UNSOUND - WALAs implementation looks suspicious.
			// Its also less precise and slower (blows up dynamic calls) as TYPE (0-CFA).
			// Its just here for academic purposes.
			cgb = WalaPointsToUtil.makeRTA(options, cfg.cache, cfg.cha, cfg.scope);
			break;
		case TYPE_BASED: // 0-CFA
			// Fastest option.
			cgb = WalaPointsToUtil.makeContextFreeType(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case INSTANCE_BASED: // 0-1-CFA
			// Best bang for buck
			cgb = WalaPointsToUtil.makeContextSensSite(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N1_OBJECT_SENSITIVE:
			// Receiver context is limited to 1-level. 
			// Uses 1-CFA as fallback for static methods.
			options.filter = new ObjSensZeroXCFABuilder.DefaultMethodFilter() {
				@Override
				public boolean restrictToOneLevelObjectSensitivity(final IMethod m) {
					return true;
				}
			};
			cgb = WalaPointsToUtil.makeObjectSens(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case OBJECT_SENSITIVE:
			// Very precise for OO heavy code - best option for really precise analysis.
			// Unlimited receiver context for application code, 1-level receiver context for library code. 
			// Uses n-CFA as fallback for static methods. Customizable: Provide objSensFilter to specify 'n' for fallback
			// n-CFA and filter for methods where object-sensitivity should be engaged. Default 'n = 1'.
			cgb = WalaPointsToUtil.makeObjectSens(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case UNLIMITED_OBJECT_SENSITIVE:
			// Very precise for OO heavy code, but also very slow.
			// Unlimited receiver context for the whole code - application as well as library. 
			// Uses 1-CFA as fallback for static methods.
			options.filter = new ObjSensZeroXCFABuilder.DefaultMethodFilter() {
				@Override
				public boolean restrictToOneLevelObjectSensitivity(final IMethod m) {
					return false;
				}
			};
			cgb = WalaPointsToUtil.makeObjectSens(options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N1_CALL_STACK: // 1-CFA
			// Slower as 0-1-CFA, yet few precision improvements
			cgb = WalaPointsToUtil.makeNCallStackSens(1, options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N2_CALL_STACK: // 2-CFA
			// Slow, but precise
			cgb = WalaPointsToUtil.makeNCallStackSens(2, options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case N3_CALL_STACK: // 3-CFA
			// Very slow and little bit more precise. Not much improvement over 2-CFA.
			cgb = WalaPointsToUtil.makeNCallStackSens(3, options, cfg.cache, cfg.cha, cfg.scope,
					cfg.additionalContextSelector, cfg.additionalContextInterpreter);
			break;
		case CUSTOM:
			cgb = cfg.customCGBFactory.createCallGraphBuilder(options, cfg.cache, cfg.cha, cfg.scope, cfg.additionalContextSelector, cfg.additionalContextInterpreter);
		}
		cfg.options = options;
		com.ibm.wala.ipa.callgraph.CallGraph callgraph = cgb.makeCallGraph(options, progress);

		System.out.println("call graph has " + callgraph.getNumberOfNodes() + " nodes.");
		
		return new CGResult(callgraph, cgb.getPointerAnalysis());
	}

	private CallGraph convertAndPruneCallGraph(final int prune, final CGResult walaCG, final IProgressMonitor progress)
			throws IllegalArgumentException, CallGraphBuilderCancelException {

		com.ibm.wala.ipa.callgraph.CallGraph curcg = walaCG.cg;

		if (prune >= 0) {
			CallGraphPruning cgp = new CallGraphPruning(walaCG.cg);
			Set<CGNode> appl = cgp.findNodes(prune, cfg.pruningPolicy);
			PrunedCallGraph pcg = new PrunedCallGraph(walaCG.cg, appl);
			curcg = pcg;
		}

		progress.worked(1);

		final CallGraph cg = CallGraph.build(this, curcg, walaCG.pts, cfg.entry, progress);

		return cg;
	}

	private void addDataFlowForHeapFields(IProgressMonitor progress) throws CancelException {
		switch (cfg.fieldPropagation) {
		case FLAT: {
			FlatHeapParams.compute(this, progress);
		}
			break;
		case OBJ_GRAPH: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isMergeException = true;
			opt.isCutOffImmutables = true;
			opt.isMergeOneFieldPerParent = true;
			opt.isMergePrunedCallNodes = true;
			opt.isMergeDuringCutoffImmutables = true;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.maxNodesPerInterface = ObjGraphParams.Options.DEFAULT_MAX_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_NO_FIELD_MERGE: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			opt.isMergeException = false;
			opt.isCutOffImmutables = true;
			opt.isMergeOneFieldPerParent = false;
			opt.isMergePrunedCallNodes = false;
			opt.isMergeDuringCutoffImmutables = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.UNLIMITED_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_NO_MERGE_AT_ALL: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = true;
			opt.isMergeOneFieldPerParent = false;
			opt.isUseAdvancedInterprocPropagation = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.UNLIMITED_NODES_PER_INTERFACE;
			opt.isMergePrunedCallNodes = false;
			opt.isMergeException = false;
			this.cfg.mergeFieldsOfPrunedCalls = false;
			opt.isMergeDuringCutoffImmutables = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_FIXPOINT_PROPAGATION: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isMergeException = true;
			opt.isCutOffImmutables = true;
			opt.isMergeOneFieldPerParent = true;
			opt.isMergePrunedCallNodes = true;
			opt.isMergeDuringCutoffImmutables = true;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.maxNodesPerInterface = ObjGraphParams.Options.DEFAULT_MAX_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_FIXP_NO_OPT: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			opt.isMergeException = false;
			opt.isCutOffImmutables = false;
			opt.isMergeOneFieldPerParent = false;
			opt.isMergePrunedCallNodes = false;
			opt.isMergeDuringCutoffImmutables = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.UNLIMITED_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_NO_ESCAPE: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isMergeException = true;
			opt.isCutOffImmutables = true;
			opt.isMergeOneFieldPerParent = true;
			opt.isMergePrunedCallNodes = true;
			opt.isMergeDuringCutoffImmutables = true;
			opt.isUseAdvancedInterprocPropagation = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.DEFAULT_MAX_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_SIMPLE_NO_OPT_NO_ESCAPE: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isUseAdvancedInterprocPropagation = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			opt.isMergeException = false;
			opt.isCutOffImmutables = false;
			opt.isMergeOneFieldPerParent = false;
			opt.isMergePrunedCallNodes = false;
			opt.isMergeDuringCutoffImmutables = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.UNLIMITED_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_SIMPLE_NO_OPT: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = true;
			opt.isUseAdvancedInterprocPropagation = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			opt.isMergeException = false;
			opt.isCutOffImmutables = false;
			opt.isMergeOneFieldPerParent = false;
			opt.isMergePrunedCallNodes = false;
			opt.isMergeDuringCutoffImmutables = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.UNLIMITED_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_SIMPLE_PROPAGATION: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = true;
			opt.isMergeException = true;
			opt.isCutOffImmutables = true;
			opt.isMergeOneFieldPerParent = true;
			opt.isMergePrunedCallNodes = true;
			opt.isMergeDuringCutoffImmutables = true;
			opt.isUseAdvancedInterprocPropagation = false;
			opt.maxNodesPerInterface = ObjGraphParams.Options.DEFAULT_MAX_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_GRAPH_NO_OPTIMIZATION: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffUnreachable = false;
			opt.isMergeException = false;
			opt.isCutOffImmutables = false;
			opt.isMergeOneFieldPerParent = false;
			opt.isMergePrunedCallNodes = false;
			opt.isMergeDuringCutoffImmutables = true;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.maxNodesPerInterface = ObjGraphParams.Options.UNLIMITED_NODES_PER_INTERFACE;
			opt.convertToObjTree = false;
			opt.doStaticFields = false;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_TREE: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffImmutables = true;
			opt.isCutOffUnreachable = true;
			opt.isMergeException = false;
			opt.isMergeOneFieldPerParent = true;
			opt.isMergePrunedCallNodes = true;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.convertToObjTree = true;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_TREE_AP: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffImmutables = true;
			opt.isCutOffUnreachable = true;
			opt.isMergeException = false;
			opt.isMergeOneFieldPerParent = true;
			opt.isMergePrunedCallNodes = false;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.convertToObjTree = true;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		case OBJ_TREE_NO_FIELD_MERGE: {
			final ObjGraphParams.Options opt = new ObjGraphParams.Options();
			opt.isCutOffImmutables = true;
			opt.isCutOffUnreachable = true;
			opt.isMergeException = false;
			opt.isMergeOneFieldPerParent = false;
			opt.isMergePrunedCallNodes = false;
			opt.isUseAdvancedInterprocPropagation = true;
			opt.convertToObjTree = true;
			opt.ignoreExceptions = cfg.exceptions == ExceptionAnalysis.IGNORE_ALL;
			ObjGraphParams.compute(this, opt, progress);
		}
			break;
		default:
			throw new IllegalStateException("Unknown field propagation option: " + cfg.fieldPropagation);
		}
	}

	private void addDataFlowForStaticFields(IProgressMonitor progress) throws CancelException {
		StaticFieldParams.compute(this, progress);
	}

	private void addDummyDataFlowToUnresolvedCalls() {
		// connect call sites
		for (final PDG pdg : pdgs) {
			if (isImmutableStub(pdg.getMethod().getDeclaringClass().getReference())) {
				// direct data deps from all formal-in to formal-outs
				final List<PDGNode> inParam = new LinkedList<PDGNode>();
				final List<PDGNode> outParam = new LinkedList<PDGNode>();
				for (final PDGEdge e : pdg.outgoingEdgesOf(pdg.entry)) {
					if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
						switch (e.to.getKind()) {
						case FORMAL_IN:
							inParam.add(e.to);
							break;
						case EXIT:
							if (pdg.isVoid()) break;
						case FORMAL_OUT:
							outParam.add(e.to);
							break;
						default: // nothing to do here
						}
					}
				}

				connectIn2OutDummys(pdg, pdg.entry, inParam, outParam);
			} else {
				for (final PDGNode call : pdg.getCalls()) {
					final Set<PDG> tgts = findPossibleTargets(cg, pdg, call);
					if (tgts.isEmpty() && !cfg.ext.isCallToModule((SSAInvokeInstruction) pdg.getInstruction(call))) {
						// do direct data deps dummies
						final List<PDGNode> inParam = new LinkedList<PDGNode>();
						final List<PDGNode> outParam = new LinkedList<PDGNode>();
						for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
							if (e.kind == PDGEdge.Kind.CONTROL_DEP_EXPR) {
								switch (e.to.getKind()) {
								case ACTUAL_IN:
									inParam.add(e.to);
									break;
								case ACTUAL_OUT:
									outParam.add(e.to);
									break;
								default: // nothing to do here
								}
							}
						}

						connectIn2OutDummys(pdg, call, inParam, outParam);
					}
				}
			}
		}
	}

	private static void connectIn2OutDummys(final PDG pdg, final PDGNode parent, final List<PDGNode> inParam,
			final List<PDGNode> outParam) {
		final PDGNode m2m = pdg.createDummyNode("many2many");
		pdg.addEdge(parent, m2m, PDGEdge.Kind.CONTROL_DEP_EXPR);

		final List<PDGEdge> toRemove = new LinkedList<PDGEdge>();

		for (final PDGNode ain : inParam) {
			for (final PDGEdge e : pdg.outgoingEdgesOf(ain)) {
				if (e.kind == PDGEdge.Kind.DATA_DEP) {
					toRemove.add(e);
				}
			}
			pdg.addEdge(ain, m2m, PDGEdge.Kind.DATA_DEP);
		}

		for (final PDGNode aout : outParam) {
			for (final PDGEdge e : pdg.incomingEdgesOf(aout)) {
				if (e.kind == PDGEdge.Kind.DATA_DEP) {
					toRemove.add(e);
				}
			}
			pdg.addEdge(m2m, aout, PDGEdge.Kind.DATA_DEP);
		}

		pdg.removeAllEdges(toRemove);
	}

	public Set<PDG> findPossibleTargets(CallGraph cg, PDG caller, PDGNode call) {
		final Set<PDG> callees = new HashSet<PDG>();

		final Node cgCaller = cg.findNode(caller.cgNode);
		final SSAInstruction instr = caller.getInstruction(call);

		for (final Edge cl : cg.findTarges(cgCaller, instr.iindex)) {
			final CGNode called = cl.to.node;
			final PDG callee = getPDGforMethod(called);
			callees.add(callee);
		}

		return callees;
	}

	public Set<PDG> getPossibleTargets(PDGNode call) {
		if (call.getKind() != PDGNode.Kind.CALL) {
			throw new IllegalArgumentException("Not a call node: " + call);
		}

		Set<PDG> tgts = new HashSet<PDG>();

		PDG pdgCaller = getPDGforId(call.getPdgId());

		for (PDGEdge out : pdgCaller.outgoingEdgesOf(call)) {
			if (out.kind == PDGEdge.Kind.CALL_STATIC || out.kind == PDGEdge.Kind.CALL_VIRTUAL) {
				PDGNode entry = out.to;
				PDG target = getPDGforId(entry.getPdgId());
				tgts.add(target);
			}
		}

		return tgts;
	}

	public Set<PDG> getPossibleCallers(final PDG callee) {
		final Set<PDG> callers = new HashSet<PDG>();

		for (final PDG pdg : getAllPDGs()) {
			if (pdg != callee) {
				if (pdg.containsVertex(callee.entry)) {
					callers.add(pdg);
				}
			} else {
				boolean found = false;
				for (final PDGNode call : pdg.getCalls()) {
					if (found) {
						break;
					}

					for (final PDGEdge e : pdg.outgoingEdgesOf(call)) {
						if (found) {
							break;
						}

						if ((e.kind == PDGEdge.Kind.CALL_STATIC || e.kind == PDGEdge.Kind.CALL_VIRTUAL)
								&& e.to == pdg.entry) {
							found = true;
						}
					}
				}

				if (found) {
					callers.add(callee);
				}
			}
		}

		return callers;
	}

	public PDG getPDGforMethod(CGNode n) {
		for (PDG pdg : pdgs) {
			if (n.equals(pdg.cgNode)) {
				return pdg;
			}
		}

		return null;
	}

	public int getNextNodeId() {
		final int id = currentNodeId;
		currentNodeId++;
		return id;
	}

	public IClassHierarchy getClassHierarchy() {
		return cfg.cha;
	}

	public ParameterFieldFactory getParameterFieldFactory() {
		return params;
	}

	public IMethod getEntry() {
		return cfg.entry;
	}

	public List<PDG> getAllPDGs() {
		return Collections.unmodifiableList(pdgs);
	}

	public PDG getPDGforId(int id) {
		for (PDG pdg : pdgs) {
			if (pdg.getId() == id) {
				return pdg;
			}
		}

		return null;
	}

	/**
	 * Returns a mapping which maps the id of a pdg node to the index of the ssa
	 * instruction it represents.
	 *
	 * @return a mapping which maps the id of a pdg node to the index of the ssa
	 *         instruction it represents
	 */
	public TIntIntMap getPDGNode2IIndex() {
		TIntIntHashMap ret = new TIntIntHashMap();

		for (PDG pdg : getAllPDGs()) {
			for (PDGNode node : pdg.vertexSet()) {
				SSAInstruction i = pdg.getInstruction(node);
				if (i != null) {
					ret.put(node.getId(), i.iindex);
				}
			}
		}

		return ret;
	}

	/**
	 * Returns a mapping between the entry nodes of the various pdgs to the id
	 * of the corresponding call graph nodes.
	 *
	 * @return a mapping between the entry nodes of the various pdgs to the id
	 *         of the corresponding call graph nodes
	 */
	public TIntIntMap getEntryNode2CGNode() {
		TIntIntHashMap ret = new TIntIntHashMap();

		for (PDG pdg : getAllPDGs()) {
			PDGNode pdgEntry = pdg.entry;
			ret.put(pdgEntry.getId(), pdg.cgNode.getGraphNodeId());
		}

		return ret;
	}

	public int getStartId() {
		return PDG_START_ID;
	}

	public int getMainId() {
		return PDG_START_ID + 1;
	}

	public PDG getMainPDG() {
		return getPDGforId(getMainId());
	}

	public Graph<PDG> createCallGraph() {
		Graph<PDG> cgPDG = new SparseNumberedGraph<PDG>();
		for (PDG pdg : pdgs) {
			cgPDG.addNode(pdg);
		}

		for (PDG pdg : pdgs) {
			for (PDGNode call : pdg.getCalls()) {
				Set<PDG> tgts = getPossibleTargets(call);
				for (PDG target : tgts) {
					cgPDG.addEdge(pdg, target);
				}
			}
		}

		return cgPDG;
	}

	public boolean isImmutableNoOutParam(TypeReference t) {
		final String name = t.getName().toString();

		for (final String im : cfg.immutableNoOut) {
			if (im.equals(name)) {
				return true;
			}
		}

		return false;
	}

	public boolean isImmutableStub(TypeReference t) {
		final String name = t.getName().toString();

		for (final String im : cfg.immutableStubs) {
			if (im.equals(name)) {
				return true;
			}
		}

		return false;
	}

	public boolean isIgnoreStaticFields(TypeReference t) {
		final String name = t.getName().toString();

		for (final String im : cfg.ignoreStaticFields) {
			if (im.equals(name)) {
				return true;
			}
		}

		return false;
	}

	public boolean isKeepPhiNodes() {
		return cfg.keepPhiNodes;
	}

	public boolean isNoBasePointerDependency() {
		return cfg.noBasePointerDependency;
	}

	private static void debugOutput(PDG pdg) {
		IMethod im = pdg.getMethod();
		final String prefix = WriteGraphToDot.sanitizeFileName(im.getName().toString());
		try {
			WriteGraphToDot.write(pdg, prefix + ".ddg.dot", new EdgeFilter<PDGEdge>() {
				public boolean accept(PDGEdge edge) {
					return edge.kind == PDGEdge.Kind.DATA_DEP;
				}
			});
			WriteGraphToDot.write(pdg, prefix + ".cdg.dot", new EdgeFilter<PDGEdge>() {
				public boolean accept(PDGEdge edge) {
					return edge.kind == PDGEdge.Kind.CONTROL_DEP;
				}
			});
			WriteGraphToDot.write(pdg, prefix + ".cfg.dot", new EdgeFilter<PDGEdge>() {
				public boolean accept(PDGEdge edge) {
					return edge.kind == PDGEdge.Kind.CONTROL_FLOW || edge.kind == PDGEdge.Kind.CONTROL_FLOW_EXC;
				}
			});
			WriteGraphToDot.write(pdg, prefix + ".pdg.dot", new EdgeFilter<PDGEdge>() {
				public boolean accept(PDGEdge edge) {
					return edge.kind == PDGEdge.Kind.CONTROL_DEP || edge.kind == PDGEdge.Kind.DATA_DEP;
				}
			});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @see CallGraph.CallGraphFilter
	 */
	public boolean ignoreCallsTo(IMethod m) {
		if (m.getDeclaringClass().getReference() == TypeReference.JavaLangObject) {
			if (m.isInit()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * @see CallGraph.CallGraphFilter
	 */
	public boolean ignoreCallsFrom(IMethod m) {
		final TypeReference tr = m.getDeclaringClass().getReference();

		return isImmutableStub(tr);
	}

	/**
	 * @see CallGraph.CallGraphFilter
	 */
	public boolean ignoreWalaFakeWorldClinit() {
		return cfg.staticInitializers != StaticInitializationTreatment.SIMPLE;
	}

	public PointerAnalysis<InstanceKey> getPointerAnalysis() {
		return cg.getPTS();
	}

	public com.ibm.wala.ipa.callgraph.CallGraph getWalaCallGraph() {
		return cg.getOrig();
	}

	public com.ibm.wala.ipa.callgraph.CallGraph getNonPrunedWalaCallGraph() {
		return nonPrunedCG;
	}

	private static <V, E> void debugDumpGraph(final DirectedGraph<V, E> g, final String fileName) {
		try {
			WriteGraphToDot.write(g, fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Configuration of the SDG computation.
	 *
	 * @author Juergen Graf <juergen.graf@gmail.com>
	 *
	 */
	public static class SDGBuilderConfig implements java.io.Serializable {
		private static final long serialVersionUID = 237647794827893127L;
		public transient PrintStream out = System.out;
		public transient AnalysisScope scope = null;
		public transient AnalysisCache cache = null;
		public transient IClassHierarchy cha = null;
		public IMethod entry = null;
		public ExternalCallCheck ext = null;
		public String[] immutableNoOut = Main.IMMUTABLE_NO_OUT;
		public String[] immutableStubs = Main.IMMUTABLE_STUBS;
		public String[] ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		public ExceptionAnalysis exceptions = ExceptionAnalysis.INTRAPROC;
		public boolean pruneDDEdgesToDanglingExceptionNodes = true;
		public MethodState defaultExceptionMethodState = null;
		public boolean accessPath = false;
		public boolean localKillingDefs = true;
		public boolean keepPhiNodes = true;
		public int prunecg = DO_NOT_PRUNE;
		public boolean mergeFieldsOfPrunedCalls = true;
		public PruningPolicy pruningPolicy = ApplicationLoaderPolicy.INSTANCE;
		public DynamicDispatchHandling dynDisp = DynamicDispatchHandling.SIMPLE;
		public PointsToPrecision pts = PointsToPrecision.INSTANCE_BASED;
		// only used iff pts is set to CUSTOM. Gets access to hopefully all
		// information needed to create a call graph builder, consistent
		// with the information internal to the SDG Builder
		public CallGraphBuilderFactory customCGBFactory = null;
		// only used iff pts is set to object sensitive. If null defaults to
		// "do object sensitive analysis for all methods"
		public ObjSensZeroXCFABuilder.MethodFilter objSensFilter = null;
		public FieldPropagation fieldPropagation = FieldPropagation.FLAT;
		/*
		 * Turns off control dependency from field access operation to base-pointer node and moves
		 * "nullpointer access exception" control dependency from instruction to basepointer node.
		 * This way data written to the field is no longer connected to the possible exception that
		 * may arise from the base pointer beeing null. This should improve precision.
		 *
		 * v1.f = v2
		 *
		 * Old style:
		 *
		 * [v1] -(dd)-> [set f] <-(dd)- [v2]
		 *   ^           | | |            ^
		 *   \--(cd)-----/ | \-----(cd)---/
		 *               (cd)
		 *               / |
		 *           [exc or normal]
		 *
		 * New style:
		 *
		 * [v1] -(dd)-> [set f] <-(dd)- [v2]
		 *   |              |            ^
		 *  (cd)            \------(cd)--/
		 *  / |
		 * [exc or normal]
		 *
		 */
		public boolean noBasePointerDependency = true;
		public boolean debugAccessPath = false;
		public String debugAccessPathOutputDir = null;
		public boolean debugCallGraphDotOutput = false;
		public boolean debugManyGraphsDotOutput = false; // CFG, CDG, PDG, DDG
		public StaticInitializationTreatment staticInitializers = StaticInitializationTreatment.NONE;
		public boolean debugStaticInitializers = false;
		public boolean computeInterference = true;
		public boolean computeSummary = true;
		/*
		 * If this flag is set, pdg nodes for all call sites of virtual methods contain
		 * the possible allocation sites of the this-pointer (the ids of PDG nodes of the
		 * respective allocation sites). If not and 'computeInterference' is set, then this 
		 * is done only for calls of Thread.start, Thread.join or Runnable.run() (or overriding
		 * methods). Otherwise, no call site contains any allocation sites.
		 */
		public boolean computeAllocationSites = false;
		public SideEffectDetectorConfig sideEffects = null;
		/**
		 *  Debugging-Option: Rename variables in the SDG when no name is available.
		 *
		 *  If this is set to false variables in the SDG will have names like "v#" or "p#" if
		 *  no actual name can be determined. If it is set to true the Type-Name will be appended
		 *  (like "v# Integer"). This shall help manually reading pdg-Files.
		 *
		 *  @todo   Enabling this may Throw errors when generation a new TypeInference.
		 */
		public boolean showTypeNameInValue = false;
		/** The methodTargetSelector from the AnalysisOptions. 
		 *
		 * It will get copied back there before CallGraphConstruction. If it's null the default of the 
		 * AnalysisOptions Constructor will be used */
		public MethodTargetSelector methodTargetSelector = null;
		/**
		 *  Context will be the Union of this and Joanas Context with additionalContextSelector having
		 *  precedence.
		 */
		public ContextSelector additionalContextSelector = null;
		/**
		 *  Will be the one queried first from the FallbackContextInterpreter.
		 */
		public SSAContextInterpreter additionalContextInterpreter = null;
		/**
		 * Special object which takes the call graph produced during SDG construction and
		 * does something useful with it
		 */
		public CGConsumer cgConsumer = null;
		/**
		 * Shall the SDG be constructed or shall the construction be aborted after the
		 * call graph has been built?
		 * It is sometimes useful to only construct the call graph, for example if one
		 * is interested in properties of the call graph itself rather than of the PDG
		 * but wants to have it built exactly as the SDGBuilderConfig dictates.
		 */
		public boolean abortAfterCG = false;
		
		/**
		 * This hook object can be used to capture the mapping between parameter nodes
		 * and mod ref candidates when the object graph algorithm is finished.
		 * This is useful for situations in which information about the correspondence
		 * between parameter nodes in the PDG and points-to sets is needed.
		 */
		public ParameterPointsToConsumer parameterPTSConsumer;
		
		/**
		 * the SDG builder stores here the analysis options used to build the call graph
		 */
		public AnalysisOptions options;
	}

	public String getMainMethodName() {
		return Util.methodName(getMainPDG().getMethod());
	}

	public TIntSet getAllocationNodes(PDGNode n) {
		return (call2alloc != null && call2alloc.containsKey(n) ? call2alloc.get(n) : null);
	}

	public PDG createAndAddPDG(final CGNode cgm, final IProgressMonitor progress) throws UnsoundGraphException,
			CancelException {
		final PDG pdg = PDG.build(this, Util.methodName(cgm.getMethod()), cgm, pdgId, cfg.ext, cfg.out, progress);
		pdgId++;
		pdgs.add(pdg);

		return pdg;
	}

	public long countNodes() {
		long count = 0;

		for (final PDG pdg : pdgs) {
			count += pdg.vertexSet().size();
		}

		return count;
	}

	private IFieldsMayMod fieldsMayMod;

	public void registerFinalModRef(final ModRefCandidates mrefs, final IProgressMonitor progress) throws CancelException {
		if (!cfg.localKillingDefs) {
			throw new IllegalStateException("Local killing definfitions is not activated.");
		}

		fieldsMayMod = FieldsMayModComputation.create(this, mrefs, progress);
	}

	public IFieldsMayMod getFieldsMayMod() {
		if (!cfg.localKillingDefs) {
			throw new IllegalStateException("Local killing definfitions is not activated.");
		}

		if (fieldsMayMod == null) {
			// default to simple always true may mod, if none has been set - e.g. when flat params is used and no
			// modref candidates are computed
			fieldsMayMod = new SimpleFieldsMayMod();
		}

		return fieldsMayMod;
	}
	
	public APResult getAPResult() {
		return apResult;
	}

}
