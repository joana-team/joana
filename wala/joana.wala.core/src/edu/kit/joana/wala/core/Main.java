/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.strings.StringStuff;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.graphs.WriteGraphToDot;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.wala.ObjSensContextSelector;

public final class Main {

	private Main() {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] argv) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		long maxMem = Runtime.getRuntime().maxMemory() / (1024 * 1024);
		System.out.println("Maximal available memory is " + maxMem + "M. Use java -Xmx (e.g. -Xmx1024M) to change this setting.");

		for (Config cfg : CFGS) {
			run(System.out, cfg);
		}
	}

	public final static String STD_EXCLUSION_REG_EXP =
		"java\\/awt\\/.*\n"
		+ "javax\\/swing\\/.*\n"
		+ "java\\/nio\\/.*\n"
		+ "java\\/net\\/.*\n"
		+ "sun\\/awt\\/.*\n"
		+ "sun\\/swing\\/.*\n"
		+ "com\\/sun\\/.*\n"
		+ "sun\\/.*\n"
		+ "apple\\/awt\\/.*\n"
		+ "com\\/apple\\/.*\n"
		+ "org\\/omg\\/.*\n"
		+ "javax\\/.*\n";

	// these classes are modeled without specific fields
	public final static String[] IMMUTABLE_STUBS = {
		"Ljava/lang/String",
		"Ljava/lang/Integer",
		"Ljava/lang/Long",
		"Ljava/lang/Character",
		"Ljava/lang/Object",
		"Ljava/lang/Throwable",
		"Ljava/lang/Exception",
	};

	public final static String[] IMMUTABLE_NO_OUT = {
		"Ljava/lang/String",
		"Ljava/lang/Integer",
		"Ljava/lang/Long",
		"Ljava/lang/Character",
	};

	public final static String[] IGNORE_STATIC_FIELDS = {
		"Ljava/lang/Integer",
		"Ljava/lang/Object",
		"Ljava/lang/Long",
		"Ljava/lang/Character",
		"Ljava/lang/Throwable",
		"Ljava/lang/Exception",
	};

	public final static int DEFAULT_PRUNE_CG = 2;
	public final static int DO_NOT_PRUNE_CG = SDGBuilder.DO_NOT_PRUNE;

	public final static String STD_CLASS_PATH = "bin/";

	public final static ExceptionAnalysis DEFAULT_EXCEPTION_ANALYSIS = ExceptionAnalysis.INTRAPROC;

	public final static boolean DEFAULT_ACCESS_PATH = false;

	public static final Config[] CFGS = {
		new Config("Test2 B", "B.main([Ljava/lang/String;)V", "../../example/joana.example.tiny-special-tests/bin", FieldPropagation.OBJ_GRAPH),
//		new Config("Test2 C", "C.main([Ljava/lang/String;)V", "../../example/joana.example.tiny-special-tests/bin"),
		new Config("Test2 C", "C.main([Ljava/lang/String;)V", "../../example/joana.example.tiny-special-tests/bin", PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH),
//		new Config("Test A", "edu.kit.ipd.wala.tests.D.invokesMethod()V"),
//		new Config("Test C",
//				"edu.kit.ipd.wala.tests.C.invokesWithConstantParam()V"),
//		new Config("Test D", "edu.kit.ipd.wala.tests.D.invokesMethod()V"),
//		new Config("JGF Barrier",
//				"def.JGFBarrierBench.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar", FieldPropagation.OBJ_GRAPH),
//		new Config("JGF Crypt",
//				"def.JGFCryptBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar", FieldPropagation.OBJ_GRAPH),
//		new Config("JGF ForkJoin",
//				"def.JGFForkJoinBench.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF LUFact",
//				"def.JGFLUFactBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF MolDyn",
//				"def.JGFMolDynBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF MonteCarlo",
//				"def.JGFMonteCarloBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF RayTracer",
//				"def.JGFRayTracerBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF Series",
//				"def.JGFSeriesBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF SOR",
//				"def.JGFSORBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config(
//				"JGF SparseMatmult",
//				"def.JGFSparseMatmultBenchSizeA.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("JGF Sync",
//				"def.JGFSyncBench.main([Ljava/lang/String;)V",
//				"eval/javagrande/benchmarks.jar"),
//		new Config("HSQLDB",
//				"org.hsqldb.Server.main([Ljava/lang/String;)V",
////				"eval/hsqldb/HSQLDB.jar", PointsToPrecision.TYPE), //STD_EXCLUSION_REG_EXP
//				"eval/hsqldb/HSQLDB.jar", STD_EXCLUSION_REG_EXP
//						+ "java\\/nio\\/.*\n" + "javax\\/.*\n"
//						+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n"
//						+ "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
//						+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n", FieldPropagation.OBJ_GRAPH),
//		new Config("jEdit",
//				"org.gjt.sp.jedit.jEdit.main([Ljava/lang/String;)V",
//				"eval/jedit/jedit.jar", STD_EXCLUSION_REG_EXP
//						+ "java\\/nio\\/.*\n" + "javax\\/.*\n"
//						+ "java\\/util\\/.*\n" + "java\\/security\\/.*\n"
//						+ "java\\/beans\\/.*\n" + "org\\/omg\\/.*\n"
//						+ "apple\\/awt\\/.*\n" + "com\\/apple\\/.*\n", FieldPropagation.OBJ_GRAPH),
				//),
	};

	public static ClassHierarchy computeClassHierarchy(PrintStream out, Config cfg) throws IOException, ClassHierarchyException {
		AnalysisScope scope = setUpAnalysisScope(out, cfg);
	    // Klassenhierarchie berechnen
		return ClassHierarchy.make(scope);
	}


	public static List<String> searchMainMethods(PrintStream out, Config cfg) throws IOException, ClassHierarchyException {
		final List<String> result = new LinkedList<String>();
		out.println("Searching for main methods in '" + cfg.classpath + "'...");
		ClassHierarchy cha = computeClassHierarchy(out, cfg);
		for (final IClass cls : cha) {
			if (!cls.isInterface() && !cls.isAbstract() && cls.getClassLoader().getName().equals(AnalysisScope.APPLICATION)) {
				for (final IMethod m : cls.getDeclaredMethods()) {
					if (m.isStatic() && "main([Ljava/lang/String;)V".equals(m.getSelector().toString())) {
						out.println("\tfound '" + m.getSignature() + "'");
						result.add(m.getSignature());
					}
				}
			}
		}

		out.println("done.");

		return result;
	}

	public static void run(PrintStream out, Config cfg) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final SDG sdg = compute(out, cfg);

		if (sdg != null) {
			out.print("Writing SDG to disk... ");
			final String fileName = cfg.outputDir + WriteGraphToDot.sanitizeFileName(sdg.getName()) + ".pdg";
			final File file = new File(fileName);
			out.print("(" + file.getAbsolutePath() + ") ");
			PrintWriter pw = new PrintWriter(file);
			SDGSerializer.toPDGFormat(sdg, pw);
			out.println("done.");
		}
	}

	/**
	 * Search file in filesystem. If not found, try to load from classloader (e.g. from inside the jarfile).
	 */
	private static Module findJarModule(final PrintStream out, final String path) throws IOException {
		final File f = new File(path);
		if (f.exists()) {
			out.print("(from file " + path + ") ");
			return new JarFileModule(new JarFile(f));
		} else {
			final URL url = Main.class.getClassLoader().getResource(path);
			final URLConnection con = url.openConnection();
			final InputStream in = con.getInputStream();
			out.print("(from jar stream " + path + ") ");
			return new JarStreamModule(new JarInputStream(in));
		}
	}

	public static AnalysisScope setUpAnalysisScope(final PrintStream out, final Config cfg) throws IOException {
		// Fuegt die normale Java Bibliothek zum Scope hinzu

		// deactivates WALA synthetic methods if cfg.nativesXML != null
		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);

		AnalysisScope scope;
		// if use stubs
		if (cfg.stubs != null) {
			scope = AnalysisScope.createJavaAnalysisScope();
			final Module stubs = findJarModule(out, cfg.stubs);
			scope.addToScope(ClassLoaderReference.Primordial, stubs);

		} else {
			scope = AnalysisScopeReader.makePrimordialScope(null);
		}

		// Nimmt unnoetige Klassen raus
		SetOfClasses exclusions = new FileOfClasses(new ByteArrayInputStream(cfg.exclusions.getBytes()));
		scope.setExclusions(exclusions);

	    ClassLoaderReference loader = scope.getLoader(AnalysisScope.APPLICATION);
	    AnalysisScopeReader.addClassPathToScope(cfg.classpath, scope, loader);

	    return scope;
	}

	public static SDG compute(PrintStream out, Config cfg) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		return compute(out, cfg, NullProgressMonitor.INSTANCE);
	}


	public static SDG compute(PrintStream out, Config cfg, IProgressMonitor progress) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		return compute(out, cfg, false, progress);
	}

	private static Pair<Long, SDGBuilder.Config> prepareBuild(PrintStream out, Config cfg, boolean computeInterference, IProgressMonitor progress) throws IOException, ClassHierarchyException {
		if (!checkOrCreateOutputDir(cfg.outputDir)) {
			out.println("Could not access/create diretory '" + cfg.outputDir +"'");
			return null;
		}
		final long startTime = System.currentTimeMillis();

		out.print("Setting up analysis scope... ");

		AnalysisScope scope = setUpAnalysisScope(out, cfg);

	    out.println("done.");

	    out.print("Creating class hierarchy... ");

	    // Klassenhierarchie berechnen
		ClassHierarchy cha = ClassHierarchy.make(scope);


	    out.println("(" + cha.getNumberOfClasses() + " classes) done.");

	    if (cfg.extern != null) {
	    	cfg.extern.setClassHierarchy(cha);
	    }

	    out.print("Setting up entrypoint " + cfg.entryMethod + "... ");


	    // Methode in der Klassenhierarchie suchen
		final MethodReference mr = StringStuff.makeMethodReference(Language.JAVA, cfg.entryMethod);
		IMethod m = cha.resolveMethod(mr);
		if (m == null) {
			fail("could not resolve " + mr);
		}

		out.println("done.");

		AnalysisCache cache = new AnalysisCache(new DefaultIRFactory());

		out.print("Building system dependence graph... ");

		ExternalCallCheck chk;
		if (cfg.extern == null) {
			chk = new ExternalCallCheck() {
				@Override
				public boolean isCallToModule(SSAInvokeInstruction invk) {
					return false;
				}

				@Override
				public void registerAliasContext(SSAInvokeInstruction invk, int callNodeId, MayAliasGraph context) {
				}

				@Override
				public void setClassHierarchy(IClassHierarchy cha) {
				}

				@Override
				public MethodInfo checkForModuleMethod(IMethod im) {
					return null;
				}

				@Override
				public boolean resolveReflection() {
					return false;
				}
			};
		} else {
			chk = cfg.extern;
		}

		final SDGBuilder.Config scfg = new SDGBuilder.Config();
		scfg.out = out;
		scfg.scope = scope;
		scfg.cache = cache;
		scfg.cha = cha;
		scfg.entry = m;
		scfg.ext = chk;
		scfg.immutableNoOut = IMMUTABLE_NO_OUT;
		scfg.immutableStubs = IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = IGNORE_STATIC_FIELDS;
		scfg.exceptions = cfg.exceptions;
		scfg.accessPath = cfg.accessPath;
		scfg.prunecg = DEFAULT_PRUNE_CG;
		scfg.pts = cfg.pts;
		if (cfg.objSensFilter != null) {
			scfg.objSensFilter = cfg.objSensFilter;
		}
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;
		scfg.fieldPropagation = cfg.fieldPropagation;
		scfg.debugManyGraphsDotOutput = cfg.debugManyGraphsDotOutput;
		scfg.computeInterference = computeInterference;

		return Pair.make(startTime, scfg);
	}

	private static void postpareBuild(long startTime, PrintStream out) {
		out.println("\ndone.");
		final long endTime = System.currentTimeMillis();

		out.println("Time needed: " + (endTime - startTime) + "ms - Memory: "
				+ ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024))
				+ "M used.");
	}

	public static SDG compute(PrintStream out, Config cfg, boolean computeInterference, IProgressMonitor progress) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		Pair<Long, SDGBuilder.Config> p = prepareBuild(out, cfg, computeInterference, progress);
		long startTime = p.fst;
		SDGBuilder.Config scfg = p.snd;
		final SDG sdg = SDGBuilder.build(scfg, progress);
		postpareBuild(startTime, out);
