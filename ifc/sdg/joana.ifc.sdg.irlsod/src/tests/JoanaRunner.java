package tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarInputStream;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefaultIRFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.irlsod.RegionClusterBasedCDomOracle;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.DynamicDispatchHandling;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;
import edu.kit.joana.wala.core.graphs.Dominators;

public class JoanaRunner {

	// public static final String CLASS_PATH =
	// "/data1/mmohr/git/MixServer_verif/bin";
	// public static final String MAIN_CLASS =
	// "Lselectvoting/system/core/Setup";
	public static final String CLASS_PATH = "example-bin";
	public static final String MAIN_CLASS = "Lorlsod/ORLSOD2";
	public static final String PDG_FILE = "orlsod2.pdg";

	private static IMethod findMethod(final IClassHierarchy cha, final String mainClass) {
		final IClass cl = cha.lookupClass(TypeReference.findOrCreate(ClassLoaderReference.Application, mainClass));
		if (cl == null) {
			throw new RuntimeException("class not found: " + mainClass);
		}
		final IMethod m = cl.getMethod(Selector.make("main([Ljava/lang/String;)V"));
		if (m == null) {
			throw new RuntimeException("main method of class " + cl + " not found!");
		}
		return m;
	}

	private static AnalysisScope makeMinimalScope(final String appClassPath) throws IOException {
		final AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
		scope.addToScope(ClassLoaderReference.Application, new BinaryDirectoryTreeModule(new File(appClassPath)));
		final URL url = JoanaRunner.class.getClassLoader().getResource("jSDG-stubs-jre1.4.jar");
		final URLConnection con = url.openConnection();
		final InputStream in = con.getInputStream();
		scope.addToScope(ClassLoaderReference.Primordial, new JarStreamModule(new JarInputStream(in)));
		return scope;
	}

