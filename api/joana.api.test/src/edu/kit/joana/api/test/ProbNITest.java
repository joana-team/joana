/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.api.test.util.SDGAnalyzer;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.SecurityNode.SecurityNodeFactory;
import edu.kit.joana.ifc.sdg.core.conc.BarrierIFCSlicer;
import edu.kit.joana.ifc.sdg.core.conc.LSODNISlicer;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNISlicer;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IConflictLeak;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * This class contains test cases whose primary purpose is to test IFC scanners
 * that check whether programs contain probabilistic data or order channels.
 * 
 * Normally, probabilistic analysis consists of two steps:
 * 1. Verifying that a program does not contain explicit or implicit flow
 *    (including possible flows of that sort that occur between threads)
 * 2. Verifying that a program does not contain probabilistic data or order channels
 *    (which is tested by those scanners here)
 * 
 * Since some programs fulfill property 2 but fail 1, it might be confusing
 * if one asserts that there are no violations after testing just for 2.
 * Thus, for clarification purposes, those are accompanied by checks verifying that
 * the IFC behavior concerning property 1 is as expected.
 * 
 * @author Martin Mohr
 */
public class ProbNITest {
	

	private static final boolean FORCE_REBUILD = true;
	private static final String outputDir = "out";

	private static final Map<String, TestData> testData = new HashMap<String, TestData>();
	
	private static final Logger debug = Log.getLogger("test.debug");
	
	private static final boolean DEBUG = debug.isEnabled();

	static {
		addTestCase("dataconf-rw-benign", "joana.api.testdata.conc.DataConflictRWBenign");
		addTestCase("dataconf-rw", "joana.api.testdata.conc.DataConflictRW");
		addTestCase("orderconf", "joana.api.testdata.conc.OrderConflict");
		addTestCase("orderconfbenign", "joana.api.testdata.conc.OrderConflictBenign");
		addTestCase("noorderconf", "joana.api.testdata.conc.NoOrderConflict");
		addTestCase("dataconf-rw-nomhp", "joana.api.testdata.conc.DataConflictRWNoMHP");
		addTestCase("nodataconf-rw-nomhp", "joana.api.testdata.conc.NoDataConflictRWNoMHP");
	}

	private static final void addTestCase(String testName, String mainClass) {
		testData.put(testName, new TestData(mainClass, testName + ".pdg"));
	}

	@BeforeClass
	public static void setUp() {
		for (String testKey : testData.keySet()) {
			final TestData td = testData.get(testKey);
			if (FORCE_REBUILD) {
				final BuildSDG b = BuildSDG.standardConcSetup(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, td.mainClass,
						td.sdgFile);
				b.run();
			} else {
				final File f = new File(td.sdgFile);
				if (!f.exists() || !f.canRead()) {
					final BuildSDG b = BuildSDG.standardConcSetup(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
							td.mainClass, td.sdgFile);
					b.run();
				}
			}
		}
	}

	/*
	 * used to assert that property 1 as defined above holds as expected
	 */
	private void assertNoTraditionalLeaks(SDG sdg) {
		BarrierIFCSlicer tradIFC = new BarrierIFCSlicer(sdg, BuiltinLattices.getBinaryLattice());
		Collection<ClassifiedViolation> exImpVios = tradIFC.checkIFlow();
		Assert.assertTrue(exImpVios.isEmpty());
	}

	/*
	 * used to assert that property 1 as defined above is violated as expected
	 */
	private void assertTraditionalLeaks(SDG sdg) {
		BarrierIFCSlicer tradIFC = new BarrierIFCSlicer(sdg, BuiltinLattices.getBinaryLattice());
		Collection<ClassifiedViolation> exImpVios = tradIFC.checkIFlow();
		Assert.assertFalse(exImpVios.isEmpty());
	}
	
