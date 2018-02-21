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
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Sets;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.summary.SummaryComputationType;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
@SuppressWarnings("deprecation")
public class SummaryComputationTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static final Stubs STUBS = Stubs.JRE_15;
	
	private static void setDefaults(SDGConfig config) {
		config.setParallel(false);
	}
	
	public static final SDGConfig classic = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(classic);
		classic.setSummaryComputationType(SummaryComputationType.JOANA_CLASSIC);
	}
	
	public static final SDGConfig classicScc = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
		); {
			setDefaults(classicScc);
			classicScc.setSummaryComputationType(SummaryComputationType.JOANA_CLASSIC_SCC);
		}
	
	public static final SDGConfig simonScc = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
	); {
		setDefaults(simonScc);
		simonScc.setSummaryComputationType(SummaryComputationType.SIMON_SCC);
	}
	
	public static final SDGConfig simonParallelScc = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
	); {
		setDefaults(simonParallelScc);
		simonParallelScc.setSummaryComputationType(SummaryComputationType.SIMON_PARALLEL_SCC);
	}
	
	public static final SDGConfig[] all = new SDGConfig[] { classic, classicScc, simonScc, simonParallelScc };

	
	private static IFCAnalysis buildAnnotateDump(Class<?> clazz, SDGConfig config) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(clazz,config, false);
		
		final String filename = clazz.getCanonicalName()
			+ "-" + config.getSummaryComputationType()
			+ ".pdg";
		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), filename);
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), filename);
		}
		
		return ana;
	}
	
	private static void testSDGSame(Class<?> clazz, SDGConfig... configs) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		IFCAnalysis[] anas = new IFCAnalysis[configs.length];
		for (int i = 0; i < anas.length; i++) {
			anas[i] = buildAnnotateDump(clazz, configs[i]);
		}

		for (int i = 1; i < anas.length; i++) {
			SDG sdg1 = anas[i-1].getProgram().getSDG();;
			SDG sdg2 = anas[i  ].getProgram().getSDG();

			final Set<SDGNode> vertices1 = sdg1.vertexSet();
			final Set<SDGNode> vertices2 = sdg2.vertexSet();

			// The following 2 blocks are, strictly speaking, unnecessary.
			// They do, however, help when debugging, especially: when vertices differ due to non-determinism
			// in the SDG contstruction.
			for (SDGNode n : vertices1) {
				boolean in2 = vertices2.contains(n);
				if (!in2) {
					SDGNode nn = null;
					for (SDGNode m : vertices2) {
						if (m.getId() == n.getId()) {
							nn = m;
						}
					}
					assertTrue("!in2: n: " + n + ",  nn:" + (nn == null ? "null" : nn), false);
				}
			}

			for (SDGNode n : vertices2) {
				boolean in1 = vertices1.contains(n);
				if (!in1) {
					SDGNode nn = null;
					for (SDGNode m : vertices1) {
						if (m.getId() == n.getId()) {
							nn = m;
						}
					}
					assertTrue("!in1: n: " + n + ",  nn:" + (nn == null ? "null" : nn), false);
				}
			}


			assertEquals(vertices1, vertices2);
			final Set<SDGEdge> edges1 = sdg1.edgeSet();
			final Set<SDGEdge> edges2 = sdg2.edgeSet();
			final Set<SDGEdge> missingIn1 = Sets.difference(edges2, edges1);
			final Set<SDGEdge> missingIn2 = Sets.difference(edges1, edges2);

			assertTrue(missingIn1.isEmpty());
			assertTrue(missingIn2.isEmpty());
		}
	}

	@Test
	public void testFlowSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         joana.api.testdata.toy.sensitivity.FlowSens.class, all);
	}

	@Test
	public void testAssChain() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.AssChain.class), all);
	}

	@Test
	public void testMicroExample() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.MicroExample.class), all);
	}

	@Test
	public void testNested() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.Nested.class), all);
	}

	@Test
	public void testNestedWithException() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.NestedWithException.class), all);
	}

	@Test
	public void testSick() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.Sick.class), all);
	}

	@Test
	public void testSick2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.Sick2.class), all);
	}

	@Test
	public void testMathRound() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.simp.MathRound.class), all);
	}

	@Test
	public void testControlDep() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.test.ControlDep.class), all);
	}

	@Test
	public void testIndependent() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.test.Independent.class), all);
	}

	@Test
	public void testObjSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.test.ObjSens.class), all);
	}

	@Test
	public void testSystemCallsTest() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.toy.test.SystemCallsTest.class), all);
	}

	@Test
	public void testVeryImplictFlow() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.toy.test.VeryImplictFlow.class), all);
	}

	@Test
	public void testMyList() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.rec.MyList.class), all);
	}

	@Test
	public void testMyList2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.rec.MyList2.class), all);
	}

	@Test
	public void testPasswordFile() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.toy.pw.PasswordFile.class), all);
	}

	@Test
	public void testDemo1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.demo.Demo1.class), all);
	}

	@Test
	public void testNonNullFieldParameter() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.demo.NonNullFieldParameter.class), all);
	}

	@Test
	public void testDeclass1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testSDGSame(         (joana.api.testdata.toy.declass.Declass1.class), all);
	}

	@Test
	public void testExampleLeakage() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.ExampleLeakage.class), all);
	}
	
	@Test
	public void testArrayAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.ArrayAccess.class), all);
	}
	
	@Test
	public void testArrayAlias() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.ArrayOverwrite.class), all);
	}
	
	@Test
	public void testFieldAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.FieldAccess.class), all);
	}
	@Test
	public void testFieldAccess2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.FieldAccess2.class), all);
	}
	
	@Test
	public void testFieldAccess3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.FieldAccess3.class), all);
	}
	
	@Test
	public void testConstants1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.Constants1.class), all);
	}
	
	@Test
	public void testConstants2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.Constants2.class), all);
	}
	
	@Test
	public void testWalaBugReflection() throws ClassHierarchyException, ApiTestException, IOException,	UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.toy.test.Reflection.class), all);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class), all);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBugComplex() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class), all);
	}
	
	@Test
	public void testMartinMohrsStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class), all);
	}
	
	@Test
	public void testWhileTrue() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(          (joana.api.testdata.seq.WhileTrue.class), all);
	}
	
	@Test
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (de.uni.trier.infsec.core.Setup.class), classic, classicScc);
		testSDGSame(         (de.uni.trier.infsec.core.Setup.class), simonScc, simonParallelScc); // TODO: find out what's going on, here
	}
	
	@Test
	public void testJLex() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSDGSame(         (JLex.Main.class), classic, classicScc);
		testSDGSame(         (JLex.Main.class), simonScc, simonParallelScc); // TODO: find out what's going on, here
		
	}
}
