/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.jsdg;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;

import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.output.JoanaStyleSDG;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.jsdg.EvaluationRunner.CFG;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges.SummaryGraph;

public class EvaluateMinMaxContextEffect {

	public static final CFG[] EVAL_CFGS = {
//		new CFG("../MoJo-FlowLess/examples/project1", "../MoJo-FlowLess/bin", "java\\/awt\\/.*\n"
//				+ "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n" + "sun\\/swing\\/.*\n"
//				+ "com\\/sun\\/.*\n" + "sun\\/.*\n"	+ "java\\/nio\\/.*\n" + "javax\\/.*\n"
//				+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n"
//				+ "java\\/text\\/.*\n" + "java\\/io\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
//				+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
		new CFG("../MoJo-FlowLess/examples/project1", "../MoJo-FlowLess/bin", "java\\/awt\\/.*\n"
				+ "javax\\/swing\\/.*\n" + "sun\\/awt\\/.*\n" + "sun\\/swing\\/.*\n"
				+ "com\\/sun\\/.*\n" + "sun\\/.*\n"	+ "java\\/nio\\/.*\n" + "javax\\/.*\n"
				+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n"
				+ "java\\/text\\/.*\n" + "java\\/io\\/.*\n" + "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
				+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n"),
///		new CFG("src", "bin"),
	};


	public static void main(String[] args) throws IllegalArgumentException, CancelException, NoSuchElementException,
			FlowAstException, IOException, PDGFormatException, WalaException {
		Analyzer.cfg = new Config();
		Analyzer.cfg.outputDir = "out/";


		for (CFG cfg : EVAL_CFGS) {
			run(cfg);
		}
	}

	public static void run(CFG cfg) throws IllegalArgumentException, CancelException, NoSuchElementException,
	FlowAstException, IOException, PDGFormatException, WalaException {
		System.out.print("Creating MoJo... ");

		AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.bin, null);
		if (cfg.exclusions != null) {
			SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
			scope.setExclusions(exclusions);
		}

		ClassHierarchy cha = ClassHierarchy.make(scope);
		MoJo mojo = MoJo.create(cha);

		System.out.println("done.");

		for (IClass cls : cha) {
			if (cls.isArrayClass() || cls.isInterface()) {
				continue;
			}

			for (IMethod im : cls.getDeclaredMethods()) {
				if (im.isAbstract() || im.isNative() || im.isClinit() || im.isSynthetic() || im.isInit()
						|| !Util.methodName(im).contains("edu.kit.")) {
					continue;
				}

				// compute # alias configurations
				// compute min-max aliases
				System.out.print("Working on " + Util.methodName(im) + ": min-max aliases... ");
				final Aliasing minMax = mojo.computeMinMaxAliasing(im);
				System.out.print("ok. ");

				if (noDifference(minMax)) {
					System.out.println("NO DIFF in aliasing cfg.");
					continue;
				}

				final String bcMethodName = im.getSignature();

				int minEdge = 0;
				int maxEdge = 0;

				final String label = sanitizeLabel(Util.methodName(im));

				{
					// minimal aliasing
					System.out.print("MIN: ");
//					System.out.print("points-to... ");
//					PrettyWalaNames.dumpGraph(minMax.lowerBound, label + "-pts-min");
					PointsTo ptsMin = MoJo.computePointsTo(minMax.lowerBound);
//					System.out.print("done, call graph... ");
					System.out.print("1");
					AnalysisOptions optPtsMin = mojo.createAnalysisOptionsWithPTS(ptsMin, im);
					System.out.print("2");
					CallGraphResult minCG = mojo.computeContextSensitiveCallGraph(optPtsMin);
					System.out.print("3");
					final Set<CGNode> roots = minCG.cg.getNodes(im.getReference());
					Util.dumpCallGraph(minCG.cg, roots, label + "-min", null);
					if (minCG.pts.getHeapGraph() == null) {
						throw new IllegalStateException();
					}
					Util.dumpHeapGraph(label + "-min", minCG.pts.getHeapGraph(), null);
//					System.out.print("done, sdg... ");
					// run analysis on callgraph with minimal alias configuration
					try {
						SDG minSDG = createSDG(minCG, optPtsMin, im);
						System.out.print("4");
						minEdge = countSummaryEdges(minSDG, bcMethodName, label + "-min");
						System.out.print("5");
		//				SDGSerializer.toPDGFormat(minSDG, new BufferedOutputStream(new FileOutputStream("out/" + prefix + "-min.pdg")));
//						System.out.print("(" + minEdge + " edges) done. ");
						System.out.print(" OK.   ");
					} catch (Exception exc) {
						exc.printStackTrace();
						// fail
//						System.out.print("failed (" + exc.getMessage() + ")");
						System.out.print(" FAIL. ");
					}
				}

				{
					// maximal aliasing
					System.out.print("MAX: ");
//					System.out.print("points-to... ");
					Util.dumpGraph(minMax.upperBound, label + "-pts-max");
					PointsTo ptsMax = MoJo.computePointsTo(minMax.upperBound);
//					System.out.print("done, call graph... ");
					System.out.print("1");
					AnalysisOptions optPtsMax = mojo.createAnalysisOptionsWithPTS(ptsMax, im);
					System.out.print("2");
					CallGraphResult maxCG = mojo.computeContextSensitiveCallGraph(optPtsMax);
					System.out.print("3");
					final Set<CGNode> roots = maxCG.cg.getNodes(im.getReference());
					Util.dumpCallGraph(maxCG.cg, roots, label + "-max", null);
					Util.dumpHeapGraph(label + "-max", maxCG.pts.getHeapGraph(), null);
//					System.out.print("done, sdg... ");
					// run analysis on callgraph with minimal alias configuration
					try {
						SDG maxSDG = createSDG(maxCG, optPtsMax, im);
		//				SDGSerializer.toPDGFormat(minSDG, new BufferedOutputStream(new FileOutputStream("out/" + prefix + "-min.pdg")));
						System.out.print("4");
						maxEdge = countSummaryEdges(maxSDG, bcMethodName, label + "-max");
						System.out.print("5");

//						System.out.print("(" + maxEdge + " edges) done. ");
						System.out.print(" OK.   ");
					} catch (Exception exc) {
						exc.printStackTrace();
						// fail
//						System.out.print("failed (" + exc.getMessage() + ")");
						System.out.print(" FAIL. ");
					}
				}

//				System.out.println();

				if (minEdge != maxEdge) {
					System.out.println("DIFF: " + minEdge + " <-> " + maxEdge + " = " + (maxEdge - minEdge));
				} else {
					System.out.println("NO DIFF");
				}

			}
		}
		System.out.println("done.");
	}

