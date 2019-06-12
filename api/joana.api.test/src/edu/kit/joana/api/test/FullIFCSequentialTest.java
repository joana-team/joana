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

	private static IFCAnalysis buildAndAnnotate(final String className) throws ApiTestException {
		return buildAndAnnotate(className, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}

	private static IFCAnalysis buildAndAnnotate(String className,
			PointsToPrecision pts, ExceptionAnalysis exc) throws ApiTestException {
		return buildAndAnnotate(className, "sensitivity.Security.SECRET", "sensitivity.Security.PUBLIC", pts, exc);
	}

	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut) throws ApiTestException {
		return buildAndAnnotate(className, secSrc, pubOut, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}

	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut, final PointsToPrecision pts, final ExceptionAnalysis exc) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_15);
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

	private static void testLeaksFound(IFCAnalysis ana, int leaks) {
		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
		assertFalse(illegal.isEmpty());
		assertEquals(leaks, illegal.size());
	}

	private static void testPrecision(IFCAnalysis ana) {
		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
		assertTrue(illegal.isEmpty());
		assertEquals(0, illegal.size());
	}

	@Test
	public void testPraktomatValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sequential.PraktomatValid",
					"sequential.PraktomatValid$Submission.matrNr",
					"sequential.PraktomatValid$Review.failures");
			testPrecision(ana);
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
			testLeaksFound(ana, 8);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeNoOpt() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
			testLeaksFound(ana, 24);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeIntra() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 15);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeInter() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
			testLeaksFound(ana, 12);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionOptimizeIgnoreExc() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNegativeArraySizeException() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.Exception_NegativeArraySizeLeak",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFirst() {
		try {
			IFCAnalysis ana = buildAndAnnotate("lob.First");
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testTermination() {
		try {
			IFCAnalysis ana = buildAndAnnotate("term.A");
			testLeaksFound(ana, 6);
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
			testLeaksFound(ana, 2);
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
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingNoOpt() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
			testLeaksFound(ana, 3);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingIntra() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 3);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingInter() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
			testLeaksFound(ana, 3);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionHandlingIgnore() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestNoOpt() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestIntra() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestInter() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testExceptionTestIgnore() {
		try {
			IFCAnalysis ana = buildAndAnnotate("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHammerObjSens() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.Hammer",
					PointsToPrecision.OBJECT_SENSITIVE,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 4);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHammerTypeBased() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.Hammer",
					PointsToPrecision.TYPE_BASED,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHammerDistributedObjSens() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.HammerDistributed",
					PointsToPrecision.OBJECT_SENSITIVE,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 4);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHammerDistributedTypeBased() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.HammerDistributed",
					PointsToPrecision.TYPE_BASED,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 6);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHammerDistributed1Stack() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.HammerDistributed",
					PointsToPrecision.N1_CALL_STACK,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 4);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testHammerDistributed2Stack() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.HammerDistributed",
					PointsToPrecision.N2_CALL_STACK,
					ExceptionAnalysis.INTRAPROC);
			testLeaksFound(ana, 4);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testInputTest() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.InputTest");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPasswordFileValueBasedLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.PasswordFileValueBasedLeak");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPasswordFileNoLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.PasswordFileNoLeak");
			testPrecision(ana);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRecursive() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.Recursive");
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testImmutableAndStringBuilder() {
		try {
			IFCAnalysis ana = buildAndAnnotate("immutable.StringAppend",
					"immutable.StringAppend.high",
					"immutable.StringAppend.low",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			testLeaksFound(ana, 2);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
