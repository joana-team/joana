package edu.kit.joana.wala.eval;
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.jar.JarFile;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.ExternalCallCheck.MethodListCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.dictionary.Dictionary;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class RunTestAccessPath {

	private static final String MODULE_BIN[] = {
			"../joana.wala.modular.testdata/dist/mojo-test-modules1.jar",
			"../joana.wala.modular.testdata/dist/mojo-test-modules2.jar"
	};

	private static final String MODULE_OUT[] = {
			"./out/accpath_module1",
			"./out/accpath_module2"
	};

	private static final String PROGRAM_SRC = "../joana.wala.modular.testdata/src";

	public static void main(String[] args) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final IProgressMonitor progress = NullProgressMonitor.INSTANCE;

		System.out.print("Parsing source files for ifc annotations... ");
		List<ClassInfo> clsInfos = MoJo.parseSourceFiles(PROGRAM_SRC);
		System.out.println("done.");
		System.out.print("Checking for syntactic errors... ");
		final int errors = MoJo.prepareFlowLessStmts(clsInfos);
		if (errors > 0) {
			System.out.print("(" + errors + " errors) ");
		}
		System.out.println("done.");

		// prepare base sdg

		MethodListCheck mlc = new MethodListCheck(null, "./out/", /* debug output */ false);

		for (ClassInfo cls : clsInfos) {
			for (MethodInfo m : cls.getMethods()) {
				if (m.hasIFCStmts()) {
					// mark as external call targets
//					System.err.println(m.toString());
					mlc.addMethod(m);
				}
			}
		}

		// build accesspath sdgs for module methods
		final Config cfg = new Config("runtestaccesspath", "<main entry not used>",
				"../joana.wala.modular.testdata/dist/mojo-test-program.jar", PointsToPrecision.INSTANCE_BASED,
				ExceptionAnalysis.INTRAPROC, true, Main.STD_EXCLUSION_REG_EXP,
				"../../contrib/lib/stubs/natives_empty.xml", "../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar", mlc,
				"./out/", FieldPropagation.OBJ_TREE);

		for (int i = 0; i < MODULE_BIN.length; i++) {
			final String binDir = MODULE_BIN[i];
			final String outputDir = MODULE_OUT[i];

			System.out.print("Setting up analysis scope... ");

			// Fuegt die normale Java Bibliothek zum Scope hinzu
			final AnalysisScope scope = AnalysisScopeReader.makePrimordialScope(null);

			if (cfg.nativesXML != null) {
				com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);
			}

			// if use stubs
			if (cfg.stubs != null) {
				scope.addToScope(ClassLoaderReference.Primordial, new JarFile(cfg.stubs));
			}

			// Nimmt unnoetige Klassen raus
			final SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
			scope.setExclusions(exclusions);

		    final ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
		    AnalysisScopeReader.addClassPathToScope(binDir, scope, loader, cfg.classpathAddEntriesFromMANIFEST);

		    System.out.println("done.");

			final ClassHierarchy cha = ClassHierarchyFactory.make(scope);
			mlc.setClassHierarchy(cha);

			System.out.print("Creating MoJo... ");

			final MoJo mojo = MoJo.create(cha);

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
						final String outputMethodDir = outputDir + (outputDir.endsWith(File.separator) ? "" : File.separator) + "m_" + sig;
						Main.checkOrCreateOutputDir(outputMethodDir);

						final long startTime = System.currentTimeMillis();

						final CallGraphResult cg = buildMaxAliasConfig(mojo, im);
						final SDGBuilder sdg = create(System.out, scope, cha, cg, im, outputMethodDir, cfg);
						sdg.cfg.debugAccessPath = true;
						sdg.cfg.debugAccessPathOutputDir = outputDir;
						AccessPath.compute(sdg, sdg.getMainPDG());

						final SDG joanaSDG = SDGBuilder.convertToJoana(System.out, sdg, progress);

						System.out.println("\ndone.");

						final long endTime = System.currentTimeMillis();

						System.out.println("Time needed: " + (endTime - startTime) + "ms - Memory: "
								+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024))
								+ "M used.");

						if (joanaSDG != null) {
							System.out.print("Writing SDG to disk... ");
							final String fileName = outputMethodDir + File.separator + "access_path.pdg";
							final File file = new File(fileName);
							System.out.print("(" + file.getAbsolutePath() + ") ");
							PrintWriter pw = new PrintWriter(file);
							SDGSerializer.toPDGFormat(joanaSDG, pw);
							System.out.println("done.");
						}

					}
				}
			}
		}

	}

	private static CallGraphResult buildMaxAliasConfig(MoJo mojo, IMethod method) throws IllegalArgumentException, CancelException {
		final Aliasing minMax = mojo.computeMinMaxAliasing(method);
		final PointsTo pts = MoJo.computePointsTo(minMax.upperBound);
		final AnalysisOptions optPts = mojo.createAnalysisOptionsWithPTS(pts, method);
		final CallGraphResult cg = mojo.computeContextSensitiveCallGraph(optPts);

		return cg;
	}

	private static SDGBuilder create(PrintStream out, AnalysisScope scope, IClassHierarchy cha, CallGraphResult cg,
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
		scfg.cha = cha;
		scfg.entry = im;
		scfg.ext = chk;
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = cfg.exceptions;
		scfg.accessPath = cfg.accessPath;
		scfg.sideEffects = cfg.sideEffects;
		scfg.fieldPropagation = cfg.fieldPropagation;
		scfg.prunecg = Main.DEFAULT_PRUNE_CG;
		scfg.pts = cfg.pts;

		final SDGBuilder sdg = SDGBuilder.create(scfg, cg.cg, cg.pts);

//		SDGVerifier.verify(sdg, false, true);

		return sdg;
	}

	@SuppressWarnings("unused")
	private static void fail(String msg) {
		throw new IllegalStateException(msg);
	}

}
