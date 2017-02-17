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
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.PrintStream;
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
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class LibraryPruningTest {

	private PrintStream out = null;
	
	public static void main(String[] args) {
		LibraryPruningTest test = new LibraryPruningTest();
		test.setOutput(System.out);
		test.testGuiPruneExtended();
	}

	public void setOutput(final PrintStream out) {
		this.out = out;
	}

	public IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut) throws ApiTestException {
		return buildAndAnnotate(className, secSrc, pubOut, PointsToPrecision.INSTANCE_BASED, ExceptionAnalysis.INTRAPROC);
	}

	public IFCAnalysis buildAndAnnotate(final String className, final String secSrc,
			final String pubOut, final PointsToPrecision pts, final ExceptionAnalysis exc) throws ApiTestException {
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		SDGConfig config = new SDGConfig(JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH, mainMethod.toBCString(), Stubs.JRE_14);
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(exc);
		config.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(pts);
		config.setExclusions(""
//				+ "java\\/awt\\/.*\n"
//				+ "javax\\/swing\\/.*\n"
				+ "java\\/nio\\/.*\n"
				+ "java\\/net\\/.*\n"
				+ "sun\\/awt\\/.*\n"
				+ "sun\\/swing\\/.*\n"
				+ "com\\/sun\\/.*\n"
				+ "sun\\/.*\n"
				+ "apple\\/awt\\/.*\n"
				+ "com\\/apple\\/.*\n"
				+ "org\\/omg\\/.*\n"
//				+ "javax\\/.*\n"
		);
		//config.setPruningPolicy(DoNotPrune.INSTANCE);
		config.setStubsPath(Stubs.JRE_15);
		SDGProgram prog = null;
	
		try {
			prog = (out == null
				? SDGProgram.createSDGProgram(config)
				: SDGProgram.createSDGProgram(config, out, NullProgressMonitor.INSTANCE));
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
	public void testGuiPrune() {
		try {
			IFCAnalysis ana = buildAndAnnotate("prune.TestGuiPrune",
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

	// You need to increase stack size  (-Xss16m is enough) and heap size (-Xmx2048m is enough) of the java vm in order
	// for this test to work. 
	@Test
	public void testGuiPruneExtended() {
		try {
			IFCAnalysis ana = buildAndAnnotate("prune.TestGuiPruneExtended",
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

}
