/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collection;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * A simple IFC demonstrator that performs a confitentiality check on a single .java file.
 * It automatically 
 * 1. compiles the source, 
 * 2. builds an sdg from the compiles class,
 * 3. annotates the sdg according to predefined rules,
 * 4. runs an ifc analysis on the sdg
 * and outputs the result.
 * 
 * @author Juergen Graf <graf@kit.edu>
 */
public final class RunSingleFileIFC {

	private static final String DEFAULT_MAIN_CLASS = "Main";
	private static final String DEFAULT_SECRET_SOURCE = "Security.SECRET";
	private static final String DEFAULT_PUBLIC_OUTPUT = "Security.PUBLIC";//"Security.leak(I)V";
	private static final String THREAD_START = "java.lang.Thread.start(V)V";
	private static final String DEFAULT_STUBS = 
//			"jSDG-stubs-jre1.4.jar";
			"../../contrib/lib/stubs/jSDG-stubs-jre1.4.jar";
	
	private static final boolean DUMP_SDG_FILES;
	static {
		final String debug = System.getProperty("dump-sdg");
		DUMP_SDG_FILES = "true".equals(debug);
	}
	
	private RunSingleFileIFC() {}
	
	public static void main(String[] args) {
		if (args.length < 1) {
			printUsage();
			return;
		}
		
		final String javaFileName = args[0];
		if (!javaFileName.endsWith(".java")) {
			errorExit("'" + javaFileName + "' is not a .java file.");
		}
		
		final File f = new File(javaFileName);
		if (!f.exists() || !f.canRead()) {
			errorExit("'" + f.getAbsolutePath() + "' could not be opened.");
		}
		
		if (!compile(f)) {
			errorExit("could not compile file '" + f.getAbsolutePath() + "'.");
		}
		
		final File dir = f.getParentFile();
		final SDGConfig config = createDefaultConfig(dir);
		final SDGProgram p = buildSDG(config);
		if (containsThreads(p)) {
			print("checking '" + javaFileName + "' for concurrent confidentiality.");
			doThreadIFCanalysis(config, javaFileName);
		} else {
			print("checking '" + javaFileName + "' for sequential confidentiality.");
			doSequentialIFCanalysis(config, p);
		}
	}
	
	private static boolean containsThreads(final SDGProgram p) {
		final SDGMethod m = p.getMethod(THREAD_START);
		
		return m != null;
	}
	
	private static void doThreadIFCanalysis(final SDGConfig config, final String javaFileName) {
		
	}
	
	private static boolean checkIFC(final SDGProgram prog) {
		return checkIFC(prog, IFCType.POSSIBILISTIC);
	}

	private static boolean checkIFC(final SDGProgram prog, final IFCType type) {
		final IFCAnalysis ana = annotateSDG(prog);
		final Collection<IllicitFlow> leaks = ana.doIFC(type);
		
		return leaks.isEmpty();
	}

	private static void doSequentialIFCanalysis(final SDGConfig config, final SDGProgram first) {
		SDGProgram currentProgram = first;
		boolean nextConfigAvailable = true;
		int numSecure = 0;
		int numLeaks = 0;
		
		while (nextConfigAvailable) {
			boolean secure = checkIFC(currentProgram);
			printResult(secure, config);
			
			if (DUMP_SDG_FILES) {
				final String fileName;
				if (secure) {
					numSecure++;
					fileName = "secure-" + numSecure + ".pdg";
				} else {
					numLeaks++;
					fileName = "leaks-" + numSecure + ".pdg";
				}

				final SDG sdg = currentProgram.getSDG();
				try {
					SDGSerializer.toPDGFormat(sdg, new FileOutputStream(fileName));
					print("writing SDG to " + fileName);
				} catch (FileNotFoundException e) {
					errorExit(e.getMessage());
				}
			}
			
			nextConfigAvailable = nextConfig(config, secure);
		
			if (nextConfigAvailable && !secure) {
				currentProgram = buildSDG(config);
			}
		}
	}
	
	private static boolean nextConfig(final SDGConfig config, boolean thisConfigIsSecure) {
		boolean nextConfigReady = false;
		
		if (config.computeInterferences()) {
			
		} else {
			// if current config is save skip all points-to
			if (thisConfigIsSecure) {
				// skip additional points-to precision. enhance exception analysis
				if (config.getPointsToPrecision() != PointsToPrecision.TYPE) {
					nextConfigReady = incExceptionPrecision(config);
					config.setPointsToPrecision(PointsToPrecision.TYPE);
				}
			} else {
				// increase points-to precision
				nextConfigReady = incPointsToPrecision(config);
	
				if (!nextConfigReady) {
					// increase exception analysis precision
					nextConfigReady = incExceptionPrecision(config);
					// reset points-to
					config.setPointsToPrecision(PointsToPrecision.TYPE);
				}
			}
		}
		
		return nextConfigReady;
	}
	
