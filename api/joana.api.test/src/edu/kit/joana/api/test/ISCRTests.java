/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGManualParser;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ISCRBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ISCRTests {
	
	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = true;
	
	private static void testBuildSuccessfull(Class<?> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		
		final String className = clazz.getCanonicalName();
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		
		SDGProgram p = BuildSDG.standardConcBuild(
			JoanaPath.JOANA_MANY_SMALL_PROGRAMS_CLASSPATH,
			mainMethod,
			clazz.getCanonicalName() + ".ISCRTests.pdg",
			PointsToPrecision.INSTANCE_BASED
		);
		
		SDG sdg = p.getSDG();
        CFG icfg = ICFGBuilder.extractICFG(sdg);
        ISCRBuilder b = new ISCRBuilder();
        b.buildISCRGraphs(icfg);
	}

	@Test
	public void testKnapsack5() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.Knapsack5.class);
	}
	
	@Test
	public void testKnapsack5Shrinked() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.shrinked.Knapsack5ExceptionControlFlow.class);
	}
	
	@Test
	public void testExceptionFallThrough() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.shrinked.ExceptionFallThrough.class);
	}
	
	@Test
	public void testExceptionFallThroughIntDiv() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(conc.kn.shrinked.ExceptionFallThroughIntDiv.class);
	}
	
	@Test
	public void testMutualRecursive() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testBuildSuccessfull(tests.MutualRecursive.class);
	}

	
	private static void testSDGFile(File file) throws IOException, RecognitionException {
		final SDG sdg = SDGManualParser.parse(
			new GZIPInputStream(
				new FileInputStream(file)
		));
		final CFG cfg = ICFGBuilder.extractICFG(sdg); 
		final ISCRBuilder b = new ISCRBuilder();
		b.buildISCRGraphs(cfg);
	}
	
	@Test
	public void testHugeCFG() throws IOException, RecognitionException {
		testSDGFile(new File("../joana.api.testdata/graphs/cfg/huge.cfg.pdg.gz"));
	}
	
}
