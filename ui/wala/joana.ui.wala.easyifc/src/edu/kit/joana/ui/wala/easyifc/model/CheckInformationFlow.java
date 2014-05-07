/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.MethodResult;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

public final class CheckInformationFlow {

	public static class CheckIFCConfig {
		public static final String DEFAULT_TMP_OUT_DIR = "./out/";
		public static final String DEFAULT_LIB_DIR = "../jSDG/lib/";

		public final String bin;
		public final String src;
		public final String tmpDir;
		public final String libDir;
		public final PrintStream out;
		public final IFCCheckResultConsumer results;
		public final IProgressMonitor progress;
		public boolean printStatistics = true;
		public AnalysisScope scope = null;

		public CheckIFCConfig(final String bin, final String src) {
			this(bin, src, DEFAULT_TMP_OUT_DIR, DEFAULT_LIB_DIR, System.out, IFCCheckResultConsumer.DEFAULT,
					NullProgressMonitor.INSTANCE);
		}

		public CheckIFCConfig(final String bin, final String src, final PrintStream out) {
			this(bin, src, DEFAULT_TMP_OUT_DIR, DEFAULT_LIB_DIR, out, IFCCheckResultConsumer.STDOUT,
					NullProgressMonitor.INSTANCE);
		}

		public CheckIFCConfig(final String bin, final String src, final String tmpDir, final String libDir,
				final PrintStream out, final IFCCheckResultConsumer results, IProgressMonitor progress) {
			if (src == null) {
				throw new IllegalArgumentException("src directory is null.");
			} else if (bin == null) {
				throw new IllegalArgumentException("bin directory is null.");
			} else if (tmpDir == null) {
				throw new IllegalArgumentException("tmpDir directory is null.");
			} else if (libDir == null) {
				throw new IllegalArgumentException("libDir directory is null.");
			} else if (out == null) {
				throw new IllegalArgumentException("output stream is null.");
			} else if (results == null) {
				throw new IllegalArgumentException("result consumer is null.");
			} else if (progress == null) {
				throw new IllegalArgumentException("progressmonitor is null.");
			}

			this.src = src;
			this.bin = bin;
			this.tmpDir = tmpDir;
			this.libDir = libDir;
			this.out = out;
			this.results = results;
			this.progress = progress;
		}

		public String toString() {
			return "check flowless at src(" + src + "), bin(" + bin + ")";
		}
	}

	@SuppressWarnings("resource")
	public static PrintStream createPrintStream(final String file) {
		PrintStream ps;

		try {
			ps = new PrintStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
			ps = System.out;
			ps.println("Could not open file '" + file + "'. Directing output to stdout.");
		}

		return ps;
	}

