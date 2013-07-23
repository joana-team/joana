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
import edu.kit.joana.ifc.sdg.core.conc.ConflictScanner;
import edu.kit.joana.ifc.sdg.core.conc.ProbabilisticNISlicer;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * @author Martin Mohr
 */
public class ProbNITest {
	

	private static final boolean FORCE_REBUILD = true;

	private static final Map<String, TestData> testData = new HashMap<String, TestData>();
	
	private static final Logger debug = Log.getLogger("test.debug");
	
	private static final boolean DEBUG = debug.isEnabled();

	static {
		addTestCase("dataconf-rw-benign", "joana.api.testdata.conc.DataConflictRWBenign");
		addTestCase("dataconf-rw", "joana.api.testdata.conc.DataConflictRW");
		addTestCase("orderconf", "joana.api.testdata.conc.OrderConflict");
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
	
	
	public void testDataConflictRWBenign() throws IOException {
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
		
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		ConflictScanner checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), mhp, false, false);
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
	}
	
	@Test
	public void testDataConflictRWLSOD() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateDataConflictRW();
		//assertNoTraditionalLeaks(sdg);
		ConflictScanner checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IIllegalFlow v : vios) {
				debug.outln(v);
			}
		}
	}
	
	private void assertNoTraditionalLeaks(SDG sdg) {
		BarrierIFCSlicer tradIFC = new BarrierIFCSlicer(sdg, BuiltinLattices.getBinaryLattice());
		Collection<ClassifiedViolation> exImpVios = tradIFC.checkIFlow();
		Assert.assertTrue(exImpVios.isEmpty());
	}
	
	@Test
	public void testDataConflictRWLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateDataConflictRW();
		//assertNoTraditionalLeaks(sdg);
		ConflictScanner checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), true);
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IIllegalFlow v : vios) {
				debug.outln(v);
			}
		}
	}
	
	@Test
	public void testOrderConflictLSOD() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD ===");
		SDG sdg = annotateOrderConflict();
		//assertNoTraditionalLeaks(sdg);
		ConflictScanner checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), false);
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IIllegalFlow v : vios) {
				debug.outln(v);
			}
		}
	}
	
	@Test
	public void testOrderConflictLSODOpt() throws IOException {
		if (DEBUG) debug.outln("=== Mohr LSOD Opt. ===");
		SDG sdg = annotateOrderConflict();
		//assertNoTraditionalLeaks(sdg);
		ConflictScanner checker = LSODNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice(), true);
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IIllegalFlow v : vios) {
				debug.outln(v);
			}
		}
	}
	
	@Test
	public void testOrderConflictGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn LSOD ===");
		SDG sdg = annotateOrderConflict();
		//assertNoTraditionalLeaks(sdg);
		ConflictScanner checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IIllegalFlow v : vios) {
				debug.outln(v);
			}
		}
	}
	
	@Test
	public void testDataConflictRWGiffhorn() throws IOException {
		if (DEBUG) debug.outln("=== Giffhorn ===");
		SDG sdg = annotateDataConflictRW();
		//assertNoTraditionalLeaks(sdg);
		ConflictScanner checker = ProbabilisticNISlicer.simpleCheck(sdg, BuiltinLattices.getBinaryLattice());
		Collection<ClassifiedViolation> vios = checker.check();
		Assert.assertFalse(vios.isEmpty());
		if (DEBUG) {
			for (IIllegalFlow v : vios) {
				debug.outln(v);
			}
		}
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
	
	private static class TestData {

		private final String mainClass;
		private final String sdgFile;

		public TestData(String mainClass, String sdgFile) {
			this.mainClass = mainClass;
			this.sdgFile = sdgFile;
		}

	}
}
