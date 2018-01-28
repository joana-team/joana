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
import java.util.LinkedList;
import java.util.Map;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.api.test.util.SDGAnalyzer;
import edu.kit.joana.ifc.sdg.core.ClassifyingIFC;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.SecurityNode.SecurityNodeFactory;
import edu.kit.joana.ifc.sdg.core.conc.BarrierIFCSlicer;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PForward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;

/**
 * @author Martin Mohr &lt;martin.mohr@kit.edu&gt;
 */
public class IFCJoinTest {

	private static final boolean FORCE_REBUILD = true;
	private static final String outputDir = "out";
	
	static {
		File fOutDir = new File(outputDir);
		if (!fOutDir.exists()) {
			fOutDir.mkdir();
		}
	}

	private static final Map<String, TestData> testData = new HashMap<String, TestData>();

	static {
		addTestCase("joining1", "joana.api.testdata.conc.Joining1");
		addTestCase("joining2", "joana.api.testdata.conc.Joining2");
	}

	private static final void addTestCase(String testName, String mainClass) {
		testData.put(testName, new TestData(mainClass, testName + ".pdg"));
	}

	@BeforeClass
	public static void setUp() {
		for (String testKey : testData.keySet()) {
			final TestData td = testData.get(testKey);
			if (FORCE_REBUILD) {
				BuildSDG.standardConcBuild(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, td.mainClass,
						td.sdgFile);
			} else {
				final File f = new File(td.sdgFile);
				if (!f.exists() || !f.canRead()) {
					BuildSDG.standardConcBuild(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
							td.mainClass, td.sdgFile);
				}
			}
		}
	}

