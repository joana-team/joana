package edu.kit.joana.wala.eval;
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.deprecated.jsdg.output.JoanaStyleSDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryEdgeComputation;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;
import edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.IKey2Origin;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import edu.kit.joana.wala.core.ExternalCallCheck.MethodListCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.dictionary.Dictionary;
import edu.kit.joana.wala.dictionary.MergeModules;
import edu.kit.joana.wala.dictionary.MergeModules.ModuleCFG;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.AliasGraphException;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PartialOrder;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.util.AliasGraphIO;
import edu.kit.joana.wala.jsdg.optimize.RemoveLibraryClinits;
import edu.kit.joana.wala.jsdg.optimize.StaticFieldMerge;
import edu.kit.joana.wala.jsdg.optimize.SummarizeDependencies;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges;
import edu.kit.joana.wala.jsdg.summary.IntraprocSummaryEdges.SummaryGraph;


public class RunTestModular {

	/**
	 * @param args
	 * @throws CancelException
	 * @throws UnsoundGraphException
	 * @throws IOException
	 * @throws WalaException
	 * @throws PDGFormatException
	 * @throws IllegalArgumentException
	 */
	public static void main(String[] args) throws IOException, UnsoundGraphException, CancelException,
			IllegalArgumentException, PDGFormatException, WalaException {
		// check for ifc annotations

		System.out.print("Parsing source files for ifc annotations... ");
		List<ClassInfo> clsInfos = MoJo.parseSourceFiles("../joana.wala.modular.testdata/src");
		System.out.println("done.");
		System.out.print("Checking for syntactic errors... ");
		final int errors = MoJo.prepareFlowLessStmts(clsInfos);
		if (errors > 0) {
			System.out.print("(" + errors + " errors) ");
		}
		System.out.println("done.");

		// prepare base sdg

		MethodListCheck mlc = new MethodListCheck(null, "./out/", /* do debug output */ false);

		for (ClassInfo cls : clsInfos) {
			for (MethodInfo m : cls.getMethods()) {
				if (m.hasIFCStmts()) {
					// mark as external call targets
//					System.err.println(m.toString());
					mlc.addMethod(m);
				}
			}
		}

//		mlc.addMethod("module.Module.encrypt(II)I");
//		mlc.addMethod("module.Module.encrypt(Lmodule/Module$Message;I)Lmodule/Module$Message;");

		Config cfg = new Config("program", "program.Program.main([Ljava/lang/String;)V",
				"../joana.wala.modular.testdata/dist/mojo-test-program.jar",
				PointsToPrecision.CONTEXT_SENSITIVE, ExceptionAnalysis.INTRAPROC, false, Main.STD_EXCLUSION_REG_EXP,
				"../../contrib/lib/stubs/natives_empty.xml", "../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar", mlc,
				"./out/", FieldPropagation.FLAT);

		Main.run(System.out, cfg);

		// precompute library methods
		final String binlib1 = "../joana.wala.modular.testdata/dist/mojo-test-modules1.jar";
		runComputeModule(binlib1, cfg, "./out/lib_module1");

		final String binlib2 = "../joana.wala.modular.testdata/dist/mojo-test-modules2.jar";
		runComputeModule(binlib2, cfg, "./out/lib_module2");

		// merge precomputed dependency graphs
		final ModuleCFG mainModule = new ModuleCFG("program.Program.main([Ljava/lang/String;)V",
				"../joana.wala.modular.testdata/dist/mojo-test-program.jar");
		final ModuleCFG[] otherModules1 = new ModuleCFG[] {
				new ModuleCFG("module1", "../joana.wala.modular.testdata/dist/mojo-test-modules1.jar")
		};
		MergeModules.runMergeModules("merge_modules1", "./out/", mainModule, otherModules1, mlc);

		final ModuleCFG[] otherModules2 = new ModuleCFG[] {
				new ModuleCFG("module2", "../joana.wala.modular.testdata/dist/mojo-test-modules2.jar")
		};
		MergeModules.runMergeModules("merge_modules2", "./out/", mainModule, otherModules2, mlc);

	}

	private static void runComputeModule(final String binDir, final Config cfg, final String outputDir)
	throws IOException, IllegalArgumentException, CancelException, PDGFormatException, WalaException {
		Main.checkOrCreateOutputDir(outputDir);

		System.out.print("Setting up analysis scope... ");

		// Fuegt die normale Java Bibliothek zum Scope hinzu
		AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

		if (cfg.nativesXML != null) {
			com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);
		}

		// if use stubs
		if (cfg.stubs != null) {
			scope.addToScope(ClassLoaderReference.Primordial, new JarFile(cfg.stubs));
		}