	/*
	 * used to check that an expected violation of property 2 as defined above is found
	 */
	private void checkSoundness(Collection<IConflictLeak<SecurityNode>> vios) {
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IConflictLeak<SecurityNode> v : vios) {
				debug.outln(v);
			}
		}
	}
	
	/*
	 * used to check that for a program that fulfills property 2 as defined above,
	 * indeed no violations are found
	 */
	private void checkPreciseEnough(Collection<IConflictLeak<SecurityNode>> vios) {
		Assert.assertTrue(vios.isEmpty());
		if (DEBUG) {
			for (IConflictLeak<SecurityNode> v : vios) {
				debug.outln(v);
			}
		}
	}
	
	/*
	 * used if a program fulfills property 2 as defined above,
	 * but our analysis currently cannot show this.
	 * Hopefully, some day such test will fail.
	 */
	private void checkTooImprecise(Collection<IConflictLeak<SecurityNode>> vios) {
		// for a precise enough analysis this should fail
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IConflictLeak<SecurityNode> v : vios) {
				debug.outln(v);
			}
		}
	}
	
	@Test
	public void testDataConflictRWBenign() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateDataConflictRWBenign();
		
		assertNoTraditionalLeaks(sdg);
		
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), mhp, false, false);
		Set<IConflictLeak<SecurityNode>> vios = checker.check();
		checkSoundness(vios);
	}
	
	@Test
	public void testDataConflictRWBenignLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateDataConflictRWBenign();
		
		assertNoTraditionalLeaks(sdg);
		
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), mhp, false, true);
		Set<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testDataConflictRWBenignGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn Opt. ===");
		SDG sdg = annotateDataConflictRWBenign();
		
		assertNoTraditionalLeaks(sdg);
		
		ProbabilisticNISlicer checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Set<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testDataConflictRWLSOD() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateDataConflictRW();
		
		assertTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkTooImprecise(vios);
	}
	
	@Test
	public void testDataConflictRWLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateDataConflictRW();
		
		assertTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), true);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testOrderConflictLSOD() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateOrderConflict();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkSoundness(vios);
	}
	
	@Test
	public void testOrderConflictLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateOrderConflict();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), true);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkSoundness(vios);
	}
	
	@Test
	public void testOrderConflictGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn LSOD ===");
		SDG sdg = annotateOrderConflict();
		
		assertNoTraditionalLeaks(sdg);
		
		ProbabilisticNISlicer checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkSoundness(vios);
	}
	
	@Test
	public void testNoOrderConflictLSOD() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateNoOrderConflict();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testNoOrderConflictLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateNoOrderConflict();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), true);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testNoOrderConflictGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn LSOD ===");
		SDG sdg = annotateNoOrderConflict();
		
		assertNoTraditionalLeaks(sdg);
		
		ProbabilisticNISlicer checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testOrderConflictBenignLSOD() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateOrderConflictBenign();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkSoundness(vios);
	}
	
	@Test
	public void testOrderConflictBenignLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateOrderConflictBenign();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), true);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testOrderConflictBenignGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn LSOD ===");
		SDG sdg = annotateOrderConflictBenign();
		
		assertNoTraditionalLeaks(sdg);
		
		ProbabilisticNISlicer checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testDataConflictRWGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn ===");
		SDG sdg = annotateDataConflictRW();
		
		assertTraditionalLeaks(sdg);
		
		ProbabilisticNISlicer checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	@Test
	public void testDataConflictRWNoMHP() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateDataConflictRWNoMHP();
		
		assertTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkTooImprecise(vios);
	}
	
	@Test
	public void testNoDataConflictRWNoMHP() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateNoDataConflictRWNoMHP();
		
		assertNoTraditionalLeaks(sdg);
		
		LSODNISlicer checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<IConflictLeak<SecurityNode>> vios = checker.check();
		checkPreciseEnough(vios);
	}
	
	private SDG annotateDataConflictRWBenign() throws IOException {
		TestData tData = testData.get("dataconf-rw-benign");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
	
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(Ljava/lang/String;)V"));
	
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(Ljava/lang/String;)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
		}
		
		return sdg;
	}
	
	private SDG annotateDataConflictRWNoMHP() throws IOException {
		TestData tData = testData.get("dataconf-rw-nomhp");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(I)V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.DataConflictRWNoMHP$Thread2.run()V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.DataConflictRWNoMHP.main([Ljava/lang/String;)V"));
		
		Collection<SDGNode> secSources = ana.collectModificationsAndAssignmentsInMethod("joana.api.testdata.conc.DataConflictRWNoMHP.main([Ljava/lang/String;)V", "Ljoana/api/testdata/conc/DataConflictRWNoMHP.x");
		Assert.assertFalse(secSources.isEmpty());
		for (SDGNode sSrc : secSources) {
			SecurityNode ssSrc = (SecurityNode) sSrc;
			ssSrc.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
			if (DEBUG) debug.outln("Annotated node " + ssSrc + " as high source.");
		}
		
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(I)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			if (DEBUG) debug.outln("Annotated node " + spSink + " as low sink.");
			for (SDGEdge out : sdg.getOutgoingEdgesOfKind(spSink, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				SecurityNode npSink = (SecurityNode) out.getTarget();
				npSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			}
		}
		
		return sdg;
	}
	
	private SDG annotateNoDataConflictRWNoMHP() throws IOException {
		TestData tData = testData.get("nodataconf-rw-nomhp");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(I)V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.NoDataConflictRWNoMHP$Thread2.run()V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.NoDataConflictRWNoMHP.main([Ljava/lang/String;)V"));
		
		Collection<SDGNode> secSources = ana.collectModificationsAndAssignmentsInMethod("joana.api.testdata.conc.NoDataConflictRWNoMHP.main([Ljava/lang/String;)V", "Ljoana/api/testdata/conc/NoDataConflictRWNoMHP.x");
		Assert.assertFalse(secSources.isEmpty());
		for (SDGNode sSrc : secSources) {
			SecurityNode ssSrc = (SecurityNode) sSrc;
			ssSrc.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
			if (DEBUG) debug.outln("Annotated node " + ssSrc + " as high source.");
		}
		
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(I)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			if (DEBUG) debug.outln("Annotated node " + spSink + " as low sink.");
			for (SDGEdge out : sdg.getOutgoingEdgesOfKind(spSink, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				SecurityNode npSink = (SecurityNode) out.getTarget();
				npSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			}
		}
		
		return sdg;
	}
	
	private SDG annotateDataConflictRW() throws IOException {
		TestData tData = testData.get("dataconf-rw");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(I)V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.DataConflictRW$Thread1.run()V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.DataConflictRW$Thread2.run()V"));
		
		Collection<SDGNode> secSources = ana.collectModificationsAndAssignmentsInMethod("joana.api.testdata.conc.DataConflictRW$Thread1.run()V", "Ljoana/api/testdata/conc/DataConflictRW.x");
		Assert.assertFalse(secSources.isEmpty());
		for (SDGNode sSrc : secSources) {
			SecurityNode ssSrc = (SecurityNode) sSrc;
			ssSrc.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
			if (DEBUG) debug.outln("Annotated node " + ssSrc + " as high source.");
		}
		
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(I)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			if (DEBUG) debug.outln("Annotated node " + spSink + " as low sink.");
			for (SDGEdge out : sdg.getOutgoingEdgesOfKind(spSink, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				SecurityNode npSink = (SecurityNode) out.getTarget();
				npSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			}
		}
		
		return sdg;
	}
	
	private SDG annotateOrderConflict() throws IOException {
		TestData tData = testData.get("orderconf");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(Ljava/lang/String;)V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.OrderConflict$Thread1.run()V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.OrderConflict$Thread2.run()V"));
		
		Collection<SDGNode> secSources = ana.collectModificationsAndAssignmentsInMethod("joana.api.testdata.conc.OrderConflict$Thread1.run()V", "Ljoana/api/testdata/conc/OrderConflict.x");
		Assert.assertFalse(secSources.isEmpty());
		for (SDGNode sSrc : secSources) {
			SecurityNode ssSrc = (SecurityNode) sSrc;
			ssSrc.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
			if (DEBUG) debug.outln("Annotated node " + ssSrc + " as high source.");
		}
		
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(Ljava/lang/String;)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			if (DEBUG) debug.outln("Annotated node " + spSink + " as low sink.");
			for (SDGEdge out : sdg.getOutgoingEdgesOfKind(spSink, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				SecurityNode npSink = (SecurityNode) out.getTarget();
				npSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
				if (DEBUG) debug.outln("Annotated node " + npSink + " as low sink.");
			}
		}
		
		return sdg;
	}
	
	private SDG annotateNoOrderConflict() throws IOException {
		TestData tData = testData.get("noorderconf");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(Ljava/lang/String;)V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.NoOrderConflict$Thread1.run()V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.NoOrderConflict$Thread2.run()V"));
		
		Collection<SDGNode> secSources = ana.collectModificationsAndAssignmentsInMethod("joana.api.testdata.conc.NoOrderConflict$Thread1.run()V", "Ljoana/api/testdata/conc/NoOrderConflict.x");
		Assert.assertFalse(secSources.isEmpty());
		for (SDGNode sSrc : secSources) {
			SecurityNode ssSrc = (SecurityNode) sSrc;
			ssSrc.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
			if (DEBUG) debug.outln("Annotated node " + ssSrc + " as high source.");
		}
		
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(Ljava/lang/String;)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			if (DEBUG) debug.outln("Annotated node " + spSink + " as low sink.");
			for (SDGEdge out : sdg.getOutgoingEdgesOfKind(spSink, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				SecurityNode npSink = (SecurityNode) out.getTarget();
				npSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
				if (DEBUG) debug.outln("Annotated node " + npSink + " as low sink.");
			}
		}
		
		return sdg;
	}
	
	private SDG annotateOrderConflictBenign() throws IOException {
		TestData tData = testData.get("orderconfbenign");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Assert.assertTrue(ana.isLocatable("java.io.PrintStream.println(Ljava/lang/String;)V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.OrderConflictBenign$Thread1.run()V"));
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.OrderConflictBenign$Thread2.run()V"));
		
		
		Collection<SDGNode> pubSinks = ana.collectCalls("java.io.PrintStream.println(Ljava/lang/String;)V");
		Assert.assertNotNull(pubSinks);
		Assert.assertFalse(pubSinks.isEmpty());
		for (SDGNode pSink : pubSinks) {
			SecurityNode spSink = (SecurityNode) pSink;
			spSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
			if (DEBUG) debug.outln("Annotated node " + spSink + " as low sink.");
			for (SDGEdge out : sdg.getOutgoingEdgesOfKind(spSink, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
				SecurityNode npSink = (SecurityNode) out.getTarget();
				npSink.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
				if (DEBUG) debug.outln("Annotated node " + npSink + " as low sink.");
			}
		}
		
		return sdg;
	}
	
	private static class TestData {

		private final String mainClass;
		private final String sdgFile;

		public TestData(String mainClass, String sdgFile) {
			this.mainClass = mainClass;
			this.sdgFile = outputDir + File.separator + sdgFile;
		}

	}
}
