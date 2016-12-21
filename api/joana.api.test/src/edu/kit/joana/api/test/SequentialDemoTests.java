/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;

/**
 * Tests for testing the sequential part of the demo test cases
 * 
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class SequentialDemoTests {

	static final boolean outputPDGFiles = false;
	static final boolean outputGraphMLFiles = false;
	
	private static IFCAnalysis buildAnnotateDump(Class<?> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(clazz, BuildSDG.top_sequential, true);
	
		final String classname = clazz.getCanonicalName();
		
		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
		}
		
		return ana;
	}
	
	private static <T> void testSound(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		// There are leaks, and we're sound and hence report them
		IFCAnalysis ana = buildAnnotateDump(clazz);
	
		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.CLASSICAL_NI);
		assertFalse(illegal.isEmpty());
	}
	
	private static <T> void testPrecise(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		// There are no leak, and  we're precise enough to find out that there aren't
		IFCAnalysis ana = buildAnnotateDump(clazz);
		
		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.CLASSICAL_NI);
		assertTrue(illegal.isEmpty());
	}

	private static <T> void testTooImprecise(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		testSound(clazz);
	}

	@Test
	public void testAliasingLeaks() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.AliasingLeaks.class);
	}

	@Test
	public void testAliasLeak() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.AliasLeak.class);
	}

	@Test
	public void testAliasPrecise() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testPrecise(     joana.api.testdata.demo.AliasPrecise.class);
	}

	@Test
	public void testAliasTooImprecise() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testTooImprecise(joana.api.testdata.demo.AliasTooImprecise.class);
	}

	@Test
	public void testDynamicDispatch() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.DynamicDispatch.class);
	}

	@Test
	public void testExceptionLeak() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.ExceptionLeak.class);
	}

	@Test
	public void testExceptionTest() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.ExceptionTest.class);
	}

	@Test
	public void testExcTest() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.ExcTest.class);
	}

	@Test
	public void testFig2_1() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.Fig2_1.class);
	}

	@Test
	public void testFig3_1() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testPrecise(     joana.api.testdata.demo.Fig3_1.class);
	}

	@Test
	public void testFig3_2() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testPrecise(     joana.api.testdata.demo.Fig3_2.class);
	}

	@Test
	public void testInformationFlowLeaks() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.InformationFlowLeaks.class);
	}

	@Test
	public void testSequentialLeaks() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.SequentialLeaks.class);
	}

	@Test
	public void testStaticFieldSideEffects() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.StaticFieldSideEffects.class);
	}

	@Test
	public void testTypesAndObjectFields() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.TypesAndObjectFields.class);
	}
}
