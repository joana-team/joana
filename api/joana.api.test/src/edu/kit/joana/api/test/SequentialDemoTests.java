/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.io.graphml.SDG2GraphML;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * Tests for testing the sequential part of the demo test cases
 * 
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class SequentialDemoTests {
	static final Stubs STUBS = Stubs.JRE_14;

	static final boolean outputPDGFiles = false;
	static final boolean outputGraphMLFiles = false;
	static final boolean generateOutputDir = outputPDGFiles || outputGraphMLFiles;
	
	static final String outputDir = "out";
	
	static final SDGConfig sequentialConfig = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			false, // no interference computation
			MHPType.NONE);

	static {
		if (generateOutputDir) {
			File fOutDir = new File(outputDir);
			if (!fOutDir.exists()) {
				fOutDir.mkdir();
			}
		}
	}
	
	public static <T> IFCAnalysis build(Class<T> clazz, SDGConfig config, boolean ignore) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		final String className = clazz.getCanonicalName();
		final String classPath;
		if (ignore) {
			classPath = JoanaPath.JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + JoanaPath.ANNOTATIONS_IGNORE_CLASSPATH;
		} else {
			classPath = JoanaPath.JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + JoanaPath.ANNOTATIONS_PASSON_CLASSPATH;
		}
		config.setClassPath(classPath);
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		config.setEntryMethod(mainMethod.toBCString());
		SDGProgram prog = SDGProgram.createSDGProgram(config);

		IFCAnalysis ana = new IFCAnalysis(prog);
		return ana;
	}

	public static <T> IFCAnalysis buldAndUseJavaAnnotations(Class<T> clazz, SDGConfig config, boolean ignore)
				throws ApiTestException, ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
			IFCAnalysis ana = build(clazz,config,ignore);
			ana.addAllJavaSourceAnnotations();
			return ana;
	}
	
	
	private static <T> void testSound(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
	IOException, UnsoundGraphException, CancelException {
		final String classname = clazz.getCanonicalName();
		{ // There are leaks, and we're sound and hence report them
			IFCAnalysis ana = buldAndUseJavaAnnotations(clazz, sequentialConfig, true);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
			}
			if (outputGraphMLFiles) {
				dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
			}

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.CLASSICAL_NI);
			assertFalse(illegal.isEmpty());
		}
	}
	
	private static <T> void testPrecise(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		final String classname = clazz.getCanonicalName();
		{ // There are no leak, and  we're precise enough to find out that there aren't
			IFCAnalysis ana = buldAndUseJavaAnnotations(clazz, sequentialConfig, true);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
			}
			if (outputGraphMLFiles) {
				dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
			}

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(IFCType.CLASSICAL_NI);
			assertTrue(illegal.isEmpty());
		}
	}

	private static <T> void testTooImprecise(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		testSound(clazz);
	}
	
	private static void dumpSDG(SDG sdg, String filename) throws FileNotFoundException {
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputDir + "/" + filename));
		SDGSerializer.toPDGFormat(sdg, bOut);
	}
	
	private static void dumpGraphML(SDG sdg, String filename) throws FileNotFoundException {
		final BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputDir + "/" + filename + ".graphml"));
		final BufferedOutputStream bOutHierachical = new BufferedOutputStream(new FileOutputStream(outputDir + "/" + filename + ".hierarchical.graphml"));
		try {
			SDG2GraphML.convert(sdg, bOut);
			SDG2GraphML.convertHierachical(sdg, bOutHierachical);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testAliasingLeaks() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.AliasingLeaks.class);
	}

	@Test
	public void testAliasLeak() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.AliasLeak.class);
	}

	@Test
	public void testAliasPrecise() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testPrecise(     joana.api.testdata.demo.AliasPrecise.class);
	}

	@Test
	public void testAliasTooImprecise() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testTooImprecise(joana.api.testdata.demo.AliasTooImprecise.class);
	}

	@Test
	public void testDynamicDispatch() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.DynamicDispatch.class);
	}

	@Test
	public void testExceptionLeak() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.ExceptionLeak.class);
	}

	@Test
	public void testExceptionTest() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.ExceptionTest.class);
	}

	@Test
	public void testExcTest() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.ExcTest.class);
	}

	@Test
	public void testFig2_1() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.Fig2_1.class);
	}

	@Test
	public void testFig3_1() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testPrecise(     joana.api.testdata.demo.Fig3_1.class);
	}

	@Test
	public void testFig3_2() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testPrecise(     joana.api.testdata.demo.Fig3_2.class);
	}

	@Test
	public void testInformationFlowLeaks() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.InformationFlowLeaks.class);
	}

	@Test
	public void testSequentialLeaks() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.SequentialLeaks.class);
	}

	@Test
	public void testStaticFieldSideEffects() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.StaticFieldSideEffects.class);
	}

	@Test
	public void testTypesAndObjectFields() throws ClassHierarchyException, ApiTestException, IOException,
											UnsoundGraphException, CancelException{
		testSound(       joana.api.testdata.demo.TypesAndObjectFields.class);
	}
}
