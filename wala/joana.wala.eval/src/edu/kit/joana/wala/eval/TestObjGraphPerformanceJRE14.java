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

import com.ibm.wala.ipa.callgraph.pruned.DoNotPrune;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.TestObjGraphPerformance.ApiTestException;
import edu.kit.joana.wala.eval.util.EvalPaths;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestObjGraphPerformanceJRE14 extends TestObjGraphPerformance {

	public static final int NUMBER_OF_RUNS = 6;
	
	@Override
	protected void postCreateConfigHook(final SDGConfig config) {
		config.setPruningPolicy(DoNotPrune.INSTANCE);
		config.setExclusions("");
		//config.setComputeSummaryEdges(false);
	}
	
	@Test
	public void test_JRE14_Battleship_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsType_Graph_StdNoOpt() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH_FIXP_NO_OPT,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsInst_Graph_StdNoOpt() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH_FIXP_NO_OPT,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsObj_Graph_StdNoOpt() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH_FIXP_NO_OPT,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsType_Graph_FastNoOpt() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH_SIMPLE_NO_OPT,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsInst_Graph_FastNoOpt() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH_SIMPLE_NO_OPT,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_Battleship_PtsObj_Graph_FastNoOpt() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH_SIMPLE_NO_OPT,
					Stubs.JRE_14, EvalPaths.JRE14_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
