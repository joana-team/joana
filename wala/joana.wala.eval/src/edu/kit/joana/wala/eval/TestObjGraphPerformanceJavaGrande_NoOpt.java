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

import org.junit.Test;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalPaths;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestObjGraphPerformanceJavaGrande_NoOpt extends TestObjGraphPerformance {

	public static final int NUMBER_OF_RUNS = 3;
	
	public static final String SUFFIX = "-noopt";
	
	@Override
	protected void postCreateConfigHook(final SDGConfig config) {
		config.setExclusions("");
	}
	
	@Test
	public void test_JRE14_JavaGrandeBarrier_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFBarrierBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFCryptBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFForkJoinBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFLUFactBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMolDynBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMonteCarloBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFRayTracerBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSeriesBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSORBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSparseMatmultBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSyncBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeBarrier_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFBarrierBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeCrypt_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFCryptBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeForkJoin_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFForkJoinBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeLUFact_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFLUFactBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeMolDyn_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMolDynBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeMonteCarlo_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMonteCarloBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeRayTracer_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFRayTracerBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSeries_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSeriesBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSOR_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSORBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSparseMatmult_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSparseMatmultBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSync_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSyncBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeBarrier_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFBarrierBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeCrypt_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFCryptBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeForkJoin_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFForkJoinBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeLUFact_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFLUFactBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeMolDyn_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMolDynBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeMonteCarlo_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFMonteCarloBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeRayTracer_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFRayTracerBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSeries_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSeriesBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSOR_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSORBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSparseMatmult_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSparseMatmultBenchSizeA");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_JavaGrandeSync_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					EvalPaths.JAVAGRANDE_STUBS, EvalPaths.JAVAGRANDE_CP, "def.JGFSyncBench");
//			cfg.setComputeInterferences(true);
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
