/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
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
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.Reason;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SPos;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;

public final class CheckInformationFlow {

	private static final String THREAD_START = "java.lang.Thread.start()V";
	private static final boolean DUMP_SDG_FILES = false;
	private static final String DEFAULT_SECRET_SOURCE = "ifc.Main.inputPIN()I";
	private static final String DEFAULT_PUBLIC_OUTPUT = "ifc.Main.print(I)V";

	
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

	private final CheckIFCConfig cfc;

	public CheckInformationFlow(final CheckIFCConfig cfc) {
		this.cfc = cfc;
		this.printStatistics = cfc.printStatistics;
	}

	private final boolean printStatistics;

	public void runCheckIFC() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException, UnsoundGraphException {
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

		final SDGConfig config = createDefaultConfig(cfc, "ifc.Main");
		final SDGProgram p = buildSDG(config);
		
		if (containsThreads(p)) {			
			cfc.out.print("checking '" + cfc.bin + "' for concurrent confidentiality.");
			config.setComputeInterferences(true);
			config.setMhpType(MHPType.PRECISE);
			final SDGProgram concProg = buildSDG(config); 
			final IFCResult result = doThreadIFCanalysis(config, concProg);
			cfc.results.consume(result);
		} else {
			cfc.out.print("checking '" + cfc.bin + "' for sequential confidentiality.");
			final IFCResult result = doSequentialIFCanalysis(config, p);
			cfc.results.consume(result);
		}

		if (printStatistics) {
			//TODO print something useful
		}
	}
	
	private IFCResult doSequentialIFCanalysis(final SDGConfig config, final SDGProgram prog) {
		final IFCResult result = new IFCResult(cfc.tmpDir);
		
		final Set<SLeak> excLeaks = checkIFC(Reason.EXCEPTION, prog, IFCType.CLASSICAL_NI);
		final boolean isSecure = excLeaks.isEmpty();
		printResult(excLeaks.isEmpty(), 0, config);
		dumpSDGtoFile(prog.getSDG(), "exc", isSecure);
		
		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			final Set<SLeak> noExcLeaks = checkIFC(Reason.BOTH_FLOW, noExcProg, IFCType.CLASSICAL_NI);
			printResult(noExcLeaks.isEmpty(), 1, config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc", noExcLeaks.isEmpty());

			excLeaks.removeAll(noExcLeaks);
			for (final SLeak leak : noExcLeaks) {
				result.addNoExcLeak(leak);
			}
			for (final SLeak leak : excLeaks) {
				result.addExcLeak(leak);
			}

			cfc.out.println("Information leaks detected. Program is NOT SECURE.");
		} else {
			cfc.out.println("No information leaks detected. Program is SECURE.");
		}
		
		return result;
	}
	
	private IFCResult doThreadIFCanalysis(final SDGConfig config, final SDGProgram prog) {
		final IFCResult result = new IFCResult(cfc.tmpDir);
		final Set<SLeak> threadLeaks = checkIFC(Reason.THREAD_EXCEPTION, prog, IFCType.RLSOD);
		final boolean isSecure = threadLeaks.isEmpty();

		printResult(threadLeaks.isEmpty(), 0, config);
		dumpSDGtoFile(prog.getSDG(), "thread", isSecure);

		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			final Set<SLeak> noExcLeaks = checkIFC(Reason.THREAD, noExcProg, IFCType.RLSOD);
			
			printResult(noExcLeaks.isEmpty(), 1, config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc_thread", noExcLeaks.isEmpty());

			threadLeaks.removeAll(noExcLeaks);
			for (final SLeak leak : noExcLeaks) {
				result.addNoExcLeak(leak);
			}
			for (final SLeak leak : threadLeaks) {
				result.addExcLeak(leak);
			}

			cfc.out.print("Information leaks detected. Program is NOT SECURE.");
		} else {
			cfc.out.print("No information leaks detected. Program is SECURE.");
		}
		
		return result;
	}
	