//		SDGVerifier.verify(sdg, false, true);

		return sdg;
	}

	public static Pair<SDG, SDGBuilder> computeAndKeepBuilder(PrintStream out, Config cfg, boolean computeInterference, IProgressMonitor progress) throws UnsoundGraphException, CancelException, IOException, ClassHierarchyException {
		Pair<Long, SDGBuilder.Config> p = prepareBuild(out, cfg, computeInterference, progress);
		long startTime = p.fst;
		SDGBuilder.Config scfg = p.snd;
		final Pair<SDG, SDGBuilder> ret = SDGBuilder.buildAndKeepBuilder(scfg, progress);
		postpareBuild(startTime, out);
//		SDGVerifier.verify(sdg, false, true);

		return ret;
	}



	public static boolean checkOrCreateOutputDir(String dir) {
		if (dir.endsWith(File.separator)) {
			dir = dir.substring(0, dir.length() - File.separator.length());
		}

		final File f = new File(dir);

		if (!f.exists()) {
			if (!f.mkdirs()) {
				return false;
			}
		}

		return f.canRead() && f.canWrite();
	}

	private static void fail(String msg) {
		throw new IllegalStateException(msg);
	}

	public static class Config {
		public String name;
		public String entryMethod;
		public String classpath;
		public String exclusions;
		public String nativesXML;
		public String stubs;
		public String outputDir;
		public ExternalCallCheck extern;
		public PointsToPrecision pts;
		// only used iff pts is set to object sensitive. If null defaults to
		// "do object sensitive analysis for all methods"
		public ObjSensContextSelector.MethodFilter objSensFilter = null;
		public ExceptionAnalysis exceptions;
		public boolean accessPath;
		public boolean debugManyGraphsDotOutput = false;
		public FieldPropagation fieldPropagation;

		public Config(String name) {
			this(name, "<no entry defined>", FieldPropagation.OBJ_GRAPH);
		}

		public Config(String name, String entryMethod, FieldPropagation fieldPropagation) {
			this(name, entryMethod, STD_CLASS_PATH, PointsToPrecision.CONTEXT_SENSITIVE, DEFAULT_EXCEPTION_ANALYSIS,
					DEFAULT_ACCESS_PATH, STD_EXCLUSION_REG_EXP, null, null, null, "./", fieldPropagation);
		}

		public Config(String name, String entryMethod, String classpath, FieldPropagation fieldPropagation) {
			this(name, entryMethod, classpath, PointsToPrecision.CONTEXT_SENSITIVE, DEFAULT_EXCEPTION_ANALYSIS,
					DEFAULT_ACCESS_PATH, STD_EXCLUSION_REG_EXP, null, null, null, "./", fieldPropagation);
		}

		public Config(String name, String entryMethod, String classpath, PointsToPrecision pts, FieldPropagation fieldPropagation) {
			this(name, entryMethod, classpath, pts, DEFAULT_EXCEPTION_ANALYSIS, DEFAULT_ACCESS_PATH,
					STD_EXCLUSION_REG_EXP, null, null, null, "./", fieldPropagation);
		}

		public Config(String name, String entryMethod, String classpath, String exclusions, FieldPropagation fieldPropagation) {
			this(name, entryMethod, classpath, PointsToPrecision.CONTEXT_SENSITIVE, DEFAULT_EXCEPTION_ANALYSIS,
					DEFAULT_ACCESS_PATH, exclusions, null, null, null, "./", fieldPropagation);
		}

		public Config(String name, String entryMethod, String classpath, PointsToPrecision pts, String exclusions,
				FieldPropagation fieldPropagation) {
			this(name, entryMethod, classpath, pts, DEFAULT_EXCEPTION_ANALYSIS, DEFAULT_ACCESS_PATH, exclusions, null,
					null, null, "./", fieldPropagation);
		}

		public Config(String name, String entryMethod, String classpath, PointsToPrecision pts, ExceptionAnalysis exceptions,
				boolean accessPath, String exclusions, String nativesXML, String stubs, ExternalCallCheck extern, String outputDir,
				FieldPropagation fieldPropagation) {
			this.name = name;
			this.pts = pts;
			this.exceptions = exceptions;
			this.accessPath = accessPath;
			this.classpath = classpath;
			this.entryMethod = entryMethod;
			this.exclusions = exclusions;
			this.nativesXML = nativesXML;
			this.stubs = stubs;
			this.extern = extern;

			if (!outputDir.endsWith(File.separator)) {
				this.outputDir = outputDir + File.separator;
			} else {
				this.outputDir = outputDir;
			}

			this.fieldPropagation = fieldPropagation;
		}

		public String toString() {
			final StringBuilder sb = new StringBuilder("joana.wala.core.Main configuration:\n");
			final Class<? extends Config> thisClass = getClass();

			for (final java.lang.reflect.Field f : thisClass.getFields()) {
				try {
					sb.append(f.getName() + " = ");
					final Class<?> fType = f.getType();

					if (fType.isPrimitive()) {
						final String  n = fType.getName();
						if (n.equals("boolean")) {
							sb.append(f.getBoolean(this) + "\n");
						} else if (n.equals("byte")) {
							sb.append(f.getByte(this) + "\n");
						} else if (n.equals("char")) {
							sb.append(f.getChar(this) + "\n");
						} else if (n.equals("double")) {
							sb.append(f.getDouble(this) + "\n");
						} else if (n.equals("float")) {
							sb.append(f.getFloat(this) + "\n");
						} else if (n.equals("int")) {
							sb.append(f.getInt(this) + "\n");
						} else if (n.equals("long")) {
							sb.append(f.getLong(this) + "\n");
						} else if (n.equals("short")) {
							sb.append(f.getShort(this) + "\n");
						} else {
							throw new IllegalStateException("unknown primitive type: " + fType.getName());
						}
					} else {
						final Object val = f.get(this);
						sb.append((val == null ? "null" : val.toString()) + "\n");
					}
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				}
			}

			return sb.toString();
		}
	}


}
