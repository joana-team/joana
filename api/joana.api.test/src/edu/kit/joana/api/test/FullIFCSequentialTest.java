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
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class FullIFCSequentialTest {

	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut) throws ApiTestException {
		return buildAndAnnotate(className, secSrc, pubOut, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut, final PointsToPrecision pts, final ExceptionAnalysis exc) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(exc);
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
	public void testPraktomatValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sequential.PraktomatValid",
					"sequential.PraktomatValid$Submission.matrNr",
					"sequential.PraktomatValid$Review.failures");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPraktomatLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sequential.PraktomatLeak",
					"sequential.PraktomatLeak$Submission.matrNr",
					"sequential.PraktomatLeak$Review.failures");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(14, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeNoOpt() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(24, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeIntra() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(15, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeInter() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(12, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeIgnoreExc() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFirst() {
		try {
			IFCAnalysis ana = buildAndAnnotate("lob.First",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(6, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testTermination() {
		try {
			IFCAnalysis ana = buildAndAnnotate("term.A",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(6, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMain() {
		try {
			IFCAnalysis ana = buildAndAnnotate("Main",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.leak(I)V->p1");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testUtil() {
		try {
			IFCAnalysis ana = buildAndAnnotate("Util",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.leak(I)V->p1");
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(2, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingNoOpt() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(3, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingIntra() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(3, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingInter() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(3, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingIgnore() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestNoOpt() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(6, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestIntra() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(6, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestInter() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
			assertEquals(6, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestIgnore() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					"sensitivity.Security.SECRET",
					"sensitivity.Security.PUBLIC",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
			assertEquals(0, illegal.size());
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
