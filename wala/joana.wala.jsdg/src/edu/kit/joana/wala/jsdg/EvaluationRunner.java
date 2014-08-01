/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.output.JoanaStyleSDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryEdgeComputation;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.AliasGraphException;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.FlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.SimpleParameter;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.util.GraphWriter;
import edu.kit.joana.wala.flowless.util.Util;
import edu.kit.joana.wala.jsdg.summary.FlowChecker;
import edu.kit.joana.wala.jsdg.summary.FlowChecker.FlowEdge;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges.SummaryGraph;
import edu.kit.joana.wala.jsdg.summary.MatchFlowParamWithSDGParam;


public class EvaluationRunner {

	public static class CFG {
		public final String src;
		public final String bin;
		public final String exclusions;

		public CFG(String src, String bin) {
			this.bin = bin;
			this.src = src;
			this.exclusions = null;
		}

		public CFG(String src, String bin, String exclusions) {
			this.bin = bin;
			this.src = src;
			this.exclusions = exclusions;
		}
	}

	public static final CFG[] EVAL_CFGS = {
		new CFG("../MoJo-FlowLess/examples/project1", "../MoJo-FlowLess/bin"),
//		new CFG("src", "bin"),
	};

	/**
	 * @param args
	 * @throws CancelException
	 * @throws IllegalArgumentException
	 * @throws FlowAstException
	 * @throws NoSuchElementException
	 * @throws IOException
	 * @throws WalaException
	 * @throws PDGFormatException
	 */
	public static void main(String[] args) throws IllegalArgumentException, CancelException, NoSuchElementException,
			FlowAstException, IOException, PDGFormatException, WalaException {
		Analyzer.cfg = new Config();
		Analyzer.cfg.outputDir = "out/";

		for (CFG cfg : EVAL_CFGS) {
			run(cfg);
		}
	}

	/**
	 *
	 * @param cfg
	 * @throws IllegalArgumentException
	 * @throws CancelException
	 * @throws NoSuchElementException
	 * @throws FlowAstException
	 * @throws IOException
	 * @throws WalaException
	 * @throws PDGFormatException
	 */
	public static void run(CFG cfg) throws IllegalArgumentException, CancelException, NoSuchElementException,
			FlowAstException, IOException, PDGFormatException, WalaException {
		System.out.print("Parsing source files... ");
		List<ClassInfo> clsInfos = MoJo.parseSourceFiles(cfg.src);
		System.out.println("done.");
		System.out.print("Checking for syntactic errors... ");
		final int errors = MoJo.prepareFlowLessStmts(clsInfos);
		if (errors > 0) {
			System.out.print("(" + errors + " errors) ");
		}
		System.out.println("done.");
		System.out.print("Creating MoJo... ");
		MoJo mojo = MoJo.create(cfg.bin);
		System.out.println("done.");

		for (ClassInfo cls : clsInfos) {
			if (cls.getName().contains("Library2") || !cls.getName().contains("Library")) {
				continue;
			}
//			System.out.println("Class " + cls);

			for (MethodInfo m : cls) {
				// bind variables

				if (m.hasIFCStmts()) {
					IMethod method = mojo.findMethod(m);
					System.out.println("Working on " + Util.methodName(method));

					if (m.hasErrors()) {
						System.out.println("IFC ERRORS FOUND - skipping method.");

						for (IFCStmt ifc : m) {
							System.out.println("\tIFC: " + ifc);
						}

						for (FlowError err : m.getErrors()) {
							System.out.println("\tIFC ERROR: " + err.exc.getMessage());
						}

						continue;
					}

					int indexOfstmt = 0;
					for (IFCStmt ifc : m) {
						indexOfstmt++;

						if (!checkIFCStmt(mojo, m, ifc, method, indexOfstmt, false)) {
							System.out.print("NO-EXC - ");
							checkIFCStmt(mojo, m, ifc, method, indexOfstmt, true);
						}
					}
				}
			}
		}
	}

