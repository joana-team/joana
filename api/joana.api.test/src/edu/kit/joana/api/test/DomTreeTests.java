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
import java.util.Set;
import java.util.function.BiFunction;

import javax.xml.stream.XMLStreamException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.io.graphml.SDG2GraphML;
import edu.kit.joana.ifc.sdg.irlsod.ClassicCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.DomTree;
import edu.kit.joana.ifc.sdg.irlsod.ICDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.RegionBasedCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.RegionClusterBasedCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.graph.ThreadInformationUtil;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class DomTreeTests {

	static enum Result { CYCLIC, ACYCLIC };
	
	static final Stubs STUBS = Stubs.JRE_14;

	static final boolean outputPDGFiles = false;
	static final boolean outputGraphMLFiles = false;
	static final boolean outputDotFiles = true;
	
	static final String outputDir = "out";
	
	static final SDGConfig top_concurrent = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			true, // interference
			MHPType.PRECISE);

	static {

		if (outputPDGFiles || outputGraphMLFiles || outputDotFiles) {
			File fOutDir = new File(outputDir);
			if (!fOutDir.exists()) {
				fOutDir.mkdir();
			}
		}
	}
	
	public static <T> IFCAnalysis build(Class<T> clazz, SDGConfig config) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		final String className = clazz.getCanonicalName();
		final String classPath;
		classPath = JoanaPath.JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + JoanaPath.ANNOTATIONS_PASSON_CLASSPATH;
		config.setClassPath(classPath);
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		config.setEntryMethod(mainMethod.toBCString());
		SDGProgram prog = SDGProgram.createSDGProgram(config);

		IFCAnalysis ana = new IFCAnalysis(prog);
		return ana;
	}
	
	private static BiFunction<SDG, PreciseMHPAnalysis, ICDomOracle> newRegionBasedCDomOracle =
		(sdg,mhp) -> {
			RegionBasedCDomOracle oracle = new RegionBasedCDomOracle(sdg, mhp);
			oracle.buildRegionGraph();
			return oracle;
		};
	
	
	/**
	 * RegionClusterBasedCDomOracle does not seem to work for the majority of test cases, so we don't test it anymore.
	 */
	@SuppressWarnings("unused")
	private static BiFunction<SDG, PreciseMHPAnalysis, ICDomOracle> newRegionClusterBasedOrcle =
		(sdg,mhp) -> {
			RegionClusterBasedCDomOracle oracle = new RegionClusterBasedCDomOracle(sdg, mhp);
			oracle.buildRegionGraph();
			return oracle;
		};
	
	private static BiFunction<SDG, PreciseMHPAnalysis, ICDomOracle> newClassicCDomOracle =
		(sdg,mhp) -> {
			ClassicCDomOracle oracle = new ClassicCDomOracle(sdg, mhp);
			return oracle;
		};
		
	private static BiFunction<SDG, PreciseMHPAnalysis, ICDomOracle> newThreadModularCDomOracle =
			(sdg,mhp) -> new ThreadModularCDomOracle(sdg);
	
	private static class Common {
		PreciseMHPAnalysis mhp;
		SDG sdg;
		DirectedGraph<ThreadInstance, DefaultEdge> tct;
		String classname;
	};
	
	private static <T> Common getCommon(Class<T> clazz) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		final Common result = new Common();
		result.classname = clazz.getCanonicalName();
		IFCAnalysis ana = build(clazz, top_concurrent);
		result.sdg = ana.getProgram().getSDG();
		result.mhp = PreciseMHPAnalysis.analyze(result.sdg);
		result.tct = ThreadInformationUtil.buildThreadCreationTree(result.sdg.getThreadsInfo());

		if (outputPDGFiles) {
			dumpSDG(ana.getProgram().getSDG(), result.classname + ".pdg");
		}
		if (outputGraphMLFiles) {
			dumpGraphML(ana.getProgram().getSDG(), result.classname + ".pdg");
		}
		if (outputDotFiles) {
			MiscGraph2Dot.export(result.tct, MiscGraph2Dot.tctExporter(), outputDir + "/" + result.classname + ".tct.dot");	
		}
		
		return result;
	}
	
	private static <T> void testDomTree(Common common, BiFunction<SDG, PreciseMHPAnalysis, ICDomOracle> newOracle, Result result)
			throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		final ICDomOracle oracle = newOracle.apply(common.sdg,common.mhp);
		final DomTree tree = new DomTree(common.sdg, oracle , common.mhp);
		
		if (outputDotFiles) {
			MiscGraph2Dot.export(tree.getTree(), MiscGraph2Dot.cdomTreeExporter(), outputDir + "/" + common.classname + ".cdom.dot");	
		}
		
		CycleDetector<VirtualNode, DefaultEdge> detector = new CycleDetector<>(tree.getTree());
		final Set<VirtualNode> cycles = detector.findCycles();
		
		switch (result) {
			case CYCLIC : assertFalse( 0 == cycles.size()); break;
			case ACYCLIC: assertTrue(  0 == cycles.size()); break;
		}
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

	public static void main(String[] args) throws
			ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		new DomTreeTests().testPossibilisticLeaks();
	}

	/*
	 * For now, one time-consuming test is enough :) 
	@Test
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       de.uni.trier.infsec.core.Setup.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
	}
	 */
	
	@Test
	public void testDe_uni_trier_infsec_core_SetupNoLeak() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(de.uni.trier.infsec.core.SetupNoLeak.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.CYCLIC);
	}
	
	@Test
	public void testPossibilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.PossibilisticLeaks.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);

	}
	
	@Test
	public void testProbabilisticOKDueToJoin() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testProbabilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.ProbabilisticLeaks.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testProbabilisticOK() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.ProbabilisticOK.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC); // see comment in test data class
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC); // see comment in test data class
	}
	
	@Test
	public void testProbabilisticSmall() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.Prob_Small.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);

	}
	
	@Test
	public void testFig2_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig2_1.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testFig2_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig2_2.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testFig2_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig2_3.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);

	}
	
	@Test
	public void testFig3_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(     joana.api.testdata.demo.Fig3_1.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);

	}
	
	@Test
	public void testFig3_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(     joana.api.testdata.demo.Fig3_2.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testFig3_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig3_3.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testLateSecretAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.LateSecretAccess.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testNoSecret() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.NoSecret.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD1.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD2.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD3.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.CYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD4() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD4.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.CYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.xrlsod.ORLSOD5a.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.CYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD5b() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.CYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSOD5Secure() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.CYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
	
	@Test
	public void testORLSODImprecise() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.xrlsod.ORLSODImprecise.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
	}
}
