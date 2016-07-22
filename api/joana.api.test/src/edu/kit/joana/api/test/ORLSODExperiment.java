package edu.kit.joana.api.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarInputStream;

import org.junit.Assert;
import org.junit.Test;

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

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.irlsod.ORLSODChecker;
import edu.kit.joana.ifc.sdg.irlsod.PathBasedORLSODChecker;
import edu.kit.joana.ifc.sdg.irlsod.PredecessorMethod;
import edu.kit.joana.ifc.sdg.irlsod.ProbInfComputer;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.ExternalCallCheck;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.DynamicDispatchHandling;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.core.SDGBuilder.SDGBuilderConfig;
import edu.kit.joana.wala.core.SDGBuilder.StaticInitializationTreatment;

public class ORLSODExperiment {

	@Test
	public void doORLSOD1() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/ORLSOD1", "orlsod1", 1, 2, 2));
	}

	@Test
	public void doORLSOD2() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/ORLSOD2", "orlsod2", 1, 2, 0));
	}

	@Test
	public void doORLSOD3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/ORLSOD3", "orlsod3", 1, 2, 0));
	}

	@Test
	public void doNoSecret() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/NoSecret", "noSecret", 0, 2, 0));
	}

	@Test
	public void doLateSecretAccess()
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/LateSecretAccess", "lateSecAccess", 1, 2, 0));
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/ORLSOD5a", "orlsod5a", 1, 2, 2));
	}

	@Test
	public void testPost_Fig2_3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/Fig2_3", "post_fig2_3", 1, 2, 1));
	}

	@Test
	public void testORLSOD_imprecise()
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		/**
		 * NOTE: The program is actually secure but ORLSOD by design fails to detect this. RLSOD and LSOD deem this
		 * program secure (no "normal" flows and o low-observable conflict). TODO: add test code which proves this silly
		 * claim!
		 */
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, "Ljoana/api/testdata/demo/xrlsod/ORLSODImprecise", "orlsod_imprecise", 1, 1, 1));
	}

	private static void doConfig(final TestConfig cfg)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		final SDG sdg = ORLSODExperiment.buildSDG(cfg.progDesc.classPath, cfg.progDesc.mainClass);
		CSDGPreprocessor.preprocessSDG(sdg);
		final CFG redCFG = ReducedCFGBuilder.extractReducedCFG(sdg);
		GraphModifier.removeCallCallRetEdges(redCFG);
		MiscGraph2Dot.export(redCFG, MiscGraph2Dot.joanaGraphExporter(), cfg.outputFiles.dotFile);
		final PreciseMHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		PruneInterferences.pruneInterferences(sdg, mhp);
		final PrintWriter pw = new PrintWriter(cfg.outputFiles.pdgFile);
		SDGSerializer.toPDGFormat(sdg, pw);
		pw.close();
		final Map<SDGNode, String> userAnn = new HashMap<SDGNode, String>();
		int noHighThings = 0;
		for (final SDGNode src : cfg.srcSelector.select(sdg)) {
			userAnn.put(src, BuiltinLattices.STD_SECLEVEL_HIGH);
			System.out.println(String.format("userAnn(%s) = %s", src, BuiltinLattices.STD_SECLEVEL_HIGH));
			noHighThings++;
		}
		Assert.assertEquals(cfg.expectedNoHighThings, noHighThings);
		int noLowThings = 0;
		for (final SDGNode snk : cfg.snkSelector.select(sdg)) {
			userAnn.put(snk, BuiltinLattices.STD_SECLEVEL_LOW);
			System.out.println(String.format("userAnn(%s) = %s", snk, BuiltinLattices.STD_SECLEVEL_LOW));
			noLowThings++;
		}
		Assert.assertEquals(cfg.expectedNoLowThings, noLowThings);
		final ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
		final ProbInfComputer probInf = new ProbInfComputer(sdg, tmdo);
		final ORLSODChecker<String> checkerPath = new PathBasedORLSODChecker<String>(sdg,
				BuiltinLattices.getBinaryLattice(), userAnn, probInf);
		final int noViosPath = checkerPath.checkIFlow().size();
		Assert.assertEquals(cfg.expectedNoViolations, noViosPath);

		// The optimization finds exactly the same number of violations.
		// Also, because the classification Map cl is context-insensitive
		// anayway, it is indeed sufficient to propagate along
		// sdg edges, instead of propagating all levels in the i2p-slice of a
		// node.
		final ORLSODChecker<String> checkerSlice = new ORLSODChecker<String>(sdg, BuiltinLattices.getBinaryLattice(),
				userAnn, probInf, PredecessorMethod.SLICE);
		Assert.assertEquals(noViosPath, checkerSlice.checkIFlow().size());
		final ORLSODChecker<String> checkerEdge = new ORLSODChecker<String>(sdg, BuiltinLattices.getBinaryLattice(),
				userAnn, probInf, PredecessorMethod.EDGE);
		Assert.assertEquals(noViosPath, checkerEdge.checkIFlow().size());

	}

	static AnalysisScope makeMinimalScope(final String appClassPath) throws IOException {
		final AnalysisScope scope = AnalysisScope.createJavaAnalysisScope();
		scope.addToScope(ClassLoaderReference.Application, new BinaryDirectoryTreeModule(new File(appClassPath)));
		final URL url = Stubs.class.getClassLoader().getResource("jSDG-stubs-jre1.4.jar");
		final URLConnection con = url.openConnection();
		final InputStream in = con.getInputStream();
		scope.addToScope(ClassLoaderReference.Primordial, new JarStreamModule(new JarInputStream(in)));
		return scope;
	}

	static IMethod findMethod(final IClassHierarchy cha, final String mainClass) {
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

	static SDG buildSDG(final String classPath, final String mainClass)
			throws IOException, ClassHierarchyException, UnsoundGraphException, CancelException {
		final SDGBuilder.SDGBuilderConfig scfg = new SDGBuilder.SDGBuilderConfig();
		scfg.out = System.out;
		scfg.scope = ORLSODExperiment.makeMinimalScope(classPath);
		scfg.cache = new AnalysisCache(new DefaultIRFactory());
		scfg.cha = ClassHierarchy.make(scfg.scope);
		scfg.entry = ORLSODExperiment.findMethod(scfg.cha, mainClass);
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

	static class TestConfig {
		ProgDesc progDesc;
		OutputFiles outputFiles;
		NodeSelector srcSelector;
		NodeSelector snkSelector;
		int expectedNoHighThings;
		int expectedNoLowThings;
		int expectedNoViolations;
	}

	static class StandardTestConfig extends TestConfig {
		StandardTestConfig(final String classPath, final String mainClass, final String shortName,
				final int expectedNoSources, final int expectedNoSinks, final int expectedNoViolations) {
			this.progDesc = new ProgDesc(classPath, mainClass);
			this.outputFiles = new OutputFiles(String.format("%s.dot", shortName), String.format("%s.pdg", shortName));
			this.srcSelector = new CriterionBasedNodeSelector(FieldAccess.staticRead(mainClass + ".HIGH"));
			this.snkSelector = new CriterionBasedNodeSelector(FieldAccess.staticWrite(mainClass + ".LOW"));
			this.expectedNoHighThings = expectedNoSources;
			this.expectedNoLowThings = expectedNoSinks;
			this.expectedNoViolations = expectedNoViolations;
		}
	}

	static class ProgDesc {
		String classPath;
		String mainClass;

		ProgDesc(final String classPath, final String mainClass) {
			this.classPath = classPath;
			this.mainClass = mainClass;
		}
	}

	static class OutputFiles {
		String dotFile;
		String pdgFile;

		OutputFiles(final String dotFile, final String pdgFile) {
			this.dotFile = dotFile;
			this.pdgFile = pdgFile;
		}
	}

	static interface NodeSelector {
		Collection<? extends SDGNode> select(SDG sdg);
	}

	@FunctionalInterface
	private static interface NodeCriterion {
		boolean accept(SDGNode n, SDG sdg);
	}

	private static class FixedNodeSelector implements NodeSelector {

		private final int[] ids;

		FixedNodeSelector(final int... ids) {
			this.ids = ids;
		}

		@Override
		public Collection<? extends SDGNode> select(final SDG sdg) {
			final List<SDGNode> ret = new LinkedList<SDGNode>();
			for (final int id : ids) {
				ret.add(sdg.getNode(id));
			}
			return ret;
		}
	}

	private static class CriterionBasedNodeSelector implements NodeSelector {
		private final NodeCriterion crit;

		public CriterionBasedNodeSelector(final NodeCriterion crit) {
			this.crit = crit;
		}

		@Override
		public Collection<? extends SDGNode> select(final SDG sdg) {
			final List<SDGNode> ret = new LinkedList<SDGNode>();
			for (final SDGNode n : sdg.vertexSet()) {
				if (crit.accept(n, sdg)) {
					ret.add(n);
				}
			}
			return ret;
		}
	}

	private static class FieldAccess implements NodeCriterion {
		enum AccessType {
			READ, WRITE;
			SDGNode.Operation toOperation() {
				switch (this) {
				case READ:
					return SDGNode.Operation.REFERENCE;
				case WRITE:
					return SDGNode.Operation.MODIFY;
				default:
					throw new UnsupportedOperationException("unhandled case: " + this);
				}
			}
		}

		private final String fieldName;
		private final int staticOrObject;
		private final SDGNode.Operation operation;

		FieldAccess(final String fieldName, final boolean isStatic, final AccessType accType) {
			this.fieldName = fieldName;
			this.staticOrObject = isStatic ? BytecodeLocation.STATIC_FIELD : BytecodeLocation.OBJECT_FIELD;
			this.operation = accType.toOperation();
		}

		@Override
		public boolean accept(final SDGNode n, final SDG sdg) {
			if (n.getOperation() == operation) {
				final SDGNode field = findRelevantFieldInCEClosure(n, sdg);
				return field != null;
			} else {
				return false;
			}
		}

		private SDGNode findRelevantFieldInCEClosure(final SDGNode fieldNode, final SDG sdg) {
			for (final SDGNode n : ceClosure(fieldNode, sdg)) {
				if (n.getBytecodeName().equals(fieldName) && (n.getBytecodeIndex() == this.staticOrObject)) {
					return n;
				}
			}
			return null;
		}

		private Set<SDGNode> ceClosure(final SDGNode start, final SDG sdg) {
			final LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
			final Set<SDGNode> done = new HashSet<SDGNode>();
			worklist.add(start);
			while (!worklist.isEmpty()) {
				final SDGNode next = worklist.poll();
				if (done.contains(next)) {
					continue;
				}
				for (final SDGEdge eOut : sdg.getOutgoingEdgesOfKind(next, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
					worklist.add(eOut.getTarget());
				}
				for (final SDGEdge eIn : sdg.getIncomingEdgesOfKind(next, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
					worklist.add(eIn.getSource());
				}
				done.add(next);
			}
			return done;
		}

		public static FieldAccess staticRead(final String fieldName) {
			return new FieldAccess(fieldName, true, AccessType.READ);
		}

		public static FieldAccess staticWrite(final String fieldName) {
			return new FieldAccess(fieldName, true, AccessType.WRITE);
		}

		public static FieldAccess objectRead(final String fieldName) {
			return new FieldAccess(fieldName, false, AccessType.READ);
		}

		public static FieldAccess objectWrite(final String fieldName) {
			return new FieldAccess(fieldName, false, AccessType.WRITE);
		}
	}

	private static class MethodCall {

	}
}