	private static boolean checkIFCStmt(MoJo mojo, MethodInfo m, IFCStmt ifc, IMethod method,
			final int indexOfstmt, final boolean ignoreExceptions) throws FileNotFoundException,
			IllegalArgumentException, CancelException, PDGFormatException, WalaException, FlowAstException {

		System.out.print("IFC: " + ifc + " computing min and max aliasing... ");
		GraphAnnotater.Aliasing mayAlias = null;
		GraphAnnotater.Aliasing unrestrictedAlias = null;
		try { //XXXXXX
			mayAlias = mojo.computeMayAliasGraphs(m, ifc);
			unrestrictedAlias = mojo.computeMinMaxAliasing(method);
		} catch (AliasGraphException exc) {
			System.out.println("ERR: " + exc.getMessage() + " - skipping method.");
			return true;
		} catch (FlowAstException exc) {
			System.out.println("ERR: " + exc.getMessage() + " - skipping method.");
			return true;
		}
		System.out.println("done.");

		final String prefix = sanitizeLabel(method.getName().toString()) + "-" + indexOfstmt
			+ (ignoreExceptions ? "-no-exc" : "");
		final String bcMethodName = method.getSignature();

		boolean isFlowOk = true;

		{
			GraphWriter<PtsParameter> gOut = new GraphWriter.DotWriter<PtsParameter>("out/", prefix + "-bottom");
			System.out.print("\tMIN alias: points-to... ");
			edu.kit.joana.deprecated.jsdg.util.Util.dumpGraph(unrestrictedAlias.lowerBound, prefix + "-pts-bottom");
			PointsTo ptsMin = MoJo.computePointsTo(unrestrictedAlias.lowerBound, gOut);
			writeToFile(ptsMin, "out/" + prefix + "-bottom.pts");
			System.out.print("done, call graph... ");
			AnalysisOptions optPtsMin = mojo.createAnalysisOptionsWithPTS(ptsMin, method);
			CallGraphResult minCG = mojo.computeContextSensitiveCallGraph(optPtsMin);
			Util.dumpSSA(minCG.cg.getFakeRootNode().getIR(), new PrintStream("out/" + prefix + "-ssa-bottom.txt"));
			edu.kit.joana.deprecated.jsdg.util.Util.dumpHeapGraph(prefix + "-bottom", minCG.pts.getHeapGraph(), null);
			System.out.print("done, sdg... ");
			// run analysis on callgraph with minimal alias configuration
			// ...
			SDG minSDG = createSDG(minCG, optPtsMin, method, ignoreExceptions);
			SDGSerializer.toPDGFormat(minSDG, new BufferedOutputStream(new FileOutputStream("out/" + prefix + "-bottom.pdg")));
			System.out.print("(" + minSDG.edgeSet().size() + " edges) done, check ifc... ");

			isFlowOk &= checkIsStatementCorrect(ifc, minSDG, ptsMin, bcMethodName, prefix + "-bottom");
			System.out.println("done.");
		}


		{
			GraphWriter<PtsParameter> gOut = new GraphWriter.DotWriter<PtsParameter>("out/", prefix + "-min");
			System.out.print("\tmin alias: points-to... ");
			edu.kit.joana.deprecated.jsdg.util.Util.dumpGraph(mayAlias.lowerBound, prefix + "-pts-min");
			PointsTo ptsMin = MoJo.computePointsTo(mayAlias.lowerBound, gOut);
			writeToFile(ptsMin, "out/" + prefix + "-min.pts");
			System.out.print("done, call graph... ");
			AnalysisOptions optPtsMin = mojo.createAnalysisOptionsWithPTS(ptsMin, method);
			CallGraphResult minCG = mojo.computeContextSensitiveCallGraph(optPtsMin);
			Util.dumpSSA(minCG.cg.getFakeRootNode().getIR(), new PrintStream("out/" + prefix + "-ssa-min.txt"));
			edu.kit.joana.deprecated.jsdg.util.Util.dumpHeapGraph(prefix + "-min", minCG.pts.getHeapGraph(), null);
			System.out.print("done, sdg... ");
			// run analysis on callgraph with minimal alias configuration
			// ...
			SDG minSDG = createSDG(minCG, optPtsMin, method, ignoreExceptions);
			SDGSerializer.toPDGFormat(minSDG, new BufferedOutputStream(new FileOutputStream("out/" + prefix + "-min.pdg")));
			System.out.print("(" + minSDG.edgeSet().size() + " edges) done, check ifc... ");

			isFlowOk &= checkIsStatementCorrect(ifc, minSDG, ptsMin, bcMethodName, prefix + "-min");
			System.out.println("done.");
		}


		{
			GraphWriter<PtsParameter> gOut = new GraphWriter.DotWriter<PtsParameter>("out/", prefix + "-max");
			System.out.print("\tmax alias: points-to... ");
			edu.kit.joana.deprecated.jsdg.util.Util.dumpGraph(mayAlias.upperBound, prefix + "-pts-max");
			PointsTo ptsMax = MoJo.computePointsTo(mayAlias.upperBound, gOut);
			writeToFile(ptsMax, "out/" + prefix + "-max.pts");
			System.out.print("done, call graph... ");
			AnalysisOptions optPtsMax = mojo.createAnalysisOptionsWithPTS(ptsMax, method);
			CallGraphResult maxCG = mojo.computeContextSensitiveCallGraph(optPtsMax);
			Util.dumpSSA(maxCG.cg.getFakeRootNode().getIR(), new PrintStream("out/" + prefix + "-ssa-max.txt"));
			edu.kit.joana.deprecated.jsdg.util.Util.dumpHeapGraph(prefix + "-max", maxCG.pts.getHeapGraph(), null);
			System.out.print("done, sdg... ");
			// run analysis on callgraph with maximal alias configuration
			// ...
			SDG maxSDG = createSDG(maxCG, optPtsMax, method, ignoreExceptions);
			SDGSerializer.toPDGFormat(maxSDG, new BufferedOutputStream(new FileOutputStream("out/" + prefix + "-max.pdg")));
			System.out.print("(" + maxSDG.edgeSet().size() + " edges) done, check ifc... ");

			isFlowOk &= checkIsStatementCorrect(ifc, maxSDG, ptsMax, bcMethodName, prefix + "-max");
			System.out.println("done.");
		}


		{
			GraphWriter<PtsParameter> gOut = new GraphWriter.DotWriter<PtsParameter>("out/", prefix + "-top");
			System.out.print("\tMAX alias: points-to... ");
			edu.kit.joana.deprecated.jsdg.util.Util.dumpGraph(unrestrictedAlias.upperBound, prefix + "-pts-top");
			PointsTo ptsMax = MoJo.computePointsTo(unrestrictedAlias.upperBound, gOut);
			writeToFile(ptsMax, "out/" + prefix + "-top.pts");
			System.out.print("done, call graph... ");
			AnalysisOptions optPtsMax = mojo.createAnalysisOptionsWithPTS(ptsMax, method);
			CallGraphResult maxCG = mojo.computeContextSensitiveCallGraph(optPtsMax);
			Util.dumpSSA(maxCG.cg.getFakeRootNode().getIR(), new PrintStream("out/" + prefix + "-ssa-top.txt"));
			edu.kit.joana.deprecated.jsdg.util.Util.dumpHeapGraph(prefix + "-max", maxCG.pts.getHeapGraph(), null);
			System.out.print("done, sdg... ");
			// run analysis on callgraph with maximal alias configuration
			// ...
			SDG maxSDG = createSDG(maxCG, optPtsMax, method, ignoreExceptions);
			SDGSerializer.toPDGFormat(maxSDG, new BufferedOutputStream(new FileOutputStream("out/" + prefix + "-top.pdg")));
			System.out.print("(" + maxSDG.edgeSet().size() + " edges) done, check ifc... ");

			isFlowOk &= checkIsStatementCorrect(ifc, maxSDG, ptsMax, bcMethodName, prefix + "-top");
			System.out.println("done.");
		}

		return isFlowOk;
	}

