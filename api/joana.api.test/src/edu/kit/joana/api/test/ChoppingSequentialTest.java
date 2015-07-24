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

import edu.kit.joana.api.IFCAnalysis;
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
	public static IFCAnalysis buildAndAnnotate(final String className) throws ApiTestException {
		return buildAndAnnotate(className, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, final PointsToPrecision pts, final ExceptionAnalysis exc) throws ApiTestException {
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
		
		return ana;
	}
	
	
	@Test
	public void testPraktomatValid() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sequential.PraktomatValid");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	@Test
	public void testPraktomatLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("sequential.PraktomatLeak");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testFirst() {
		try {
			IFCAnalysis ana = buildAndAnnotate("lob.First");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testTermination() {
		try {
			IFCAnalysis ana = buildAndAnnotate("term.A");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testMain() {
		try {
			IFCAnalysis ana = buildAndAnnotate("Main");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testUtil() {
		try {
			IFCAnalysis ana = buildAndAnnotate("Util");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
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
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testInputTest() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.InputTest");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPasswordFileValueBasedLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.PasswordFileValueBasedLeak");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testPasswordFileNoLeak() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.PasswordFileNoLeak");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testRecursive() {
		try {
			IFCAnalysis ana = buildAndAnnotate("tests.Recursive");
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testImmutableAndStringBuilder() {
		try {
			IFCAnalysis ana = buildAndAnnotate("immutable.StringAppend",
					PointsToPrecision.INSTANCE_BASED,
					ExceptionAnalysis.IGNORE_ALL);
			final SDG sdg = ana.getIFC().getSDG();
			assertTrue(chopsEqualFor(sdg));
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