	public static SDG buildSDG() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		return buildSDG(CLASS_PATH, MAIN_CLASS);
	}

	public static SDG buildSDG(final String classPath, final String mainClass)
			throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.out = System.out;
		scfg.scope = makeMinimalScope(classPath);
		scfg.cache = new AnalysisCache(new DefaultIRFactory());
		scfg.cha = ClassHierarchy.make(scfg.scope);
		scfg.entry = findMethod(scfg.cha, mainClass);
		scfg.ext = ExternalCallCheck.EMPTY;
		scfg.immutableNoOut = Main.IMMUTABLE_NO_OUT;
		scfg.immutableStubs = Main.IMMUTABLE_STUBS;
		scfg.ignoreStaticFields = Main.IGNORE_STATIC_FIELDS;
		scfg.exceptions = ExceptionAnalysis.INTERPROC;
		scfg.pruneDDEdgesToDanglingExceptionNodes = true;
		scfg.defaultExceptionMethodState = MethodState.DEFAULT;
		scfg.accessPath = false;
		scfg.sideEffects = null;
		scfg.prunecg = 2;
		scfg.pruningPolicy = ApplicationLoaderPolicy.INSTANCE;
		scfg.pts = PointsToPrecision.N1_OBJECT_SENSITIVE;
		scfg.customCGBFactory = null;
		scfg.staticInitializers = StaticInitializationTreatment.SIMPLE;
		scfg.fieldPropagation = FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL;
		scfg.debugManyGraphsDotOutput = false;
		scfg.computeInterference = true;
		scfg.computeAllocationSites = true;
		scfg.cgConsumer = null;
		scfg.additionalContextSelector = null;
		scfg.dynDisp = DynamicDispatchHandling.PRECISE;
		scfg.debugManyGraphsDotOutput = true;
		final SDG sdg = SDGBuilder.build(scfg);
		return sdg;
	}

	public static void main(final String[] args)
			throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final SDG sdg = buildSDG(CLASS_PATH, MAIN_CLASS);
		CSDGPreprocessor.preprocessSDG(sdg);
		System.out.println(sdg.getThreadsInfo());
		final DirectedGraph<ThreadInstance, DefaultEdge> tct = buildThreadCreationTree(sdg.getThreadsInfo());
		DomExperiment.export(tct, DomExperiment.tctExporter(), "tct.dot");
		for (final ThreadInstance ti : sdg.getThreadsInfo()) {
			final DirectedGraph<VirtualNode, SDGEdge> threadGraph = unfoldVirtualCFGFor(sdg, ti.getId());
			DomExperiment.export(threadGraph, DomExperiment.threadGraphExporter(),
					String.format("thread-%d.dot", ti.getId()));
			final Dominators<VirtualNode, SDGEdge> threadDom = Dominators.compute(threadGraph,
					new VirtualNode(ti.getEntry(), ti.getId()));
			DomExperiment.export(threadDom.getDominationTree(), DomExperiment.genericExporter(),
					String.format("thread-%d-dom.dot", ti.getId()));
		}
		final PrintWriter pw = new PrintWriter(PDG_FILE);
		SDGSerializer.toPDGFormat(sdg, pw);
		pw.close();
	}

	public static DirectedGraph<ThreadInstance, DefaultEdge> buildThreadCreationTree(
			final ThreadsInformation threadInfo) {
		final DirectedGraph<ThreadInstance, DefaultEdge> tct = new DefaultDirectedGraph<ThreadInstance, DefaultEdge>(
				DefaultEdge.class);
		for (final ThreadInstance ti1 : threadInfo) {
			if (ti1.getThreadContext() == null) {
				continue;
			}
			ThreadInstance lowestAnc = null;
			for (final ThreadInstance ti2 : threadInfo) {
				if ((lowestAnc == null) || (isAncestor(ti2, ti1) && isAncestor(lowestAnc, ti2))) {
					lowestAnc = ti2;
				}
			}
			tct.addVertex(lowestAnc);
			tct.addVertex(ti1);
			tct.addEdge(lowestAnc, ti1);
		}
		return tct;
	}

	public static CFG unfoldCFGFor(final SDG sdg, final int thread) {
		final CFG icfg = ICFGBuilder.extractICFG(sdg);
		final CFG ret = new CFG();
		for (final SDGNode n : icfg.vertexSet()) {
			if (!RegionClusterBasedCDomOracle.possiblyExecutesIn(n, thread)) {
				continue;
			}
			for (final SDGEdge e : icfg.outgoingEdgesOf(n)) {
				if (!RegionClusterBasedCDomOracle.possiblyExecutesIn(e.getTarget(), thread)) {
					continue;
				}
				ret.addVertex(n);
				ret.addVertex(e.getTarget());
				ret.addEdge(n, e.getTarget(), e);
			}
		}
		return ret;
	}

	public static DirectedGraph<VirtualNode, SDGEdge> unfoldVirtualCFGFor(final SDG sdg, final int thread) {
		final CFG icfg = ICFGBuilder.extractICFG(sdg);
		final DirectedGraph<VirtualNode, SDGEdge> ret = new DefaultDirectedGraph<VirtualNode, SDGEdge>(SDGEdge.class);
		for (final SDGNode n : icfg.vertexSet()) {
			if (!RegionClusterBasedCDomOracle.possiblyExecutesIn(n, thread)) {
				continue;
			}
			for (final SDGEdge e : icfg.outgoingEdgesOf(n)) {
				if (!RegionClusterBasedCDomOracle.possiblyExecutesIn(e.getTarget(), thread)) {
					continue;
				}
				final VirtualNode vn = new VirtualNode(n, thread);
				final VirtualNode vm = new VirtualNode(e.getTarget(), thread);
				ret.addVertex(vn);
				ret.addVertex(vm);
				ret.addEdge(vn, vm, e);
			}
		}
		return ret;
	}

	@SuppressWarnings("unused")
	private static boolean isCallCallRetEdge(final SDGEdge e) {
		// @formatter:off
		return     (e.getKind() == SDGEdge.Kind.CONTROL_FLOW)
				&& (e.getSource().getKind() == SDGNode.Kind.CALL)
				&& BytecodeLocation.isCallRetNode(e.getTarget());
		// @formatter:on
	}

	private static boolean isAncestor(final ThreadInstance ti1, final ThreadInstance ti2) {
		if (ti1.getThreadContext() == null) {
			return true;
		}
		if (ti2.getThreadContext() == null) {
			return false;
		}
		return isSuffixOf(ti1.getThreadContext(), ti2.getThreadContext())
				&& !ti1.getThreadContext().equals(ti2.getThreadContext());
	}

	private static <A> boolean isSuffixOf(final List<A> ls1, final List<A> ls2) {
		if (ls1 == null) {
			return true;
		}
		if (ls2 == null) {
			return false;
		}
		if (ls1.size() > ls2.size()) {
			return false;
		}
		final List<A> ls1Rev = new LinkedList<A>(ls1);
		final List<A> ls2Rev = new LinkedList<A>(ls2);
		Collections.reverse(ls1Rev);
		Collections.reverse(ls2Rev);
		return isPrefixOf(ls1Rev, ls2Rev);
	}

	private static <A> boolean isPrefixOf(final List<A> ls1, final List<A> ls2) {
		if (ls1 == null) {
			return true;
		}
		if (ls2 == null) {
			return false;
		}
		if (ls1.size() > ls2.size()) {
			return false;
		}
		final Iterator<A> iter1 = ls1.iterator();
		final Iterator<A> iter2 = ls2.iterator();
		while (iter1.hasNext()) {
			final A x1 = iter1.next();
			final A x2 = iter2.next();
			if (!x1.equals(x2)) {
				return false;
			}
		}
		return true;
	}
}
