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
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalPaths;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestIFCApplicationUPM extends TestObjGraphPerformance {

	@Override
	protected void postCreateConfigHook(final SDGConfig config) {
		config.setPruningPolicy(ApplicationLoaderPolicy.INSTANCE);
//		config.setExclusions("");
//		config.setComputeSummaryEdges(true);
	}
	
	@Test
	public void test_JRE14_UPM_PtsInst_Graph_Std() {
		try {
			final String currentTestcase = currentMethodName();
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}

			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED,
					FieldPropagation.OBJ_GRAPH,
					Stubs.NO_STUBS, EvalPaths.JRE14_UPM, "com.od.upm.gui.MainWindow");
//			final SDGProgram prog = buildSDGProgram(cfg, System.out);
//			for (final SDGMethod m : prog.getAllMethods()) {
//				final String name = m.toString();
//				if (name.contains("askUserForPassword") 
//						|| (name.contains("javax") && name.contains("swing")) 
//						|| name.contains("OutputStream") 
//						|| name.contains("HTTPTransport")) {
//					System.out.println(name);
//				}
//				
//			}
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_UPM_PtsType_Graph_Std() {
		try {
			final String currentTestcase = currentMethodName();
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}

			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED,
					FieldPropagation.OBJ_GRAPH,
					Stubs.NO_STUBS, EvalPaths.JRE14_UPM, "com.od.upm.gui.MainWindow");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_UPM_PtsObj_Graph_Std() {
		try {
			final String currentTestcase = currentMethodName();
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}

			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE,
					FieldPropagation.OBJ_GRAPH,
					Stubs.NO_STUBS, EvalPaths.JRE14_UPM, "com.od.upm.gui.MainWindow");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_UPM_PtsInst_Graph_Fast() {
		try {
			final String currentTestcase = currentMethodName();
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}

			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.INSTANCE_BASED,
					FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION,
					Stubs.NO_STUBS, EvalPaths.JRE14_UPM, "com.od.upm.gui.MainWindow");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_UPM_PtsType_Graph_Fast() {
		try {
			final String currentTestcase = currentMethodName();
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}

			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.TYPE_BASED,
					FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION,
					Stubs.NO_STUBS, EvalPaths.JRE14_UPM, "com.od.upm.gui.MainWindow");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void test_JRE14_UPM_PtsObj_Graph_Fast() {
		try {
			final String currentTestcase = currentMethodName();
			if (areWeLazy(currentTestcase)) {
				System.out.println("skipping " + currentTestcase + " as pdg and log already exist.");
				return;
			}

			final SDGConfig cfg = createConfig(currentTestcase, PointsToPrecision.OBJECT_SENSITIVE,
					FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION,
					Stubs.NO_STUBS, EvalPaths.JRE14_UPM, "com.od.upm.gui.MainWindow");
			final SDG sdg = buildSDG(cfg, System.out);
			assertFalse(sdg.vertexSet().isEmpty());
			outputStatistics(sdg, cfg, currentTestcase);
		} catch (ApiTestException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
