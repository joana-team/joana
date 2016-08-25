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
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class XLSODTests {

	static final Stubs STUBS = Stubs.JRE_14;

	static final boolean outputPDGFiles = false;
	static final boolean outputGraphMLFiles = false;
	
	static final String outputDir = "out";
	
	static final SDGConfig top_concurrent = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			true, // interference
			MHPType.PRECISE);
	static final SDGConfig bottom_concurrent = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS,
			ExceptionAnalysis.ALL_NO_ANALYSIS, FieldPropagation.OBJ_GRAPH, PointsToPrecision.TYPE_BASED, false, // no
																											// access
																											// paths
			true, // interference
			MHPType.SIMPLE);

	static final IEditableLattice<SDGConfig> configurations;
	static {
		configurations = new EditableLatticeSimple<SDGConfig>();
		for (SDGConfig c : new SDGConfig[] { top_concurrent, bottom_concurrent }) {
			configurations.addElement(c);
		}
		configurations.setImmediatelyGreater(bottom_concurrent, top_concurrent);

		if (outputPDGFiles || outputGraphMLFiles) {
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
	
	
	private static <T> void testSound(Class<T> clazz, IFCType ifcType) throws ClassHierarchyException, ApiTestException,
	IOException, UnsoundGraphException, CancelException {
		final String classname = clazz.getCanonicalName();
		{ // There are leaks, and we're sound and hence report them
			IFCAnalysis ana = buldAndUseJavaAnnotations(clazz, top_concurrent, true);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
			}
			if (outputGraphMLFiles) {
				dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
			}

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(ifcType);
			assertFalse(illegal.isEmpty());
		}
	}
	
	private static <T> void testPrecise(Class<T> clazz, IFCType ifcType) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		final String classname = clazz.getCanonicalName();
		{ // There are no leak, and  we're precise enough to find out that there aren't
			IFCAnalysis ana = buldAndUseJavaAnnotations(clazz, top_concurrent, true);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
			}
			if (outputGraphMLFiles) {
				dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
			}

			Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(ifcType);
			assertTrue(illegal.isEmpty());
		}
	}

	private static <T> void testTooImprecise(Class<T> clazz, IFCType ifcType) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		testSound(clazz, ifcType);
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
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       de.uni.trier.infsec.core.Setup.class, IFCType.LSOD);
		testSound(       de.uni.trier.infsec.core.Setup.class, IFCType.RLSOD);
		testSound(       de.uni.trier.infsec.core.Setup.class, IFCType.iRLSOD);
		testSound(       de.uni.trier.infsec.core.Setup.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testDe_uni_trier_infsec_core_SetupNoLeak() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       de.uni.trier.infsec.core.SetupNoLeak.class, IFCType.LSOD);
		testTooImprecise(de.uni.trier.infsec.core.SetupNoLeak.class, IFCType.RLSOD);
		testTooImprecise(     de.uni.trier.infsec.core.SetupNoLeak.class, IFCType.iRLSOD);
		testPrecise(     de.uni.trier.infsec.core.SetupNoLeak.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testPossibilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.PossibilisticLeaks.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.PossibilisticLeaks.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.PossibilisticLeaks.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.PossibilisticLeaks.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testProbabilisticOKDueToJoin() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPrecise(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testProbabilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.ProbabilisticLeaks.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.ProbabilisticLeaks.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.ProbabilisticLeaks.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.ProbabilisticLeaks.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testProbabilisticOK() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testTooImprecise(joana.api.testdata.demo.ProbabilisticOK.class, IFCType.LSOD); // see comment in test data class
		testPrecise(     joana.api.testdata.demo.ProbabilisticOK.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.ProbabilisticOK.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.ProbabilisticOK.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testProbabilisticSmall() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testTooImprecise(joana.api.testdata.demo.Prob_Small.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.Prob_Small.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.Prob_Small.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.Prob_Small.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testFig2_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.Fig2_1.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.Fig2_1.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.Fig2_1.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.Fig2_1.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testContextSens() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(         joana.api.testdata.demo.xrlsod.ContextSens.class, IFCType.LSOD);
		testTooImprecise(  joana.api.testdata.demo.xrlsod.ContextSens.class, IFCType.RLSOD);
		testTooImprecise(  joana.api.testdata.demo.xrlsod.ContextSens.class, IFCType.iRLSOD);
		testPrecise(       joana.api.testdata.demo.xrlsod.ContextSens.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testContextSensClassical() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ContextSensClassical.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSensClassical.class, IFCType.RLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSensClassical.class, IFCType.iRLSOD);
		testPrecise(joana.api.testdata.demo.xrlsod.ContextSensClassical.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testContextSensTiming() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ContextSensTiming.class, IFCType.LSOD);
		testPrecise(joana.api.testdata.demo.xrlsod.ContextSensTiming.class, IFCType.RLSOD);
		// both iRLSOD implementations are imprecise since they lack context-sensitivity
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSensTiming.class, IFCType.iRLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSensTiming.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testContextSens2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ContextSens2.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSens2.class, IFCType.RLSOD);
		// both iRLSOD implementations are imprecise since they lack context-sensitivity
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSens2.class, IFCType.iRLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ContextSens2.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testTimeSens() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.TimeSens.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.TimeSens.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.TimeSens.class, IFCType.iRLSOD);
		// timingiRLSOD implementation is imprecise -- see comment in test class
		testTooImprecise(joana.api.testdata.demo.xrlsod.TimeSens.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testFig2_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.Fig2_2.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.Fig2_2.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.Fig2_2.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.Fig2_2.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testFig2_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.Fig2_3.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.Fig2_3.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.Fig2_3.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.Fig2_3.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testFig3_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPrecise(     joana.api.testdata.demo.Fig3_1.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_1.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_1.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_1.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testFig3_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testPrecise(     joana.api.testdata.demo.Fig3_2.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_2.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_2.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_2.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testFig3_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.Fig3_3.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_3.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_3.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.Fig3_3.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testLateSecretAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.LateSecretAccess.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.LateSecretAccess.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.LateSecretAccess.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.LateSecretAccess.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testNoSecret() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.NoSecret.class, IFCType.LSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.NoSecret.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.NoSecret.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.NoSecret.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD1.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD1.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD1.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD1.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD2.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD2.class, IFCType.RLSOD);
		testTooImprecise(     joana.api.testdata.demo.xrlsod.ORLSOD2.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD2.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.RLSOD);
		testTooImprecise(     joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD4() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD4.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD4.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD4.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD4.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD5a.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD5a.class, IFCType.RLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD5a.class, IFCType.iRLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD5a.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD5b() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class, IFCType.LSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class, IFCType.RLSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class, IFCType.iRLSOD);
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD5Secure() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class, IFCType.RLSOD);
		testTooImprecise(     joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSODImprecise() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSODImprecise.class, IFCType.LSOD);
		testPrecise     (joana.api.testdata.demo.xrlsod.ORLSODImprecise.class, IFCType.RLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSODImprecise.class, IFCType.iRLSOD);
		// This kind of "embarassing" regression (wrt. RLSOD) is due classification of
		// H2 = H; as high, and the fact that the iRLSOD check does not differentiate between
		// the security level of the value read/written at some such a program point, and it's effect on
		// the "timing" it has on subsequent points
		
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSODImprecise.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testSwitchManyCases() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(     joana.api.testdata.demo.xrlsod.SwitchManyCases.class, IFCType.LSOD);
		testSound(     joana.api.testdata.demo.xrlsod.SwitchManyCases.class, IFCType.RLSOD);
		testSound(     joana.api.testdata.demo.xrlsod.SwitchManyCases.class, IFCType.iRLSOD);
		testSound(     joana.api.testdata.demo.xrlsod.SwitchManyCases.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testTimingCascade() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(     joana.api.testdata.demo.xrlsod.TimingCascade.class, IFCType.LSOD);
		testSound(     joana.api.testdata.demo.xrlsod.TimingCascade.class, IFCType.RLSOD);
		testSound(     joana.api.testdata.demo.xrlsod.TimingCascade.class, IFCType.iRLSOD);
		testSound(     joana.api.testdata.demo.xrlsod.TimingCascade.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testAlmostTimingCascade1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.AlmostTimingCascade1.class, IFCType.LSOD);
		testPrecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade1.class, IFCType.RLSOD);
		testPrecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade1.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.AlmostTimingCascade1.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testAlmostTimingCascade2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.AlmostTimingCascade2.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade2.class, IFCType.RLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade2.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.AlmostTimingCascade2.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testAlmostTimingCascade3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.AlmostTimingCascade3.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade3.class, IFCType.RLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade3.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.AlmostTimingCascade3.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testAlmostTimingCascade4() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {

		testSound(       joana.api.testdata.demo.xrlsod.AlmostTimingCascade4.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade4.class, IFCType.RLSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.AlmostTimingCascade4.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.AlmostTimingCascade4.class, IFCType.timingiRLSOD);
	}
}
