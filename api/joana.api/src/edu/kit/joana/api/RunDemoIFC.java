/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGActualParameter;
import edu.kit.joana.api.sdg.SDGCall;
import edu.kit.joana.api.sdg.SDGCallReturnNode;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IUnaryViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * A simple IFC demonstrator that performs a confidentiality check on a single .java file.
 * It automatically 
 * 1. compiles the source, 
 * 2. detects if the program contains threads
 * 3. builds an sdg from the compiled class with or without thread analysis (depending on 2.),
 * 4. annotates the sdg according to predefined rules,
 * 5. runs a standard or probabilistic (depending on 2.) ifc analysis on the sdg
 * and outputs the result.
 * 
 * @author Juergen Graf <graf@kit.edu>
 */
public final class RunDemoIFC {

	private static final String DEFAULT_MAIN_CLASS = "Main";
	private static final String DEFAULT_SECRET_SOURCE = "Main.inputPIN()I";
	private static final String DEFAULT_PUBLIC_OUTPUT = "Main.print(I)V";
	private static final String THREAD_START = "java.lang.Thread.start()V";
	private static final Stubs DEFAULT_STUBS = Stubs.JRE_14;
	
	private static final boolean DUMP_SDG_FILES;
	static {
		final String debug = System.getProperty("dump-sdg");
		DUMP_SDG_FILES = "true".equals(debug);
	}
	
	private static long lastSDGtime = 0;
	private static long lastSDGsize = 0;
	private static long lastViolations = 0;
	
