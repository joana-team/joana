/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.common.collect.ImmutableMultimap;
import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.SPos;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.annotations.cause.UnknownCause;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGBuildPreparation;
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
import edu.kit.joana.ui.annotations.ChopComputation;
import edu.kit.joana.ui.annotations.EntryPointKind;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.Reason;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;
import edu.kit.joana.util.Config;
import edu.kit.joana.util.Maybe;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.CGConsumer;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.summary.SummaryComputation;
import edu.kit.joana.wala.summary.SummaryComputationType;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.WorkPackage.EntryPoint;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public final class CheckInformationFlow {

	private static final String THREAD_START = "java.lang.Thread.start()V";
	private static final boolean DUMP_SDG_FILES = true;
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
		final EntryPointKind kind = entryPoint.getKind();
		final String file = entryPoint.getSdgFile();
		
		SDGProgram p;
		boolean containsThreads;
		boolean rebuiltWithoutExceptionEdges;
		switch (kind) {
			case UNKNOWN: {
				p = buildSDG(config);
				rebuiltWithoutExceptionEdges = true;
				containsThreads = containsThreads(p);
				if (!containsThreads) break;
			}
			case CONCURRENT: {
				p = null;
				config.setComputeInterferences(true);
				config.setMhpType(MHPType.PRECISE_UNSAFE);
				p = buildSDG(config);
				rebuiltWithoutExceptionEdges = true;
				containsThreads = true;
				break;
			}
			case SEQUENTIAL: {
				p = buildSDG(config);
				containsThreads = false;
				rebuiltWithoutExceptionEdges = true;
				break;
			}
			case FROMFILE: {
				if (file == null) {
					throw new IllegalArgumentException("must provide file path when using " + EntryPointKind.FROMFILE);
				}
				p = SDGProgram.loadSDG(file, MHPType.PRECISE_UNSAFE);
				final IClassHierarchy cha; {
					final PrintStream out = IOFactory.createUTF8PrintStream(new ByteArrayOutputStream());
					com.ibm.wala.util.collections.Pair<Long, SDGBuilder.SDGBuilderConfig> pair = SDGBuildPreparation.prepareBuild(out, SDGProgram.makeBuildPreparationConfig(config), NullProgressMonitor.INSTANCE);
					cha = pair.snd.cha;
				}
				
				// TODO: find relevant classes, similar to SDGProgram.findClassesRelevantForAnnotation()
				p.fillWithAnnotations(cha, cha);
				containsThreads = containsThreads(p);
				rebuiltWithoutExceptionEdges = false;
				break;
			}
			
			default: {
				assert false;
				containsThreads = false;
				rebuiltWithoutExceptionEdges = false;
				p = null;
			}
		}
		
		assert p != null;
		
		if (containsThreads) {			
			cfc.out.println("checking '" + cfc.bin + "' for concurrent confidentiality.");
			final WeakReference<SDGProgram> pRef = new WeakReference<SDGProgram>(p);
			p = null;
			final IFCResult result = doThreadIFCanalysis(config, pRef, entryPoint, cfc.filter, rebuiltWithoutExceptionEdges);
			cfc.results.consume(result);
		} else {
			cfc.out.println("checking '" + cfc.bin + "' for sequential confidentiality.");
			final WeakReference<SDGProgram> pRef = new WeakReference<SDGProgram>(p);
			p = null;
			final IFCResult result = doSequentialIFCanalysis(config, pRef, entryPoint, cfc.filter, progress);
			cfc.results.consume(result);
		}
	}
	
	private IFCResult doSequentialIFCanalysis(final SDGConfig config, final WeakReference<SDGProgram> progRef,
			final EntryPointConfiguration entryPoint, final IFCResultFilter filter,
			final IProgressMonitor progress) throws CancelException {
		SDGProgram prog = progRef.get();
		dumpSDGtoFile(prog.getSDG(), "exc");
		final IFCResult result = new IFCResult(entryPoint, filter, prog.getNodeCollector());
		//final Pair<Set<SLeak>, Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>>> excResult =
		//		checkIFC(Reason.EXCEPTION, prog, IFCType.CLASSICAL_NI, annotationMethod, entryPoint);
		final Pair<Set<SLeak>, Collection<IFCAnnotation>> excResult =
				checkIFC(Reason.EXCEPTION, prog, IFCType.CLASSICAL_NI, annotationMethod, entryPoint);
		prog = null;
		final Set<SLeak> excLeaks = excResult.getFirst();
		result.setAnnotations2(excResult.getSecond());
		final boolean isSecure = excLeaks.isEmpty();
		printResult(excLeaks.isEmpty(), 0, config);
		
		if (!isSecure) {
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc");
			//final Pair<Set<SLeak>, Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>>> noExcResult = 
			//		checkIFC(Reason.BOTH_FLOW, noExcProg, IFCType.CLASSICAL_NI, annotationMethod, entryPoint);
			final Pair<Set<SLeak>, Collection<IFCAnnotation>> noExcResult = 
					checkIFC(Reason.BOTH_FLOW, noExcProg, IFCType.CLASSICAL_NI, annotationMethod, entryPoint);
			final Set<SLeak> noExcLeaks = noExcResult.getFirst();
			result.setAnnotations2(noExcResult.getSecond());
			printResult(noExcLeaks.isEmpty(), 1, config);
			

			if (!noExcLeaks.isEmpty()) {
				// run without control deps
				stripControlDeps(noExcProg, progress);
				dumpSDGtoFile(noExcProg.getSDG(), "no_cdeps");
				//final Pair<Set<SLeak>, Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>>> directResult =
				//	checkIFC(Reason.DIRECT_FLOW, noExcProg, IFCType.CLASSICAL_NI, annotationMethod, entryPoint);
				final Pair<Set<SLeak>, Collection<IFCAnnotation>> directResult =
					checkIFC(Reason.DIRECT_FLOW, noExcProg, IFCType.CLASSICAL_NI, annotationMethod, entryPoint);
				final Set<SLeak> directLeaks = directResult.getFirst();
				result.setAnnotations2(directResult.getSecond());
				printResult(directLeaks.isEmpty(), 2, config);
				
				
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
	
	private IFCResult doThreadIFCanalysis(final SDGConfig config, final WeakReference<SDGProgram> progRef,
			final EntryPointConfiguration entryPoint, final IFCResultFilter filter, final boolean rebuiltWithoutExceptionEdges) {
		SDGProgram prog = progRef.get();
		final IFCType ifcType = cfc.selectedIFCType;
		dumpSDGtoFile(prog.getSDG(), "thread");
		cfc.out.println("using " + ifcType + " algorithm.");
		
		final IFCResult result = new IFCResult(entryPoint, filter, prog.getNodeCollector());
		//final Pair<Set<SLeak>, Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>>> threadResult =
		//	checkIFC(Reason.THREAD_EXCEPTION, prog, ifcType, annotationMethod, entryPoint);
		final Pair<Set<SLeak>, Collection<IFCAnnotation>> threadResult;
		if (rebuiltWithoutExceptionEdges) {
			threadResult = checkIFC(Reason.THREAD_EXCEPTION, prog, ifcType, annotationMethod, entryPoint);
		} else {
			threadResult = checkIFC(Reason.THREAD,           prog, ifcType, annotationMethod, entryPoint);
		}
		prog = null;
		final Set<SLeak> threadLeaks = threadResult.getFirst();
		result.setAnnotations2(threadResult.getSecond());
		final boolean isSecure = threadLeaks.isEmpty();
		
		printResult(threadLeaks.isEmpty(), 0, config);
		
		
		if (isSecure) {
			cfc.out.println("No information leaks detected. Program is SECURE.");
		} else if (rebuiltWithoutExceptionEdges) {
			
			config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
			final SDGProgram noExcProg = buildSDG(config);
			dumpSDGtoFile(noExcProg.getSDG(), "no_exc_thread");
			//final Pair<Set<SLeak>, Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>>> noExcResult =
			//	checkIFC(Reason.THREAD, noExcProg, ifcType, annotationMethod, entryPoint);
			final Pair<Set<SLeak>, Collection<IFCAnnotation>> noExcResult =
					checkIFC(Reason.THREAD, noExcProg, ifcType, annotationMethod, entryPoint);
			final Set<SLeak> noExcLeaks = noExcResult.getFirst();
			result.setAnnotations2(noExcResult.getSecond());
			
			printResult(noExcLeaks.isEmpty(), 1, config);
			

			threadLeaks.removeAll(noExcLeaks);
			for (final SLeak leak : noExcLeaks) {
				result.addNoExcLeak(leak);
			}
			for (final SLeak leak : threadLeaks) {
				result.addExcLeak(leak);
			}
			
			cfc.out.println("Information leaks detected. Program is NOT SECURE.");
		} else {
			for (final SLeak leak : threadLeaks) {
				result.addNoExcLeak(leak);
			}
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
		final WorkPackage<SDG> wp = createSummaryWorkpackage(sdg);
		SummaryComputation.computeHeapDataDep(wp, progress);
	}
	
	private static WorkPackage<SDG> createSummaryWorkpackage(final SDG sdg) {
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
	
	private void dumpSDGtoFile(final SDG sdg, final String suffix) {
		if (DUMP_SDG_FILES) {
			final String fileName = sdg.getName() + "-" + suffix + ".pdg";
	
			try {
				final File f = new File(fileName);
				final FileOutputStream fOut = new FileOutputStream(f);
				SDGSerializer.toPDGFormat(sdg, fOut);
				cfc.out.println("writing SDG to " + f.getAbsolutePath());
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e.getMessage());
			}
//			
//			try {
//				final File f = new File(fileName+".graphml");
//				final FileOutputStream fOut = new FileOutputStream(f);
//				SDG2GraphML.convertHierachical(sdg, fOut);
//				cfc.out.println("writing SDG to " + f.getAbsolutePath());
//				
//			} catch (XMLStreamException e) {
//				throw new RuntimeException(e.getMessage());
//			} catch (FileNotFoundException e) {
//				throw new RuntimeException(e.getMessage());
//			}
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
		final SDGConfig config = new SDGConfig(cfc.bin, mainMethod.toBCString(), Stubs.JRE_15_INCOMPLETE);
		config.setClasspathAddEntriesFromMANIFEST(false); // we trust eclipse, of course!
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(ExceptionAnalysis.INTERPROC);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(PointsToPrecision.INSTANCE_BASED);
		config.setSummaryComputationType(SummaryComputationType.JOANA_CLASSIC_SCC);
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
		config.setPruningPolicy(ApplicationLoaderPolicy.INSTANCE);
		//System.setProperty(Config.C_OBJGRAPH_MAX_NODES_PER_INTERFACE, "-1");
		System.setProperty(Config.C_OBJGRAPH_CUT_OFF_IMMUTABLE, "true");
		System.setProperty(Config.C_OBJGRAPH_CUT_OFF_UNREACHABLE, "true");
		
		return config;
	}
	
	private static SDGProgram buildSDG(final SDGConfig config) {
		SDGProgram prog = null;
		
		try {
			final long t1 = System.currentTimeMillis();
			prog = SDGProgram.createSDGProgram(config, System.out, NullProgressMonitor.INSTANCE);
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
			case PRECISE_UNSAFE:
				sb.append("precise analysis (optimized implementation)");
				break;
			}
		}
		
		sb.append("]");
		
		return sb.toString();
	}
	
	//private static Pair<Set<SLeak>, Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>>> checkIFC(final Reason reason, final SDGProgram prog, final IFCType type, final AnnotationMethod annotationMethod, final EntryPointConfiguration entryPoint) {
	private static Pair<Set<SLeak>, Collection<IFCAnnotation>> checkIFC(final Reason reason, final SDGProgram prog, final IFCType type, final AnnotationMethod annotationMethod, final EntryPointConfiguration entryPoint) {
		final IFCAnalysis ana = new IFCAnalysis(prog,entryPoint.lattice());
		ana.addSinkClasses(entryPoint.getClassSinks());
		if (AnnotationMethod.FROM_ANNOTATIONS == annotationMethod) {
			entryPoint.annotateSDG(ana);
		} else {
			ana.addSourceAnnotationsToCallers(JavaMethodSignature.fromString(DEFAULT_SECRET_SOURCE), BuiltinLattices.STD_SECLEVEL_HIGH, UnknownCause.INSTANCE);
			
			// annotate sinks
			ana.addSinkAnnotationsToActualsAtCallsites(JavaMethodSignature.fromString(DEFAULT_PUBLIC_OUTPUT), BuiltinLattices.STD_SECLEVEL_LOW, UnknownCause.INSTANCE);
			 
			Pair.pair(ImmutableMultimap.of() , ImmutableMultimap.of());
		}

		if (type == IFCType.RLSOD || type == IFCType.LSOD) {
			//ana.setTimesensitivity(true);
		}
		final Collection<? extends IViolation<SecurityNode>> leaks = ana.doIFC(type);
		lastViolations = leaks.size();
		
		final Set<SLeak> sleaks = extractLeaks(ana, leaks, reason, entryPoint.getChopComputation());
		
		return Pair.pair(sleaks, ana.getAnnotations());
	}

	private static Set<SLeak> extractLeaks(final IFCAnalysis ana,
			final Collection<? extends IViolation<SecurityNode>> leaks, final Reason reason, final ChopComputation chopComputation) {
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

					final SortedMap<SPos, Set<SecurityNode>> chop = new TreeMap<>();
					chop.compute(ssource, (sp, ns) -> {
						if (ns == null) ns = new HashSet<>(1);
						ns.add(source);
						return ns;
					});
					chop.compute(ssink, (sp, ns) -> {
						if (ns == null) ns = new HashSet<>(1);
						ns.add(sink);
						return ns;
					});
					final Maybe<SecurityNode> mayTrigger = orderConf.getTrigger();
					final SPos trpos;
					if (mayTrigger.isJust()) {
						final SecurityNode trigger = mayTrigger.extract();
						trpos = new SPos(trigger.getSource(), trigger.getSr(), trigger.getEr(), trigger.getSc(), trigger.getEc());
						chop.compute(trpos, (sp, ns) -> {
							if (ns == null) ns = new HashSet<>(1);
							ns.add(trigger);
							return ns;
						});
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
					
					final Collection<SDGNode> nodes;
					if (chopComputation == ChopComputation.ALL) {
						final NonSameLevelChopper chopper = new NonSameLevelChopper(sdg);
						nodes = chopper.chop(source, sink);
					} else {
						assert chopComputation == ChopComputation.NONE;
						nodes = Collections.emptySet();
					}
					
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
					
					final SortedMap<SPos, Set<SecurityNode>> chop = new TreeMap<>();
					chop.compute(ssource, (sp, ns) -> {
						if (ns == null) ns = new HashSet<>(1);
						ns.add(source);
						return ns;
					});
					chop.compute(ssink, (sp, ns) -> {
						if (ns == null) ns = new HashSet<>(1);
						ns.add(sink);
						return ns;
					});
					final Maybe<SecurityNode> mayTrigger = dataConf.getTrigger();
					final SPos trpos;
					if (mayTrigger.isJust()) {
						final SecurityNode trigger = mayTrigger.extract();
						trpos = new SPos(trigger.getSource(), trigger.getSr(), trigger.getEr(), trigger.getSc(), trigger.getEc());
						chop.compute(trpos, (sp, ns) -> {
							if (ns == null) ns = new HashSet<>(1);
							ns.add(trigger);
							return ns;
						});
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
		final SortedMap<SPos, Set<SDGNode>> positions = new TreeMap<>();
		
		for (final SDGNode n : nodes) {
			if (n.getSource() != null) {
				final SPos spos = new SPos(n.getSource(), n.getSr(), n.getEr(), n.getSc(), n.getEc());
				if (!spos.isAllZero()) {
					positions.compute(spos, (sp, ns) -> {
						if (ns == null) ns = new HashSet<>();
						ns.add(n);
						return ns;
					});
				}
			}
		}

		final SPos ssource = new SPos(source.getSource(), source.getSr(), source.getEr(), source.getSc(), source.getEc());
		final SPos ssink = new SPos(sink.getSource(), sink.getSr(), sink.getEr(), sink.getSc(), sink.getEc());

		if (positions.isEmpty()) {
			final SortedMap<SPos, Set<SecurityNode>> chop = new TreeMap<>();
			chop.compute(ssource, (sp, ns) -> {
				if (ns == null) ns = new HashSet<>();
				ns.add(source);
				return ns;
			});
			chop.compute(ssink, (sp, ns) -> {
				if (ns == null) ns = new HashSet<>();
				ns.add(sink);
				return ns;
			});
			final SLeak sleak = new SLeak(ssource, ssink, reason, chop);
			
			return sleak;
		}
		
		final SLeak leak = new SLeak(ssource, ssink, reason, positions);
		return leak;
	}

}
