/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn.benchmark;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.demandpa.alg.DemandRefinementPointsTo;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.pruned.CallGraphPruning;
import com.ibm.wala.ipa.callgraph.pruned.PrunedCallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.FileUtil;

import de.wwu.sdpn.wala.dpngen.symbols.StackSymbol;
import de.wwu.sdpn.wala.util.ThreadSensContextSelector;
import edu.kit.joana.deprecated.jsdg.Analyzer;
import edu.kit.joana.deprecated.jsdg.Messages;
import edu.kit.joana.deprecated.jsdg.SDGFactory;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.output.JoanaCFGSanitizer;
import edu.kit.joana.deprecated.jsdg.output.JoanaStyleSDG;
import edu.kit.joana.deprecated.jsdg.sdg.PDG;
import edu.kit.joana.deprecated.jsdg.sdg.SDG;
import edu.kit.joana.deprecated.jsdg.sdg.dataflow.SummaryEdgeComputation;
import edu.kit.joana.deprecated.jsdg.sdg.interference.CSDGPreprocessor;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.IPointerAnalysis;
import edu.kit.joana.deprecated.jsdg.sdg.pointsto.PointsToWrapper;
import edu.kit.joana.deprecated.jsdg.util.Debug;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.deprecated.jsdg.util.Util;
import edu.kit.joana.deprecated.jsdg.wala.objecttree.InstanceAndPointerKeyFactoryAdapter;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegions;
import edu.kit.joana.ifc.wala.sdpn.LockAwareThreadRegions;
import edu.kit.joana.wala.util.VerboseProgressMonitor;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class BenchmarkCSDGwithSDPNBuilder {

	private static final String JRE14_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-jre1.4.jar";
	private static final String J2ME_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-j2me2.0.jar";
	private static final String JAVACARD_LIB = "Primordial,Java,jarFile,../jSDG/lib/jSDG-stubs-javacard.jar";

	private static final boolean DO_CACHE = false;
	private static final boolean SKIP_PRIMORDIAL = true;

	private static final boolean INTERPRET_KILL = true;
	private static final boolean UNSAFE_KILL = false;
	private static final boolean IGNORE_WAIT = false;
	private static final boolean NO_EXCEPTIONS = false;
	private static final boolean ITERABLE_ANALYSIS = true;
	private static final boolean THREAD_CONTEXTS = true;

	private static final long XSB_TIMEOUT = 1000 * 60 * 2;

	private static final long TIMEOUT = 1000 * 60 * 30;

	public static void main(String args[]) throws IllegalArgumentException,
			CancelException, PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
		Setting s = Setting.apply("bin", "Lexamples/BSP03", JRE14_LIB,
				DO_CACHE, SKIP_PRIMORDIAL, INTERPRET_KILL, UNSAFE_KILL,
				IGNORE_WAIT, NO_EXCEPTIONS, ITERABLE_ANALYSIS, THREAD_CONTEXTS,
				XSB_TIMEOUT, TIMEOUT);

		AppResult r = new AppResult(s);
		runAnalysis(r);
		System.out.println(r.makeString(true));

		System.exit(0); // xsb listener thread needs to be destroyed.
	}

	public static void runAnalysis(final AppResult appResult)
			throws IllegalArgumentException, CancelException,
			PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
		final Setting setting = appResult.setting();
		final boolean do_cache = setting.do_cache();
		final boolean skip_primordial = setting.skip_primordial();
		final boolean interpret_kill = setting.interpret_kill();
		final boolean ignore_wait = setting.ignore_wait();
		final boolean unsafe_kill = setting.unsafe_kill();
		final boolean no_exceptions = setting.no_exceptions();
		final boolean iterable_analysis = setting.iterable_analysis();
		final boolean thread_contexts = setting.thread_contexts();
		final long xsb_timeout = setting.xsb_timeout();
		final long timeout = setting.timeout();

		long start = System.currentTimeMillis();

		final String main = setting.main();

		final String mainClassSimpleName = main.replace('/', '.')
				.replace('$', '.').substring(1);

		SDGFactory.Config cfg = setting.jsdgConf();

		Analyzer.cfg = cfg;

		IProgressMonitor progress = new TimeoutProgressMonitor(timeout,
				new VerboseProgressMonitor(System.out));

		final Result result = getJoanaSDG(cfg, thread_contexts, progress);

		{
			progress.beginTask("Saving SDG to " + cfg.outputSDGfile, -1);
			BufferedOutputStream bOut = new BufferedOutputStream(
					new FileOutputStream(cfg.outputSDGfile));
			SDGSerializer.toPDGFormat(result.sdg, bOut);
			progress.done();
		}

		Util.dumpCallGraph(result.cg, mainClassSimpleName, null);

		List<SDGEdge> toRemove = new LinkedList<SDGEdge>();
		int ifEdges = 0;

		for (SDGEdge e : result.sdg.edgeSet()) {
			if ((e.getKind() == SDGEdge.Kind.INTERFERENCE /*
														 * || e.getKind() ==
														 * SDGEdge
														 * .Kind.INTERFERENCE_WRITE
														 */)
					&& (e.getSource().kind != SDGNode.Kind.SYNCHRONIZATION && e
							.getTarget().kind != SDGNode.Kind.SYNCHRONIZATION)) {
				// ignore interference edges between syncs
				ifEdges++;
			}
		}

		System.out.println("Found " + ifEdges + " potential interferences.");

		// final MHPAnalysis mhp = result.mhp;
		final ThreadRegions tr = LockAwareThreadRegions.compute(result.sdg);
		// result.mhp.getTR();
		System.out.println("Lock sensitive thread regions: " + tr.size()
				+ " - normal regions: " + result.mhp.getThreadRegions().size());
		// TODO caching does not work, because our thread regions are not fine
		// grained enough - a synchronized/monitorenter
		// does not result in a new region.
		// entries are true iff the two thread regions in question may never run
		// in parallel
		// so iff all pairs of thread regions of two nodes are true, the dpn
		// analysis can be skipped
		// and the interference deleted.
		boolean[][] nopar = null;
		boolean[][] surepar = null;
		if (do_cache) {
			nopar = new boolean[tr.size()][tr.size()];
			surepar = new boolean[tr.size()][tr.size()];
		}

		int cached = 0;
		int current = 0;

		long startSDPN = System.currentTimeMillis();
		long timeJSDG = startSDPN - start;

		appResult.timeJSDG_$eq(timeJSDG);
		appResult.numberExpected_$eq(ifEdges);

		final DPN4IFCAnalysis dpn = new DPN4IFCAnalysis(result.cg, result.pts,
				unsafe_kill, ignore_wait, no_exceptions, iterable_analysis,
				xsb_timeout);
		dpn.init(progress);

		for (SDGEdge e : result.sdg.edgeSet()) {
			if (progress.isCanceled())
				throw CancelException.make("Canceled while checking interference edges.");
			if ((e.getKind() == SDGEdge.Kind.INTERFERENCE /*
														 * || e.getKind() ==
														 * SDGEdge
														 * .Kind.INTERFERENCE_WRITE
														 */)
					&& (e.getSource().kind != SDGNode.Kind.SYNCHRONIZATION && e
							.getTarget().kind != SDGNode.Kind.SYNCHRONIZATION)) {


				if (do_cache) {
					if (isSurePar(tr, surepar, e)) {
						cached++;
						// we know for sure that the two instructions may happen
						// in parallel - no need to run dpn
						System.out.print(++current + " of " + ifEdges + ": ");
						System.out.println("[Cached] Edge definitely exists: "
								+ e.getSource().getLabel() + " -> "
								+ e.getTarget().getLabel());
						continue;
					} else if (isNoPar(tr, nopar, e)) {
						cached++;
						// we know for sure that the two instructions may NEVER
						// happen in parallel - no need to run dpn
						// remove the interference edge
						System.out.print(++current + " of " + ifEdges + ": ");
						System.out.println("[Cached] Edge cannot exist: "
								+ e.getSource().getLabel() + " -> "
								+ e.getTarget().getLabel());
						toRemove.add(e);
						continue;
					}
				}

				final SDGNode from = result.sdg.getEntry(e.getSource());
				final SDGNode to = result.sdg.getEntry(e.getTarget());
				final CGNode nodeFrom = result.cg.getNode(from.tmp); // get
																		// cgnode
																		// for
																		// corresponding
																		// method
																		// of
																		// edge
				final CGNode nodeTo = result.cg.getNode(to.tmp); // get cgnode
																	// for
																	// corresponding
																	// method of
																	// edge
				final SDGNode source = e.getSource();
				final SDGNode target = e.getTarget();
				final int indexFrom = findCorrespondingInstructionIndex(
						nodeFrom, source); // get instruction index for from
											// node
				final int indexTo = findCorrespondingInstructionIndex(nodeTo,
						target); // get instruction index for to node

				//debug
//				if(e.getSource().getBytecodeIndex() != -2 || e.getTarget().getBytecodeIndex() != -2)
//					continue;
//				if (skip_primordial
//						&& (nodeFrom.getMethod().getDeclaringClass()
//								.getClassLoader().getReference()
//								.equals(ClassLoaderReference.Primordial) || nodeTo
//								.getMethod().getDeclaringClass()
//								.getClassLoader().getReference()
//								.equals(ClassLoaderReference.Primordial)))
//					continue;
//				de.wwu.sdpn.wala.runner.PDFCFG.ghostviewFullIR(nodeFrom.getIR(), "/tmp/run.ir.dot", "/tmp/run.ir.pdf");
				//end debug
				System.out.print(++current + " of " + ifEdges + ": ");
				{
					boolean skip = false;
					final SSAInstruction[] irFrom = nodeFrom.getIR()
							.getInstructions();
					if (indexFrom < 0 || irFrom == null
							|| irFrom.length < indexFrom
							|| irFrom[indexFrom] == null) {
						System.out.println("Illegal index from-node "
								+ indexFrom + "@" + nodeFrom.getMethod() + ": "
								+ source.getBytecodeMethod() + " - "
								+ source.getKind() + "|" + source.getLabel());
						Util.dumpSSA(nodeFrom.getIR(), System.out);
						skip = true;
					}

					final SSAInstruction[] irTo = nodeTo.getIR()
							.getInstructions();
					if (indexTo < 0 || irTo == null || irTo.length < indexTo
							|| irTo[indexTo] == null) {
						System.out.println("Illegal index to-node " + indexTo
								+ "@" + nodeTo.getMethod() + ": "
								+ target.getBytecodeMethod() + " - "
								+ target.getKind() + "|" + target.getLabel());
						Util.dumpSSA(nodeTo.getIR(), System.out);
						skip = true;
					}

					if (skip)
						continue;
				}

				if (skip_primordial
						&& (nodeFrom.getMethod().getDeclaringClass()
								.getClassLoader().getReference()
								.equals(ClassLoaderReference.Primordial) || nodeTo
								.getMethod().getDeclaringClass()
								.getClassLoader().getReference()
								.equals(ClassLoaderReference.Primordial))) {
					System.out.println("Skipping interference from "
							+ indexFrom + "@" + nodeFrom.getMethod() + " to "
							+ indexTo + "@" + nodeTo.getMethod());
				} else {
					System.out.println("Checking interference from "
							+ indexFrom + "@" + nodeFrom.getMethod() + " to "
							+ indexTo + "@" + nodeTo.getMethod());
					System.out.println("\t" + edge2str(result.sdg, e));

					IFResult ifr = new IFResult(appResult, indexFrom + "@"
							+ nodeFrom.getMethod(), indexTo + "@"
							+ nodeTo.getMethod(), edge2str(result.sdg, e));

					try {
						if (interpret_kill) {
							long startDPN = System.currentTimeMillis();
							boolean possible = dpn.mayFlowFromTo(nodeFrom,
									indexFrom, nodeTo, indexTo, ifr,
									edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor.INSTANCE);
							long time = System.currentTimeMillis() - startDPN;
							ifr.time_$eq(time);
							if (!possible) {
								System.out
										.println("Removing interference from "
												+ indexFrom + "@"
												+ nodeFrom.getMethod() + " to "
												+ indexTo + "@"
												+ nodeTo.getMethod());
								System.out.println("\t"
										+ edge2str(result.sdg, e));
								toRemove.add(e);
								if (do_cache) {
									setNoPar(tr, nopar, e);
								}
							} else {
								if (do_cache) {
									setSurePar(tr, surepar, e);
								}
							}
						} else {
							final StackSymbol symFrom = dpn.getSS4NodeAndIndex(
									nodeFrom, indexFrom);
							final StackSymbol symTo = dpn.getSS4NodeAndIndex(
									nodeTo, indexTo);

							long startDPN = System.currentTimeMillis();
							boolean possible = dpn.mayHappenSuccessively(
									symFrom, symTo, ifr,
									edu.kit.joana.deprecated.jsdg.wala.NullProgressMonitor.INSTANCE);
							ifr.overwrite_$eq(false);
							long time = System.currentTimeMillis() - startDPN;
							ifr.time_$eq(time);

							if (!possible) {
								System.out
										.println("Removing interference from "
												+ indexFrom + "@"
												+ nodeFrom.getMethod() + " to "
												+ indexTo + "@"
												+ nodeTo.getMethod());
								System.out.println("\t"
										+ edge2str(result.sdg, e));
								toRemove.add(e);
								if (do_cache) {
									setNoPar(tr, nopar, e);
								}
							} else {
								if (do_cache) {
									setSurePar(tr, surepar, e);
								}
							}
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						ifr.setError(ex.toString(),scala.Predef.exceptionWrapper(ex).getStackTraceString());
					}
				}
			}

		}

		result.sdg.removeAllEdges(toRemove);

		{
			progress.beginTask("Saving sdpn-optimized SDG to "
					+ cfg.outputSDGfile + "-sdpn", -1);
			BufferedOutputStream bOut = new BufferedOutputStream(
					new FileOutputStream(cfg.outputSDGfile + "-sdpn"));
			SDGSerializer.toPDGFormat(result.sdg, bOut);
			progress.done();
		}

		for (final SDGEdge e : toRemove) {
			System.out.println("REMOVED: " + edge2str(result.sdg, e));
		}

		System.out.println("Removed " + toRemove.size() + " of " + ifEdges
				+ " interference edges. Cached " + cached + " runs.");
	}

	private static int findCorrespondingInstructionIndex(CGNode method,
			SDGNode n) {
		/*
		 * For normal nodes the instruction index is stored in n.tmp. For entry
		 * nodes n.tmp contains the id of the CGNode for the corresponding
		 * method. When we try to find a matching instruction index for a entry
		 * node, we have to search for the first non-null instruction. Which may
		 * not always be on pos 0.
		 */

		// -1 == no instructions here -> we have to ignore it -> no dpn analysis
		// for this edge...
		int iindex = -1;

		if (n.kind == SDGNode.Kind.ENTRY) {
			final SSAInstruction[] ir = method.getIR().getInstructions();
			if (ir != null) {
				for (int i = 0; i < ir.length; i++) {
					if (ir[i] != null) {
						iindex = i;
						break;
					}
				}
			}
		} else if (n.kind == SDGNode.Kind.EXIT) {
			final SSAInstruction[] ir = method.getIR().getInstructions();
			if (ir != null) {
				iindex = ir.length - 1;
			}
		} else {
			iindex = n.tmp;
		}

		return iindex;
	}

	private static String edge2str(edu.kit.joana.ifc.sdg.graph.SDG sdg, SDGEdge e) {
		final SDGNode from = e.getSource();
		final SDGNode to = e.getTarget();
		final SDGNode eFrom = sdg.getEntry(e.getSource());
		final SDGNode eTo = sdg.getEntry(e.getTarget());

		return eFrom.getLabel() + "{" + from.getLabel() + " @"
				+ from.getBytecodeIndex() + "} -|" + e.getKind() + "|-> "
				+ eTo.getLabel() + "{" + to.getLabel() + " @"
				+ to.getBytecodeIndex() + "}";
	}

	private static int[] getThreadRegions(final ThreadRegions tr,
			final SDGNode n) {
		final int[] tr1 = new int[n.getThreadNumbers().length];
		int pos = 0;

		for (int i : n.getThreadNumbers()) {
			tr1[pos] = tr.getThreadRegion(n, i).getID();
			pos++;
		}

		return tr1;
	}

	private static boolean isSurePar(ThreadRegions tr, boolean[][] sure,
			SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		for (int i1 = 0; i1 < tr1.length; i1++) {
			for (int i2 = 0; i2 < tr2.length; i2++) {
				if (sure[i1][i2]) {
					return true;
				}
			}
		}

		return false;
	}

	private static boolean isNoPar(ThreadRegions tr, boolean[][] nopar,
			SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		for (int i1 = 0; i1 < tr1.length; i1++) {
			for (int i2 = 0; i2 < tr2.length; i2++) {
				// all entries have to be true
				if (!nopar[i1][i2]) {
					return false;
				}
			}
		}

		return true;
	}

	private static void setSurePar(ThreadRegions tr, boolean[][] sure, SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		if (tr1.length == 1 && tr2.length == 1) {
			sure[tr1[0]][tr2[0]] = true;
			sure[tr2[0]][tr1[0]] = true;
		}
	}

	private static void setNoPar(ThreadRegions tr, boolean[][] nopar, SDGEdge e) {
		final SDGNode n1 = e.getSource();
		final SDGNode n2 = e.getTarget();
		final int[] tr1 = getThreadRegions(tr, n1);
		final int[] tr2 = getThreadRegions(tr, n2);

		for (int i1 = 0; i1 < tr1.length; i1++) {
			for (int i2 = 0; i2 < tr2.length; i2++) {
				// all entries have to be true
				nopar[i1][i2] = true;
				nopar[i2][i1] = true;
			}
		}
	}

	private static class Result {
		public final edu.kit.joana.ifc.sdg.graph.SDG sdg;
		public final SDG jsdg;
		public final CallGraph cg;
		public final PointerAnalysis pts;
		public final MHPAnalysis mhp;

		private Result(final edu.kit.joana.ifc.sdg.graph.SDG sdg, final SDG jsdg,
				final CallGraph cg, final PointerAnalysis pts,
				final MHPAnalysis mhp) {
			this.sdg = sdg;
			this.jsdg = jsdg;
			this.cg = cg;
			this.pts = pts;
			this.mhp = mhp;
		}

	}

	public static final Result getJoanaSDG(Config cfg, boolean thread_contexts,
			final IProgressMonitor progress) throws IllegalArgumentException,
			CancelException, PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
		final Date start = initSDGcomputation(cfg);


		final Result jresult = getOrigSDG(cfg, thread_contexts, progress);

		edu.kit.joana.ifc.sdg.graph.SDG joanaSdg = JoanaStyleSDG.createJoanaSDG(jresult.jsdg,
				cfg.addControlFlow, cfg.nonTermination, cfg.useSummaryOpt,
				progress);

		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		// as long as we can not cope with nodes that do not belong to the
		// control flow we do this...
		JoanaCFGSanitizer.sanitizeCFG(joanaSdg);

		// assert assertVerify(joanaSdg, !cfg.useWalaSdg, cfg.addControlFlow);

		final Date beforeThreadAllocation = new Date();

		final MHPAnalysis mhp;

		if (cfg.computeInterference) {
			progress.beginTask("Creating cSDG from SDG " + cfg.outputSDGfile,
					-1);

			progress.subTask("Running Thread Allocation Analysis");
			Log.info("Running Thread Allocation Analysis");

			mhp = CSDGPreprocessor.runMHP(joanaSdg, progress);

			Log.info("Thread Allocation done.");
			progress.done();
		} else {
			mhp = null;
		}

		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		final Date beforeSummaryEdge = new Date();

		if (cfg.computeSummaryEdges) {
			progress.subTask("Compute Summary Edges");
			Log.info("Compute Summary Edges");
			SummaryEdgeComputation.compute(joanaSdg, progress);

			Log.info("Summary Edges done.");
			progress.done();
		}

		if (progress.isCanceled()) {
			throw CancelException.make("Operation aborted.");
		}

		final Date end = new Date();

		long start2end = end.getTime() - start.getTime();
		long summary2end = end.getTime() - beforeSummaryEdge.getTime();
		long start2thread = beforeThreadAllocation.getTime() - start.getTime();
		long threadAlloc = beforeSummaryEdge.getTime()
				- beforeThreadAllocation.getTime();

		Log.info("Start 2 End: " + start2end / 1000 + "s (" + start2end + "ms)");
		Log.info("Create: " + start2thread / 1000 + "s (" + start2thread
				+ "ms)");
		Log.info("Summary: " + summary2end / 1000 + "s (" + summary2end + "ms)"
				+ (cfg.computeSummaryEdges ? "" : " [deactivated]"));
		Log.info("Thread: " + threadAlloc / 1000 + "s (" + threadAlloc + "ms)"
				+ (cfg.computeInterference ? "" : " [deactivated]"));

		return new Result(joanaSdg, jresult.jsdg, jresult.cg, jresult.pts, mhp);
	}

	private static Result getOrigSDG(Config cfg, boolean thread_contexts,
			IProgressMonitor progress) throws IllegalArgumentException,
			CancelException, PDGFormatException, IOException, WalaException,
			InvalidClassFileException {
		progress.beginTask(Messages.getString("Analyzer.Task_Prepare_IR"), -1); //$NON-NLS-1$

		com.ibm.wala.ipa.callgraph.impl.Util.setNativeSpec(cfg.nativesXML);

		progress.subTask(Messages.getString("Analyzer.SubTask_Analysis_Scope")); //$NON-NLS-1$

		ClassLoader loader = cfg.getClass().getClassLoader();
		AnalysisScope scope = Util.makeAnalysisScope(cfg, loader);
		// AnalysisScopeReader.makeJavaBinaryAnalysisScope(cfg.scopeFile,
		// cfg.classpath, null);
		progress.done();

		ClassHierarchy cha = ClassHierarchy.make(scope, progress);

		Iterable<Entrypoint> entrypoints = com.ibm.wala.ipa.callgraph.impl.Util
				.makeMainEntrypoints(scope, cha, cfg.mainClass);
		AnalysisOptions options = new AnalysisOptions(scope, entrypoints);
		AnalysisCache cache = new AnalysisCache();

		progress.subTask(Messages
				.getString("Analyzer.SubTask_Call_Graph_Builder") + cfg.pointsTo); //$NON-NLS-1$
		SSAPropagationCallGraphBuilder builder = SDGFactory
				.getCallGraphBuilder(cfg.pointsTo, options, cache, cha, scope);

		/**
		 * Change the wala internal pointer and instancekeyfactory of the
		 * callgraph builder to our adapter. So we can keep track of the created
		 * InstanceKeys and PointerKeys. This information is used later on when
		 * creating subobject trees for accessed field variables.
		 */
		InstanceAndPointerKeyFactoryAdapter adapter = null;
		InstanceKeyFactory ikFact = builder.getInstanceKeys();
		PointerKeyFactory pkFact = builder.getPointerKeyFactory();
		adapter = new InstanceAndPointerKeyFactoryAdapter(ikFact, pkFact);

		builder.setInstanceKeys(adapter);
		builder.setPointerKeyFactory(adapter);

		/**
		 * Use the thread sensitive context selector if set.
		 */
		if (thread_contexts) {
			ContextSelector osel = builder.getContextSelector();
			ContextSelector ts = new ThreadSensContextSelector();
			ContextSelector nsel = new DelegatingContextSelector(ts, osel);
			builder.setContextSelector(nsel);
		}

		progress.done();

		progress.subTask(Messages.getString("Analyzer.SubTask_Call_Graph")); //$NON-NLS-1$
		CallGraph cg = builder.makeCallGraph(options, progress);

		if (cfg.optimizeCg >= 0) {
			CallGraphPruning opt = new CallGraphPruning(cg);
			System.out.println("Call Graph has " + cg.getNumberOfNodes()
					+ " Nodes.");
			Set<CGNode> sopt = opt.findApplicationNodes(cfg.optimizeCg);
			cg = new PrunedCallGraph(cg, sopt);
			System.out.println("Optimized Call Graph has "
					+ cg.getNumberOfNodes() + " Nodes.");
		}

		if (Debug.Var.DUMP_CALLGRAPH.isSet()) {
			Util.dumpCallGraph(cg,
					cfg.mainClass.replace('/', '.').substring(1), progress);
		}

		if (Debug.Var.DUMP_HEAP_GRAPH.isSet()) {
			PointerAnalysis pta = builder.getPointerAnalysis();
			HeapGraph hg = pta.getHeapGraph();
			Util.dumpHeapGraph(cfg.mainClass.replace('/', '.').substring(1)
					+ "." + cfg.pointsTo, hg, null);
		}

		PointerAnalysis pta = builder.getPointerAnalysis();
		progress.done();

		DemandRefinementPointsTo demandPts = null;
		if (cfg.useDemandPts) {
			throw new UnsupportedOperationException();
			// MemoryAccessMap mam = new PABasedMemoryAccessMap(cg,
			// builder.getPointerAnalysis());
			// demandPts = new DemandRefinementPointsTo(cg,
			// new ThisFilteringHeapModel(builder,cha), mam, cha, options,
			// getStateMachineFactory());
		}

		IPointerAnalysis pts = new PointsToWrapper(demandPts, pta);

		progress.subTask(Messages.getString("Analyzer.SubTask_Search_Main")); //$NON-NLS-1$
		IMethod main = edu.kit.joana.deprecated.jsdg.util.Util.searchMethod(entrypoints,
				"main([Ljava/lang/String;)V"); //$NON-NLS-1$
		progress.done();

		progress.done();

		SDG sdg = SDG.create(main, cg, cache, adapter, pts, cfg, progress);
		sdg.setAnalysisScope(scope);
		sdg.setPointerAnalysis(pta);

		progress.done();

		if (Debug.Var.PRINT_FIELD_PTS_INFO.isSet()) {
			Log.info("search for field allocs called " + PDG.searchFieldAllocs
					+ " times.");
		}

		if (Debug.Var.PRINT_UNRESOLVED_CLASSES.isSet()) {
			for (TypeReference tRef : cg.getClassHierarchy()
					.getUnresolvedClasses()) {
				Log.warn("Could not resolve: " + Util.typeName(tRef.getName()));
			}
		}

		return new Result(null, sdg, cg, pta, null);
	}

	private static Date initSDGcomputation(Config cfg) throws IOException {
		if (cfg == null || !cfg.requiredFieldsSet()) {
			throw new IllegalArgumentException("Configuration is not valid: "
					+ cfg);
		}

		File outputDir = new File(cfg.outputDir);
		if (!outputDir.exists()) {
			outputDir.mkdir();
		}

		File logFile = new File(cfg.logFile);
		try {
			if (!logFile.exists()) {
				if (logFile.getParentFile() != null)
					logFile.getParentFile().mkdirs();
				logFile.createNewFile();
			}
		} catch (IOException e) {
			System.out.println("Could not create: " + logFile);
			throw e;
		}

		Log.setLogFile(cfg.logFile);
		if (cfg.logLevel != null) {
			Log.setMinLogLevel(cfg.logLevel);
		}

		Date date = new Date();
		Log.info("Starting Analysis at " + date);

		boolean assertions = false;
		assert (assertions = true);
		Log.info("Assertions are turned " + (assertions ? "ON" : "OFF"));
		Log.info("Java Datamodel: " + System.getProperty("sun.arch.data.model")
				+ "bit");
		Runtime run = Runtime.getRuntime();
		Log.info("Avaliable Processors: " + run.availableProcessors());
		Log.info("Free Memory: " + run.freeMemory());
		Log.info("Total Memory: " + run.totalMemory());
		Log.info("Maximum Memory: " + run.maxMemory());
		Log.info(Debug.getSettings());

		Log.info("SDGFactory.getSDG started with: \n" + cfg);

		return date;
	}

	private static String[] getJarsInDirectory(String dir) {
		Collection<File> col = FileUtil.listFiles(dir, ".*\\.jar$", true);
		String[] result = new String[col.size()];
		int i = 0;
		for (File jarFile : col) {
			result[i++] = jarFile.getAbsolutePath();
		}
		return result;
	}

}
