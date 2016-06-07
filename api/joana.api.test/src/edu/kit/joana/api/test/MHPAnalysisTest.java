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
import org.junit.Test;

import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.api.test.util.SDGAnalyzer;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.SimpleMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.util.Pair;

/**
 * Class for testing our MHP analyses (mostly {@link PreciseMHPAnalysis}).
 * 
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class MHPAnalysisTest {

	private static final boolean FORCE_REBUILD = true;
	
	private static final String TEST_PACKAGE = "joana.api.testdata.conc.";
	
	private static final String TEST_CLASSNAME = "Ljoana/api/testdata/conc/";

	private static final Map<String, TestData> testData = new HashMap<String, TestData>();

	static {
		addTestCase("dataconf-rw-benign", "joana.api.testdata.conc.DataConflictRWBenign");
		addTestCase("dataconf-rw", "joana.api.testdata.conc.DataConflictRW");
		addTestCase("dataconf-rw-nomhp", "joana.api.testdata.conc.DataConflictRWNoMHP");
		addTestCase("nodataconf-rw-nomhp", "joana.api.testdata.conc.NoDataConflictRWNoMHP");
		addTestCase("sequential-spawn", "joana.api.testdata.conc.SequentialSpawn");
		addTestCase("branched-spawn", "joana.api.testdata.conc.BranchedSpawn");
		addTestCase("both-branches-spawn", "joana.api.testdata.conc.BothBranchesSpawn");
		addTestCase("dynamic-spawn", "joana.api.testdata.conc.DynamicSpawn");
		addTestCase("more-recursive-spawn", "joana.api.testdata.conc.MoreRecursiveSpawn");
		addTestCase("interproc-join", "joana.api.testdata.conc.InterprocJoin");
		addTestCase("fork-join", "joana.api.testdata.conc.ForkJoin");
	}

	private static final void addTestCase(String testName, String mainClass) {
		testData.put(testName, new TestData(mainClass, testName + ".pdg"));
	}

	public static SDG buildOrLoad(String key) {
		TestData t = testData.get(key);
		if (t == null) {
			Assert.fail("wrong test key: " + key);
		}
		if (FORCE_REBUILD) {
			final BuildSDG b = BuildSDG.standardConcSetup(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, t.mainClass,
					t.sdgFile);
			b.run();
		} else {
			final File f = new File(t.sdgFile);
			if (!f.exists() || !f.canRead()) {
				final BuildSDG b = BuildSDG.standardConcSetup(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
						t.mainClass, t.sdgFile);
				b.run();
			}
		}
		SDG result = null;
		try {
			result = SDG.readFrom(t.sdgFile);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Could not read SDG for test key " + key);
		}
		return result;
	}
	
	/*
	 * wrapper method used to assert that for two nodes that may happen in parallel,
	 * the MHP analysis will indeed show that they may happen in parallel
	 */
	private void checkSoundness(MHPAnalysis mhp, SDGNode m, SDGNode n) {
		Assert.assertTrue(mhp.isParallel(m, n));
	}
	
	/*
	 * wrapper method used only for our simple MHP analysis.
	 * It is used when both nodes are in different threads, but semantically
	 * may not happen in parallel (which a more precise analysis might show).
	 * In this case we require the simple MHP analysis to find that the nodes
	 * may happen in parallel due to its specification.
	 * 
	 */
	private void checkSimpleSoundness(MHPAnalysis mhp, SDGNode m, SDGNode n) {
		Assert.assertTrue(mhp.isParallel(m, n));
	}
	
	/*
	 * wrapper method used to assert that for two nodes that may not happen in parallel,
	 * the MHP analysis is precise enough to show that they may not happen in parallel
	 */
	private void checkPrecision(MHPAnalysis mhp, SDGNode m, SDGNode n) {
		Assert.assertFalse(mhp.isParallel(m, n));
	}
	
	/*
	 * wrapper method used when two nodes may not happen in parallel,
	 * but the MHP analysis is currently too imprecise to show this.
	 * Hopefully, such a test case will fail some day
	 */
	private void checkTooImprecise(MHPAnalysis mhp, SDGNode m, SDGNode n) {
		Assert.assertTrue(mhp.isParallel(m, n));
	}
	
	@Test
	public void testDataConflictRWBenign() {
		SDG sdg = buildOrLoad("dataconf-rw-benign");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "DataConflictRWBenign$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "DataConflictRWBenign$Thread2.run()V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkSoundness(mhp, p1, p2);
	}
	
	@Test
	public void testDataConflictRW() {
		SDG sdg = buildOrLoad("dataconf-rw");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode n = getAssignmentInMethod(ana, "DataConflictRW$Thread1.run()V", "DataConflictRW.x");
		SDGNode p = getIntPrintInMethod(ana, "DataConflictRW$Thread2.run()V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkPrecision(mhp, n, p);
	}
	
	@Test
	public void testDataConflictRWNoMhp() {
		SDG sdg = buildOrLoad("dataconf-rw-nomhp");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode n = getAssignmentInMethod(ana, "DataConflictRWNoMHP.main([Ljava/lang/String;)V",
											"DataConflictRWNoMHP.x");
		SDGNode p = getIntPrintInMethod(ana, "DataConflictRWNoMHP$Thread2.run()V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkPrecision(mhp, n, p);
	}
	
	@Test
	public void testNoDataConflictRWNoMhp() {
		SDG sdg = buildOrLoad("nodataconf-rw-nomhp");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p = getIntPrintInMethod(ana, "NoDataConflictRWNoMHP$Thread2.run()V");
		SDGNode n = getAssignmentInMethod(ana, "NoDataConflictRWNoMHP.main([Ljava/lang/String;)V",
											"NoDataConflictRWNoMHP.x");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkPrecision(mhp, n, p);
	}
	
	@Test
	public void testSequentialSpawn() {
		SDG sdg = buildOrLoad("sequential-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode mx = getAssignmentInMethod(ana, "SequentialSpawn.main([Ljava/lang/String;)V",
												"SequentialSpawn.x");
		SDGNode p1 = getStringPrintInMethod(ana, "SequentialSpawn$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "SequentialSpawn$Thread2.run()V");
		SDGNode my = getAssignmentInMethod(ana, "SequentialSpawn.main([Ljava/lang/String;)V",
												"SequentialSpawn.y");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkPrecision(mhp, mx, p1);
		checkPrecision(mhp, mx, p2);
		checkPrecision(mhp, mx, my);
		checkSoundness(mhp, p1, p2);
		checkPrecision(mhp, p1, my);
		checkSoundness(mhp, p2, my);
	}
	
	@Test
	public void testBranchedSpawn() {
		SDG sdg = buildOrLoad("branched-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "BranchedSpawn$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "BranchedSpawn$Thread2.run()V");
		SDGNode p3 = getStringPrintInMethod(ana, "BranchedSpawn$Thread3.run()V");
		SDGNode p4 = getStringPrintInMethod(ana, "BranchedSpawn$Thread4.run()V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkSoundness(mhp, p1, p2);
		checkPrecision(mhp, p1, p3);
		checkSoundness(mhp, p1, p4);
		checkPrecision(mhp, p2, p3);
		checkSoundness(mhp, p2, p4);
		checkSoundness(mhp, p3, p4);
	}
	
	@Test
	public void testBothBranchesSpawn() {
		SDG sdg = buildOrLoad("both-branches-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "BothBranchesSpawn$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "BothBranchesSpawn$Thread2.run()V");
		SDGNode ps = getStringPrintInMethod(ana, "BothBranchesSpawn.main([Ljava/lang/String;)V");
		SDGNode p3 = getStringPrintInMethod(ana, "BothBranchesSpawn$Thread3.run()V");
		SDGNode p4 = getStringPrintInMethod(ana, "BothBranchesSpawn$Thread4.run()V");
		SDGNode pi = getIntPrintInMethod(ana, "BothBranchesSpawn.main([Ljava/lang/String;)V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkPrecision(mhp, p1, p1);
		checkSoundness(mhp, p1, p2);
		checkSoundness(mhp, p1, ps);
		checkSoundness(mhp, p1, p3);
		checkSoundness(mhp, p1, p4);
		checkSoundness(mhp, p1, pi);
		checkPrecision(mhp, p2, p2);
		checkTooImprecise(mhp, p2, ps);
		checkTooImprecise(mhp, p2, p3);
		checkTooImprecise(mhp, p2, p4);
		checkTooImprecise(mhp, p2, pi);
		checkPrecision(mhp, ps, ps);
		checkPrecision(mhp, ps, p3);
		checkPrecision(mhp, ps, p4);
		checkPrecision(mhp, ps, pi);
		checkPrecision(mhp, p3, p3);
		checkTooImprecise(mhp, p3, p4);
		checkTooImprecise(mhp, p3, pi);
		checkPrecision(mhp, p4, p4);
		checkSoundness(mhp, p4, pi);
		checkPrecision(mhp, pi, pi);
	}
	
	@Test
	public void testDynamicSpawn() {
		SDG sdg = buildOrLoad("dynamic-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "DynamicSpawn$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "DynamicSpawn$Thread2.run()V");
		SDGNode p3 = getStringPrintInMethod(ana, "DynamicSpawn$Thread3.run()V");
		SDGNode p4 = getStringPrintInMethod(ana, "DynamicSpawn$Thread4.run()V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkSoundness(mhp, p1, p1);
		checkSoundness(mhp, p1, p2);
		checkSoundness(mhp, p1, p3);
		checkSoundness(mhp, p1, p4);
		checkSoundness(mhp, p2, p2);
		checkPrecision(mhp, p2, p3);
		checkPrecision(mhp, p2, p4);
		checkSoundness(mhp, p3, p3);
		checkSoundness(mhp, p3, p4);
		checkSoundness(mhp, p4, p4);
	}
	
	@Test
	public void testDynamicSpawnSimpleMHP() {
		SDG sdg = buildOrLoad("dynamic-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "DynamicSpawn$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "DynamicSpawn$Thread2.run()V");
		SDGNode p3 = getStringPrintInMethod(ana, "DynamicSpawn$Thread3.run()V");
		SDGNode p4 = getStringPrintInMethod(ana, "DynamicSpawn$Thread4.run()V");
		MHPAnalysis mhp = SimpleMHPAnalysis.analyze(sdg);
		checkSoundness(mhp, p1, p1);
		checkSoundness(mhp, p1, p2);
		checkSoundness(mhp, p1, p3);
		checkSoundness(mhp, p1, p4);
		checkSoundness(mhp, p2, p2);
		checkSimpleSoundness(mhp, p2, p3);
		checkSimpleSoundness(mhp, p2, p4);
		checkSoundness(mhp, p3, p3);
		checkSoundness(mhp, p3, p4);
		checkSoundness(mhp, p4, p4);
	}
	
	@Test
	public void testRecursiveSpawn() {
		SDG sdg = buildOrLoad("more-recursive-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "MoreRecursiveSpawn$Thread1.run()V");
		Pair<SDGNode,SDGNode> pair2 = getPrintsInRecursiveMethod(sdg, ana, "MoreRecursiveSpawn$Thread2.run()V");
		SDGNode p2 = pair2.getFirst();
		SDGNode p2a = pair2.getSecond();
		Pair<SDGNode,SDGNode> pair3 = getPrintsInRecursiveMethod(sdg, ana, "MoreRecursiveSpawn$Thread3.run()V");
		SDGNode p3 = pair3.getFirst();
		SDGNode p3a = pair3.getSecond();
		SDGNode p4 = getStringPrintInMethod(ana, "MoreRecursiveSpawn$Thread4.run()V");
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		checkSoundness(mhp, p1, p1);
		checkSoundness(mhp, p1, p2);
		checkSoundness(mhp, p1, p2a);
		checkSoundness(mhp, p1, p3);
		checkSoundness(mhp, p1, p3a);
		checkSoundness(mhp, p1, p4);
		checkPrecision(mhp, p2, p2);
		checkSoundness(mhp, p2, p2a);
		checkPrecision(mhp, p2, p3);
		checkPrecision(mhp, p2, p3a);
		checkPrecision(mhp, p2, p4);
		checkSoundness(mhp, p2a, p2a);
		checkPrecision(mhp, p2a, p3);
		checkPrecision(mhp, p2a, p3a);
		checkPrecision(mhp, p2a, p4);
		checkPrecision(mhp, p3, p3);
		checkSoundness(mhp, p3, p3a);
		checkSoundness(mhp, p3, p4);
		checkSoundness(mhp, p3a, p3a);
		checkSoundness(mhp, p3a, p4);
		checkSoundness(mhp, p4, p4);
	}
	
	@Test
	public void testRecursiveSpawnSimpleMHP() {
		SDG sdg = buildOrLoad("more-recursive-spawn");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "MoreRecursiveSpawn$Thread1.run()V");
		Pair<SDGNode,SDGNode> pair2 = getPrintsInRecursiveMethod(sdg, ana, "MoreRecursiveSpawn$Thread2.run()V");
		SDGNode p2 = pair2.getFirst();
		SDGNode p2a = pair2.getSecond();
		Pair<SDGNode,SDGNode> pair3 = getPrintsInRecursiveMethod(sdg, ana, "MoreRecursiveSpawn$Thread3.run()V");
		SDGNode p3 = pair3.getFirst();
		SDGNode p3a = pair3.getSecond();
		SDGNode p4 = getStringPrintInMethod(ana, "MoreRecursiveSpawn$Thread4.run()V");
		MHPAnalysis mhp = SimpleMHPAnalysis.analyze(sdg);
		checkSoundness(mhp, p1, p1);
		checkSoundness(mhp, p1, p2);
		checkSoundness(mhp, p1, p2a);
		checkSoundness(mhp, p1, p3);
		checkSoundness(mhp, p1, p3a);
		checkSoundness(mhp, p1, p4);
		checkPrecision(mhp, p2, p2);
		checkSoundness(mhp, p2, p2a);
		checkSimpleSoundness(mhp, p2, p3);
		checkSimpleSoundness(mhp, p2, p3a);
		checkSimpleSoundness(mhp, p2, p4);
		checkSoundness(mhp, p2a, p2a);
		checkSimpleSoundness(mhp, p2a, p3);
		checkSimpleSoundness(mhp, p2a, p3a);
		checkSimpleSoundness(mhp, p2a, p4);
		checkPrecision(mhp, p3, p3);
		checkSoundness(mhp, p3, p3a);
		checkSoundness(mhp, p3, p4);
		checkSoundness(mhp, p3a, p3a);
		checkSoundness(mhp, p3a, p4);
		checkSoundness(mhp, p4, p4);
	}
	
	@Test
	public void testInterprocJoin() {
		SDG sdg = buildOrLoad("interproc-join");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		
		//subtest 1
		SDGNode p1 = getStringPrintInMethod(ana, "InterprocJoin$Thread1.run()V");
		SDGNode pz = getPrintInMethod(ana, "InterprocJoin.main([Ljava/lang/String;)V", "Z");
		checkSoundness(mhp, p1, pz);

		//subtest 2
		SDGNode p2 = getStringPrintInMethod(ana, "InterprocJoin$Thread2.run()V");
		SDGNode pi = getIntPrintInMethod(ana, "InterprocJoin.main([Ljava/lang/String;)V");
		checkSoundness(mhp, p2, pi);

		//subtest 3
		SDGNode p3 = getStringPrintInMethod(ana, "InterprocJoin$Thread3.run()V");
		SDGNode ps = getStringPrintInMethod(ana, "InterprocJoin.main([Ljava/lang/String;)V");
		checkPrecision(mhp, p3, ps);
	}
	
	@Test
	public void testForkJoin() {
		SDG sdg = buildOrLoad("fork-join");
		SDGAnalyzer ana = new SDGAnalyzer(sdg);
		MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		SDGNode p1 = getStringPrintInMethod(ana, "ForkJoin$Thread1.run()V");
		SDGNode p2 = getStringPrintInMethod(ana, "ForkJoin$Thread2.run()V");
		SDGNode ps = getStringPrintInMethod(ana, "ForkJoin.main([Ljava/lang/String;)V");
		checkPrecision(mhp, p1, p1);
		checkSoundness(mhp, p1, p2);
		checkSoundness(mhp, p1, ps);
		checkSoundness(mhp, p2, p2);
		checkSoundness(mhp, p2, ps);
		checkPrecision(mhp, ps, ps);
	}

	private SDGNode getIntPrintInMethod(SDGAnalyzer ana, String shortName) {
		return getPrintInMethod(ana, shortName, "I");
	}

	private SDGNode getStringPrintInMethod(SDGAnalyzer ana, String shortName) {
		return getPrintInMethod(ana, shortName, "Ljava/lang/String;");
	}
	
	private SDGNode getPrintInMethod(SDGAnalyzer ana, String shortName, String printParam) {
		String methodName = TEST_PACKAGE + shortName;
		Assert.assertTrue(ana.isLocatable(methodName));
		Collection<SDGNode> prints = ana.collectAllCallsInMethods(methodName,
													"java.io.PrintStream.println("+printParam+")V");
		Assert.assertNotNull(prints);
		Assert.assertEquals(1, prints.size());
		return (SDGNode) prints.toArray()[0];
	}
	
	private Pair<SDGNode,SDGNode> getPrintsInRecursiveMethod(SDG sdg, SDGAnalyzer ana, String shortName) {
		String methodName = TEST_PACKAGE + shortName;
		Assert.assertTrue(ana.isLocatable(methodName));
		Collection<SDGNode> prints = ana.collectAllCallsInMethods(methodName,
													"java.io.PrintStream.println(Ljava/lang/String;)V");
		Assert.assertNotNull(prints);
		Assert.assertEquals(2, prints.size());
		SDGNode[] arr = prints.toArray(new SDGNode[2]);
		SDGNode n1 = arr[0];
		ThreadInstance t1 = sdg.getThreadsInfo().getThread(n1.getThreadNumbers()[0]);
		SDGNode n2 = arr[1];
		ThreadInstance t2 = sdg.getThreadsInfo().getThread(n2.getThreadNumbers()[0]);
		Pair<SDGNode,SDGNode> p = null;
		//TODO: make more efficient by checking whether one is a suffix of the other
		//instead of just using containsAll
		if (t1.getThreadContext().containsAll(t2.getThreadContext())) {
			p = Pair.<SDGNode,SDGNode>pair(n2, n1);
		} else if (t2.getThreadContext().containsAll(t1.getThreadContext())) {
			p = Pair.<SDGNode,SDGNode>pair(n1, n2);
		} else {
			Assert.fail("No recursive thread instance detected");
		}
		return p;
	}

	private SDGNode getAssignmentInMethod(SDGAnalyzer ana,
										String shortMethodName, String shortVarName) {
		String varName = TEST_CLASSNAME + shortVarName;
		String methodName = TEST_PACKAGE + shortMethodName;
		Assert.assertTrue(ana.isLocatable(methodName));
		Collection<SDGNode> statements =
				ana.collectModificationsAndAssignmentsInMethod(methodName, varName);
		Assert.assertNotNull(statements);
		Assert.assertEquals(1, statements.size());
		return (SDGNode) statements.toArray()[0];
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
