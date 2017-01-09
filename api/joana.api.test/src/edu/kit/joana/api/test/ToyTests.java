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
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ToyTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static IFCAnalysis buildAnnotateDump(Class<?> clazz, boolean ignore) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(clazz, BuildSDG.top_sequential, ignore);
		
		final String filename = clazz.getCanonicalName()
								+ (ignore ? ".ignore" : ".passon")
								+ ".pdg";
		
		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), filename);
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), filename);
		}
		
		return ana;
	}
		
	@Deprecated
	public static <T> IFCAnalysis buildAndAnnotate(Class<T> clazz, SDGConfig config, boolean ignore)
			throws ApiTestException, ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ana = BuildSDG.build(clazz,config,ignore);
		SDGProgramPart secret = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET");
		SDGProgramPart secret_string = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET_STRING");
		SDGProgramPart secret_bool = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET_BOOL");
		SDGProgramPart secret_object = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET_OBJECT");

		SDGProgramPart output = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(I)V");
		SDGProgramPart output_string = ana
				.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(Ljava/lang/String;)V");
		SDGProgramPart output_bool = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(Z)V");
		SDGProgramPart output_object = ana
				.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(Ljava/lang/Object;)V");

		assertTrue(secret != null || secret_string != null || secret_bool != null || secret_object != null);
		assertTrue(output != null || output_string != null || output_bool != null || output_object != null);

		if (secret != null)
			ana.addSourceAnnotation(secret, BuiltinLattices.STD_SECLEVEL_HIGH);
		if (secret_string != null)
			ana.addSourceAnnotation(secret_string, BuiltinLattices.STD_SECLEVEL_HIGH);
		if (secret_bool != null)
			ana.addSourceAnnotation(secret_bool, BuiltinLattices.STD_SECLEVEL_HIGH);
		if (secret_object != null)
			ana.addSourceAnnotation(secret_object, BuiltinLattices.STD_SECLEVEL_HIGH);

		if (output != null)
			ana.addSinkAnnotation(output, BuiltinLattices.STD_SECLEVEL_LOW);
		if (output_string != null)
			ana.addSinkAnnotation(output_string, BuiltinLattices.STD_SECLEVEL_LOW);
		if (output_bool != null)
			ana.addSinkAnnotation(output_bool, BuiltinLattices.STD_SECLEVEL_LOW);
		if (output_object != null)
			ana.addSinkAnnotation(output_object, BuiltinLattices.STD_SECLEVEL_LOW);
		return ana;
	}

	private static void testPreciseEnough(Class<?> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		{ // There are leaks if secret is really passed on
			IFCAnalysis ana = buildAnnotateDump(clazz, false);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			//System.out.println(illegal);
			assertFalse(illegal.isEmpty());
		}

		{ // Otherwise, we're precise enough to find out that there aren't
			IFCAnalysis ana = buildAnnotateDump(clazz, true);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
		}
	}

	private static void testTooImprecise(Class<?> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		{ // There are leaks if secret is really passed on
			IFCAnalysis ana = buildAnnotateDump(clazz, false);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}

		{ // Otherwise there aren't, but the analysis not precise enough to
			IFCAnalysis ana = buildAnnotateDump(clazz, true);

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}
	}

	@Test
	public void testFlowSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.sensitivity.FlowSens.class);
	}

	@Test
	public void testAssChain() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.simp.AssChain.class);
	}

	@Test
	public void testMicroExample() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.toy.simp.MicroExample.class);
	}

	@Test
	public void testNested() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.simp.Nested.class);
	}

	@Test
	public void testNestedWithException() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise(joana.api.testdata.toy.simp.NestedWithException.class);
	}

	@Test
	public void testSick() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.simp.Sick.class);
	}

	@Test
	public void testSick2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.simp.Sick2.class);
	}

	// we're not precise enough here because JOANA thinks Math.round can throw an exception.
	// TODO: find out why
	@Test
	public void testMathRound() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise(joana.api.testdata.toy.simp.MathRound.class);
	}

	@Test
	public void testControlDep() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.test.ControlDep.class);
	}

	@Test
	public void testIndependent() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.test.Independent.class);
	}

	@Test
	public void testObjSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.test.ObjSens.class);
	}

	@Test
	public void testSystemCallsTest() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.toy.test.SystemCallsTest.class);
	}

	@Test
	public void testVeryImplictFlow() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.toy.test.VeryImplictFlow.class);
	}

	@Test
	public void testMyList() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough(joana.api.testdata.toy.rec.MyList.class);
	}

	// we're precise enough for MyList, but not for MyList2 because JOANA thinks
	// only MyList2.add can throw an exception (see also comments in MyList2.main).
	// TODO: find out why (maybe because of recursion?)
	@Test
	public void testMyList2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise(joana.api.testdata.toy.rec.MyList2.class);
	}

	@Test
	public void testPasswordFile() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.toy.pw.PasswordFile.class);
	}

	@Test
	public void testDemo1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		/**
		 * We are to imprecise at the moment (Dec 2012) to rule out information flow here in the 'ignore' case.
		 * See Demo1 source code for further information
		 */
		testTooImprecise(joana.api.testdata.toy.demo.Demo1.class);
	}

	@Test
	public void testDeclass1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise(joana.api.testdata.toy.declass.Declass1.class);
	}

	@Test
	public void testExampleLeakage() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.seq.ExampleLeakage.class);
	}
	
	@Test
	public void testArrayAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.seq.ArrayAccess.class);
	}
	
	@Test
	public void testFieldAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.seq.FieldAccess.class);
	}
	@Test
	public void testFieldAccess2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.seq.FieldAccess2.class);
	}
	
	@Test
	public void testFieldAccess3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.seq.FieldAccess3.class);
	}
	
	@Test
	public void testConstants1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough(joana.api.testdata.seq.Constants1.class);
	}
	
	@Test
	public void testConstants2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testTooImprecise(joana.api.testdata.seq.Constants2.class);
	}
	
	// TODO: This should crash when we turn on reflection
	@Test
	public void testWalaBugReflection() throws ClassHierarchyException, ApiTestException, IOException,	UnsoundGraphException, CancelException {
		BuildSDG.build(joana.api.testdata.toy.test.Reflection.class,BuildSDG.bottom_sequential, false);
	}
	
}
