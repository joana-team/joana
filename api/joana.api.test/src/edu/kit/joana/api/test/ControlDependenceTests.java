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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import edu.kit.joana.ifc.sdg.graph.SDGEdge.Kind;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.IntraproceduralSlicerForward;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ControlDependenceVariant;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ControlDependenceTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static final Stubs STUBS = Stubs.JRE_14;
	private static final ExceptionAnalysis exceptionAnalyses[] = ExceptionAnalysis.values();
	
	public static final SDGConfig classic = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		classic.setControlDependenceVariant(ControlDependenceVariant.CLASSIC);
		classic.setParallel(false);
		classic.setComputeSummaryEdges(false);
	}
	
	public static final SDGConfig adaptive = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
	); {
		adaptive.setControlDependenceVariant(ControlDependenceVariant.ADAPTIVE);
		adaptive.setParallel(false);
		adaptive.setComputeSummaryEdges(false);
	}

	public static final SDGConfig ntscd = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		ntscd.setControlDependenceVariant(ControlDependenceVariant.NTSCD);
		ntscd.setParallel(false);
		ntscd.setComputeSummaryEdges(false);
	}
	
	public static final SDGConfig nticd_lfp = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		nticd_lfp.setControlDependenceVariant(ControlDependenceVariant.NTICD_LFP);
		nticd_lfp.setParallel(false);
		nticd_lfp.setComputeSummaryEdges(false);
	}
	
	public static final SDGConfig nticd_gfp = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		nticd_gfp.setControlDependenceVariant(ControlDependenceVariant.NTICD_GFP);
		nticd_gfp.setParallel(false);
		nticd_gfp.setComputeSummaryEdges(false);
	}
	
	public static final SDGConfig nticd_gfp_worklist = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
		); {
			nticd_gfp_worklist.setControlDependenceVariant(ControlDependenceVariant.NTICD_GFP_WORKLIST_SYMBOLIC);
			nticd_gfp_worklist.setParallel(false);
			nticd_gfp_worklist.setComputeSummaryEdges(false);
		}
	
	public static final SDGConfig nticd = nticd_gfp;
	
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
		

	private static void testCDGSubsetClosure(Class<?> clazz, SDGConfig sub, SDGConfig sup) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		testCDGSubsetClosure(clazz, sub, sup, exceptionAnalyses);
	}
	private static void testCDGSubsetClosure(Class<?> clazz, SDGConfig sub, SDGConfig sup, ExceptionAnalysis... eas) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		for (ExceptionAnalysis ea : eas) {
			sub.setExceptionAnalysis(ea);
			sup.setExceptionAnalysis(ea);

			final IFCAnalysis anaSub = buildAnnotateDump(clazz, sub);
			final IFCAnalysis anaSup = buildAnnotateDump(clazz, sup);
	
			final SDG sdgSub = anaSub.getProgram().getSDG();
			final SDG sdgSup   = anaSup.getProgram().getSDG();
			
			
			assertEquals(sdgSub.vertexSet(), sdgSup.vertexSet());
			final Set<SDGEdge> subEdges = sdgSub.edgeSet();
			final Set<SDGEdge> supEdges = sdgSup.edgeSet();
			final Set<SDGEdge> missingInSup = Sets.difference(subEdges, supEdges);
			
			final Slicer forwardCfgSlicer = new IntraproceduralSlicerForward(sdgSup) {
				@Override
				protected boolean isAllowedEdge(SDGEdge e) {
					final SDGEdge.Kind kind = e.getKind();
					return (kind == Kind.CONTROL_DEP_COND) && super.isAllowedEdge(e);
				}
			};
			for (SDGEdge missing : missingInSup) {
				if (missing.getKind() != SDGEdge.Kind.HELP) {
					assertEquals(SDGEdge.Kind.CONTROL_DEP_COND, missing.getKind());
					
					// TODO: optimize runtime
					final Collection<SDGNode> slice = forwardCfgSlicer.slice(missing.getSource());
					assertTrue(slice.contains(missing.getTarget()));
				}
			}
		}
	}
	
	private static void testCDGSame(Class<?> clazz, SDGConfig... configs) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		testCDGSame(clazz, exceptionAnalyses, configs);
	}
	
	private static void testCDGSame(Class<?> clazz, ExceptionAnalysis[] eas, SDGConfig... configs) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		for (ExceptionAnalysis ea : eas) {

			IFCAnalysis[] anas = new IFCAnalysis[configs.length];
			for (int i = 0; i < anas.length; i++) {
				configs[i].setExceptionAnalysis(ea);
				anas[i] = buildAnnotateDump(clazz, configs[i]);
			}
			
			for (int i = 1; i < anas.length; i++) {
				SDG sdg1 = anas[i-1].getProgram().getSDG();;
				SDG sdg2 = anas[i  ].getProgram().getSDG();
				
				final Set<SDGNode> vertices1 = sdg1.vertexSet();
				final Set<SDGNode> vertices2 = sdg2.vertexSet();
				
				for (SDGNode n : vertices1) {
					boolean in2 = vertices2.contains(n);
					if (!in2) {
						SDGNode nn;
						for (SDGNode m : vertices2) {
							if (m.getId() == n.getId()) {
								nn = m;
							}
						}
						assertTrue(false);
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
	}
	private void testUnbuildable(Class<?> clazz, SDGConfig... configs) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		testUnbuildable(clazz, exceptionAnalyses, configs);
	}
	
	private void testUnbuildable(Class<?> clazz, ExceptionAnalysis[] eas, SDGConfig... configs) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		for (ExceptionAnalysis ea : eas) {
			for (SDGConfig config : configs) {
				config.setExceptionAnalysis(ea);
				try {
					@SuppressWarnings("unused")
					IFCAnalysis anaClassic = buildAnnotateDump(clazz, config);
					assertTrue("should've thrown!!!", false);
				} catch (IllegalStateException e) {
					assertTrue("Unexpected exception:" + e.toString(), e.toString().startsWith("java.lang.IllegalStateException: Null node at dfsW="));
				}
			}
		}
	}
	

	@Test
	public void testFlowSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure(joana.api.testdata.toy.sensitivity.FlowSens.class, classic, ntscd);
		testCDGSame(         joana.api.testdata.toy.sensitivity.FlowSens.class, classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testAssChain() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.AssChain.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.AssChain.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testMicroExample() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.MicroExample.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.MicroExample.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testNested() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.Nested.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.Nested.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testNestedWithException() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.NestedWithException.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.NestedWithException.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testSick() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.Sick.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.Sick.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testSick2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.Sick2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.Sick2.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testMathRound() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.MathRound.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.MathRound.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testControlDep() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.ControlDep.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.ControlDep.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testIndependent() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.Independent.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.Independent.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testObjSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.ObjSens.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.ObjSens.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testSystemCallsTest() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.SystemCallsTest.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.SystemCallsTest.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testVeryImplictFlow() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.VeryImplictFlow.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.VeryImplictFlow.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testMyList() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.rec.MyList.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.rec.MyList.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testMyList2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.rec.MyList2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.rec.MyList2.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testPasswordFile() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.pw.PasswordFile.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.pw.PasswordFile.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testDemo1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.demo.Demo1.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.demo.Demo1.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testNonNullFieldParameter() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.demo.NonNullFieldParameter.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.demo.NonNullFieldParameter.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testDeclass1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.declass.Declass1.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.declass.Declass1.class), classic, nticd, nticd_gfp_worklist);
	}

	@Test
	public void testExampleLeakage() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.ExampleLeakage.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.ExampleLeakage.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testArrayAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.ArrayAccess.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.ArrayAccess.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testArrayAlias() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.ArrayOverwrite.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.ArrayOverwrite.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testFieldAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.FieldAccess.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess.class), classic, nticd, nticd_gfp_worklist);
	}
	@Test
	public void testFieldAccess2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.FieldAccess2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess2.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testFieldAccess3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.FieldAccess3.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess3.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testConstants1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.Constants1.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.Constants1.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testConstants2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.Constants2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.Constants2.class), classic, nticd, nticd_gfp_worklist);
	}
	
	// TODO: This should crash when we turn on reflection
	@Test
	public void testWalaBugReflection() throws ClassHierarchyException, ApiTestException, IOException,	UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.Reflection.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.Reflection.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBugComplex() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testMartinMohrsStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class), classic, nticd, nticd_gfp_worklist);
	}
	
	@Test
	public void testWhileTrue() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testUnbuildable(       joana.api.testdata.seq.WhileTrue.class, classic);
		testCDGSame(          (joana.api.testdata.seq.WhileTrue.class), adaptive, nticd, nticd_gfp_worklist);
		testCDGSubsetClosure( (joana.api.testdata.seq.WhileTrue.class), nticd, ntscd);
	}
	
	@Test
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testUnbuildable(     (de.uni.trier.infsec.core.Setup.class),
			new ExceptionAnalysis[] { ExceptionAnalysis.INTERPROC, ExceptionAnalysis.INTRAPROC },
			classic
		);
		testCDGSame(         (de.uni.trier.infsec.core.Setup.class),
			new ExceptionAnalysis[] { ExceptionAnalysis.IGNORE_ALL, ExceptionAnalysis.ALL_NO_ANALYSIS  },
			classic, nticd, nticd_gfp_worklist
		);
		testCDGSubsetClosure((de.uni.trier.infsec.core.Setup.class), nticd, ntscd);
	}
	
	
}
