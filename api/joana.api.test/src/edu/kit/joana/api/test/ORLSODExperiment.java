package edu.kit.joana.api.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.classLoader.BinaryDirectoryTreeModule;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.irlsod.ORLSODChecker;
import edu.kit.joana.ifc.sdg.irlsod.PathBasedORLSODChecker;
import edu.kit.joana.ifc.sdg.irlsod.PredecessorMethod;
import edu.kit.joana.ifc.sdg.irlsod.ProbInfComputer;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import joana.api.testdata.demo.Fig2_3;
import joana.api.testdata.demo.xrlsod.LateSecretAccess;
import joana.api.testdata.demo.xrlsod.NoSecret;
import joana.api.testdata.demo.xrlsod.ORLSOD1;
import joana.api.testdata.demo.xrlsod.ORLSOD2;
import joana.api.testdata.demo.xrlsod.ORLSOD3;
import joana.api.testdata.demo.xrlsod.ORLSOD5a;
import joana.api.testdata.demo.xrlsod.ORLSODImprecise;

public class ORLSODExperiment {

	@Test
	public void doORLSOD1() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD1.class, "orlsod1", 1, 2, 2));
	}

	// imprecise due to current iRLSOD implementation
	@Test
	public void doORLSOD2() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException{
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD2.class, "orlsod2", 1, 2, 2));
	}

	// imprecise due to current iRLSOD implementation
	@Test
	public void doORLSOD3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD3.class, "orlsod3", 1, 2, 2));
	}

	@Test
	public void doNoSecret() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, NoSecret.class, "noSecret", 0, 2, 0));
	}

	@Test
	public void doLateSecretAccess()
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, LateSecretAccess.class, "lateSecAccess", 1, 2, 0));
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD5a.class, "orlsod5a", 1, 2, 2));
	}

	@Test
	public void testPost_Fig2_3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, Fig2_3.class, "post_fig2_3", 1, 2, 1));
	}

	@Test
	public void testORLSOD_imprecise()
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		/**
		 * NOTE: The program is actually secure but ORLSOD by design fails to detect this. RLSOD and LSOD deem this
		 * program secure (no "normal" flows and o low-observable conflict). TODO: add test code which proves this silly
		 * claim!
		 */
		doConfig(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSODImprecise.class, "orlsod_imprecise", 1, 1, 1));
	}

	
	
	public static <T> IFCAnalysis build(Class<T> clazz, SDGConfig config) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		final String className = clazz.getCanonicalName();
		final String classPath;
		classPath = JoanaPath.JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + JoanaPath.ANNOTATIONS_PASSON_CLASSPATH;
		config.setClassPath(classPath);
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		config.setEntryMethod(mainMethod.toBCString());
		SDGProgram prog = SDGProgram.createSDGProgram(config);

		IFCAnalysis ana = new IFCAnalysis(prog);
		return ana;
	}

	public static <T> IFCAnalysis buldAndUseJavaAnnotations(Class<T> clazz, SDGConfig config)
				throws ApiTestException, ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
			IFCAnalysis ana = build(clazz,config);
			ana.addAllJavaSourceAnnotations();
			return ana;
	}
	static final Stubs STUBS = Stubs.JRE_14;
	
	static final SDGConfig standardTestConfig = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			true, // interference
			MHPType.PRECISE);
	private static void doConfig(final TestConfig cfg)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		final IFCAnalysis ana = buldAndUseJavaAnnotations(cfg.progDesc.mainClass, standardTestConfig);
		final SDG sdg = ana.getProgram().getSDG();
		final CFG redCFG = ReducedCFGBuilder.extractReducedCFG(sdg);
		GraphModifier.removeCallCallRetEdges(redCFG);
		MiscGraph2Dot.export(redCFG, MiscGraph2Dot.joanaGraphExporter(), cfg.outputFiles.dotFile);
		final Map<SDGNode, String> userAnn = new HashMap<>();
		ana.getAnnotatedNodes().forEach((k,v) -> userAnn.put(k,v.getAnnotation().getLevel1()));

		Assert.assertEquals(
		    cfg.expectedNoHighThings,
		    userAnn.values().stream().filter(l -> BuiltinLattices.STD_SECLEVEL_HIGH.equals(l)).count()
		);
		
		Assert.assertEquals(
		    cfg.expectedNoLowThings,
		    userAnn.values().stream().filter(l -> BuiltinLattices.STD_SECLEVEL_LOW.equals(l)).count()
		);

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
		scope.addToScope(ClassLoaderReference.Primordial, new JarStreamModule(in));
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

	static class TestConfig {
		ProgDesc progDesc;
		OutputFiles outputFiles;
		int expectedNoHighThings;
		int expectedNoLowThings;
		int expectedNoViolations;
	}

	static class StandardTestConfig extends TestConfig {
		StandardTestConfig(final String classPath, final Class<?> mainClass, final String shortName,
				final int expectedNoSources, final int expectedNoSinks, final int expectedNoViolations) {
			this.progDesc = new ProgDesc(classPath, mainClass);
			this.outputFiles = new OutputFiles(String.format("%s.dot", shortName), String.format("%s.pdg", shortName));
			this.expectedNoHighThings = expectedNoSources;
			this.expectedNoLowThings = expectedNoSinks;
			this.expectedNoViolations = expectedNoViolations;
		}
	}

	static class ProgDesc {
		String classPath;
		Class<?> mainClass;

		ProgDesc(final String classPath, final Class<?> mainClass) {
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
}