	private void dumpSDGtoFile(final SDG sdg, final String suffix, final boolean isSecure) {
		if (DUMP_SDG_FILES) {
			final String fileName = sdg.getName() + "-" + suffix + (isSecure ? "-secure.pdg" : "-illegal.pdg");
	
			try {
				SDGSerializer.toPDGFormat(sdg, new FileOutputStream(fileName));
				cfc.out.println("writing SDG to " + fileName);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e.getMessage());
			}
		}
	}
	
	private void printResult(final boolean secure, final int numRun, final SDGConfig config) {
		cfc.out.println(numRun + (secure ? "\t SECURE  " : "\t ILLEGAL ")  + analysisInfo() + "\t" + configToString(config));
	}
	
	private static int lastSDGsize, lastViolations;
	private static long lastSDGtime; 
	
	private static String analysisInfo() {
		return "<sdg:(" + lastSDGsize + ")" + lastSDGtime + "ms, leaks:" + lastViolations + ">";
	}

	
	private static boolean containsThreads(final SDGProgram p) {
		final SDGMethod m = p.getMethod(THREAD_START);
		
		return m != null;
	}
	
	private static SDGConfig createDefaultConfig(final CheckIFCConfig cfc, final String mainClass) {
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(mainClass);
		final SDGConfig config = new SDGConfig(cfc.bin, mainMethod.toBCString(), Stubs.JRE_14);
		config.setNativesXML(cfc.libDir + "natives_empty.xml");
//		cfg.stubs = cfc.libDir + "jSDG-stubs-jre1.4.jar";
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
	
	private static SDGProgram buildSDG(final SDGConfig config) {
		SDGProgram prog = null;
		
		try {
			final long t1 = System.currentTimeMillis();
			prog = SDGProgram.createSDGProgram(config);
			final long t2 = System.currentTimeMillis();
			lastSDGtime = t2 - t1;
			lastSDGsize = prog.getSDG().vertexSet().size();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return prog;
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
	
	private static Set<SLeak> checkIFC(final Reason reason, final SDGProgram prog, final IFCType type) {
		final IFCAnalysis ana = annotateSDG(prog);
		final Collection<? extends IViolation<SecurityNode>> leaks = ana.doIFC(type);
		lastViolations = leaks.size();
		
		final Set<SLeak> sleaks = extractLeaks(ana, leaks, reason);
		
		return sleaks;
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
	
	private static Set<SLeak> extractLeaks(final IFCAnalysis ana,
			final Collection<? extends IViolation<SecurityNode>> leaks, final Reason reason) {
		final TreeSet<SLeak> sleaks = new TreeSet<SLeak>();
		for (final IViolation<SecurityNode> leak : leaks) {
			leak.accept(new IViolationVisitor<SecurityNode>() {
				
				@Override
				public void visitOrderConflict(final OrderConflict<SecurityNode> orderConf) {
					final SecurityNode source = orderConf.getConflictEdge().getSource();
					final SecurityNode sink = orderConf.getConflictEdge().getTarget();
					final SPos one = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
					final SPos two = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());

					final SPos ssource;
					final SPos ssink;
					if (one.compareTo(two) < 0) {
						ssource = one;
						ssink = two;
					} else {
						ssource = two;
						ssink = one;
					}

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
					
					final Reason realReason;
					if (reason == Reason.THREAD) {
						realReason = Reason.BOTH_FLOW;
					} else if (reason == Reason.THREAD_EXCEPTION){
						realReason = Reason.EXCEPTION;
					} else {
						realReason = reason;
					}
					
					final SLeak sleak = extractSourceLeaks(source, sink, realReason, nodes);
					if (sleak != null) {
						sleaks.add(sleak);
					}
				}
				
				@Override
				public void visitDataConflict(final DataConflict<SecurityNode> dataConf) {
					final SecurityNode source = dataConf.getConflictEdge().getSource();
					final SecurityNode sink = dataConf.getConflictEdge().getTarget();
					final SPos one = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
					final SPos two = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());

					final SPos ssource;
					final SPos ssink;
					if (one.compareTo(two) < 0) {
						ssource = one;
						ssink = two;
					} else {
						ssource = two;
						ssink = one;
					}
					
					final Set<SPos> slice = new TreeSet<SPos>();
					slice.add(ssource);
					slice.add(ssink);
					final SLeak sleak = new SLeak(ssource, ssink, reason, slice);
					sleaks.add(sleak);
				}
			});
		}
		
		return sleaks;
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

}
