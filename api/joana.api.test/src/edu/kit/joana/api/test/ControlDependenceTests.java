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
@SuppressWarnings("deprecation")
public class ControlDependenceTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static final Stubs STUBS = Stubs.JRE_15;
	private static final ExceptionAnalysis exceptionAnalyses[] = ExceptionAnalysis.values();
	
	private static void setDefaults(SDGConfig config) {
		config.setParallel(false);
		config.setComputeSummaryEdges(false);
	}
	
	public static final SDGConfig classic = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(classic);
		classic.setControlDependenceVariant(ControlDependenceVariant.CLASSIC);
	}
	
	public static final SDGConfig adaptive = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
	); {
		setDefaults(adaptive);
		adaptive.setControlDependenceVariant(ControlDependenceVariant.ADAPTIVE);
	}

	public static final SDGConfig ntscd = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(ntscd);
		ntscd.setControlDependenceVariant(ControlDependenceVariant.NTSCD);
	}
	
	public static final SDGConfig ntscd_imaxdom = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
		); {
			setDefaults(ntscd_imaxdom);
			ntscd_imaxdom.setControlDependenceVariant(ControlDependenceVariant.NTSCD_IMAXDOM);
		}

	
	public static final SDGConfig nticd_lfp = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(nticd_lfp);
		nticd_lfp.setControlDependenceVariant(ControlDependenceVariant.NTICD_LFP);
	}
	
	public static final SDGConfig nticd_gfp = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(nticd_gfp);
		nticd_gfp.setControlDependenceVariant(ControlDependenceVariant.NTICD_GFP);
	}
	
	public static final SDGConfig nticd_gfp_worklist = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(nticd_gfp_worklist);
		nticd_gfp_worklist.setControlDependenceVariant(ControlDependenceVariant.NTICD_GFP_WORKLIST_SYMBOLIC);
	}
	
	public static final SDGConfig nticd_lfp_dual_worklist = new SDGConfig(
		JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
		null,
		STUBS
	); {
		setDefaults(nticd_lfp_dual_worklist);
		nticd_lfp_dual_worklist.setControlDependenceVariant(ControlDependenceVariant.NTICD_LFP_DUAL_WORKLIST);
	}
	
	public static final SDGConfig nticd_isinkdom = new SDGConfig(
			JoanaPath.JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
		); {
			setDefaults(nticd_isinkdom);
			nticd_isinkdom.setControlDependenceVariant(ControlDependenceVariant.NTICD_ISINKDOM);
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
					final boolean pathMissingDueTopecificallyExcludedEdgeDuringPDGCreation =
						   (missing.getTarget().getKind() == SDGNode.Kind.FORMAL_OUT)
						&& ("_exception_".equals(missing.getTarget().getLabel()))
						&& (slice.stream().anyMatch(n -> n.getKind() == SDGNode.Kind.CALL));
					assertTrue(pathMissingDueTopecificallyExcludedEdgeDuringPDGCreation || slice.contains(missing.getTarget()));
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
		testCDGSame(         joana.api.testdata.toy.sensitivity.FlowSens.class, classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         joana.api.testdata.toy.sensitivity.FlowSens.class, ntscd, ntscd_imaxdom);
	}

	@Test
	public void testAssChain() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.AssChain.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.AssChain.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.AssChain.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testMicroExample() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.MicroExample.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.MicroExample.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.MicroExample.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testNested() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.Nested.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.Nested.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.Nested.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testNestedWithException() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.NestedWithException.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.NestedWithException.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.NestedWithException.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testSick() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.Sick.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.Sick.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.Sick.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testSick2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.Sick2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.Sick2.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.Sick2.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testMathRound() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.simp.MathRound.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.simp.MathRound.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.simp.MathRound.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testControlDep() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.ControlDep.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.ControlDep.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.test.ControlDep.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testIndependent() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.Independent.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.Independent.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.test.Independent.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testObjSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.ObjSens.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.ObjSens.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.test.ObjSens.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testSystemCallsTest() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.SystemCallsTest.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.SystemCallsTest.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.test.SystemCallsTest.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testVeryImplictFlow() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.VeryImplictFlow.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.VeryImplictFlow.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.test.VeryImplictFlow.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testMyList() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.rec.MyList.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.rec.MyList.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.rec.MyList.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testMyList2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.rec.MyList2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.rec.MyList2.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.rec.MyList2.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testPasswordFile() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.pw.PasswordFile.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.pw.PasswordFile.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.pw.PasswordFile.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testDemo1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.demo.Demo1.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.demo.Demo1.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.demo.Demo1.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testNonNullFieldParameter() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.demo.NonNullFieldParameter.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.demo.NonNullFieldParameter.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.demo.NonNullFieldParameter.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testDeclass1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.declass.Declass1.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.declass.Declass1.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.declass.Declass1.class), ntscd, ntscd_imaxdom);
	}

	@Test
	public void testExampleLeakage() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.ExampleLeakage.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.ExampleLeakage.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.ExampleLeakage.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testArrayAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.ArrayAccess.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.ArrayAccess.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.ArrayAccess.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testArrayAlias() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.ArrayOverwrite.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.ArrayOverwrite.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.ArrayOverwrite.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testFieldAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.FieldAccess.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testFieldAccess2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.FieldAccess2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess2.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess2.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testFieldAccess3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.FieldAccess3.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess3.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.FieldAccess3.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testConstants1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.Constants1.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.Constants1.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.Constants1.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testConstants2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.Constants2.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.Constants2.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.Constants2.class), ntscd, ntscd_imaxdom);
	}
	
	// TODO: This should crash when we turn on reflection
	@Test
	public void testWalaBugReflection() throws ClassHierarchyException, ApiTestException, IOException,	UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.toy.test.Reflection.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.toy.test.Reflection.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.toy.test.Reflection.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBug.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testStrangeTryCatchFinallyWalaBugComplex() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.StrangeTryCatchFinallyWalaBugComplex.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testMartinMohrsStrangeTryCatchFinallyWalaBug() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class), classic, ntscd);
		testCDGSame(         (joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class), classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (joana.api.testdata.seq.MartinMohrsStrangeTryCatchFinallyWalaBug.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testWhileTrue() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testUnbuildable(       joana.api.testdata.seq.WhileTrue.class, classic);
		testCDGSame(          (joana.api.testdata.seq.WhileTrue.class),           nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSubsetClosure( (joana.api.testdata.seq.WhileTrue.class), nticd, ntscd);
		testCDGSame(          (joana.api.testdata.seq.WhileTrue.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testWhileTrueLeakInLoop() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testUnbuildable(     (joana.api.testdata.seq.WhileTrueLeakInLoop.class),
				new ExceptionAnalysis[] { ExceptionAnalysis.IGNORE_ALL, ExceptionAnalysis.INTERPROC},
				classic
			);
		testCDGSame(          (joana.api.testdata.seq.WhileTrueLeakInLoop.class),           nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSubsetClosure( (joana.api.testdata.seq.WhileTrueLeakInLoop.class), nticd, ntscd);
		testCDGSame(          (joana.api.testdata.seq.WhileTrueLeakInLoop.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testWhileTrueLeakInLoopNoMethodCall() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testUnbuildable(      (joana.api.testdata.seq.WhileTrueLeakInLoopNoMethodCall.class), classic);
		testCDGSame(          (joana.api.testdata.seq.WhileTrueLeakInLoopNoMethodCall.class),           nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSubsetClosure( (joana.api.testdata.seq.WhileTrueLeakInLoopNoMethodCall.class), nticd, ntscd);
		testCDGSame(          (joana.api.testdata.seq.WhileTrueLeakInLoopNoMethodCall.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testUnbuildable(     (de.uni.trier.infsec.core.Setup.class),
			new ExceptionAnalysis[] { ExceptionAnalysis.IGNORE_ALL },
			classic
		);
		testCDGSame(         (de.uni.trier.infsec.core.Setup.class),
			new ExceptionAnalysis[] { ExceptionAnalysis.IGNORE_ALL },
			                   nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom
		);
		testCDGSame(         (de.uni.trier.infsec.core.Setup.class),
			new ExceptionAnalysis[] { ExceptionAnalysis.INTERPROC, ExceptionAnalysis.INTRAPROC, ExceptionAnalysis.ALL_NO_ANALYSIS },
			adaptive, classic, nticd, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom
		);
		testCDGSubsetClosure((de.uni.trier.infsec.core.Setup.class), nticd, ntscd);
		testCDGSame(         (de.uni.trier.infsec.core.Setup.class), ntscd, ntscd_imaxdom);
	}
	
	@Test
	public void testJLex() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testCDGSubsetClosure((JLex.Main.class), classic, ntscd);
		testCDGSame(         (JLex.Main.class), classic, nticd_gfp_worklist, nticd_lfp_dual_worklist, nticd_isinkdom);
		testCDGSame(         (JLex.Main.class), ntscd, ntscd_imaxdom);
	}
	
	
}