	public static void main(String[] argv) {
		final CheckIFCConfig[] RUNS = new CheckIFCConfig[] {
				new CheckIFCConfig("../MoJo-TestCode/bin", "../MoJo-TestCode/src", createPrintStream("check_flow.log")),
//				new CheckFlowConfig("../../3.7/runtime-EclipseApplication/eVoting-Joana/bin", "../../3.7/runtime-EclipseApplication/eVoting-Joana/src", createPrintStream("cf_evoting.log")),
//				new CheckFlowConfig("../../3.7/workspace-ifc/eVoting-Joana/bin", "../../3.7/workspace-ifc/eVoting-Joana/src", createPrintStream("cf_evoting.log")),
//				new CheckFlowConfig("../MoJo-TestCode/bin", "../MoJo-TestCode/src2", createPrintStream("check_flow.log")),
//				new CheckFlowConfig("../MoJo-TestCode/bin", "../MoJo-TestCode/src3", createPrintStream("check_flow.log")),
		};

		for (final CheckIFCConfig run : RUNS) {
			try {
				final CheckInformationFlow check = new CheckInformationFlow(run);
				check.runCheckFlowLess();
			} catch (ClassHierarchyException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (CancelException e) {
				e.printStackTrace();
			} catch (UnsoundGraphException e) {
				e.printStackTrace();
			}
		}
	}

	private final CheckIFCConfig cfc;

	public CheckInformationFlow(final CheckIFCConfig cfc) {
		this.cfc = cfc;
		this.printStatistics = cfc.printStatistics;
	}

	private static final String WITH_EXC_SUFFIX = "cf_with_exc";
	private static final String NO_EXC_SUFFIX = "cf_no_exc";

	private final boolean printStatistics;
	private long timePreprareSDG = 0;
	private long numPreparedSDGs = 0;
	private long timeAdjustSDG = 0;
	private long numAdjustSDGs = 0;
	private long startPrepareTime, endPrepareTime;
//	private long startAdjustTime, endAdjustTime;

	public void runCheckFlowLess() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException, UnsoundGraphException {
		cfc.out.print("Parsing source files... ");
		List<ClassInfo> clsInfos = MoJo.parseSourceFiles(cfc.src);
		cfc.out.println("done.");
		cfc.out.print("Checking for syntactic errors... ");
		final int errors = MoJo.prepareFlowLessStmts(clsInfos);
		if (errors > 0) {
			cfc.out.print("(" + errors + " errors) ");
		}
		cfc.out.println("done.");

//		final MethodListCheck mlc = new MethodListCheck(null, cfc.tmpDir, /* do debug output */ false);
//
//		for (ClassInfo cls : clsInfos) {
//			for (MethodInfo m : cls.getMethods()) {
//				if (m.hasIFCStmts()) {
//					// mark as external call targets
////					System.err.println(m.toString());
//					mlc.addMethod(m);
//				}
//			}
//		}

		final Config cfg = new Config(WITH_EXC_SUFFIX);
		cfg.entryMethod = "<main entry not used>";
		cfg.classpath=  cfc.bin;
		cfg.pts = PointsToPrecision.INSTANCE_BASED;
		cfg.exceptions = ExceptionAnalysis.INTRAPROC;
		cfg.accessPath = true;
		cfg.exclusions = Main.STD_EXCLUSION_REG_EXP;
		cfg.nativesXML = cfc.libDir + "natives_empty.xml";
		cfg.stubs = cfc.libDir + "jSDG-stubs-jre1.4.jar";
		cfg.extern = ExternalCallCheck.EMPTY;
		cfg.outputDir = cfc.tmpDir;
		cfg.fieldPropagation = FieldPropagation.OBJ_TREE;

		cfc.out.println(cfg);

		if (cfc.scope == null) {
			cfc.out.print("Setting up analysis scope... ");
			cfc.scope = createAnalysisScope(cfg);
		    cfc.out.println("done.");
		} else {
			cfc.out.println("Using provided analysis scope.");
		}

		cfc.out.print("Creating MoJo... (class hierarchy: ");
		final ClassHierarchy cha = ClassHierarchy.make(cfc.scope);
		cfg.extern.setClassHierarchy(cha);
		cfc.out.print(cha.getNumberOfClasses() + " classes) ");
		final MoJo mojo = MoJo.create(cha, cfg.outputDir);
		cfc.out.println("done.");

		for (final ClassInfo cls : clsInfos) {
			for (final MethodInfo m : cls.getMethods()) {
				MonitorUtil.throwExceptionIfCanceled(cfc.progress);

				if (m.hasIFCStmts() && !m.hasErrors()) {
					final IMethod start = mojo.findMethod(m);
					final MethodResult mres = new MethodResult(m, cfg.outputDir);
					checkFlowLessForMethod(start, mres, mojo, cfg, cfc.progress);
					if (!mres.isAllValid()) {
						cfg.name = NO_EXC_SUFFIX;
						cfg.exceptions = ExceptionAnalysis.IGNORE_ALL;
						cfc.out.println("Without exceptions:");
						checkFlowLessForMethod(start, mres, mojo, cfg, cfc.progress);
						cfg.exceptions = ExceptionAnalysis.INTRAPROC;
						cfg.name = WITH_EXC_SUFFIX;
					}
					cfc.results.consume(mres);
				} else if (m.hasErrors()) {
					cfc.out.println("Found " + m.getErrors().size()
							+ " errors in flowless ifc annotation of " + m + " - skipping method.");
					for (final FlowError ferr : m.getErrors()) {
						cfc.out.println("\t" + ferr);
					}
					final MethodResult mres = new MethodResult(m, cfg.outputDir);
					cfc.results.consume(mres);
				}
			}
		}

		if (printStatistics) {
			System.out.println("Total prepared SDGs     : " + numPreparedSDGs);
			System.out.println("Total prepared SDGs time: " + timePreprareSDG);
			System.out.println("Total adjusted SDGs     : " + numAdjustSDGs);
			System.out.println("Total adjusted SDGs time: " + timeAdjustSDG);
			final long avgPrepare = (numPreparedSDGs > 0 ? (timePreprareSDG / numPreparedSDGs) : 0);
			final long avgAdjust = (numAdjustSDGs > 0 ? (timeAdjustSDG / numAdjustSDGs) : 0);
			System.out.println("Avg. prepared SDGs time : " + avgPrepare);
			System.out.println("Avg. adjusted SDGs time : " + avgAdjust);
			System.out.println("Speed gain by adjust: " + (avgAdjust > 0 ? (avgPrepare / avgAdjust) : 0) + "x faster");
			numPreparedSDGs = 0;
			timePreprareSDG = 0;
			numAdjustSDGs = 0;
			timeAdjustSDG = 0;
		}
	}

	private void checkFlowLessForMethod(final IMethod im, final MethodResult m, final MoJo mojo, final Config cfg,
			final IProgressMonitor progress)
			throws IllegalArgumentException, CancelException, ClassHierarchyException, IOException, UnsoundGraphException {
		if (printStatistics) { startPrepareTime = System.currentTimeMillis(); }
		cfc.out.println("Checking '" + m + "'");
		final Aliasing minMax = mojo.computeMinMaxAliasing(im);
		final PointsTo ptsMax = MoJo.computePointsTo(minMax.upperBound);
		final AnalysisOptions opt = mojo.createAnalysisOptionsWithPTS(ptsMax, im);
		final CallGraphResult cgr;
		switch (cfg.pts) {
		case TYPE_BASED:
			cgr = mojo.computeContextSensitiveCallGraph(opt);
			break;
		case INSTANCE_BASED:
			cgr = mojo.computeContextSensitiveCallGraph(opt);
			break;
		case OBJECT_SENSITIVE:
			cgr = mojo.computeObjectSensitiveCallGraph(opt, cfg.objSensFilter);
			break;
		default:
			throw new IllegalStateException();
		}

		final SDG sdg = create(cfc.out, opt.getAnalysisScope(), mojo, cgr, im, cfg.outputDir, cfg);

		boolean resetNeeded = false;

		if (printStatistics) {
			endPrepareTime = System.currentTimeMillis();
			numPreparedSDGs++;
			timePreprareSDG += (endPrepareTime - startPrepareTime);
		}

		for (final IFCStmt stmt : m.getInfo().getIFCStmts()) {
			cfc.out.print("IFC check '" + stmt + "': ");
			if (resetNeeded) {
				resetNeeded = false;
			}

			final FlowStmtResult stmtResult = m.findOrCreateStmtResult(stmt);
//			try {
//				final List<BasicIFCStmt> simplified = FlowLessSimplifier.simplify(stmt);
//				//FlowLess2SDGMatcher.printDebugMatches = true;
//				final Matcher match = FlowLess2SDGMatcher.findMatchingNodes(sdg, sdg.getRoot(), stmt);
//				if (simplified.isEmpty()) {
//					cfc.out.println("ERROR(empty simplified statements)");
//					stmtResult.addPart(new FlowStmtResultPart(null, "ERROR(empty simplified statements)",
//							false, false, cfg.exceptions, sdg.getFileName()));
//				} else {
//					checkBasicIFCStmts(alias, match, simplified, m.getInfo(), stmtResult, cfg.exceptions, progress);
//				}
//			} catch (FlowAstException e) {
				cfc.out.println("ERROR( not implemented )");
				stmtResult.addPart(new FlowStmtResultPart(null, "ERROR( not implementd )", false, false,
						cfg.exceptions, sdg.getFileName()));
//			}

			resetNeeded = true;
		}

	}

	public static class EntityNotFoundException extends FlowAstException {

		public EntityNotFoundException(String message) {
			super(message);
		}

		private static final long serialVersionUID = -1553942031552394940L;
		
	}
	

	public static ProgramSourcePositions sliceIFCStmt(final IFCStmt stmt, final FlowStmtResultPart fp,
			final String tmpDir, final IProgressMonitor progress)
			throws IOException, CancelException, FlowAstException {
		if (!fp.hasAlias()) {
			throw new IllegalArgumentException("Cannot create slice, as no alias context is provided.");
		}
		
//		final String pathToSDG = tmpDir + (tmpDir.endsWith(File.separator) ? "" : File.separator)
//				+ fp.getSDGFilename() + ".pdg";


		final ProgramSourcePositions pspos = new ProgramSourcePositions();

		return pspos;
	}

	private SDG create(PrintStream out, AnalysisScope scope, MoJo mojo, CallGraphResult cg, IMethod im,
			String outDir, Config cfg) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		return create(out, scope, mojo, cg, im, outDir, cfg, cfg.exceptions);
	}

