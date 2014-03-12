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
		return buildAndAnnotate(className, secSrc, pubOut, PointsToPrecision.INSTANCE_BASED);
	}
	
	public static IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut, PointsToPrecision pts) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
		config.setComputeInterferences(false);
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

}