	@Test
	public void testJoining1() throws IOException {
		TestData tData = testData.get("joining1");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Collection<String> methNames = ana.collectAllMethodNames();
		Assert.assertFalse(methNames.isEmpty());
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.Joining1.main([Ljava/lang/String;)V"));
		Assert.assertNotEquals(null, ana.locateEntryOf("joana.api.testdata.conc.Joining1.main([Ljava/lang/String;)V"));
		Collection<SDGNode> coll = ana.collectModificationsAndAssignmentsInMethod(
				"joana.api.testdata.conc.Thread0.run()V", "Ljoana/api/testdata/conc/Joining1.f");
		Assert.assertFalse(coll.isEmpty());
		Assert.assertTrue(coll.size() == 1);
		Assert.assertEquals(240, coll.iterator().next().getId());
		Collection<SDGNode> calls = ana.collectCallsInMethod("joana.api.testdata.conc.Joining1.main([Ljava/lang/String;)V", "java.io.PrintStream.println(I)V");
		Assert.assertFalse(calls.isEmpty());
		Assert.assertTrue(calls.size() == 1);
		Assert.assertTrue(calls.iterator().next().getId() == 21);
		SDGNode call = calls.iterator().next();
		Collection<SDGNode> actualIns = new LinkedList<SDGNode>();
		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(call, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			if (e.getTarget().getKind() == SDGNode.Kind.ACTUAL_IN) {
				actualIns.add(e.getTarget());
			}
		}
		Assert.assertFalse(actualIns.isEmpty());

		for (SDGNode n : coll) {
			SecurityNode secN = (SecurityNode) n;
			secN.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
		}

		for (SDGNode n : actualIns) {
			SecurityNode secN = (SecurityNode) n;
			secN.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
		}

		
		ClassifyingIFC ifc = new ClassifyingIFC(new BarrierIFCSlicer(sdg, BuiltinLattices.getBinaryLattice()));
		Collection<ClassifiedViolation> vios = ifc.checkIFlow();
		Assert.assertFalse(vios.isEmpty());
		Assert.assertEquals(1, vios.size());
		ClassifiedViolation vio = vios.iterator().next();
		Collection<SDGNode> chop = computeSomeChop(sdg, vio.getSource(), vio.getSink());
		Assert.assertFalse(chop.isEmpty());
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		Assert.assertFalse(mhp.isParallel(sdg.getNode(138), sdg.getNode(19)));
	}
	
	@Test
	public void testJoining2() throws IOException {
		TestData tData = testData.get("joining2");
		SDG sdg = SDG.readFrom(tData.sdgFile, new SecurityNodeFactory());
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		Collection<String> methNames = ana.collectAllMethodNames();
		Assert.assertFalse(methNames.isEmpty());
		Assert.assertTrue(ana.isLocatable("joana.api.testdata.conc.ThreadA.run()V"));
		Assert.assertNotEquals(null, ana.locateEntryOf("joana.api.testdata.conc.ThreadA.run()V"));
		Collection<SDGNode> coll = ana.collectModificationsAndAssignmentsInMethod(
				"joana.api.testdata.conc.ThreadA.run()V", "Ljoana/api/testdata/conc/Joining2.f");
		Assert.assertFalse(coll.isEmpty());
		Assert.assertTrue(coll.size() == 1);
		int id1 = coll.iterator().next().getId();
		Assert.assertEquals(284, id1);
		
		Collection<SDGNode> calls = ana.collectCallsInMethod("joana.api.testdata.conc.ThreadB.run()V", "java.io.PrintStream.println(I)V");
		Assert.assertFalse(calls.isEmpty());
		Assert.assertTrue(calls.size() == 1);
		int id2 = calls.iterator().next().getId();
		Assert.assertEquals(371	, id2);
		SDGNode call = calls.iterator().next();
		Collection<SDGNode> actualIns = new LinkedList<SDGNode>();
		for (SDGEdge e : sdg.getOutgoingEdgesOfKind(call, SDGEdge.Kind.CONTROL_DEP_EXPR)) {
			if (e.getTarget().getKind() == SDGNode.Kind.ACTUAL_IN) {
				actualIns.add(e.getTarget());
			}
		}
		Assert.assertFalse(actualIns.isEmpty());

		for (SDGNode n : coll) {
			SecurityNode secN = (SecurityNode) n;
			secN.setProvided(BuiltinLattices.STD_SECLEVEL_HIGH);
		}

		for (SDGNode n : actualIns) {
			SecurityNode secN = (SecurityNode) n;
			secN.setRequired(BuiltinLattices.STD_SECLEVEL_LOW);
		}

		
		ClassifyingIFC ifc = new ClassifyingIFC(new BarrierIFCSlicer(sdg, BuiltinLattices.getBinaryLattice()));
		Collection<ClassifiedViolation> vios = ifc.checkIFlow();
		Assert.assertFalse(vios.isEmpty());
		Assert.assertEquals(1, vios.size());
		ClassifiedViolation vio = vios.iterator().next();
		Collection<SDGNode> chop = computeSomeChop(sdg, vio.getSource(), vio.getSink());
		Assert.assertFalse(chop.isEmpty());
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		Assert.assertEquals(1, sdg.getNode(id1).getThreadNumbers().length);
		Assert.assertEquals(1, sdg.getNode(id2).getThreadNumbers().length);
		int thread1 = sdg.getNode(id1).getThreadNumbers()[0];
		int thread2 = sdg.getNode(id2).getThreadNumbers()[0];
		Assert.assertNotEquals(thread1, thread2);
		
		System.out.println(mhp.getThreadRegion(sdg.getNode(id1), thread1));
		System.out.println(mhp.getThreadRegion(sdg.getNode(id2), thread2));
		Assert.assertFalse(mhp.isParallel(sdg.getNode(id1), sdg.getNode(id2)));
		
	}
	
	private static Collection<SDGNode> computeSomeChop(SDG sdg, SDGNode src, SDGNode snk) {
		I2PForward forw = new I2PForward(sdg);
		I2PBackward backw = new I2PBackward(sdg);
		Collection<SDGNode> ret = new LinkedList<SDGNode>();
		ret.addAll(forw.slice(src));
		ret.retainAll(backw.slice(snk));
		return ret;
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
