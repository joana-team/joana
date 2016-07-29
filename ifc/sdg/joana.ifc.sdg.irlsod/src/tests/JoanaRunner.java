package tests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.irlsod.RegionClusterBasedCDomOracle;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.graph.ThreadInformationUtil;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;
import edu.kit.joana.util.Stubs;
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
	public static final String CLASS_PATH = "example/bin";
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
		final URL url = new File(Stubs.JRE_14.getPaths()[0]).toURI().toURL();
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
		final DirectedGraph<ThreadInstance, DefaultEdge> tct = ThreadInformationUtil.buildThreadCreationTree(sdg.getThreadsInfo());
		MiscGraph2Dot.export(tct, MiscGraph2Dot.tctExporter(), "tct.dot");
		for (final ThreadInstance ti : sdg.getThreadsInfo()) {
			final DirectedGraph<VirtualNode, SDGEdge> threadGraph = unfoldVirtualCFGFor(sdg, ti.getId());
			MiscGraph2Dot.export(threadGraph, MiscGraph2Dot.threadGraphExporter(),
					String.format("thread-%d.dot", ti.getId()));
			final Dominators<VirtualNode, SDGEdge> threadDom = Dominators.compute(threadGraph,
					new VirtualNode(ti.getEntry(), ti.getId()));
			MiscGraph2Dot.export(threadDom.getDominationTree(), MiscGraph2Dot.genericExporter(),
					String.format("thread-%d-dom.dot", ti.getId()));
		}
		final PrintWriter pw = new PrintWriter(PDG_FILE);
		SDGSerializer.toPDGFormat(sdg, pw);
		pw.close();
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
}