	private static int counter = 0;

	private static String sanitizeLabel(String str) {
		if (str.length() > 40) {
			str = str.substring(0, 39) + "_" + counter++;
		}

		return str;
	}

	private static int countSummaryEdges(SDG sdg, String bcMethodName, String prefix) throws FileNotFoundException {
		final SDGNode entry = searchEntry(sdg, bcMethodName);

		final Set<SDGNode> formIn = sdg.getFormalInsOfProcedure(entry);
		final Set<SDGNode> formOut = sdg.getFormalOutsOfProcedure(entry);

		final SummaryGraph<SDGNode> summary = IntraprocSummaryEdges.compute(sdg, entry, formIn, formOut);

		IntraprocSummaryEdges.writeToDotFile(summary, "out/summary-" + prefix + ".dot", summary.toString() + " of " + prefix);

		return summary.edgeSet().size();
	}

	private static SDGNode searchEntry(SDG sdg, String bcMethodName) {
		for (SDGNode node : sdg.vertexSet()) {
			if (node.kind == Kind.ENTRY && bcMethodName.equals(node.getBytecodeName())) {
				return node;
			}
		}

		return null;
	}

	private static boolean noDifference(Aliasing alias) {
		for (PtsParameter p : alias.lowerBound) {
			for (Iterator<PtsParameter> succs = alias.lowerBound.getSuccNodes(p); succs.hasNext();) {
				PtsParameter succ = succs.next();
				if (!alias.upperBound.hasEdge(p, succ)) {
					return false;
				}
			}
		}

		for (PtsParameter p : alias.upperBound) {
			for (Iterator<PtsParameter> succs = alias.upperBound.getSuccNodes(p); succs.hasNext();) {
				PtsParameter succ = succs.next();
				if (!alias.lowerBound.hasEdge(p, succ)) {
					return false;
				}
			}
		}

		return true;
	}

	private static SDG createSDG(CallGraphResult cgResult, AnalysisOptions opt, IMethod method) throws CancelException, PDGFormatException, WalaException {
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
		cfg.ignoreExceptions = true;
		cfg.nonTermination = false;
		cfg.outputDir = "out/";

		edu.kit.joana.deprecated.jsdg.sdg.SDG jSDG = edu.kit.joana.deprecated.jsdg.sdg.SDG.create(method, cgResult.cg, cgResult.cache, k2o, pts, cfg, progress);

		SDG sdg = JoanaStyleSDG.createJoanaSDG(jSDG, cfg.addControlFlow, cfg.nonTermination, cfg.useSummaryOpt, progress);

		return sdg;
	}

}
