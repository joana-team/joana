/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.ipa.callgraph.pruned.DoNotPrune;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalException;
import edu.kit.joana.wala.eval.util.EvalPaths;
import edu.kit.joana.wala.eval.util.EvalTimingStats;
import edu.kit.joana.wala.eval.util.EvalTimingStats.TaskInfo;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestObjGraphPerformance {

	public static class ApiTestException extends Exception {

		private static final long serialVersionUID = 7000978878774124747L;

		public ApiTestException(Throwable t) {
			super(t);
		}
	}
	
	public static EvalTimingStats stats = new EvalTimingStats(); 
	
	public static SDGConfig createConfig(final String testCase, final PointsToPrecision pts,
			final FieldPropagation fprop, final Stubs stubs, final String cp, final String className) {
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		final SDGConfig config = new SDGConfig(cp, mainMethod.toBCString(), stubs);
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
		config.setFieldPropagation(fprop);
		config.setPointsToPrecision(pts);
		//config.setPruningPolicy(DoNotPrune.INSTANCE);
		config.setComputeAllocationSites(false);
		config.setComputeAccessPaths(false);
		config.setNotifier(stats);
		stats.setCurrentTask(testCase);
		
		return config;
	}
	
	public static SDG buildSDG(final SDGConfig config) throws ApiTestException {
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

	public static String currentMethodName() {
		final Throwable t = new Throwable();
		final StackTraceElement e = t.getStackTrace()[1];
		return e.getMethodName();
	}
	
	public static void outputStatistics(final SDG sdg, final SDGConfig cfg, final String testMethodName) {
		stats.readAdditionalStats(sdg, cfg);
		final TaskInfo ti = stats.getCurrent();
		System.out.println(ti);
		try {
			final String sdgFileName = EvalPaths.getOutputPath(testMethodName + ".pdg");
			System.out.println("Writing sdg to file '" + sdgFileName + "'");
			final FileOutputStream sdgOut = new FileOutputStream(sdgFileName); 
			SDGSerializer.toPDGFormat(sdg, sdgOut);
			sdgOut.flush();
		} catch (final EvalException e) {
			System.err.println("Could not write sdg to file: " + e.getMessage());
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.err.println("Could not write sdg to file: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Could not write sdg to file: " + e.getMessage());
			e.printStackTrace();
		} 
	}
	
	@Test
	public void test_J2ME_Barcode_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.J2ME_STUBS, EvalPaths.J2ME_BARCODE, "MainEmulator");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_J2ME_Safe_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.J2ME_STUBS, EvalPaths.J2ME_SAFE, "MainEmulator");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_J2ME_KeePass_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.J2ME_STUBS, EvalPaths.J2ME_KEEPASS, "MainEmulator");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_J2ME_OneTimePass_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.J2ME_STUBS, EvalPaths.J2ME_ONETIMEPASS, "MainEmulator");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_J2ME_bExplore_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.J2ME_STUBS, EvalPaths.J2ME_BEXPLORE, "MainEmulator");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	@Test
	public void test_JRE14_HSQLDB_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_HSQLDB, "org.hsqldb.Server");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_HSQLDB_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_HSQLDB, "org.hsqldb.Server");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeBarrier_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFBarrierBench");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeCrypt_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFCryptBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeForkJoin_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFForkJoinBench");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeLUFact_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFLUFactBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeMolDyn_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMolDynBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeMonteCarlo_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMonteCarloBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeRayTracer_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFRayTracerBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSeries_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSeriesBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSOR_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSORBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSparseMatmult_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSparseMatmultBenchSizeA");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSync_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSyncBench");
			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_Purse_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JC_STUBS, EvalPaths.JC_PURSE, "javacard.framework.JCMainPurse");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_Safe_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JC_STUBS, EvalPaths.JC_SAFE, "javacard.framework.JCMainSafeApplet");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_Wallet_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JC_STUBS, EvalPaths.JC_WALLET, "javacard.framework.JCMain");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsType_Graph_NoEscape() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH_NO_ESCAPE,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsType_Graph_Fast() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsType_Graph_Standard() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsType_Graph_Fixpoint() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH_FIXPOINT_PROPAGATION,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsType_Graph_NoMergeAtAll() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsObj_Graph_NoEscape() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH_NO_ESCAPE,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsObj_Graph_Fast() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsObj_Graph_Standard() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsObj_Graph_Fixpoint() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH_FIXPOINT_PROPAGATION,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsObj_Graph_NoMerge() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH_NO_MERGE_AT_ALL,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

//	@Test
//	public void test_JC_CorporateCard_PtsObj_Tree() {
//		try {
//			final String currentTestcase = currentMethodName();
//			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_TREE,
//					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
//			final SDG sdg = buildSDG(cfg);
//			assertFalse(sdg.vertexSet().isEmpty());
//			outputStatistics(sdg, cfg, currentTestcase);
//		} catch (ApiTestException e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}
//	}
//
	@Test
	public void test_JC_CorporateCard_PtsInstance_Tree() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_TREE,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JC_CorporateCard_PtsType_Tree() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_TREE,
					EvalPaths.JC_STUBS, EvalPaths.JC_CORPORATECARD, "javacard.framework.JCMainCorporateCard");
			final SDG sdg = buildSDG(cfg);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
