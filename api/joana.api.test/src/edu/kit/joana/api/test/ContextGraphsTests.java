/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.antlr.runtime.RecognitionException;
import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGManualParser;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ContextGraphsTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static void testBuildSuccessfull(Class<?> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		
		final String className = clazz.getCanonicalName();
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		
		SDGProgram p = BuildSDG.standardConcBuild(
			JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH,
			mainMethod,
			clazz.getCanonicalName() + ".ContextGraphsTests.pdg",
			PointsToPrecision.INSTANCE_BASED
		);
		
		final SDG sdg = p.getSDG();
		final Nanda nanda = new Nanda(sdg, new NandaBackward());
	}

	@Test
	public void testKnapsack5() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.Knapsack5.class);
	}
	
	@Test
	public void testKnapsack5Shrinked() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.shrinked.Knapsack5ExceptionControlFlow.class);
	}
	
	@Test
	public void testExceptionFallThrough() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.shrinked.ExceptionFallThrough.class);
	}
	
	@Test
	public void testExceptionFallThroughIntDiv() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.shrinked.ExceptionFallThroughIntDiv.class);
	}
	
	@Test
	public void testMutualRecursive() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(tests.MutualRecursive.class);
	}
	
	@Test
	public void testAlarmClock() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.ac.AlarmClock.class);
	}
	
	@Test
	public void testBB() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.bb.ProducerConsumer.class);
	}
	
	@Test
	public void testDP() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.dp.DiningPhilosophers.class);
	}
	
	@Test
	public void testDS() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.ds.DiskSchedulerDriver.class);
	}
	
	@Test
	public void testLG() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.lg.LaplaceGrid.class);
	}
	
	@Test
	public void testSQ() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.sq.QueueComponent.class);
	}
	
	@Test
	public void testDaisy() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.daisy.DaisyTest.class);
	}
	
	@Test
	public void testVolpano() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(tests.VolpanoSmith98Page3.class);
	}
	
	@Test
	public void testThreadSpawning() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(tests.ThreadSpawning.class);
	}
	
	@Test
	public void testThreadJoining() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(tests.ThreadJoining.class);
	}
	
	@Test
	public void testSynchronization() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(tests.Synchronization.class);
	}
	
	@Test
	public void testUnresolvedCallPWF() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.unresolvedcall.PrintWriterFlush.class);
	}
	
	@Test
	public void testUnresolvedCallSimple() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.unresolvedcall.Simple.class);
	}

	
	private static void testSDGFile(File file) throws IOException, RecognitionException {
		final SDG sdg = SDGManualParser.parse(
			new GZIPInputStream(
				new FileInputStream(file)
		));
		final Nanda nanda = new Nanda(sdg, new NandaBackward());
	}
	
	@Ignore @Test
	public void testHugeCFG() throws IOException, RecognitionException {
		testSDGFile(new File(JoanaPath.JOANA_API_TEST_DATA_GRAPHS+"/cfg/huge.cfg.pdg.gz"));
	}
	
}
