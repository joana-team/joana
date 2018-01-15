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

import org.junit.Ignore;
import org.junit.Test;

import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalPaths;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestObjGraphPerformanceJRE15_NoOptExcNoExc extends TestObjGraphPerformance {

	public static final int NUMBER_OF_RUNS = 1;
	
	public static final String SUFFIX = "-noopt-noexc";

	
	@Override
	protected void postCreateConfigHook(final SDGConfig config) {
		// TDOD: this is different from TestObjGraphPerformanceJRE14_NoOptExcNoExc,
		// since we cannot currently compute sdgs within reasonable
		// space for unpruned callgraphs using the full JRE1.5 stubs. 
		config.setPruningPolicy(ApplicationLoaderPolicy.INSTANCE);

		config.setExclusions("");
		config.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
		//config.setComputeSummaryEdges(false);
	}
	
	@Test
	public void test_JRE15_Battleship_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_15, EvalPaths.JRE15_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE15_Battleship_PtsInst_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_15, EvalPaths.JRE15_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	@Ignore("Even callgraph-Construction takes too long for now")
	public void test_JRE15_Battleship_PtsObj_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_15, EvalPaths.JRE15_BATTLESHIP, "Main");
			final SDG sdg = buildSDG(cfg, NUMBER_OF_RUNS, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
