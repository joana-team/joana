/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ControlDependenceVariant;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ControlDependenceTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static final Stubs STUBS = Stubs.JRE_14;
	
	public static final SDGConfig classic = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		classic.setControlDependenceVariant(ControlDependenceVariant.CLASSIC);
	}

	public static final SDGConfig ntscd = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		ntscd.setControlDependenceVariant(ControlDependenceVariant.NTSCD);
	}
	
	private static IFCAnalysis buildAnnotateDump(Class<?> clazz, SDGConfig config) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(clazz,config, false);
		
		final String filename = clazz.getCanonicalName()
			+ "-" + config.getControlDependenceVariant()
			+ ".pdg";
		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), filename);
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), filename);
		}
		
		return ana;
	}
		

	private static void testCDGSame(Class<?> clazz) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		final IFCAnalysis anaClassic = buildAnnotateDump(clazz, classic);
		final IFCAnalysis anaNTSCD = buildAnnotateDump(clazz, ntscd);

		final CFG cfgClassic = ICFGBuilder.extractICFG(anaClassic.getProgram().getSDG());
		final CFG cfgCNTSCD = ICFGBuilder.extractICFG(anaNTSCD.getProgram().getSDG());
		assertEquals(cfgClassic, cfgCNTSCD);
	}
	
	private static void testClassicUnbuildable(Class<?> clazz) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		try {
			IFCAnalysis anaClassic = buildAnnotateDump(clazz, classic);
			assertTrue("should've thrown!!!", false);
		} catch (IllegalStateException e) {
			assertTrue("Unexpected exception:" + e.toString(), e.toString().startsWith("java.lang.IllegalStateException: Null node at dfsW="));
		}
		IFCAnalysis anaNTSCD = buildAnnotateDump(clazz, ntscd);
	}

	@Test
	public void testFlowSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.sensitivity.FlowSens.class);
	}

	@Test
	public void testAssChain() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.simp.AssChain.class);
	}

	@Test
	public void testMicroExample() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.toy.simp.MicroExample.class);
	}

	@Test
	public void testNested() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.simp.Nested.class);
	}

	@Test
	public void testNestedWithException() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.simp.NestedWithException.class);
	}

	@Test
	public void testSick() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.simp.Sick.class);
	}

	@Test
	public void testSick2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.simp.Sick2.class);
	}

	@Test
	public void testMathRound() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.simp.MathRound.class);
	}

	@Test
	public void testControlDep() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.test.ControlDep.class);
	}

	@Test
	public void testIndependent() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.test.Independent.class);
	}

	@Test
	public void testObjSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.test.ObjSens.class);
	}

	@Test
	public void testSystemCallsTest() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.toy.test.SystemCallsTest.class);
	}

	@Test
	public void testVeryImplictFlow() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.toy.test.VeryImplictFlow.class);
	}

	@Test
	public void testMyList() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.rec.MyList.class);
	}

	// we're precise enough for MyList, but not for MyList2 because JOANA thinks
	// only MyList2.add can throw an exception (see also comments in MyList2.main).
	// TODO: find out why (maybe because of recursion?)
	@Test
	public void testMyList2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.rec.MyList2.class);
	}

	@Test
	public void testPasswordFile() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.toy.pw.PasswordFile.class);
	}

	@Test
	public void testDemo1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		/**
		 * We are to imprecise at the moment (Dec 2012) to rule out information flow here in the 'ignore' case.
		 * See Demo1 source code for further information
		 */
		testCDGSame(joana.api.testdata.toy.demo.Demo1.class);
	}

	@Test
	public void testNonNullFieldParameter() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		/**
		 * We are to imprecise at the moment (Dec 2012) to rule out information flow here in the 'ignore' case.
		 * See NonNullFieldParameter source code for further information
		 */
		testCDGSame(joana.api.testdata.toy.demo.NonNullFieldParameter.class);
	}

	@Test
	public void testDeclass1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSame(joana.api.testdata.toy.declass.Declass1.class);
	}

	@Test
	public void testExampleLeakage() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.ExampleLeakage.class);
	}
	
	@Test
	public void testArrayAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.ArrayAccess.class);
	}
	
	@Test
	public void testArrayAlias() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.ArrayOverwrite.class);
	}
	
	@Test
	public void testFieldAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.FieldAccess.class);
	}
	@Test
	public void testFieldAccess2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.FieldAccess2.class);
	}
	
	@Test
	public void testFieldAccess3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.FieldAccess3.class);
	}
	
	@Test
	public void testConstants1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.Constants1.class);
	}
	
	@Test
	public void testConstants2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.Constants2.class);
	}
	
	// TODO: This should crash when we turn on reflection
	@Test
	public void testWalaBugReflection() throws ClassHierarchyException, ApiTestException, IOException,	UnsoundGraphException, CancelException {
		BuildSDG.build(joana.api.testdata.toy.test.Reflection.class,BuildSDG.bottom_sequential, false);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBugComplex() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class);
	}
	
	@Test
	public void testMartinMohrsStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSame(joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class);
	}
	
	@Test
	public void testWhileTrue() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testClassicUnbuildable(joana.api.testdata.seq.WhileTrue.class);
	}
}