	private SDG create(PrintStream out, AnalysisScope scope, MoJo mojo, CallGraphResult cg, IMethod im,
			String outDir, Config cfg, ExceptionAnalysis exc) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		if (!Main.checkOrCreateOutputDir(outDir)) {
			out.println("Could not access/create diretory '" + cfg.outputDir +"'");
			return null;
		}

		out.print("Building system dependence graph... ");

		final ExternalCallCheck chk;
		if (cfg.extern == null) {
			chk = ExternalCallCheck.EMPTY;
		} else {
			chk = cfg.extern;
		}

		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.out = out;
		scfg.scope = scope;
		scfg.cache = cg.cache;
		scfg.cha = mojo.getHierarchy();
		scfg.entry = im;
		scfg.ext = chk;
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = cfg.exceptions;
		scfg.accessPath = cfg.accessPath;
		scfg.sideEffects = cfg.sideEffects;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;
		scfg.pts = cfg.pts;
		if (cfg.objSensFilter != null) {
			scfg.objSensFilter = cfg.objSensFilter;
		}
		scfg.debugAccessPath = true;
		scfg.debugAccessPathOutputDir = "out/";
		scfg.computeInterference = false;
		scfg.staticInitializers = StaticInitializationTreatment.NONE;
		scfg.debugStaticInitializers = false;
		scfg.fieldPropagation = cfg.fieldPropagation;
		scfg.debugManyGraphsDotOutput = cfg.debugManyGraphsDotOutput;

