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

import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.util.Config;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalPaths;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class TestIFCApplicationFreeCSWithSummariesAndMerges extends TestObjGraphPerformance {

	@Override
	protected void postCreateConfigHook(final SDGConfig config) {
		config.setPruningPolicy(ApplicationLoaderPolicy.INSTANCE);
		config.setComputeSummaryEdges(true);
		
		// reset to defaults
		System.clearProperty(Config.C_OBJGRAPH_MAX_NODES_PER_INTERFACE);
		System.clearProperty(Config.C_OBJGRAPH_CUT_OFF_IMMUTABLE);
		System.clearProperty(Config.C_OBJGRAPH_CUT_OFF_UNREACHABLE);
	}
	
	@Test
	public void test_JRE14_FreeCS_PtsInst_Graph_Std_Summary_Merge() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED,
					FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_FREECS, "freecs.Server");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_FreeCS_PtsType_Graph_Std_Summary_Merge() {
		try {
			final String currentTestcase = currentMethodName();
			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED, FieldPropagation.OBJ_GRAPH,
					Stubs.JRE_14, EvalPaths.JRE14_FREECS, "freecs.Server");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
