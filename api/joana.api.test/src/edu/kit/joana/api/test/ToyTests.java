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

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IllicitFlow;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.MHPType;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ToyTests {

	static final String CLASSPATH_APP = "../joana.api.testdata/bin";
	static final String CLASSPATH_ANNOTATIONS_IGNORE = "../joana.api.annotations.ignore/bin";
	static final String CLASSPATH_ANNOTATIONS_PASSON = "../joana.api.annotations.passon/bin";
	static final Stubs STUBS = Stubs.JRE_14;

	static final boolean outputPDGFiles = false;
	
	static final String outputDir = "out";
	
	static final SDGConfig top_sequential = new SDGConfig(CLASSPATH_APP, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			false, // no interference
			MHPType.NONE);
	static final SDGConfig bottom_sequential = new SDGConfig(CLASSPATH_APP, null, STUBS,
			ExceptionAnalysis.ALL_NO_ANALYSIS, FieldPropagation.OBJ_GRAPH, PointsToPrecision.TYPE, false, // no
																											// access
																											// paths
			false, // no interference
			MHPType.NONE);

	static final IEditableLattice<SDGConfig> configurations;
	static {
		configurations = new EditableLatticeSimple<SDGConfig>();
		for (SDGConfig c : new SDGConfig[] { top_sequential, bottom_sequential }) {
			configurations.addElement(c);
		}
		configurations.setImmediatelyGreater(bottom_sequential, top_sequential);

		if (outputPDGFiles) {
			File fOutDir = new File(outputDir);
			if (!fOutDir.exists()) {
				fOutDir.mkdir();
			}
		}
	}

	public static IFCAnalysis buildAndAnnotate(final String className, SDGConfig config, boolean ignore)
			throws ApiTestException, ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		final String classPath;
		if (ignore) {
			classPath = CLASSPATH_APP + ":" + CLASSPATH_ANNOTATIONS_IGNORE;
		} else {
			classPath = CLASSPATH_APP + ":" + CLASSPATH_ANNOTATIONS_PASSON;
		}
		config.setClassPath(classPath);
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		config.setEntryMethod(mainMethod.toBCString());

		SDGProgram prog = SDGProgram.createSDGProgram(config);

		IFCAnalysis ana = new IFCAnalysis(prog);
		SDGProgramPart secret = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET");
		SDGProgramPart secret_string = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET_STRING");
		SDGProgramPart secret_bool = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET_BOOL");
		SDGProgramPart secret_object = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.SECRET_OBJECT");

		SDGProgramPart output = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(I)V");
		SDGProgramPart output_string = ana
				.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(Ljava/lang/String;)V");
		SDGProgramPart output_bool = ana.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(Z)V");
		SDGProgramPart output_object = ana
				.getProgramPart("edu.kit.joana.api.annotations.Annotations.leak(Ljava/lang/Object;)V");

		assertTrue(secret != null || secret_string != null || secret_bool != null || secret_object != null);
		assertTrue(output != null || output_string != null || output_bool != null || output_object != null);

		if (secret != null)
			ana.addSourceAnnotation(secret, BuiltinLattices.STD_SECLEVEL_HIGH);
		if (secret_string != null)
			ana.addSourceAnnotation(secret_string, BuiltinLattices.STD_SECLEVEL_HIGH);
		if (secret_bool != null)
			ana.addSourceAnnotation(secret_bool, BuiltinLattices.STD_SECLEVEL_HIGH);
		if (secret_object != null)
			ana.addSourceAnnotation(secret_object, BuiltinLattices.STD_SECLEVEL_HIGH);

		if (output != null)
			ana.addSinkAnnotation(output, BuiltinLattices.STD_SECLEVEL_LOW);
		if (output_string != null)
			ana.addSinkAnnotation(output_string, BuiltinLattices.STD_SECLEVEL_LOW);
		if (output_bool != null)
			ana.addSinkAnnotation(output_bool, BuiltinLattices.STD_SECLEVEL_LOW);
		if (output_object != null)
			ana.addSinkAnnotation(output_object, BuiltinLattices.STD_SECLEVEL_LOW);
		return ana;
	}

	private static void testPreciseEnough(String classname) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		{ // There are leaks if secret is really passed on
			IFCAnalysis ana = buildAndAnnotate(classname, top_sequential, false);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".passon.pdg");
			}

			Collection<IllicitFlow> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}

		{ // Otherwise, we're precise enough to find out that there aren't
			IFCAnalysis ana = buildAndAnnotate(classname, top_sequential, true);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".ignore.pdg");
			}

			Collection<IllicitFlow> illegal = ana.doIFC();
			assertTrue(illegal.isEmpty());
		}
	}

	private static void testTooImprecise(String classname) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		{ // There are leaks if secret is really passed on
			IFCAnalysis ana = buildAndAnnotate(classname, top_sequential, false);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".passon.pdg");
			}

			Collection<IllicitFlow> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}

		{ // Otherwise there aren't, but the analysis not precise enough to
			// proof this
			IFCAnalysis ana = buildAndAnnotate(classname, top_sequential, true);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".ignore.pdg");
			}

			Collection<IllicitFlow> illegal = ana.doIFC();
			assertFalse(illegal.isEmpty());
		}
	}
	
	private static void dumpSDG(SDG sdg, String filename) throws FileNotFoundException {
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputDir + "/" + filename));
		SDGSerializer.toPDGFormat(sdg, bOut);
	}

	@Test
	public void testFlowSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.sensitivity.FlowSens");
	}

	@Test
	public void testAssChain() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.simp.AssChain");
	}

	@Test
	public void testMicroExample() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough("joana.api.testdata.toy.simp.MicroExample");
	}

	@Test
	public void testNested() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.simp.Nested");
	}

	// TODO: find out why we're not precise enough here.
	@Test
	public void testSick() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise("joana.api.testdata.toy.simp.Sick");
	}

	@Test
	public void testSick2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.simp.Sick2");
	}

	@Test
	public void testControlDep() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.test.ControlDep");
	}

	@Test
	public void testIndependent() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.test.Independent");
	}

	@Test
	public void testObjSens() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.test.ObjSens");
	}

	@Test
	public void testSystemCallsTest() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough("joana.api.testdata.toy.test.SystemCallsTest");
	}

	@Test
	public void testVeryImplictFlow() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough("joana.api.testdata.toy.test.VeryImplictFlow");
	}

	@Test
	public void testMyList() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testPreciseEnough("joana.api.testdata.toy.rec.MyList");
	}

	// TODO: find out why we're precise enough for MyList, but not for MyList2
	@Test
	public void testMyList2() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise("joana.api.testdata.toy.rec.MyList2");
	}

	@Test
	public void testPasswordFile() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough("joana.api.testdata.toy.pw.PasswordFile");
	}

	@Test
	public void testDemo1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		/**
		 * We are to imprecise at the moment (Dec 2012) to rule out information flow here in the 'ignore' case.
		 * See Demo1 source code for further information
		 */
		testTooImprecise("joana.api.testdata.toy.demo.Demo1");
	}

	@Test
	public void testDeclass1() throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException,
			CancelException {
		testTooImprecise("joana.api.testdata.toy.declass.Declass1");
	}

	@Test
	public void testExampleLeakage() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPreciseEnough("joana.api.testdata.seq.ExampleLeakage");
	}
}
