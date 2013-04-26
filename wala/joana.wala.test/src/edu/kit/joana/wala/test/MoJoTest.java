/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 *
 */
package edu.kit.joana.wala.test;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.jar.JarFile;


import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.ExternalCallCheck.MethodListCheck;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class MoJoTest {

	private final String bin;
	private final String src;
	private final String out;
	private MoJo mojo;

	private MoJoTest(final String src, final String bin, final String out) {
		this.src = src;
		this.bin = bin;
		this.out = out;
	}

	private static MoJoTest cached = null;

	public static MoJoTest create(final String src, final String bin, final String out) throws IOException, ClassHierarchyException {
		if (cached == null || !(cached.src.equals(src) && cached.bin.equals(bin))) {
			System.out.printf("Creating new MoJo for src:%s, bin:%s, out:%s\n", src, bin, out);
			MoJoTest mj = new MoJoTest(src, bin, out);

			mj.init();

			cached = mj;
		} else {
			System.out.printf("Using cached MoJo for src:%s, bin:%s, out:%s\n", src, bin, out);
		}

		return cached;
	}

	public Config createDefaultConfig() {
		final Config cfg = new Config("mojotest", "<main entry not used>", bin,
				PointsToPrecision.CONTEXT_SENSITIVE, ExceptionAnalysis.INTRAPROC, true, Main.STD_EXCLUSION_REG_EXP,
				"../jSDG/lib/natives_empty.xml", "../jSDG/lib/jSDG-stubs-jre1.4.jar", ExternalCallCheck.EMPTY, out,
				FieldPropagation.OBJ_TREE);

		return cfg;
	}

	public SDG analyzeMethod(String method) throws IllegalArgumentException, CancelException, ClassHierarchyException, IOException, UnsoundGraphException {
		return analyzeMethod(method, createDefaultConfig());
	}

	public SDG analyzeMethod(final String method, final Config cfg) throws IllegalArgumentException, CancelException, ClassHierarchyException, IOException, UnsoundGraphException {
		final long startTime = System.currentTimeMillis();

		final IMethod im = mojo.findMethod(method);
		assertNotNull("Could not find method " + method, im);

		final Aliasing minMax = mojo.computeMinMaxAliasing(im);
		assertNotNull(minMax);

		final PointsTo ptsMax = MoJo.computePointsTo(minMax.upperBound);
		assertNotNull(ptsMax);

		final AnalysisOptions opt = mojo.createAnalysisOptionsWithPTS(ptsMax, im);
		final CallGraphResult cgr = mojo.computeContextSensitiveCallGraph(opt);

		final SDGBuilder sdg = create(System.out, opt.getAnalysisScope(), mojo, cgr, im, out, cfg);

		final SDG joanaSDG = SDGBuilder.convertToJoana(System.out, sdg, NullProgressMonitor.INSTANCE);

		AccessPath.computeMinMaxAliasSummaryEdges(System.out, sdg, sdg.getMainPDG(), joanaSDG, NullProgressMonitor.INSTANCE);

		System.out.println("\ndone.");

		final long endTime = System.currentTimeMillis();

		System.out.println("Time needed: " + (endTime - startTime) + "ms - Memory: "
				+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024))
				+ "M used.");

		if (out != null) {
			System.out.print("Writing SDG to disk... ");
			joanaSDG.setFileName(cfg.name != null ? sdg.getMainMethodName() + "-" + cfg.name: sdg.getMainMethodName());
			final String fileName = getSDGFileName(joanaSDG.getFileName());
			final File file = new File(fileName);
			System.out.print("(" + file.getAbsolutePath() + ") ");
			PrintWriter pw = new PrintWriter(file);
			SDGSerializer.toPDGFormat(joanaSDG, pw);
			System.out.println("done.");
		}

		return joanaSDG;
	}

	public String getSDGFileName(String method) {
		return out + File.separator + method + ".pdg";
	}

	private static SDGBuilder create(PrintStream out, AnalysisScope scope, MoJo mojo, CallGraphResult cg,
			IMethod im, String outDir, Config cfg) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		if (!Main.checkOrCreateOutputDir(outDir)) {
			out.println("Could not access/create diretory '" + cfg.outputDir +"'");
			return null;
		}

		out.print("Building system dependence graph... ");

		ExternalCallCheck chk;
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
		scfg.debugAccessPath = false;
		scfg.debugAccessPathOutputDir = "out/";
		scfg.computeInterference = false;
		scfg.staticInitializers = StaticInitializationTreatment.NONE;
		scfg.debugStaticInitializers = false;
		scfg.fieldPropagation = cfg.fieldPropagation;
		scfg.debugManyGraphsDotOutput = cfg.debugManyGraphsDotOutput;

		final SDGBuilder sdg = SDGBuilder.create(scfg, cg.cg, cg.pts);

//		SDGVerifier.verify(sdg, false, true);

		return sdg;
	}

	private void init() throws IOException, ClassHierarchyException {
		System.out.print("Parsing source files for ifc annotations... ");
		List<ClassInfo> clsInfos = MoJo.parseSourceFiles(src);
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

		final Config cfg = new Config("runtestaccesspath", "<main entry not used>", "../Test-Modular/dist/mojo-test-program.jar",
				PointsToPrecision.CONTEXT_SENSITIVE, ExceptionAnalysis.INTRAPROC, true, Main.STD_EXCLUSION_REG_EXP,
				"../jSDG/lib/natives_empty.xml", "../jSDG/lib/jSDG-stubs-jre1.4.jar", mlc, "./out/", FieldPropagation.FLAT);

		System.out.print("Setting up analysis scope... ");

		// Fuegt die normale Java Bibliothek zum Scope hinzu
		final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

		if (cfg.nativesXML != null) {
			com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);
		}

		File f = new File(cfg.stubs);
		System.out.println(f.getAbsolutePath());

		// if use stubs
		if (cfg.stubs != null) {
			scope.addToScope(ClassLoaderReference.Primordial, new JarFile(cfg.stubs));
		}

		// Nimmt unnoetige Klassen raus
		final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
		scope.setExclusions(exclusions);

	    final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
	    AnalysisScopeReader.addClassPathToScope(bin, scope, loader);

	    System.out.println("done.");

		final ClassHierarchy cha = ClassHierarchy.make(scope);
		mlc.setClassHierarchy(cha);

		System.out.print("Creating MoJo... ");

		this.mojo = MoJo.create(cha);

		System.out.println("done.");
	}

}