	private RunDemoIFC() {}
	
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
			config.setComputeInterferences(true);
			config.setMhpType(MHPType.PRECISE);
			final SDGProgram concProg = buildSDG(config); 
			doThreadIFCanalysis(config, concProg, f);
		} else {
			print("checking '" + javaFileName + "' for sequential confidentiality.");
			doSequentialIFCanalysis(config, p, f);
		}
	}
	
	private static boolean containsThreads(final SDGProgram p) {
		final SDGMethod m = p.getMethod(THREAD_START);
		
		return m != null;
	}
	
	private static Set<SLeak> checkIFC(final Reason reason, final SDGProgram prog) {
		return checkIFC(reason, prog, IFCType.CLASSICAL_NI);
	}

	private static Set<SLeak> checkIFC(final Reason reason, final SDGProgram prog, final IFCType type) {
		final IFCAnalysis ana = annotateSDG(prog);
		final Collection<? extends IViolation<SecurityNode>> leaks = ana.doIFC(type);
		lastViolations = leaks.size();
		
		final Set<SLeak> sleaks = extractLeaks(ana, leaks, reason);
		
		return sleaks;
	}

	private static Set<SLeak> extractLeaks(final IFCAnalysis ana,
			final Collection<? extends IViolation<SecurityNode>> leaks, final Reason reason) {
		final TreeSet<SLeak> sleaks = new TreeSet<SLeak>();
		for (final IViolation<SecurityNode> leak : leaks) {
			leak.accept(new IViolationVisitor<SecurityNode>() {
				
				@Override
				public void visitOrderConflict(final OrderConflict<SecurityNode> orderConf) {
					final SecurityNode source = orderConf.getConflictEdge().getSource();
					final SecurityNode sink = orderConf.getConflictEdge().getTarget();
					final SPos ssource = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
					final SPos ssink = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());
					final Set<SPos> slice = new TreeSet<SPos>();
					slice.add(ssource);
					slice.add(ssink);
					final SLeak sleak = new SLeak(ssource, ssink, reason, slice);
					sleaks.add(sleak);
				}
				
				@Override
				public void visitIllegalFlow(final IIllegalFlow<SecurityNode> iFlow) {
					final SDG sdg = ana.getProgram().getSDG();
					final SecurityNode source = iFlow.getSource();
					final SecurityNode sink = iFlow.getSink();
					final NonSameLevelChopper chopper = new NonSameLevelChopper(sdg);
					final Collection<SDGNode> nodes = chopper.chop(source, sink);
					final SLeak sleak = extractSourceLeaks(source, sink, reason, nodes);
					if (sleak != null) {
						sleaks.add(sleak);
					}
				}
				
				@Override
				public void visitDataConflict(final DataConflict<SecurityNode> dataConf) {
					final SecurityNode source = dataConf.getConflictEdge().getSource();
					final SecurityNode sink = dataConf.getConflictEdge().getTarget();
					final SPos ssource = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
					final SPos ssink = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());
					final Set<SPos> slice = new TreeSet<SPos>();
					slice.add(ssource);
					slice.add(ssink);
					final SLeak sleak = new SLeak(ssource, ssink, reason, slice);
					sleaks.add(sleak);
				}

				@Override
				public <L> void visitUnaryViolation(IUnaryViolation<SecurityNode, L> unVio) {
					throw new UnsupportedOperationException("IUnaryViolations are not supported yet (only occur with iRLSOD and not with RLSOD or CLASSICAL_NI).");
				}
			});
		}
		
		return sleaks;
	}
	
	public static enum Reason { DIRECT_FLOW(1), INDIRECT_FLOW(2), BOTH_FLOW(3), EXCEPTION(4), THREAD(5);

		public final int importance;
	
		private Reason(final int importance) {
			this.importance = importance;
		}
	
	}
	
	public static class SLeak implements Comparable<SLeak> {
		private final SPos source;
		private final SPos sink;
		private final Reason reason;
		private final Set<SPos> slice;
		
		public SLeak(final SPos source, final SPos sink, final Reason reason, final Set<SPos> slice) {
			this.source = source;
			this.sink = sink;
			this.reason = reason;
			this.slice = slice;
		}
		
		public int hashCode() {
			return source.hashCode() + 23 * sink.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o instanceof SLeak) {
				final SLeak l = (SLeak) o;
				return source.equals(l.source) && sink.equals(l.sink);
			}
			
			return false;
		}
		
		public String toString() {
			return "from '" + source.toString() + "' to '" + sink.toString() + "'";
		}
		
		public String toString(final File srcFile) {
			StringBuffer sbuf = new StringBuffer();
			switch (reason) {
			case DIRECT_FLOW:
				sbuf.append("explicit flow:\n");
				break;
			case INDIRECT_FLOW:
				sbuf.append("implicit flow:\n");
				break;
			case BOTH_FLOW:
				sbuf.append("explicit and implicit flow:\n");
				break;
			case EXCEPTION:
				sbuf.append("implicit flow due to exceptions:\n");
				break;
			case THREAD:
				sbuf.append("possibilistic or probabilistic flow:\n");
				break;
			default:
				sbuf.append("reason: " + reason + "\n");
			}
			
			sbuf.append("from '" + source.toString() + "' to '" + sink.toString() + "'\n");
			
			for (final SPos pos : slice) {
				sbuf.append(pos.toString() + "\t");
				final String code = pos.getSourceCode(srcFile);
				sbuf.append(code + "\n");
			}
			
			return sbuf.toString();
		}

		@Override
		public int compareTo(final SLeak o) {
			if (o == this || this.equals(o)) {
				return 0;
			}

			if (!source.equals(o.source)) {
				return source.compareTo(o.source);
			}
			
			if (!sink.equals(o.sink)) {
				return sink.compareTo(o.sink);
			}
			
			return 0;
		}
		
	}
	
	public static class SPos implements Comparable<SPos> {
		private final String sourceFile;
		private final int startChar;
		private final int endChar;
		private final int startLine;
		private final int endLine;
		
		public SPos(final String sourceFile, final int startLine, final int endLine, final int startChar,
				final int endChar) {
			this.sourceFile = sourceFile;
			this.startLine = startLine;
			this.endLine = endLine;
			this.startChar = startChar;
			this.endChar = endChar;
		}
		
		public int hashCode() {
			return sourceFile.hashCode() + 13 * startLine;
		}
		
		public boolean isAllZero() {
			return startLine == 0 && endLine == 0 && startChar == 0 && endChar == 0;
		}
		
		public boolean hasCharPos() {
			return !(startChar == 0 && startChar == endChar);
		}
		
		public boolean isMultipleLines() {
			return startLine != endLine;
		}
		
		public boolean equals(Object o) {
			if (o instanceof SPos) {
				final SPos spos = (SPos) o;
				return sourceFile.equals(spos.sourceFile) && startLine == spos.startLine && endLine == spos.endLine
						&& startChar == spos.startChar && endChar == spos.endChar;
			}
			
			return false;
		}

		@Override
		public int compareTo(SPos o) {
			if (sourceFile.compareTo(o.sourceFile) != 0) {
				return sourceFile.compareTo(o.sourceFile);
			}
			
			if (startLine != o.startLine) {
				return startLine - o.startLine;
			}
			
			if (endLine != o.endLine) {
				return endLine - o.endLine;
			}
			
			if (startChar != o.startChar) {
				return startChar - o.startChar;
			}
			
			if (endChar != o.endChar) {
				return endChar - o.endChar;
			}
			
			return 0;
		}
		
		public String toString() {
			if (hasCharPos() && isMultipleLines()) {
				return sourceFile + ":(" + startLine + "," + startChar + ")-(" + endLine + "," + endChar +")"; 
			} else if (hasCharPos()) {
				return sourceFile + ":(" + startLine + "," + startChar + "-" + endChar +")"; 
			} else if (isMultipleLines()) {
				return sourceFile + ":" + startLine + "-" + endLine; 
			} else {
				return sourceFile + ":" + startLine; 
			}
		}
		
		public String getSourceCode(final File sourceFile) {
			final File f = sourceFile;
			try {
				String code = "";
				final BufferedReader read = new BufferedReader(new FileReader(f));
				for (int i = 0; i < startLine-1; i++) {
					read.readLine();
				}

				if (!isMultipleLines()) {
					final String line = read.readLine();
					if (hasCharPos()) {
						code = line.substring(startChar, endChar);
					} else {
						code = line;
					}
				} else {
					{
						final String line = read.readLine();
						if (hasCharPos()) {
							code = line.substring(startChar);
						} else {
							code = line;
						}
					}
					
					for (int i = startLine; i < endLine-1; i++) {
						code += read.readLine();
					}
					
					{
						final String line = read.readLine();
						if (hasCharPos()) {
							code += line.substring(0, endChar);
						} else {
							code += line;
						}
					}
				}

				read.close();
				
				return code;
			} catch (IOException e) {}
			
			return  "error getting source";
		}
	}
	
	private static SLeak extractSourceLeaks(final SecurityNode source, final SecurityNode sink, final Reason reason,
			final Collection<SDGNode> nodes) {
		final TreeSet<SPos> positions = new TreeSet<SPos>();
		
		final List<SPos> toRemove = new LinkedList<SPos>();
		
		for (final SDGNode n : nodes) {
			if (n.getSource() != null) {
				final SPos spos = new SPos(n.getSource(), n.getSr(), n.getEr(), n.getSc(), n.getEc());
				if (n.kind == SDGNode.Kind.ENTRY || n.kind == SDGNode.Kind.FORMAL_IN 
						|| n.kind == SDGNode.Kind.FORMAL_OUT
						|| (n.kind == SDGNode.Kind.CALL && n.getUnresolvedCallTarget() != null
							&& n.getUnresolvedCallTarget().contains("Object.<init>"))) {
					toRemove.add(spos);
				}
				if (!spos.isAllZero()) {
					positions.add(spos);
				}
			}
			
			positions.removeAll(toRemove);
		}

		if (positions.isEmpty()) {
			final SPos ssource = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
			final SPos ssink = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());
			final Set<SPos> slice = new TreeSet<SPos>();
			slice.add(ssource);
			slice.add(ssink);
			final SLeak sleak = new SLeak(ssource, ssink, reason, slice);
			
			return sleak;
		}
		final SPos ssource = positions.first();
		final SPos ssink = positions.last();
		
		final SLeak leak = new SLeak(ssource, ssink, reason, positions);
		return leak;
	}
	
	private static void doSequentialIFCanalysis(final SDGConfig config, final SDGProgram prog, final File srcFile) {
		final Set<SLeak> excLeaks = checkIFC(Reason.EXCEPTION, prog);
		final boolean isSecure = excLeaks.isEmpty();
		printResult(excLeaks.isEmpty(), 0, config);
		dumpSDGtoFile(prog.getSDG(), "exc", isSecure);
		
		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			final Set<SLeak> bothLeaks = checkIFC(Reason.BOTH_FLOW, noExcProg);
			printResult(bothLeaks.isEmpty(), 1, config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc", bothLeaks.isEmpty());

			excLeaks.removeAll(bothLeaks);
			for (final SLeak leak : bothLeaks) {
				System.out.println(leak.toString(srcFile));
			}
			for (final SLeak leak : excLeaks) {
				System.out.println(leak.toString(srcFile));
			}

			print("Information leaks detected. Program is NOT SECURE.");
		} else {
			print("No information leaks detected. Program is SECURE.");
		}
	}
	
	private static void doThreadIFCanalysis(final SDGConfig config, final SDGProgram prog, final File srcFile) {
		final Set<SLeak> threadLeaks = checkIFC(Reason.THREAD, prog, IFCType.RLSOD);
		final boolean isSecure = threadLeaks.isEmpty();

		printResult(threadLeaks.isEmpty(), 0, config);
		dumpSDGtoFile(prog.getSDG(), "thread", isSecure);

		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			final Set<SLeak> bothLeaks = checkIFC(Reason.THREAD, noExcProg, IFCType.RLSOD);
			
			printResult(bothLeaks.isEmpty(), 1, config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc_thread", bothLeaks.isEmpty());

			threadLeaks.removeAll(bothLeaks);
			for (final SLeak leak : bothLeaks) {
				System.out.println(leak.toString(srcFile));
			}
			for (final SLeak leak : threadLeaks) {
				System.out.println(leak.toString(srcFile));
			}

			print("Information leaks detected. Program is NOT SECURE.");
		} else {
			print("No information leaks detected. Program is SECURE.");
		}
	}

	private static void printResult(final boolean secure, final int numRun, final SDGConfig config) {
		info(numRun + (secure ? "\t SECURE  " : "\t ILLEGAL ")  + analysisInfo() + "\t" + configToString(config));
	}
	
	private static String analysisInfo() {
		return "<sdg:(" + lastSDGsize + ")" + lastSDGtime + "ms, leaks:" + lastViolations + ">";
	}
	
	private static String configToString(final SDGConfig config) {
		final StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append("points-to: ");
		if (config.computeInterferences()) {
			sb.append("instance-based");
		} else {
			switch (config.getPointsToPrecision()) {
			case RTA:
				sb.append("rapid-type");
				break;
			case TYPE_BASED:
				sb.append("type-based");
				break;
			case INSTANCE_BASED:
				sb.append("instance-based");
				break;
			case N1_CALL_STACK:
				sb.append("1-call-stack");
				break;
			case N2_CALL_STACK:
				sb.append("2-call-stack");
				break;
			case N3_CALL_STACK:
				sb.append("3-call-stack");
				break;
			case N1_OBJECT_SENSITIVE:
				sb.append("1-object-sensitive");
				break;
			case OBJECT_SENSITIVE:
				sb.append("object-sensitive");
				break;
			case UNLIMITED_OBJECT_SENSITIVE:
				sb.append("*-object-sensitive");
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
		config.setExceptionAnalysis(ExceptionAnalysis.INTERPROC);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(PointsToPrecision.OBJECT_SENSITIVE);
		config.setDefaultExceptionMethodState(new MethodState() {
			@Override
			public boolean throwsException(final SSAAbstractInvokeInstruction node) {
				if (node.isSpecial()) {
					if (node.getDeclaredTarget().getSignature().contains("Object.<init>")) {
						return false;
					}
				} else if (node.isStatic()) {
					final String sig = node.getDeclaredTarget().getSignature();
					if (sig.contains("inputPIN") || sig.contains("print")) {
						return false;
					}
				}
				
				return true;
			}
		});
		
		return config;
	}
	
	private static IFCAnalysis annotateSDG(final SDGProgram p) {
		final IFCAnalysis ana = new IFCAnalysis(p);

		// annotate sources
		{
			final Collection<SDGCall> calls = p.getCallsToMethod(JavaMethodSignature.fromString(DEFAULT_SECRET_SOURCE));
			for (final SDGCall call : calls) {
				final SDGCallReturnNode ret = call.getReturn();
				ana.addSourceAnnotation(ret, BuiltinLattices.STD_SECLEVEL_HIGH);
			}
		}
		
		// annotate sinks
		{
			final Collection<SDGCall> calls = p.getCallsToMethod(JavaMethodSignature.fromString(DEFAULT_PUBLIC_OUTPUT));
			for (final SDGCall call : calls) {
				final Collection<SDGActualParameter> params = call.getActualParameters();
				for (final SDGActualParameter aIn : params) {
					if (aIn.getIndex() == 1) {
						ana.addSinkAnnotation(aIn, BuiltinLattices.STD_SECLEVEL_LOW);
					}
				}
			}
		}
		
		return ana;
	}
	
	private static SDGProgram buildSDG(final SDGConfig config) {
		SDGProgram prog = null;
		
		try {
			final long t1 = System.currentTimeMillis();
			prog = SDGProgram.createSDGProgram(config);
			final long t2 = System.currentTimeMillis();
			lastSDGtime = t2 - t1;
			lastSDGsize = prog.getSDG().vertexSet().size();
		} catch (Exception e) {
			errorExit(e);
		}

		return prog;
	}

	private static boolean compile(final File javaFile) {
		final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		if (compiler == null) {
			errorExit("could not find system java compiler.");
		}
		
		return 0 == compiler.run(System.in, System.out, System.err,
				"-nowarn", "-source", "1.4", "-target", "1.4", javaFile.getAbsolutePath());
	}
	
	private static void dumpSDGtoFile(final SDG sdg, final String suffix, final boolean isSecure) {
		if (DUMP_SDG_FILES) {
			final String fileName = sdg.getName() + "-" + suffix + (isSecure ? "-secure.pdg" : "-illegal.pdg");
	
			try {
				SDGSerializer.toPDGFormat(sdg, new FileOutputStream(fileName));
				debug("writing SDG to " + fileName);
			} catch (FileNotFoundException e) {
				errorExit(e.getMessage());
			}
		}
	}
	
	private static void print(String msg) {
		System.out.println(msg);
	}

	private static void info(String msg) {
//		System.out.println(msg);
	}
	
	private static void debug(String msg) {
//		System.out.println(msg);
	}
	
	private static void printUsage() {
		System.out.println("Provide a .java file as single argument.");
	}
	
	private static void errorExit(String msg) {
		System.err.println(msg);
		System.exit(1);
	}
	
	private static void errorExit(Throwable t) {
		t.printStackTrace();
		errorExit(t.getMessage());
	}
	
}
