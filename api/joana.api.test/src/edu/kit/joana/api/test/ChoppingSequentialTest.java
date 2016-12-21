/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collection;
import java.util.Random;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Juergen Graf <graf@kit.edu>
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ChoppingSequentialTest {
	private static final int srceSize = 3;
	private static final int sinkSize = 3;
	
	private static final boolean chopsEqualFor(SDG sdg) {
		final RepsRosayChopper    reps = new RepsRosayChopper(sdg);
		final NonSameLevelChopper nslv = new NonSameLevelChopper(sdg);
		final Random r = new Random(42);
		final Collection<SDGNode> sources = sdg.getNRandomNodes(srceSize, r);
		final Collection<SDGNode> sinks   = sdg.getNRandomNodes(sinkSize, r);
		
		return reps.chop(sources, sinks).equals(nslv.chop(sources, sinks));
	}
	public static SDG build(final String className) throws ApiTestException {
		return build(className, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}
	
	public static SDG build(final String className, final PointsToPrecision pts, final ExceptionAnalysis exc) throws ApiTestException {
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
		
		return prog.getSDG();
	}
	
	private static void testChopsEqualFor(String className) {
		testChopsEqualFor(className, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}
	
	private static void testChopsEqualFor(final String className, final PointsToPrecision pts,
											final ExceptionAnalysis exc) {
		try {
			final SDG sdg = build(className, pts, exc);
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testPraktomatValid() {
		testChopsEqualFor("sequential.PraktomatValid");
	}
	
	@Test
	public void testPraktomatLeak() {
		testChopsEqualFor("sequential.PraktomatLeak");
	}

	@Test
	public void testExceptionOptimizeNoOpt() {
		testChopsEqualFor("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
	}

	@Test
	public void testExceptionOptimizeIntra() {
		testChopsEqualFor("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testExceptionOptimizeInter() {
		testChopsEqualFor("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
	}

	@Test
	public void testExceptionOptimizeIgnoreExc() {
		testChopsEqualFor("exc.ExceptionOptimize",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
	}

	@Test
	public void testFirst() {
		testChopsEqualFor("lob.First");
	}

	@Test
	public void testTermination() {
		testChopsEqualFor("term.A");
	}

	@Test
	public void testMain() {
		testChopsEqualFor("Main");
	}

	@Test
	public void testUtil() {
		testChopsEqualFor("Util");
	}

	@Test
	public void testExceptionHandlingNoOpt() {
		testChopsEqualFor("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
	}

	@Test
	public void testExceptionHandlingIntra() {
		testChopsEqualFor("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testExceptionHandlingInter() {
		testChopsEqualFor("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
	}

	@Test
	public void testExceptionHandlingIgnore() {
		testChopsEqualFor("exc.ExceptionHandling",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
	}

	@Test
	public void testExceptionTestNoOpt() {
		testChopsEqualFor("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.ALL_NO_ANALYSIS);
	}

	@Test
	public void testExceptionTestIntra() {
		testChopsEqualFor("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testExceptionTestInter() {
		testChopsEqualFor("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.INTERPROC);
	}

	@Test
	public void testExceptionTestIgnore() {
		testChopsEqualFor("exc.ExceptionTest",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
	}

	@Test
	public void testHammerObjSens() {
		testChopsEqualFor("tests.Hammer",
					PointsToPrecision.OBJECT_SENSITIVE,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testHammerTypeBased() {
		testChopsEqualFor("tests.Hammer",
					PointsToPrecision.TYPE_BASED,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testHammerDistributedObjSens() {
		testChopsEqualFor("tests.HammerDistributed",
					PointsToPrecision.OBJECT_SENSITIVE,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testHammerDistributedTypeBased() {
		testChopsEqualFor("tests.HammerDistributed",
					PointsToPrecision.TYPE_BASED,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testHammerDistributed1Stack() {
		testChopsEqualFor("tests.HammerDistributed",
					PointsToPrecision.N1_CALL_STACK,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testHammerDistributed2Stack() {
		testChopsEqualFor("tests.HammerDistributed",
					PointsToPrecision.N2_CALL_STACK,
					ExceptionAnalysis.INTRAPROC);
	}

	@Test
	public void testInputTest() {
		testChopsEqualFor("tests.InputTest");
	}

	@Test
	public void testPasswordFileValueBasedLeak() {
		testChopsEqualFor("tests.PasswordFileValueBasedLeak");
	}

	@Test
	public void testPasswordFileNoLeak() {
		testChopsEqualFor("tests.PasswordFileNoLeak");
	}

	@Test
	public void testRecursive() {
		testChopsEqualFor("tests.Recursive");
	}

	@Test
	public void testImmutableAndStringBuilder() {
		testChopsEqualFor("immutable.StringAppend",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
	}

}
