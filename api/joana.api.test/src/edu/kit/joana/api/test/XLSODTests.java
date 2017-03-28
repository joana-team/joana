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

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class XLSODTests {

	static final boolean outputPDGFiles = false;
	static final boolean outputGraphMLFiles = false;
	
	private static IFCAnalysis buildAnnotateDump(Class<?> clazz) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		IFCAnalysis ana = BuildSDG.buldAndUseJavaAnnotations(clazz, BuildSDG.top_concurrent, true);
	
		final String classname = clazz.getCanonicalName();
		
		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
		}
		
		return ana;
	}
	
	private static void testSound(Class<?> clazz, IFCType ifcType) throws ClassHierarchyException, ApiTestException,
	IOException, UnsoundGraphException, CancelException {
		// There are leaks, and we're sound and hence report them
		IFCAnalysis ana = buildAnnotateDump(clazz);

		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(ifcType);
		assertFalse(illegal.isEmpty());
	}
	
	private static void testPrecise(Class<?> clazz, IFCType ifcType) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		// There are no leak, and  we're precise enough to find out that there aren't
		IFCAnalysis ana = buildAnnotateDump(clazz);

		Collection<? extends IViolation<SecurityNode>> illegal = ana.doIFC(ifcType);
		assertTrue(illegal.isEmpty());
	}

	private static void testTooImprecise(Class<?> clazz, IFCType ifcType) throws ClassHierarchyException, ApiTestException,
			IOException, UnsoundGraphException, CancelException {
		testSound(clazz, ifcType);
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
		testPrecise(     de.uni.trier.infsec.core.SetupNoLeak.class, IFCType.iRLSOD);
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
		testPrecise(  joana.api.testdata.demo.xrlsod.ContextSens.class, IFCType.iRLSOD);
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
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD2.class, IFCType.iRLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD2.class, IFCType.timingiRLSOD);
	}
	
	@Test
	public void testORLSOD3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testSound(       joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.LSOD);
		testTooImprecise(joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.RLSOD);
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD3.class, IFCType.iRLSOD);
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
		testPrecise(     joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class, IFCType.iRLSOD);
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