		// Nimmt unnoetige Klassen raus
		SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
		scope.setExclusions(exclusions);

	    ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
	    AnalysisScopeReader.addClassPathToScope(binDir, scope, loader);

	    System.out.println("done.");

		ClassHierarchy cha = ClassHierarchy.make(scope);

		System.out.print("Creating MoJo... ");

		MoJo mojo = MoJo.create(cha);

		System.out.println("done.");

		for (IClass cls : cha) {
			for (IMethod im : cls.getDeclaredMethods()) {
				if (im.getDeclaringClass().isInterface() || im.isAbstract()) {
					// skip methods without a body
					continue;
				}

				MethodInfo nfo = cfg.extern.checkForModuleMethod(im);
				if (nfo != null) {
					System.out.println("Found " + nfo + " for " + im);
					final String sig = Dictionary.extractSignature(im);
					final String outputMethodDir = outputDir
							+ (outputDir.endsWith(File.separator) ? "" : File.separator) + "m_" + sig;
					Main.checkOrCreateOutputDir(outputMethodDir);
					computeVariants(mojo, im, nfo, outputMethodDir);
				}
			}
		}

	}


	private static void computeAliasVariant(final MoJo mojo, final MayAliasGraph alias, final IMethod method,
			final boolean ignoreExceptions, final String outDir)
	throws IllegalArgumentException, CancelException, PDGFormatException, WalaException, FileNotFoundException {
		final Logger debug = Log.getLogger(Log.L_MOJO_DEBUG);
		final Logger info = Log.getLogger(Log.L_MOJO_INFO);
		
		info.out("Preparing points-to config and call graph...");
		final PointsTo pts = MoJo.computePointsTo(alias);
		if (debug.isEnabled()) { AliasGraphIO.dumpToDot(alias, outDir + ".alias.dot"); }
		AliasGraphIO.writeOut(alias, new FileOutputStream(outDir + ".alias"));

		final AnalysisOptions optPts = mojo.createAnalysisOptionsWithPTS(pts, method);
		final CallGraphResult cg = mojo.computeContextSensitiveCallGraph(optPts);
		if (debug.isEnabled()) {
			final PrintStream ssaOut = new PrintStream(outDir + ".ssa.txt");
			Util.dumpSSA(cg.cg.getFakeRootNode().getIR(), ssaOut);
			Util.dumpPhiSSA(cg.cg.getFakeRootNode().getIR(), ssaOut);
			ssaOut.flush();
			ssaOut.close();
			
			Util.dumpHeapGraphToFile(outDir + ".heap.dot", cg.pts.getHeapGraph(), cg.cg.getFakeRootNode().getMethod());
		}
		info.out("done, sdg... ");
		// run analysis on callgraph with minimal alias configuration
		// ...
		final edu.kit.joana.ifc.sdg.graph.SDG sdg = createSDG(cg, optPts, method, ignoreExceptions);
		SDGSerializer.toPDGFormat(sdg, new BufferedOutputStream(new FileOutputStream(outDir + ".pdg")));
		info.out("(" + sdg.edgeSet().size() + " edges)");

		if (debug.isEnabled()) {
			final int summary = outputSummaryEdges(sdg, method.getReference().getSignature(), outDir + ".sum.dot");
			debug.out(" (" + summary + " sum)");
		}

		info.outln(" done.");
	}

	private static int outputSummaryEdges(final SDG sdg, final String bcMethodName, final String filename) throws FileNotFoundException {
		final SDGNode entry = searchEntry(sdg, bcMethodName);

		Set<SDGNode> formIn = sdg.getFormalInsOfProcedure(entry);
		formIn = filterStatic(sdg, formIn);
		Set<SDGNode> formOut = sdg.getFormalOutsOfProcedure(entry);
		formOut = filterStatic(sdg, formOut);

		final SummaryGraph summary = IntraprocSummaryEdges.compute(sdg, entry, formIn, formOut);

		IntraprocSummaryEdges.writeToDotFile(summary, filename, summary.toString());

		return summary.edgeSet().size();
	}

	private static Set<SDGNode> filterStatic(final SDG sdg, final Set<SDGNode> nodes) {
		final Set<SDGNode> filtered = new HashSet<SDGNode>();

		for (SDGNode n : nodes) {
			if (n.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER) {
				filtered.add(n);
			}
		}

		LinkedList<SDGNode> work = new LinkedList<SDGNode>();
		work.addAll(filtered);
		while (!work.isEmpty()) {
			final SDGNode n = work.removeFirst();

			for (SDGEdge edge : sdg.getOutgoingEdgesOfKind(n, SDGEdge.Kind.PARAMETER_STRUCTURE)) {
				final SDGNode tgt = edge.getTarget();
				if (!filtered.contains(tgt)) {
					filtered.add(tgt);
					work.add(tgt);
				}
			}
		}

		return filtered;
	}

	private static SDGNode searchEntry(SDG sdg, String bcMethodName) {
		for (SDGNode node : sdg.vertexSet()) {
			if (node.kind == Kind.ENTRY && bcMethodName.equals(node.getBytecodeName())) {
				return node;
			}
		}

		return null;
	}

	private static void computeAliasVariants(MoJo mojo, Aliasing aliasing, IMethod method, boolean ignoreExceptions,
			final String outDir) throws IllegalArgumentException, FileNotFoundException, CancelException,
			PDGFormatException, WalaException {
		computeAliasVariant(mojo, aliasing.lowerBound, method, ignoreExceptions, outDir + "-low");

		final PartialOrder.Cmp cmp = aliasing.lowerBound.compareTo(aliasing.upperBound);
		switch (cmp) {
		case SMALLER:
			// ok we have a difference -> continue
			computeAliasVariant(mojo, aliasing.upperBound, method, ignoreExceptions, outDir + "-up");
			break;
		case EQUAL:
			// no difference -> we can stop here
			System.out.println("no diff between max and min aliasing of " + method);
			return;
		case BIGGER:
			System.err.println("min is smaller as max aliasing of " + method);
			return;
		case UNCOMPARABLE:
			System.err.println("uncomparable max and min aliasing of " + method);
			return;
		}
	}

	private static void computeVariants(final MoJo mojo, final IMethod im, final MethodInfo nfo, final String outDir)
	throws IllegalArgumentException, FileNotFoundException, CancelException, PDGFormatException, WalaException {
		final boolean ignoreExceptions = true;
		final Aliasing minMax = mojo.computeMinMaxAliasing(im);

		computeAliasVariants(mojo, minMax, im, ignoreExceptions, outDir + File.separator + "minmax");

		if (nfo.hasIFCStmts()) {
			int num = 0;
			for (IFCStmt ifc : nfo.getIFCStmts()) {
				num++;

				if (ifc.hasAliasStmt()) {
					try {
						final Aliasing stmtAlias = mojo.computeMayAliasGraphs(nfo, ifc);

						computeAliasVariants(mojo, stmtAlias, im, ignoreExceptions, outDir + File.separator + "ifc" + num);
					} catch (AliasGraphException exc) {
						System.err.println("Could not compute alias graph for " + nfo);
						System.err.println(exc.getMessage());
						exc.printStackTrace();
					} catch (NoSuchElementException exc) {
						System.err.println("Could not compute alias graph for " + nfo);
						System.err.println(exc.getMessage());
						exc.printStackTrace();
					} catch (FlowAstException exc) {
						System.err.println("Could not compute alias graph for " + nfo);
						System.err.println(exc.getMessage());
						exc.printStackTrace();
					}
				}
			}
		}

		//TODO do pair method param alias
	}

	private static edu.kit.joana.ifc.sdg.graph.SDG createSDG(CallGraphResult cgResult, AnalysisOptions opt,
			IMethod method,	boolean ignoreExceptions) throws CancelException, PDGFormatException, WalaException {
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
		edu.kit.joana.deprecated.jsdg.SDGFactory.Config cfg = new edu.kit.joana.deprecated.jsdg.SDGFactory.Config();
		cfg.computeSummaryEdges = true;
		cfg.useSummaryOpt = false;
		cfg.addControlFlow = true;
		cfg.computeInterference = false;
		cfg.ignoreExceptions = ignoreExceptions;
		cfg.optimizeExceptions = false;//!ignoreExceptions;
		cfg.nonTermination = false;

		cfg.immutables = new String[] {"java.lang.String", "java.lang.Integer", "java.lang.Float", "java.lang.Double", "java.lang.Boolean", "java.lang.Character" };

		edu.kit.joana.deprecated.jsdg.sdg.SDG jSDG = edu.kit.joana.deprecated.jsdg.sdg.SDG.create(method, cgResult.cg, cgResult.cache, k2o, pts, cfg, progress);

		edu.kit.joana.ifc.sdg.graph.SDG sdg = JoanaStyleSDG.createJoanaSDG(jSDG, cfg.addControlFlow, cfg.nonTermination, cfg.useSummaryOpt, progress);

		RemoveLibraryClinits.removeLibraryClinits(sdg);
		StaticFieldMerge.mergeStaticFields(sdg);

		final Logger log = Log.getLogger(Log.L_WALA_CORE_DEBUG);
		
        if (cfg.computeSummaryEdges) {
            progress.subTask("Compute Summary Edges");
            log.outln("Compute Summary Edges");
            SummaryEdgeComputation.compute(sdg, progress);

            log.outln("Summary Edges done.");
            progress.done();
        }

        SummarizeDependencies.transformToSummary(sdg, method);

		return sdg;
	}
}