	private static boolean incExceptionPrecision(final SDGConfig config) {
		switch (config.getExceptionAnalysis()) {
		case ALL_NO_ANALYSIS:
			config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
			return true;
		case INTRAPROC:
			config.setExceptionAnalysis(ExceptionAnalysis.INTERPROC);
			return true;
		case INTERPROC:
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			return true;
		case IGNORE_ALL:
			return false;
		}
		
		return false;
	}
	
	private static boolean incPointsToPrecision(final SDGConfig config) {
		switch (config.getPointsToPrecision()) {
		case TYPE:
			config.setPointsToPrecision(PointsToPrecision.CONTEXT_SENSITIVE);
			return true;
		case CONTEXT_SENSITIVE:
			config.setPointsToPrecision(PointsToPrecision.OBJECT_SENSITIVE);
			return true;
		case OBJECT_SENSITIVE:
			return false;
		}
		
		return false;
	}
	
	private static void printResult(final boolean secure, final SDGConfig config) {
		if (secure) {
			print("SECURE  " + configToString(config));
		} else {
			print("ILLEGAL " + configToString(config));
		}
	}
	
	private static String configToString(final SDGConfig config) {
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("points-to: ");
		if (config.computeInterferences()) {
			sb.append("context-sensitive");
		} else {
			switch (config.getPointsToPrecision()) {
			case TYPE:
				sb.append("type-based");
				break;
			case CONTEXT_SENSITIVE:
				sb.append("context-sensitive");
				break;
			case OBJECT_SENSITIVE:
				sb.append("object-sensitive");
				break;
			}
		}
		
		sb.append(", exceptions: ");
		switch (config.getExceptionAnalysis()) {
		case ALL_NO_ANALYSIS:
			sb.append("very imprecise (unoptimized)");
			break;
		case INTRAPROC:
			sb.append("intraprocedural optimized");
			break;
		case INTERPROC:
			sb.append("interprocedural optimized");
			break;
		case IGNORE_ALL:
			sb.append("ignore all effects (unsound)");
			break;
		}

		if (config.computeInterferences()) {
			sb.append(", may-happen-in-parallel: ");
			switch (config.getMhpType()) {
			case NONE:
				sb.append("very imprecise");
				break;
			case SIMPLE:
				sb.append("simple optimizations");
				break;
			case PRECISE:
				sb.append("precise analysis");
				break;
			}
		}
		
		sb.append("]");
		
		return sb.toString();
	}
	
	private static SDGConfig createDefaultConfig(final File binDir) {
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(DEFAULT_MAIN_CLASS);
		final SDGConfig config = new SDGConfig(binDir.getAbsolutePath(), mainMethod.toBCString(), DEFAULT_STUBS);
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(ExceptionAnalysis.ALL_NO_ANALYSIS);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(PointsToPrecision.TYPE);
		
		return config;
	}
	
	private static IFCAnalysis annotateSDG(final SDGProgram p) {
		final IFCAnalysis ana = new IFCAnalysis(p);
		final SDGProgramPart secret = ana.getProgramPart(DEFAULT_SECRET_SOURCE);
		if (secret == null) {
			errorExit("could not find the default secret information source '" + DEFAULT_SECRET_SOURCE + "'");
		}
		ana.addSourceAnnotation(secret, BuiltinLattices.STD_SECLEVEL_HIGH);
		final SDGProgramPart output = ana.getProgramPart(DEFAULT_PUBLIC_OUTPUT);
		if (output == null) {
			errorExit("could not find the default public output channel '" + DEFAULT_PUBLIC_OUTPUT + "'");
		}
		ana.addSinkAnnotation(output, BuiltinLattices.STD_SECLEVEL_LOW);
		
		return ana;
	}
	
	private static SDGProgram buildSDG(final SDGConfig config) {
		SDGProgram prog = null;
		
		try {
			prog = SDGProgram.createSDGProgram(config);
		} catch (Exception e) {
			errorExit(e);
		}

		return prog;
	}

	private static boolean compile(final File javaFile) {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		return 0 == compiler.run(System.in, System.out, System.err,
				"-nowarn", "-source", "1.4", "-target", "1.4", javaFile.getAbsolutePath());
	}
	
	private static void print(String msg) {
		System.out.println(msg);
	}
	
	private static void printUsage() {
		System.out.println("Provide a .java file as single argument.");
	}
	
	private static void errorExit(String msg) {
		System.err.println(msg);
		System.exit(1);
	}
	
	private static void errorExit(Throwable t) {
		errorExit(t.getMessage());
	}
	
}
