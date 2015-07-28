/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.dictionary.accesspath;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import com.ibm.wala.util.intset.BitVector;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerForward;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.Main.Config;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.accesspath.APResult;
import edu.kit.joana.wala.core.accesspath.AccessPath;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.MethodResult;
import edu.kit.joana.wala.dictionary.util.ProgramSourcePositions;
import edu.kit.joana.wala.flowless.MoJo;
import edu.kit.joana.wala.flowless.MoJo.CallGraphResult;
import edu.kit.joana.wala.flowless.pointsto.GraphAnnotater.Aliasing;
import edu.kit.joana.wala.flowless.pointsto.PointsToSetBuilder.PointsTo;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.ExplicitFlowStmt;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.Parameter;
import edu.kit.joana.wala.flowless.spec.ast.PrimitiveAliasStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo.ParamInfo;
import edu.kit.joana.wala.util.ParamNum;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public final class CheckFlowLessWithAlias {

	public static class CheckFlowConfig {
		public static final String DEFAULT_TMP_OUT_DIR = "./out/";
		public static final String DEFAULT_LIB_DIR = "../jSDG/lib/";

		public final String bin;
		public final String[] src;
		public final String tmpDir;
		public final String libDir;
		public final PrintStream out;
		public final FlowCheckResultConsumer results;
		public final IProgressMonitor progress;
		public boolean printStatistics = true;
		public AnalysisScope scope = null;

		public CheckFlowConfig(final String bin, final String[] src) {
			this(bin, src, DEFAULT_TMP_OUT_DIR, DEFAULT_LIB_DIR, System.out, FlowCheckResultConsumer.DEFAULT,
					NullProgressMonitor.INSTANCE);
		}

		public CheckFlowConfig(final String bin, final String[] src, final PrintStream out) {
			this(bin, src, DEFAULT_TMP_OUT_DIR, DEFAULT_LIB_DIR, out, FlowCheckResultConsumer.STDOUT,
					NullProgressMonitor.INSTANCE);
		}

		public CheckFlowConfig(final String bin, final String[] src, final String tmpDir, final String libDir,
				final PrintStream out, final FlowCheckResultConsumer results, IProgressMonitor progress) {
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
		final CheckFlowConfig[] RUNS = new CheckFlowConfig[] {
				new CheckFlowConfig("../MoJo-TestCode/bin", new String[] {"../MoJo-TestCode/src"}, createPrintStream("check_flow.log")),
//				new CheckFlowConfig("../../3.7/runtime-EclipseApplication/eVoting-Joana/bin", "../../3.7/runtime-EclipseApplication/eVoting-Joana/src", createPrintStream("cf_evoting.log")),
//				new CheckFlowConfig("../../3.7/workspace-ifc/eVoting-Joana/bin", "../../3.7/workspace-ifc/eVoting-Joana/src", createPrintStream("cf_evoting.log")),
//				new CheckFlowConfig("../MoJo-TestCode/bin", "../MoJo-TestCode/src2", createPrintStream("check_flow.log")),
//				new CheckFlowConfig("../MoJo-TestCode/bin", "../MoJo-TestCode/src3", createPrintStream("check_flow.log")),
		};

		for (final CheckFlowConfig run : RUNS) {
			try {
				final CheckFlowLessWithAlias check = new CheckFlowLessWithAlias(run);
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

	private final CheckFlowConfig cfc;

	public CheckFlowLessWithAlias(final CheckFlowConfig cfc) {
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
	private long startAdjustTime, endAdjustTime;

	public void runCheckFlowLess() throws IOException, ClassHierarchyException, IllegalArgumentException, CancelException, UnsoundGraphException {
		cfc.out.print("Parsing source files... ");
		final List<ClassInfo> clsInfos = new LinkedList<ClassInfo>();
		for (final String src : cfc.src) {
			List<ClassInfo> clsNfoTmp = MoJo.parseSourceFiles(src);
			clsInfos.addAll(clsNfoTmp);
		}
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
			cgr = mojo.computeContextInsensitiveCallGraph(opt);
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

		final SDGResult sdgResult = create(cfc.out, opt.getAnalysisScope(), mojo, cgr, im, cfg.outputDir, cfg);
		final SDG sdg = sdgResult.sdg;

		final AliasSDG alias = AliasSDG.create(sdg, sdgResult.ap);
		alias.precomputeSummary(progress);
		boolean resetNeeded = false;

		if (printStatistics) {
			endPrepareTime = System.currentTimeMillis();
			numPreparedSDGs++;
			timePreprareSDG += (endPrepareTime - startPrepareTime);
		}

		for (final IFCStmt stmt : m.getInfo().getIFCStmts()) {
			cfc.out.print("IFC check '" + stmt + "': ");
			if (resetNeeded) {
				alias.reset();
				resetNeeded = false;
			}

			final FlowStmtResult stmtResult = m.findOrCreateStmtResult(stmt);
			try {
				final List<BasicIFCStmt> simplified = FlowLessSimplifier.simplify(stmt);
				//FlowLess2SDGMatcher.printDebugMatches = true;
				final Matcher match = FlowLess2SDGMatcher.findMatchingNodes(sdg, sdg.getRoot(), stmt);
				if (simplified.isEmpty()) {
					cfc.out.println("ERROR(empty simplified statements)");
					stmtResult.addPart(new FlowStmtResultPart(null, "ERROR(empty simplified statements)",
							false, false, cfg.exceptions, sdg.getFileName()));
				} else {
					checkBasicIFCStmts(alias, match, simplified, m.getInfo(), stmtResult, cfg.exceptions, progress);
				}
			} catch (FlowAstException e) {
				cfc.out.println("ERROR(" + e.getMessage() + ")");
				stmtResult.addPart(new FlowStmtResultPart(null, "ERROR(" + e.getMessage() + ")", false, false,
						cfg.exceptions, sdg.getFileName()));
			}

			resetNeeded = true;
		}

	}

	public static class EntityNotFoundException extends FlowAstException {

		public EntityNotFoundException(String message) {
			super(message);
		}

		private static final long serialVersionUID = -1553942031552394940L;
		
	}
	
	private void checkBasicIFCStmts(final AliasSDG alias, final Matcher match,
			final List<BasicIFCStmt> stmts, final MethodInfo mInfo, final FlowStmtResult stmtResult,
			final ExceptionAnalysis excCfg, final IProgressMonitor progress) throws CancelException, EntityNotFoundException {
		for (final BasicIFCStmt s : stmts) {
			for (final PrimitiveAliasStmt noAlias : s.aMinus) {
				final Parameter[] noalias = noAlias.getParams().toArray(new Parameter[1]);
				for (int i = 0; i < noalias.length; i++) {
					assert match.hasMatchFor(noalias[i]);
					final SDGNode n1 = match.getMatch(noalias[i]);
					if (n1 == null) {
						throw new EntityNotFoundException("found no matching parameter for '" + noalias[i] + "' in "
								+ mInfo.toString());
					}
					for (int j = i + 1; j < noalias.length; j++) {
						assert match.hasMatchFor(noalias[j]);
						final SDGNode n2 = match.getMatch(noalias[j]);
						if (n2 == null) {
							throw new EntityNotFoundException("found no matching parameter for '" + noalias[j] + "' in "
									+ mInfo.toString());
						}
						alias.setNoAlias(n1.getId(), n2.getId());
					}
				}
			}
		}

		if (printStatistics) { startAdjustTime = System.currentTimeMillis(); }
		alias.adjustMaxSDG(progress);
		alias.recomputeSummary(progress);
		if (printStatistics) {
			endAdjustTime = System.currentTimeMillis();
			numAdjustSDGs++;
			timeAdjustSDG += (endAdjustTime - startAdjustTime);
		}

		final Set<SDGEdge.Kind> omit = new HashSet<SDGEdge.Kind>();
		final SummarySlicerBackward ssb = new SummarySlicerBackward(alias.getSDG(), omit);
		ssb.addToOmit(SDGEdge.Kind.SUMMARY); // use only SUMMARY_HEAP

		final List<FlowStmtResultPart> toInfere = new LinkedList<FlowStmtResultPart>();

		for (final BasicIFCStmt s : stmts) {
			for (final ExplicitFlowStmt fl : s.flow.fMinus) {
				if (hasIllegalFlow(fl, match, ssb, alias)) {
					final FlowStmtResultPart illegalPart = new FlowStmtResultPart(s, fl.toString(), false, false,
							excCfg,	alias.getFileName());
					illegalPart.setAlias(alias.getNoAlias());
					stmtResult.addPart(illegalPart);
					cfc.out.println("illegal flow found:" + s.flow);
					if (s.shouldBeInferred) {
						toInfere.add(illegalPart);
					}
				} else {
					final FlowStmtResultPart okPart = new FlowStmtResultPart(s, fl.toString(), true, false, excCfg,
							alias.getFileName());
					okPart.setAlias(alias.getNoAlias());
					stmtResult.addPart(okPart);
					cfc.out.println("ok:" + s.flow);
				}
			}
		}

		for (final FlowStmtResultPart part : toInfere) {
			// special case => try inference of valid alias configurations
			inferValidAliasConfigurations(cfc, alias, part.getBasicStmt(), match, mInfo, stmtResult, excCfg, progress);
		}
	}

	public static ProgramSourcePositions sliceIFCStmt(final IFCStmt stmt, final FlowStmtResultPart fp,
			final String tmpDir, final IProgressMonitor progress)
			throws IOException, CancelException, FlowAstException {
		if (!fp.hasAlias()) {
			throw new IllegalArgumentException("Cannot create slice, as no alias context is provided.");
		}
		final String pathToSDG = tmpDir + (tmpDir.endsWith(File.separator) ? "" : File.separator)
				+ fp.getSDGFilename() + ".pdg";
		final AliasSDG alias = AliasSDG.readFrom(pathToSDG);
		alias.setNoAlias(fp.getAlias());
		alias.adjustMaxSDG(progress);
		alias.recomputeSummary(progress);
		final SDG sdg = alias.getSDG();
		final Matcher match = FlowLess2SDGMatcher.findMatchingNodes(sdg, sdg.getRoot(), stmt);


		final List<SDGNode> from = new LinkedList<SDGNode>();

		for (final ExplicitFlowStmt fl : fp.getBasicStmt().flow.fMinus) {
			for (final Parameter fromp : fl.getFrom()) {
				final Set<SDGNode> nf = match.getFineReachIN(fromp);
				from.addAll(nf);
			}
		}

		final SummarySlicerForward ssf = new SummarySlicerForward(sdg);
		ssf.addToOmit(SDGEdge.Kind.SUMMARY); // use only SUMMARY_HEAP
		final Collection<SDGNode> slice = ssf.slice(from);

		final ProgramSourcePositions pspos = new ProgramSourcePositions();
		for (final SDGNode s : slice) {
			if (s.getSource() != null) {
				pspos.addSourcePosition(s.getSource(), s.getSr(), s.getEr(), s.getSc(), s.getEc());
			}
		}

		return pspos;
	}

	public static boolean hasIllegalFlow(final ExplicitFlowStmt fl, final Matcher match,
			final SummarySlicerBackward ssb, final AliasSDG sdg) {
		final Set<SDGNode> from = new HashSet<SDGNode>();
		for (final Parameter fp : fl.getFrom()) {
			final Set<SDGNode> nf = match.getFineReachIN(fp);
			if (nf.isEmpty()) {
				final Set<SDGNode> fineIn = match.getFineReachOUT(fp);
				from.addAll(fineIn);
			} else {
				from.addAll(nf);
			}
		}

		adjustSetWithPotentialAliases(sdg, ParamKind.IN, from);

		final Set<SDGNode> to = new HashSet<SDGNode>();
		for (final Parameter tp : fl.getTo()) {
			final Set<SDGNode> nt = match.getFineReachOUT(tp);
			if (nt.isEmpty()) {
				final Set<SDGNode> fineIn = match.getFineReachIN(tp);
				to.addAll(fineIn);
			} else {
				to.addAll(nt);
			}
		}

		adjustSetWithPotentialAliases(sdg, ParamKind.OUT, to);

//		cfc.out.println("edu.kit.joana.deprecated.jsdg.slicing: " + to);
//		cfc.out.println("input: " + from);
		final Collection<SDGNode> slice = ssb.slice(to);
		boolean illegalFlowFound = false;
		for (final SDGNode fn : from) {
			if (slice.contains(fn)) {
				illegalFlowFound = true;
				break;
			}
		}

		return illegalFlowFound;
	}

	private enum ParamKind { IN, OUT }

	private static void adjustSetWithPotentialAliases(final AliasSDG alias, final ParamKind kind,
			final Set<SDGNode> toAdjust) {
		if (toAdjust.isEmpty()) {
			return;
		}
		// add potential aliases (non-ruled out ones) to outgoing and incoming nodes nodes
		// as long as not one of their fields are already in the from or to set:
		// add n to set S iff: n not in roots(S) but exists n' in roots(S) with n potential alias of n'
		// and alias(n, n') not forbidden by context configuration
		final SDG sdg = alias.getSDG();
		final Set<SDGNode> roots = new HashSet<SDGNode>();
		for (final SDGNode n : toAdjust) {
			final SDGNode root = findParamRoot(sdg, n);
			if (root != null) {
				roots.add(root);
			}
		}

		for (final SDGNode root : roots) {
			final TIntSet potentialAliases = root.getAliasDataSources();
			final int rootId = root.getId();
			if (potentialAliases != null) {
				for (final TIntIterator it = potentialAliases.iterator(); it.hasNext();) {
					final int id = it.next();
					if (!alias.isNoAlias(rootId, id)) {
						final SDGNode other = sdg.getNode(id);
						if (!roots.contains(other)) {
							addSubFieldsToSet(sdg, kind, other, toAdjust);
						}
					}
				}
			}
		}
	}

	private static void addSubFieldsToSet(final SDG sdg, final ParamKind kind, final SDGNode n,
			final Set<SDGNode> set) {
		final LinkedList<SDGNode> work = new LinkedList<SDGNode>();
		final Set<SDGNode> visited = new HashSet<SDGNode>();
		work.add(n);
		while (!work.isEmpty()) {
			final SDGNode cur = work.removeFirst();
			visited.add(cur);

			if ((kind == ParamKind.IN && isInput(cur)) || (kind == ParamKind.OUT && !isInput(cur))) {
				set.add(cur);
			}

			for (final SDGEdge out : sdg.outgoingEdgesOf(cur)) {
				if (out.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
					final SDGNode next = out.getTarget();
					if (!visited.contains(next)) {
						work.add(next);
					}
				}
			}
		}
	}

	private static boolean isInput(final SDGNode n) {
		return n.kind == SDGNode.Kind.FORMAL_IN || n.kind == SDGNode.Kind.ACTUAL_IN;
	}

	private static SDGNode findParamRoot(final SDG sdg, final SDGNode n) {
		if (isRootParam(n)) {
			return n;
		}

		final Set<SDGNode> visited = new HashSet<SDGNode>();
		final LinkedList<SDGNode> work = new LinkedList<SDGNode>();
		work.add(n);

		// we assume non-recursive tree structured nodes here. So we dont need to keep track of already visited nodes
		// or multiple parent nodes.
		while (!work.isEmpty()) {
			final SDGNode root = work.removeFirst();

			for (final SDGEdge e : sdg.incomingEdgesOf(root)) {
				if (e.getKind() == SDGEdge.Kind.PARAMETER_STRUCTURE) {
					final SDGNode next = e.getSource();
					if (isRootParam(next)) {
						return next;
					}

					if (!visited.contains(next)) {
						work.add(next);
					}
				}
			}
		}

		throw new IllegalStateException("No root node found for " + n.getId() + "|" + n.getKind() + "|" + n.getLabel());
	}

	private static boolean isRootParam(final SDGNode n) {
		final int bc = n.getBytecodeIndex();
		return bc == BytecodeLocation.ROOT_PARAMETER || bc == BytecodeLocation.STATIC_FIELD;
	}

	public static void inferValidAliasConfigurations(final CheckFlowConfig cfc, final AliasSDG alias, final BasicIFCStmt ifc,
			final Matcher match, final MethodInfo mInfo, final FlowStmtResult stmtResult,
			final ExceptionAnalysis excCfg,	final IProgressMonitor progress) throws CancelException {
		cfc.out.println("infering valid alias configurations... ");
		final TIntSet formals = findNonPrimitiveMethodInputParams(alias.getSDG(), alias.getSDG().getRoot());
		final TIntObjectMap<TIntSet> potentialAliases = buildPotentialAliasesMap(alias.getSDG(), formals);

		int total = 0;
		int checked = 0;
		final SortedSet<Permutations> working = new TreeSet<Permutations>();
		final Permutations current = new Permutations(formals);
		Permutations noChange = null;
		for (boolean thereIsMore = true; thereIsMore;) {
			thereIsMore = current.hasNext();
			total++;
			MonitorUtil.throwExceptionIfCanceled(progress);

			boolean isSubSetOfWorking = false;
			for (final Permutations works : working) {
				if (works.isSubSet(current)) {
					isSubSetOfWorking = true;
					break;
				}
			}

			if (!isSubSetOfWorking && !(noChange != null && current.containsAny(noChange))
					&& !current.containsImpossibleAliases(potentialAliases)) {
				// check flow for current permutation
//				cfc.out.print("#" + total + ":" + current.getParameterAliases(nf));

				checked++;
				if (checkPermutationFlow(current, alias, ifc, match, progress)) {
					// flow ok with this permutation
					final Permutations copy = current.clone();
					working.add(copy);
				}

				if (current.isNoChange()) {
//					if (noChange == null) {
//						noChange = current.clone();
//					} else {
//						noChange.addAll(current);
//					}
				}
			}

			current.next();
		}

		if (working.isEmpty()) {
			cfc.out.println("no valid alias configurations found for " + ifc + ". checked " + checked
					+ " of " + total + " total combinations.");
			final FlowStmtResultPart noValid = new FlowStmtResultPart(ifc,
					"no valid alias configuration could be inferred", false, true, excCfg, alias.getFileName());
			noValid.setAlias(alias.getNoAlias());
			stmtResult.addPart(noValid);
		} else {
			cfc.out.print("valid alias configurations for " + ifc + ":");
			final NameFinder name = new NameFinder(alias.getSDG(), mInfo);
			for (final Permutations valid : working) {
				final String palias = valid.getParameterAliases(name);
				cfc.out.print(" " + palias);
				final FlowStmtResultPart validInfered = new FlowStmtResultPart(ifc, palias, true, true, excCfg,
						alias.getFileName());

				alias.reset();
				valid.adjustAlias(alias);
				validInfered.setAlias(alias.getNoAlias());
				stmtResult.addPart(validInfered);
			}
			cfc.out.println(" done." + " Checked " + checked + " of " + total + " total combinations.");
		}
	}

	private static boolean checkPermutationFlow(final Permutations perm, final AliasSDG alias, final BasicIFCStmt ifc,
			final Matcher match, final IProgressMonitor progress) throws CancelException {
		alias.reset();
		perm.resetNoChange();
		perm.adjustAlias(alias);
		if (alias.adjustMaxSDG(progress) > 0) {
			alias.recomputeSummary(progress);
		} else {
			perm.markAsNoChange();
		}

		//cfc.out.println("checking " + perm.getParameterAliases());

		final SummarySlicerBackward ssb = new SummarySlicerBackward(alias.getSDG());
		ssb.addToOmit(SDGEdge.Kind.SUMMARY); // use only SUMMARY_HEAP
		boolean isFlowIllegal = false;

		for (final ExplicitFlowStmt fl : ifc.flow.fMinus) {
			isFlowIllegal |= hasIllegalFlow(fl, match, ssb, alias);
			if (isFlowIllegal) {
				break;
			}
		}

		return !isFlowIllegal;
	}

	private static class NameFinder {
		private final SDG sdg;
		private final MethodInfo mInfo;

		private NameFinder(final SDG sdg, final MethodInfo mInfo) {
			this.sdg = sdg;
			this.mInfo = mInfo;
		}

		public String findParamName(final int id) {
			final SDGNode n = sdg.getNode(id);

			final String pNumStr = n.getBytecodeName().substring(BytecodeLocation.ROOT_PARAM_PREFIX.length());
			final int pNum = Integer.parseInt(pNumStr);
			final ParamNum num = ParamNum.fromParamNum(mInfo.isStatic(), pNum);
			final ParamInfo name = mInfo.getParameters().get(num.getIMethodNum());

			return name.name;
		}
	}

	private static class Permutations implements Comparable<Permutations>, Cloneable {
		private final int[] params;
		private final BitVector bv;
		/** number of possible permutations for a single alias (p1,p1), (p1,p2), ...
		 *  note that (p1, p2) == (p2, p1) */
		private final int numOfPermutations;
		private boolean noChange = false;

		private Permutations(final int[] params) {
			this.params = params;
			// computes the number of distinguishable alias pairs. With n params these
			// are n + n-1 + n-2 + ... + n - (n-1) == (n*(n+1))/2
			// e.g. params = {a, b, c} => n = 3
			// valid pairs: aa, ab, ac, bb, bc, cc => #valid = 6 == (3 * 4)/2
			// each of these pairs is represented by a bit in the bitvector bv.
			// So the bitvector for the alias configuration { ab, bb, bc } is 010110.
			//
			// aa ab ac bb bc cc
			// 0  1  0  1  1  0
			this.numOfPermutations = ((params.length * (params.length + 1)) / 2);
			this.bv = new BitVector(numOfPermutations);
		}

		private Permutations(final TIntSet params) {
			this(params.toArray());
		}

		public boolean hasNext() {
			// when all bits are set, all combinations have been tried.
			return bv.populationCount() != numOfPermutations;
		}

		public void next() {
			// add 1 to the current bit vector
			for (int pos = 0; pos < numOfPermutations; pos++) {
				if (!bv.get(pos)) {
					bv.set(pos);
					break;
				} else {
					bv.clear(pos);
				}
			}
		}

		public void markAsNoChange() {
			noChange = true;
		}

		public boolean isNoChange() {
			return noChange;
		}

		public void resetNoChange() {
			noChange = false;
		}

		public boolean containsAny(final Permutations other) {
			return !this.bv.intersectionEmpty(other.bv);
		}

		public boolean isSubSet(final Permutations other) {
			return this.bv.isSubset(other.bv);
		}

		public void addAll(final Permutations other) {
			this.bv.or(other.bv);
		}

		public Permutations clone() {
			final Permutations clone = new Permutations(params);
			clone.bv.or(bv);

			return clone;
		}

		public void adjustAlias(final AliasSDG alias) {
			int pos = 0;
			for (int p1 = 0; p1 < params.length; p1++) {
				for (int p2 = p1; p2 < params.length; p2++) {
					if (bv.get(pos)) {
						final int node1Id = params[p1];
						final int node2Id = params[p2];

						alias.setNoAlias(node1Id, true, node2Id, true);
//						cfc.out.print(" (" + node1Id + "," + node2Id + ")");
					}

					pos++;
				}
			}
		}

		@SuppressWarnings("unused")
		public String getParameterAliases() {
			final StringBuilder sb = new StringBuilder("[");
			int pos = 0;
			for (int p1 = 0; p1 < params.length; p1++) {
				for (int p2 = p1; p2 < params.length; p2++) {
					if (bv.get(pos)) {
						final int node1Id = params[p1];
						final int node2Id = params[p2];

						sb.append("(" + node1Id + "," + node2Id + ")");
					}

					pos++;
				}
			}

			sb.append("]");

			return sb.toString();
		}

		public String getParameterAliases(final NameFinder name) {
			final StringBuilder sb = new StringBuilder();
			int pos = 0;
			boolean isFirst = true;

			for (int p1 = 0; p1 < params.length; p1++) {
				for (int p2 = p1; p2 < params.length; p2++) {
					if (bv.get(pos)) {
						final int node1Id = params[p1];
						final int node2Id = params[p2];

						if (isFirst) {
							isFirst = false;
						} else {
							sb.append(" & ");
						}

						sb.append("!{" + name.findParamName(node1Id) + ", " + name.findParamName(node2Id) + "}");
					}

					pos++;
				}
			}

			return sb.toString();
		}

		public String toString() {
			return bv.toString();
		}

		@Override
		public int compareTo(final Permutations o) {
			if (this.bv.equals(o.bv)) {
				return 0;
			} else if (this.bv.isSubset(o.bv)) {
				// o > this
				return -1;
			} else if (o.bv.isSubset(this.bv)) {
				// o < this
				return 1;
			} else {
				// more non-alias configurations are considered bigger then less
				final int pop = this.bv.populationCount() - o.bv.populationCount();
				if (pop != 0) {
					return pop;
				} else {
					// fall back to get total order: bitstring interpreted as binary number.
					final int thisMax = this.bv.max();
					final int otherMax = o.bv.max();
					if (thisMax == otherMax) {
						for (int i = thisMax; i >= 0; i--) {
							final boolean thisI = this.bv.get(i);
							final boolean otherI = o.bv.get(i);

							if (thisI && !otherI) {
								// this > other
								return 1;
							} else if (!thisI && otherI) {
								// this < other
								return -1;
							}
						}

						// should not happen, as equals should have returned true before.
						return 0;
					} else {
						return thisMax - otherMax;
					}
				}
			}
		}

		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof Permutations) {
				final Permutations other = (Permutations) obj;
				return this.bv.equals(other.bv);
			}

			return false;
		}

		public boolean containsImpossibleAliases(final TIntObjectMap<TIntSet> possible) {
			int pos = 0;

			for (int p1 = 0; p1 < params.length; p1++) {
				for (int p2 = p1; p2 < params.length; p2++) {
					if (bv.get(pos)) {
						final int node1Id = params[p1];
						final int node2Id = params[p2];

						final TIntSet allowed = possible.get(node1Id);
						if (allowed == null || !allowed.contains(node2Id)) {
							// System.err.println("Ruled out impossible alias: (" + node1Id + "," + node2Id +")");
							return true;
						}
					}

					pos++;
				}
			}

			return false;
		}

	}

	@SuppressWarnings("unused")
	private static TIntObjectMap<TIntSet> findAllMethodInputParams(final SDG sdg) {
		final TIntObjectMap<TIntSet> params = new TIntObjectHashMap<TIntSet>();

		for (final SDGNode n : sdg.vertexSet()) {
			if (n.kind == SDGNode.Kind.ENTRY) {
				final TIntSet formals = findNonPrimitiveMethodInputParams(sdg, n);
				params.put(n.getId(), formals);
			}
		}

		return params;
	}

	private static TIntObjectMap<TIntSet> buildPotentialAliasesMap(final SDG sdg, final TIntSet nodes) {
		final TIntObjectMap<TIntSet> pots = new TIntObjectHashMap<TIntSet>();

		for (final TIntIterator it = nodes.iterator(); it.hasNext();) {
			final int id = it.next();
			final SDGNode n = sdg.getNode(id);
			final TIntSet set = n.getAliasDataSources();
			if (set != null) {
				pots.put(id, set);
			}
		}

		return pots;
	}

	private static TIntSet findNonPrimitiveMethodInputParams(final SDG sdg, final SDGNode entry) {
		final TIntSet formals = new TIntHashSet();
		for (final SDGNode fIn : sdg.getFormalInsOfProcedure(entry)) {
			if (fIn.getBytecodeIndex() == BytecodeLocation.ROOT_PARAMETER ||
					fIn.getBytecodeIndex() == BytecodeLocation.STATIC_FIELD) {
				if (!isPrimitive(fIn)) {
					formals.add(fIn.getId());
				}
			}
		}

		return formals;
	}

	private static boolean isPrimitive(final SDGNode n) {
		// primitive bytecode types are all of length 1. All object types are at least of size 3, because the class
		// name is surrounded with 'L' and ';': L<class>;
		return n.getType().length() == 1;

	}

	private SDGResult create(PrintStream out, AnalysisScope scope, MoJo mojo, CallGraphResult cg, IMethod im,
			String outDir, Config cfg) throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		return create(out, scope, mojo, cg, im, outDir, cfg, cfg.exceptions);
	}

	private SDGResult create(PrintStream out, AnalysisScope scope, MoJo mojo, CallGraphResult cg, IMethod im,
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
		scfg.debugAccessPathOutputDir = outDir;//"out/";
		scfg.computeInterference = false;
		scfg.staticInitializers = StaticInitializationTreatment.NONE;
		scfg.debugStaticInitializers = false;
		scfg.fieldPropagation = cfg.fieldPropagation;
		scfg.debugManyGraphsDotOutput = cfg.debugManyGraphsDotOutput;

		final SDGBuilder sdg = SDGBuilder.create(scfg, cg.cg, cg.pts);
		final APResult ap = sdg.getAPResult();

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

		return new SDGResult(joanaSDG, ap);
	}
	
	private static class SDGResult {
		public final SDG sdg;
		public final APResult ap;
		
		public SDGResult(final SDG sdg, final APResult ap) {
			this.sdg = sdg;
			this.ap = ap;
		}
	}
	
	/**
	 * Search file in filesystem. If not found, try to load from classloader (e.g. from inside the jarfile).
	 */
	private static Module findJarModule(final String path) throws IOException {
		final File f = new File(path);
		if (f.exists()) {
			return new JarFileModule(new JarFile(f));
		} else {
			final URL url = CheckFlowLessWithAlias.class.getClassLoader().getResource(path);
			if (url != null) {
				final URLConnection con = url.openConnection();
				final InputStream in = con.getInputStream();
				return new JarStreamModule(new JarInputStream(in));
			} else {
				// special fall-back for eclipse plug-in
				final URL url2 = new URL("platform:/plugin/joana.contrib.lib/stubs" + path);
				final InputStream in = url2.openConnection().getInputStream();
				return new JarStreamModule(new JarInputStream(in));
			}
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