	private static SDG createSDG(CallGraphResult cgResult, AnalysisOptions opt, IMethod method,
			boolean ignoreExceptions) throws CancelException, PDGFormatException, WalaException {
		DemandRefinementPointsTo demandPts = null;
//		if (cfg.useDemandPts) {
//		    MemoryAccessMap mam = new PABasedMemoryAccessMap(cg, builder.getPointerAnalysis());
//			demandPts = new DemandRefinementPointsTo(cg,
//				new ThisFilteringHeapModel(builder,cha), mam, cha, options,
//			        getStateMachineFactory());
//		}

		IPointerAnalysis pts = new PointsToWrapper(demandPts, cgResult.pts);
		IProgressMonitor progress =
			NullProgressMonitor.INSTANCE;
			//new VerboseProgressMonitor(System.out);
		IKey2Origin k2o = null;
		Config cfg = new Config();
		cfg.computeSummaryEdges = true;
		cfg.useSummaryOpt = false;
		cfg.addControlFlow = true;
		cfg.computeInterference = false;
		cfg.ignoreExceptions = ignoreExceptions;
		cfg.optimizeExceptions = false;//!ignoreExceptions;
		cfg.nonTermination = false;

		edu.kit.joana.deprecated.jsdg.sdg.SDG jSDG = edu.kit.joana.deprecated.jsdg.sdg.SDG.create(method, cgResult.cg, cgResult.cache, k2o, pts, cfg, progress);

		SDG sdg = JoanaStyleSDG.createJoanaSDG(jSDG, cfg.addControlFlow, cfg.nonTermination, cfg.useSummaryOpt, progress);

		final Logger log = Log.getLogger(Log.L_WALA_CORE_DEBUG);

        if (cfg.computeSummaryEdges) {
            progress.subTask("Compute Summary Edges");
            log.outln("Compute Summary Edges");
            SummaryEdgeComputation.compute(sdg, progress);

            log.outln("Summary Edges done.");
            progress.done();
        }

		return sdg;
	}