		final SDGBuilder sdg = SDGBuilder.create(scfg, cg.cg, cg.pts);


		final SDG joanaSDG = SDGBuilder.convertToJoana(cfc.out, sdg, NullProgressMonitor.INSTANCE);

		AccessPath.computeMinMaxAliasSummaryEdges(cfc.out, sdg, sdg.getMainPDG(), joanaSDG, NullProgressMonitor.INSTANCE);

		cfc.out.println("\ndone.");

		cfc.out.print("Writing SDG to disk... ");
		joanaSDG.setFileName(cfg.name != null ? sdg.getMainMethodName() + "-" + cfg.name : sdg.getMainMethodName());
		final String fileName =
				(outDir.endsWith(File.separator) ? outDir : outDir + File.separator) + joanaSDG.getFileName() + ".pdg";
		final File file = new File(fileName);
		cfc.out.print("(" + file.getAbsolutePath() + ") ");
		PrintWriter pw = new PrintWriter(file);
		SDGSerializer.toPDGFormat(joanaSDG, pw);
		cfc.out.println("done.");

		return joanaSDG;
	}

	/**
	 * Search file in filesystem. If not found, try to load from classloader (e.g. from inside the jarfile).
	 */
	private static Module findJarModule(final String path) throws IOException {
		final File f = new File(path);
		if (f.exists()) {
			return new JarFileModule(new JarFile(f));
		} else {
			final URL url = CheckInformationFlow.class.getClassLoader().getResource(path);
			final URLConnection con = url.openConnection();
			final InputStream in = con.getInputStream();
			return new JarStreamModule(new JarInputStream(in));
		}
	}

	private static AnalysisScope createAnalysisScope(final Config cfg) throws IOException {
		final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

		if (cfg.nativesXML != null) {
			com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);
		}

		// if use stubs
		if (cfg.stubs != null) {
			scope.addToScope(ClassLoaderReference.Primordial, findJarModule(cfg.stubs));
		}

		// Nimmt unnoetige Klassen raus
		final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
		scope.setExclusions(exclusions);

	    final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
	    AnalysisScopeReader.addClassPathToScope(cfg.classpath, scope, loader);

	    return scope;
	}
}
