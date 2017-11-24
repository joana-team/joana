/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;


import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.alg.CycleDetector;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ISCRBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ISCRGraph;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import static org.junit.Assert.assertFalse;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ISCRCyclicityTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static void testAcyclicISCR(Class<?> clazz) throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {

		final String className = clazz.getCanonicalName();
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);

		final SDGProgram p = BuildSDG.standardConcBuild(
				JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH,
				mainMethod,
				clazz.getCanonicalName() + ".ISCRCyclicityTests.pdg",
				PointsToPrecision.INSTANCE_BASED
				);

		final SDG sdg = p.getSDG();
		final CFG icfg = ICFGBuilder.extractICFG(sdg);

		ISCRBuilder b = new ISCRBuilder();
		Map<SDGNode, ISCRGraph> iscrGraphs = b.buildISCRGraphs(icfg);

		for (ISCRGraph iscrGraph : iscrGraphs.values() ) {
			@SuppressWarnings("deprecation")
			final Set<SDGEdge> irrelevantEdges = iscrGraph.getData()
					.edgeSet()
					.stream()
					.filter(e -> e.getKind() == SDGEdge.Kind.NO_FLOW)
					.collect(Collectors.toSet());
			@SuppressWarnings("deprecation")
			final CFG relevantEdgesISCRGraph = new CFG(iscrGraph.getData());
			relevantEdgesISCRGraph.removeAllEdges(irrelevantEdges);
			final CycleDetector<SDGNode, SDGEdge> cycleDetector = new CycleDetector<SDGNode, SDGEdge>(relevantEdgesISCRGraph);
			assertFalse(cycleDetector.detectCycles());
		}
	}

	// FIXME: this program demonstrates that the ISCRGraph we compute is *not* acyclic!!
	@Test(expected = AssertionError.class)
	public void testKnapsack5() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testAcyclicISCR(conc.kn.Knapsack5.class);
	}
	
	// FIXME: this program demonstrates that the ISCRGraph we compute is *not* acyclic!!
	@Test(expected = AssertionError.class)
	public void testDaisy() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		testAcyclicISCR(conc.daisy.DaisyTest.class);
	}
	
	
	@Test
	public void testMutualRecursive() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testAcyclicISCR(tests.MutualRecursive.class);
	}
	
	@Test
	public void testKnapsack5Shrinked() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testAcyclicISCR(conc.kn.shrinked.Knapsack5ExceptionControlFlow.class);
	}
	
	@Test
	public void testExceptionFallThrough() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testAcyclicISCR(conc.kn.shrinked.ExceptionFallThrough.class);
	}
	
	@Test
	public void testExceptionFallThroughIntDiv() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testAcyclicISCR(conc.kn.shrinked.ExceptionFallThroughIntDiv.class);
	}
}
