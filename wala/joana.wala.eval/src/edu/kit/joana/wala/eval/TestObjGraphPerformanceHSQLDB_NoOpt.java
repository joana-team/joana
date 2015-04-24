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
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalPaths;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestObjGraphPerformanceHSQLDB_NoOpt extends TestObjGraphPerformance {

	public static final String SUFFIX = "-noopt";
	
	@Override
	protected void postCreateConfigHook(final SDGConfig config) {
		config.setExclusions("");
	}

	@Test
	public void test_JRE14_HSQLDB_PtsType_Graph() {
		try {
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
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
			final String currentTestcase = currentMethodName() + SUFFIX;
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}
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
	
}
