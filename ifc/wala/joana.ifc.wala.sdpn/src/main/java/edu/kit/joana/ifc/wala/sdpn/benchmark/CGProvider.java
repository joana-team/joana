/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn.benchmark;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Set;

import scala.Tuple2;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.pruned.CallGraphPruning;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.output.JoanaCFGSanitizer;
import edu.kit.joana.deprecated.jsdg.output.JoanaStyleSDG;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryEdgeComputation;
import edu.kit.joana.deprecated.jsdg.sdg.interference.CSDGPreprocessor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.InstanceAndPointerKeyFactoryAdapter;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.wala.util.VerboseProgressMonitor;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class CGProvider {
	public static Tuple2<CallGraph,PointerAnalysis> getCGnPA(final Setting setting) throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {

		final String main = setting.main();

		final String mainClassSimpleName = main.replace('/', '.').replace('$', '.').substring(1);

		SDGFactory.Config cfg = setting.jsdgConf();

		Analyzer.cfg = cfg;

		IProgressMonitor progress = new VerboseProgressMonitor(System.out);
		final Result result = getJoanaSDG(cfg, progress);

		{
		progress.beginTask("Saving SDG to " + cfg.outputSDGfile, -1);
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(cfg.outputSDGfile));
		SDGSerializer.toPDGFormat(result.sdg, bOut);
		progress.done();
		}

		Util.dumpCallGraph(result.cg, mainClassSimpleName, null);

		return new Tuple2<CallGraph, PointerAnalysis>(result.cg, result.pts);

	}










	private static class Result {
		public final edu.kit.joana.ifc.sdg.graph.SDG sdg;
		public final SDG jsdg;
		public final CallGraph cg;
		public final PointerAnalysis pts;
		public final MHPAnalysis mhp;

		private Result(final edu.kit.joana.ifc.sdg.graph.SDG sdg, final SDG jsdg, final CallGraph cg, final PointerAnalysis pts,
				final MHPAnalysis mhp) {
			this.sdg = sdg;
			this.jsdg = jsdg;
			this.cg = cg;
			this.pts = pts;
			this.mhp = mhp;
		}

	}

	public static final Result getJoanaSDG(Config cfg, final IProgressMonitor progress)
	throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		final Date start = initSDGcomputation(cfg);


		final Result jresult = getOrigSDG(cfg, progress);

		edu.kit.joana.ifc.sdg.graph.SDG joanaSdg = JoanaStyleSDG.createJoanaSDG(jresult.jsdg, cfg.addControlFlow, cfg.nonTermination, cfg.useSummaryOpt, progress);

        if (progress.isCanceled()) {
            throw CancelException.make("Operation aborted.");
        }

        // as long as we can not cope with nodes that do not belong to the control flow we do this...
        JoanaCFGSanitizer.sanitizeCFG(joanaSdg);

//        assert assertVerify(joanaSdg, !cfg.useWalaSdg, cfg.addControlFlow);

        final Date beforeThreadAllocation = new Date();

        final MHPAnalysis mhp;

        if (cfg.computeInterference) {
			progress.beginTask("Creating cSDG from SDG " + cfg.outputSDGfile, -1);

			progress.subTask("Running Thread Allocation Analysis");
			Log.info("Running Thread Allocation Analysis");

			mhp = CSDGPreprocessor.runMHP(joanaSdg, progress);

			Log.info("Thread Allocation done.");
	        progress.done();
        } else {
        	mhp = null;
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

        return new Result(joanaSdg, jresult.jsdg, jresult.cg, jresult.pts, mhp);
	}

	private static Result getOrigSDG(Config cfg, IProgressMonitor progress)
	throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);


		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		ClassLoader loader = cfg.getClass().getClassLoader();
		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
			//AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile, cfg.classpath, null);
		progress.done();

		ClassHierarchy cha = ClassHierarchy.make(scope, progress);

		Iterable<Entrypoint> entrypoints =
			com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, cfg.mainClass);
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
	    AnalysisCache cache = new AnalysisCache();

	    progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
		SSAPropagationCallGraphBuilder builder = SDGFactory.getCallGraphBuilder(cfg.pointsTo, options, cache, cha, scope);

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

		if (Debug.Var.DUMP_CALLGRAPH.isSet()) {
			Util.dumpCallGraph(cg, cfg.mainClass.replace('/','.').substring(1), progress);
		}

		if (Debug.Var.DUMP_HEAP_GRAPH.isSet()) {
			PointerAnalysis pta = builder.getPointerAnalysis();
			HeapGraph hg = pta.getHeapGraph();
			Util.dumpHeapGraph(cfg.mainClass.replace('/','.').substring(1) +
				"." + cfg.pointsTo, hg, null);
		}

		PointerAnalysis pta = builder.getPointerAnalysis();
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

		SDG sdg = SDG.create(main, cg, cache, adapter, pts, cfg, progress);
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

		return new Result(null, sdg, cg, pta, null);
	}

	private static Date initSDGcomputation(Config cfg) throws FileNotFoundException {
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

}

