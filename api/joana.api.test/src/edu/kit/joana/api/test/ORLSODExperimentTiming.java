package edu.kit.joana.api.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.irlsod.PredecessorMethod;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.TimimgClassificationChecker;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;
import joana.api.testdata.demo.Fig2_3;
import joana.api.testdata.demo.xrlsod.LateSecretAccess;
import joana.api.testdata.demo.xrlsod.NoSecret;
import joana.api.testdata.demo.xrlsod.ORLSOD1;
import joana.api.testdata.demo.xrlsod.ORLSOD2;
import joana.api.testdata.demo.xrlsod.ORLSOD3;
import joana.api.testdata.demo.xrlsod.ORLSOD5Secure;
import joana.api.testdata.demo.xrlsod.ORLSOD5a;
import joana.api.testdata.demo.xrlsod.ORLSODImprecise;
import edu.kit.joana.api.test.ORLSODExperiment.StandardTestConfig;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;

public class ORLSODExperimentTiming {

	@Test
	public void doORLSOD1() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD1.class, "orlsod1", 1, 2, 4));
	}

	@Test
	public void doORLSOD2() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD2.class, "orlsod2", 1, 2, 0));
	}

	@Test
	public void doORLSOD3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD3.class, "orlsod3", 1, 2, 0));
	}

	@Test
	public void doNoSecret() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, NoSecret.class, "noSecret", 0, 2, 0));
	}

	@Test
	public void doLateSecretAccess()
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, LateSecretAccess.class,
				"lateSecAccess", 1, 2, 0));
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD5a.class, "orlsod5a", 1, 2, 6));
	}

	@Test
	public void testORLSODSecure() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSOD5Secure.class, "orlsod5secure",
				1, 2, 0));
	}

	@Test
	public void testPost_Fig2_3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		doConfigTiming(
				new ORLSODExperiment.StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, Fig2_3.class, "post_fig2_3", 1, 2, 4));
	}

	@Test
	public void testORLSOD_imprecise()
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		/**
		 * NOTE: The program is actually secure AND TimingClassification does detect this RLSOD and LSOD deem this
		 * program secure (no "normal" flows and o low-observable conflict). TODO: add test code which proves this silly
		 * claim!
		 */
		doConfigTiming(new StandardTestConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, ORLSODImprecise.class, "orlsod_imprecise", 1, 1, 0));
	}
	
	private static void doConfigTiming(final ORLSODExperiment.TestConfig cfg)
			throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException, ApiTestException {
		final IFCAnalysis ana = ORLSODExperiment.buldAndUseJavaAnnotations(cfg.progDesc.mainClass, ORLSODExperiment.standardTestConfig);
		final SDG sdg = ana.getProgram().getSDG();
		
		// currently, IFCAnalysis doesn't give access to the mhp analysis, so we redo it here manually.
		// TODO: find a better way!
		Assert.assertEquals(MHPType.PRECISE, ORLSODExperiment.standardTestConfig.getMhpType());
		final PreciseMHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
				
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
		final TimimgClassificationChecker<String> checkerSlice = new TimimgClassificationChecker<>(sdg,
				BuiltinLattices.getBinaryLattice(), userAnn, mhp, tmdo, PredecessorMethod.SLICE);
		final int noVios = checkerSlice.checkIFlow().size();
		Assert.assertEquals(cfg.expectedNoViolations, noVios);

		final TimimgClassificationChecker<String> checkerEdge = new TimimgClassificationChecker<>(sdg,
				BuiltinLattices.getBinaryLattice(), userAnn, mhp, tmdo, PredecessorMethod.EDGE);
		checkerEdge.checkIFlow();
		Assert.assertEquals(checkerSlice.getCL(), checkerEdge.getCL());
		Assert.assertEquals(checkerSlice.getCLT(), checkerEdge.getCLT());

	}

}
