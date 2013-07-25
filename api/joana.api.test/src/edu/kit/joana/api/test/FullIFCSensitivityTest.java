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
public class FullIFCSensitivityTest {


	public static IFCAnalysis buildAndAnnotate(final String className) throws ApiTestException {
		return buildAndAnnotate(className, PointsToPrecision.CONTEXT_SENSITIVE);
	}
	
	public static IFCAnalysis buildWithThreadsAndAnnotate(final String className, MHPType mhpType) throws ApiTestException {
		return buildAndAnnotate(className, PointsToPrecision.CONTEXT_SENSITIVE, true, mhpType);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, PointsToPrecision pts) throws ApiTestException {
		return buildAndAnnotate(className, pts, false, MHPType.NONE);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, final PointsToPrecision pts,
			final boolean computeInterference, MHPType mhpType) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
		config.setComputeInterferences(computeInterference);
		config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(pts);
		config.setMhpType(mhpType);
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
		SDGProgramPart secret = ana.getProgramPart("sensitivity.Security.SECRET");
		assertNotNull(secret);
		ana.addSourceAnnotation(secret, BuiltinLattices.STD_SECLEVEL_HIGH);
		SDGProgramPart output = ana.getProgramPart("sensitivity.Security.leak(I)V");
		assertNotNull(output);
		ana.addSinkAnnotation(output, BuiltinLattices.STD_SECLEVEL_LOW);
		
		return ana;
	}
	
	@Test
	public void testFlowSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FlowSensLeak");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFlowSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FlowSensValid");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFieldSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FieldSensLeak");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.size() > 0);
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFieldSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.FieldSensValid");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testContextSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ContextSensLeak");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testContextSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ContextSensValid");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testObjectSensLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ObjectSensLeak", PointsToPrecision.OBJECT_SENSITIVE);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testObjectSensValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ObjectSensValid", PointsToPrecision.OBJECT_SENSITIVE);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testObjectSensValidFailOnContextSens() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.ObjectSensValid", PointsToPrecision.CONTEXT_SENSITIVE);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testTimeSensValid() {
		try {
			IFCAnalysis ana = buildWithThreadsAndAnnotate("sensitivity.TimeSensValid", MHPType.PRECISE);
			ana.setTimesensitivity(true);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testTimeSensLeak() {
		try {
			IFCAnalysis ana = buildWithThreadsAndAnnotate("sensitivity.TimeSensLeak", MHPType.PRECISE);
			ana.setTimesensitivity(true);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testLockSensValid() {
		try {
			IFCAnalysis ana = buildWithThreadsAndAnnotate("sensitivity.LockSensValid", MHPType.PRECISE);
			ana.setTimesensitivity(true);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty()); // with lock-sensitive IFC, this test will hopefully fail some day
			assertEquals(2, illegal.size()); 
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testKillingDefValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.KillingDefValid");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testKillingDefLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sensitivity.KillingDefLeak");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
