/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class FullIFCConcurrentTest {

	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut) throws ApiTestException {
		return buildAndAnnotate(className, secSrc, pubOut, PointsToPrecision.INSTANCE_BASED);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut, final PointsToPrecision pts) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
		config.setComputeInterferences(true);
		config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(pts);
		SDGProgram prog = null;
		
		try {
			prog = SDGProgram.createSDGProgram(config);
		} catch (ClassHierarchyException e) {
			throw new ApiTestException(e);
		} catch (IOException e) {
			throw new ApiTestException(e);
		} catch (UnsoundGraphException e) {
			throw new ApiTestException(e);
		} catch (CancelException e) {
			throw new ApiTestException(e);
		}
		
		IFCAnalysis ana = new IFCAnalysis(prog);
		SDGProgramPart secret = ana.getProgramPart(secSrc);
		assertNotNull(secret);
		ana.addSourceAnnotation(secret, BuiltinLattices.STD_SECLEVEL_HIGH);
		SDGProgramPart output = ana.getProgramPart(pubOut);
		assertNotNull(output);
		ana.addSinkAnnotation(output, BuiltinLattices.STD_SECLEVEL_LOW);
		
		return ana;
	}
	
	@Test
	public void testAlarmClock() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.ac.AlarmClock",
					"conc.ac.Clock.max",
					"conc.ac.Client.name");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(16, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testProducerConsumer() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.bb.ProducerConsumer",
					"conc.bb.BoundedBuffer.putIn",
					"conc.bb.BoundedBuffer.takeOut");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(12, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDaytimeClientServer() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.cliser.dt.Main",
					"conc.cliser.dt.DaytimeUDPClient.message",
					"conc.cliser.dt.DaytimeIterativeUDPServer.recieved");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(96, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testKnockKnock() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.cliser.kk.Main",
					"conc.cliser.kk.KnockKnockThread.message",
					"conc.cliser.kk.KnockKnockTCPClient.received1");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(180, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDaisy() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.daisy.DaisyTest",
					"conc.daisy.DaisyUserThread.iterations",
					"conc.daisy.DaisyDir.dirsize");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(8, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDiningPhilosophers() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.dp.DiningPhilosophers",
					"conc.dp.Philosopher.id",
					"conc.dp.DiningServer.state");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(160, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testDiskScheduler() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.ds.DiskSchedulerDriver",
					"conc.ds.DiskScheduler.position",
					"conc.ds.DiskReader.active");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(22, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testKnapsack() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.kn.Knapsack5",
					"conc.kn.Knapsack5$Item.profit",
					"conc.kn.PriorityRunQueue.numThreadsWaiting");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(176, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testLaplaceGrid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.lg.LaplaceGrid",
					"conc.lg.Partition.values",
					"conc.lg.Partition.in");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(1100, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testProbChannel() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.pc.ProbChannel",
					"conc.pc.ProbChannel.x",
					"sensitivity.Security.PUBLIC");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(12, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSharedQueue() {
		try {
			IFCAnalysis ana = buildAndAnnotate("conc.sq.SharedQueue",
					"conc.sq.SharedQueue.next",
					"conc.sq.Semaphore.count");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(280, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


	@Test
	public void testProbChannelMulti() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.probch.ProbChannel",
					"sensitivity.Security.SECRET",
					"tests.probch.ProbChannel$Data.a");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.LSOD, MHPType.SIMPLE);
			assertFalse(illegal.isEmpty());
			assertEquals(78, illegal.size());
			illegal = ana.doIFC(IFCType.LSOD, MHPType.PRECISE);
			assertFalse(illegal.isEmpty());
			assertEquals(75, illegal.size());
			illegal = ana.doIFC(IFCType.RLSOD, MHPType.SIMPLE);
			assertFalse(illegal.isEmpty());
			assertEquals(18, illegal.size());
			illegal = ana.doIFC(IFCType.RLSOD, MHPType.PRECISE);
			assertFalse(illegal.isEmpty());
			assertEquals(16, illegal.size());
			illegal = ana.doIFC(IFCType.CLASSICAL_NI);
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}


}