	private static SDGNode searchEntry(SDG sdg, String bcMethodName) {
		for (SDGNode node : sdg.vertexSet()) {
			if (node.kind == Kind.ENTRY && bcMethodName.equals(node.getBytecodeName())) {
				return node;
			}
		}

		return null;
	}

	private static boolean checkIsStatementCorrect(IFCStmt ifc, SDG sdg, PointsTo pts, String bcMethodName,
			String prefix) throws FlowAstException, FileNotFoundException {
		final SDGNode entry = searchEntry(sdg, bcMethodName);

		final Set<SDGNode> nodesIn = sdg.getFormalInsOfProcedure(entry);
		final Set<SDGNode> nodesOut = sdg.getFormalOutsOfProcedure(entry);
		final SummaryGraph<SDGNode> summary = IntraprocSummaryEdges.compute(sdg, entry, nodesIn, nodesOut);

		IntraprocSummaryEdges.writeToDotFile(summary, "out/summary-" + prefix + ".dot", summary.toString() + " of " + prefix);

		boolean flowOk = true;

		// match sdg form in-out with ifc in-out
		int numFlow = 0;
		for (FlowStmt flow : ifc.getFlowStmts()) {
			numFlow++;
			final Map<SimpleParameter, Set<SDGNode>> param2node =
				MatchFlowParamWithSDGParam.mapParams2Nodes(sdg, nodesIn, nodesOut, flow);

			writeMappingToFile(nodesIn, nodesOut, param2node, "out/param2node-" + prefix + "-" + numFlow + ".txt");

///			printOutMapping(param2node);

			Set<FlowEdge> illegal = FlowChecker.searchIllegalFlow(param2node, summary, flow);

			if (!illegal.isEmpty()) {
				System.out.print("{ ");
				for (FlowEdge ill : illegal) {
					System.out.print(ill + " ");
				}
				System.out.print("} ");

				flowOk = false;
			}
		}

		if (!flowOk) {
			System.out.print("flow ILLEGAL ");
		} else {
			System.out.print("flow ok ");
		}

		return flowOk;
	}

	private static void writeMappingToFile(Set<SDGNode> nodesIn, Set<SDGNode> nodesOut,
			Map<SimpleParameter, Set<SDGNode>> map, String file) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(file);
		pw.println("Input nodes:");
		for (SDGNode in : nodesIn) {
			pw.println("[" + in.getId() + "|" + in.getKind() + "]" + in.getLabel());
		}
		pw.println("Output nodes:");
		for (SDGNode out : nodesOut) {
			pw.println("[" + out.getId() + "|" + out.getKind() + "]" + out.getLabel());
		}

		pw.println("Param 2 Node Mapping:");
		for (SimpleParameter key : map.keySet()) {
			pw.print(key.toString() + ": { ");
			for (SDGNode node : map.get(key)) {
				pw.print("[" + node.getId() + "|" + node.getKind() + "]" + node.getLabel() + " ");
			}
			pw.println("}");
		}
		pw.flush();
		pw.close();
	}

	private static int counter = 0;
	private static final int MAX_LABEL_LENGTH = 50;

	private static String sanitizeLabel(String str) {
		if (str.length() > MAX_LABEL_LENGTH) {
			str = str.substring(0, MAX_LABEL_LENGTH - 1) + "_" + counter++;
		}

		return str;
	}

	private static void writeToFile(PointsTo pts, String fileName) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(fileName);
		out.println(pts.toString());
		out.flush();
		out.close();
	}
}
