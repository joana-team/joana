/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.pruned.DoNotPrune;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.IBinaryViolation;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.IUnaryViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolationVisitor;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.Reason;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SPos;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;
import edu.kit.joana.util.Config;
import edu.kit.joana.util.Maybe;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public final class CheckInformationFlow {

	private static final String THREAD_START = "java.lang.Thread.start()V";
	private static final boolean DUMP_SDG_FILES = false;
	private static final String DEFAULT_SECRET_SOURCE = "ifc.Main.inputPIN()I";
	private static final String DEFAULT_PUBLIC_OUTPUT = "ifc.Main.print(I)V";


	
	public static class CheckIFCConfig {
		public static final String DEFAULT_LIB_DIR = "../jSDG/lib/";
		public static final String DEFAULT_THIRD_PARTY_LIB = null;

		public final String bin;
		public final String src;
		public final String libDir;
		public final String thirdPartyLib;
		public final PrintStream out;
		public final IFCCheckResultConsumer results;
		public final IProgressMonitor progress;
		public AnalysisScope scope = null;
		public final IFCType selectedIFCType;
		public final IFCResultFilter filter;

		public CheckIFCConfig(final String bin, final String src) {
			this(bin, src, DEFAULT_LIB_DIR, DEFAULT_THIRD_PARTY_LIB, System.out, IFCCheckResultConsumer.STDOUT,
					IFCResultFilter.DEFAULT, NullProgressMonitor.INSTANCE, IFCType.RLSOD);
		}

		public CheckIFCConfig(final String bin, final String src, final PrintStream out) {
			this(bin, src, DEFAULT_LIB_DIR, DEFAULT_THIRD_PARTY_LIB, out, IFCCheckResultConsumer.STDOUT,
					IFCResultFilter.DEFAULT, NullProgressMonitor.INSTANCE, IFCType.RLSOD);
		}

		public CheckIFCConfig(final String bin, final String src, final String libDir, final String thirdPartyLib,
				final PrintStream out, final IFCCheckResultConsumer results, final IFCResultFilter filter,
				final IProgressMonitor progress, final IFCType selectedIFCType) {
			if (src == null) {
				throw new IllegalArgumentException("src directory is null.");
			} else if (bin == null) {
				throw new IllegalArgumentException("bin directory is null.");
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
			this.libDir = libDir;
			this.thirdPartyLib = thirdPartyLib;
			this.out = out;
			this.results = results;
			this.filter = filter;
			this.progress = progress;
			this.selectedIFCType = selectedIFCType;
	}

		public String toString() {
			return "check information flow at src(" + src + "), bin(" + bin + ")";
		}
	}

	private static enum AnnotationMethod { HARDCODED, FROM_ANNOTATIONS };

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
	private final AnnotationMethod annotationMethod;

	public CheckInformationFlow(final CheckIFCConfig cfc) {
		this.cfc = cfc;
		this.annotationMethod = AnnotationMethod.FROM_ANNOTATIONS;
	}


	public void runCheckIFC(EntryPointConfiguration entryPoint, final IProgressMonitor progress) throws IOException, ClassHierarchyException,
			IllegalArgumentException, CancelException, UnsoundGraphException {
		final SDGConfig config = entryPoint.getSDGConfigFor(cfc);
		final SDGProgram p = buildSDG(config);
		
		if (containsThreads(p)) {			
			cfc.out.println("checking '" + cfc.bin + "' for concurrent confidentiality.");
			config.setComputeInterferences(true);
			config.setMhpType(MHPType.PRECISE);
			final SDGProgram concProg = buildSDG(config); 
			final IFCResult result = doThreadIFCanalysis(config, concProg, entryPoint, cfc.filter);
			cfc.results.consume(result);
		} else {
			cfc.out.println("checking '" + cfc.bin + "' for sequential confidentiality.");
			final IFCResult result = doSequentialIFCanalysis(config, p, entryPoint, cfc.filter, progress);
			cfc.results.consume(result);
		}
	}
	
	private IFCResult doSequentialIFCanalysis(final SDGConfig config, final SDGProgram prog,
			final EntryPointConfiguration entryPoint, final IFCResultFilter filter,
			final IProgressMonitor progress) throws CancelException {
		final IFCResult result = new IFCResult(entryPoint, filter);
		
		final Set<SLeak> excLeaks = checkIFC(Reason.EXCEPTION, prog, IFCType.CLASSICAL_NI, annotationMethod);
		final boolean isSecure = excLeaks.isEmpty();
		printResult(excLeaks.isEmpty(), 0, config);
		dumpSDGtoFile(prog.getSDG(), "exc", isSecure);
		
		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			final Set<SLeak> noExcLeaks = checkIFC(Reason.BOTH_FLOW, noExcProg, IFCType.CLASSICAL_NI, annotationMethod);
			printResult(noExcLeaks.isEmpty(), 1, config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc", noExcLeaks.isEmpty());

			if (!noExcLeaks.isEmpty()) {
				// run without control deps
				stripControlDeps(noExcProg, progress);
				final Set<SLeak> directLeaks = checkIFC(Reason.DIRECT_FLOW, noExcProg, IFCType.CLASSICAL_NI, annotationMethod);
				printResult(directLeaks.isEmpty(), 2, config);
				dumpSDGtoFile(noExcProg.getSDG(), "no_cdeps", directLeaks.isEmpty());
				
				noExcLeaks.removeAll(directLeaks);
				excLeaks.removeAll(directLeaks);
				excLeaks.removeAll(noExcLeaks);
				
				for (final SLeak leak : directLeaks) {
					result.addDirectLeak(leak);
				}

				for (final SLeak leak : noExcLeaks) {
					result.addNoExcLeak(leak);
				}
				
				for (final SLeak leak : excLeaks) {
					result.addExcLeak(leak);
				}
			} else {
				excLeaks.removeAll(noExcLeaks);
				
				for (final SLeak leak : noExcLeaks) {
					result.addNoExcLeak(leak);
				}
			
				for (final SLeak leak : excLeaks) {
					result.addExcLeak(leak);
				}
			}

			cfc.out.println("Information leaks detected. Program is NOT SECURE.");
		} else {
			cfc.out.println("No information leaks detected. Program is SECURE.");
		}
		
		return result;
	}
	
	private IFCResult doThreadIFCanalysis(final SDGConfig config, final SDGProgram prog,
			final EntryPointConfiguration entryPoint, final IFCResultFilter filter) {
		final IFCType ifcType = cfc.selectedIFCType;
		cfc.out.println("using " + ifcType + " algorithm.");
		
		final IFCResult result = new IFCResult(entryPoint, filter);
		final Set<SLeak> threadLeaks = checkIFC(Reason.THREAD_EXCEPTION, prog, ifcType, annotationMethod);
		final boolean isSecure = threadLeaks.isEmpty();
		
		printResult(threadLeaks.isEmpty(), 0, config);
		dumpSDGtoFile(prog.getSDG(), "thread", isSecure);
		
		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			final Set<SLeak> noExcLeaks = checkIFC(Reason.THREAD, noExcProg, ifcType, annotationMethod);
			
			printResult(noExcLeaks.isEmpty(), 1, config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc_thread", noExcLeaks.isEmpty());

			threadLeaks.removeAll(noExcLeaks);
			for (final SLeak leak : noExcLeaks) {
				result.addNoExcLeak(leak);
			}
			for (final SLeak leak : threadLeaks) {
				result.addExcLeak(leak);
			}
			
			cfc.out.println("Information leaks detected. Program is NOT SECURE.");
		} else {
			cfc.out.println("No information leaks detected. Program is SECURE.");
		}
		
		return result;
	}
	
	private void stripControlDeps(final SDGProgram prog, final IProgressMonitor progress) throws CancelException {
		final SDG sdg = prog.getSDG();
		final List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		for (final SDGEdge e : sdg.edgeSet()) {
			switch (e.getKind()) {
			case CONTROL_DEP_COND:
			case CONTROL_DEP_UNCOND:
			case SUMMARY:
			case SUMMARY_DATA:
			case SUMMARY_NO_ALIAS:
				toRemove.add(e);
				break;
			default: // nothing to do
			}
		}
		sdg.removeAllEdges(toRemove);
		
		// rerun summary computation
		final WorkPackage wp = createSummaryWorkpackage(sdg);
		SummaryComputation.computeHeapDataDep(wp, progress);
	}
	
	private static WorkPackage createSummaryWorkpackage(final SDG sdg) {
		final Set<EntryPoint> entries = new TreeSet<EntryPoint>();
		final SDGNode root = sdg.getRoot();

		final TIntSet formalIns = new TIntHashSet();
		for (final SDGNode fIn : sdg.getFormalIns(root)) {
			formalIns.add(fIn.getId());
		}
		
		final TIntSet formalOuts = new TIntHashSet();
		for (final SDGNode fOut : sdg.getFormalOuts(root)) {
			formalOuts.add(fOut.getId());
		}
		
		final EntryPoint ep = new EntryPoint(root.getId(), formalIns, formalOuts);
		entries.add(ep);

		return WorkPackage.create(sdg, entries, "no_control_deps");
	}
	
	private void dumpSDGtoFile(final SDG sdg, final String suffix, final boolean isSecure) {
		if (DUMP_SDG_FILES) {
			final String fileName = sdg.getName() + "-" + suffix + (isSecure ? "-secure.pdg" : "-illegal.pdg");
	
			try {
				final File f = new File(fileName);
				final FileOutputStream fOut = new FileOutputStream(f);
				SDGSerializer.toPDGFormat(sdg, fOut);
				cfc.out.println("writing SDG to " + f.getAbsolutePath());
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
	
	public static SDGConfig createDefaultConfig(final CheckIFCConfig cfc, final JavaMethodSignature mainMethod) {
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
				}
				
				return true;
			}
		});
		if (cfc.thirdPartyLib != null) {
			config.setThirdPartyLibsPath(cfc.thirdPartyLib);
		}
		config.setPruningPolicy(DoNotPrune.INSTANCE);
		System.setProperty(Config.C_OBJGRAPH_MAX_NODES_PER_INTERFACE, "-1");
		System.setProperty(Config.C_OBJGRAPH_CUT_OFF_IMMUTABLE, "true");
		System.setProperty(Config.C_OBJGRAPH_CUT_OFF_UNREACHABLE, "true");
		
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
			case CUSTOM:
				sb.append("custom");
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
	
	private static Set<SLeak> checkIFC(final Reason reason, final SDGProgram prog, final IFCType type, final AnnotationMethod annotationMethod) {
		final IFCAnalysis ana = annotateSDG(prog, annotationMethod);
		if (type == IFCType.RLSOD) {
			ana.setTimesensitivity(true);
		}
		final Collection<? extends IViolation<SecurityNode>> leaks = ana.doIFC(type);
		lastViolations = leaks.size();
		
		final Set<SLeak> sleaks = extractLeaks(ana, leaks, reason);
		
		return sleaks;
	}

	
	private static IFCAnalysis annotateSDG(final SDGProgram p, final AnnotationMethod annotationMethod) {
		final IFCAnalysis ana = new IFCAnalysis(p);
		
		if (AnnotationMethod.FROM_ANNOTATIONS == annotationMethod) {
			ana.addAllJavaSourceAnnotations();
			return ana;
		}

		ana.addSourceAnnotationsToCallers(JavaMethodSignature.fromString(DEFAULT_SECRET_SOURCE), BuiltinLattices.STD_SECLEVEL_HIGH);
		
		// annotate sinks
		ana.addSinkAnnotationsToActualsAtCallsites(JavaMethodSignature.fromString(DEFAULT_PUBLIC_OUTPUT), BuiltinLattices.STD_SECLEVEL_LOW);
		
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

					final SortedSet<SPos> chop = new TreeSet<SPos>();
					chop.add(ssource);
					chop.add(ssink);
					final Maybe<SecurityNode> mayTrigger = orderConf.getTrigger();
					final SPos trpos;
					if (mayTrigger.isJust()) {
						final SecurityNode trigger = mayTrigger.extract();
						trpos = new SPos(trigger.getSource(), trigger.getSr(), trigger.getEr(), trigger.getSc(), trigger.getEc());
						chop.add(trpos);
					} else {
						trpos = null;
					}
					
					final SLeak sleak = new SLeak(ssource, ssink,
							(reason == Reason.THREAD ? Reason.THREAD_ORDER : reason), chop);
					for (final SecurityNode sn : orderConf.getAllTriggers()) {
						final SPos strigger= new SPos(sn.getSource(), sn.getSr(), sn.getEr(), sn.getSc(), sn.getEc());
						sleak.addTrigger(strigger);
					}
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
					
					final SortedSet<SPos> chop = new TreeSet<SPos>();
					chop.add(ssource);
					chop.add(ssink);
					final Maybe<SecurityNode> mayTrigger = dataConf.getTrigger();
					final SPos trpos;
					if (mayTrigger.isJust()) {
						final SecurityNode trigger = mayTrigger.extract();
						trpos = new SPos(trigger.getSource(), trigger.getSr(), trigger.getEr(), trigger.getSc(), trigger.getEc());
						chop.add(trpos);
					} else {
						trpos = null;
					}
					
					final SLeak sleak = new SLeak(ssource, ssink,
							(reason == Reason.THREAD ? Reason.THREAD_DATA : reason), chop, trpos);
					sleaks.add(sleak);
				}

				@Override
				public <L> void visitUnaryViolation(IUnaryViolation<SecurityNode, L> unVio) {
					SLeak sleak = extractSourceLeaks(unVio.getNode(), unVio.getNode(), reason, Collections.<SDGNode>singleton(unVio.getNode()));
					assert sleak != null;
					sleaks.add(sleak);
				}

				@Override
				public <L> void visitBinaryViolation(IBinaryViolation<SecurityNode, L> binVio) {
					HashSet<SDGNode> chop = new HashSet<SDGNode>();
					chop.add(binVio.getNode());
					chop.add(binVio.getInfluencedBy());
					SLeak sleak = extractSourceLeaks(binVio.getInfluencedBy(), binVio.getNode(), reason, chop);
					assert sleak != null;
					sleaks.add(sleak);
				}
			});
		}
		
		return sleaks;
	}
	
	private static SLeak extractSourceLeaks(final SecurityNode source, final SecurityNode sink, final Reason reason,
			final Collection<SDGNode> nodes) {
		final TreeSet<SPos> positions = new TreeSet<SPos>();
		
		for (final SDGNode n : nodes) {
			if (n.getSource() != null) {
				final SPos spos = new SPos(n.getSource(), n.getSr(), n.getEr(), n.getSc(), n.getEc());
				if (!spos.isAllZero()) {
					positions.add(spos);
				}
			}
		}

		final SPos ssource = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
		final SPos ssink = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());

		if (positions.isEmpty()) {
			final SortedSet<SPos> chop = new TreeSet<SPos>();
			chop.add(ssource);
			chop.add(ssink);
			final SLeak sleak = new SLeak(ssource, ssink, reason, chop);
			
			return sleak;
		}
		
		final SLeak leak = new SLeak(ssource, ssink, reason, positions);
		return leak;
	}

}
