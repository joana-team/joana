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

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
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
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.io.graphml.SDG2GraphML;
import edu.kit.joana.ifc.sdg.irlsod.DomTree;
import edu.kit.joana.ifc.sdg.irlsod.ICDomOracle;
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

	static final Stubs STUBS = Stubs.JRE_14;

	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = false;
	
	static final String outputDir = "out";
	
	static final SDGConfig top_concurrent = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			true, // interference
			MHPType.PRECISE);

	static {

		if (outputPDGFiles || outputGraphMLFiles) {
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

	
	private static <T> void testDomTree(Class<T> clazz) throws ClassHierarchyException, ApiTestException,
	IOException, UnsoundGraphException, CancelException {
		final String classname = clazz.getCanonicalName();
		{ // There are leaks, and we're sound and hence report them
			IFCAnalysis ana = build(clazz, top_concurrent);

			if (outputPDGFiles) {
				dumpSDG(ana.getProgram().getSDG(), classname + ".pdg");
			}
			if (outputGraphMLFiles) {
				dumpGraphML(ana.getProgram().getSDG(), classname + ".pdg");
			}
			final SDG sdg = ana.getProgram().getSDG();
			final MHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
			final ICDomOracle oracle = new ThreadModularCDomOracle(sdg);
			final DomTree tree = new DomTree(sdg  , oracle , mhp);
			final DirectedGraph<ThreadInstance, DefaultEdge> tct = ThreadInformationUtil.buildThreadCreationTree(sdg.getThreadsInfo());
			MiscGraph2Dot.export(tree.getTree(), MiscGraph2Dot.cdomTreeExporter(), outputDir + "/" + classname + ".cdom.dot");
			MiscGraph2Dot.export(tct,            MiscGraph2Dot.tctExporter(),      outputDir + "/" + classname + ".tct.dot");
			CycleDetector<VirtualNode, DefaultEdge> detector = new CycleDetector<>(tree.getTree());
			System.out.println(detector.findCycles());
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


	/*
	 * For now, one time-consuming test is enough :) 
	@Test
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       de.uni.trier.infsec.core.Setup.class);
	}
	 */
	
	@Test
	public void testDe_uni_trier_infsec_core_SetupNoLeak() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       de.uni.trier.infsec.core.SetupNoLeak.class);

	}
	
	@Test
	public void testPossibilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.PossibilisticLeaks.class);

	}
	
	@Test
	public void testProbabilisticOKDueToJoin() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class);

	}
	
	@Test
	public void testProbabilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.ProbabilisticLeaks.class);
	}
	
	@Test
	public void testProbabilisticOK() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(joana.api.testdata.demo.ProbabilisticOK.class); // see comment in test data class
	}
	
	@Test
	public void testProbabilisticSmall() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(joana.api.testdata.demo.Prob_Small.class);

	}
	
	@Test
	public void testFig2_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.Fig2_1.class);
	}
	
	@Test
	public void testFig2_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.Fig2_2.class);
	}
	
	@Test
	public void testFig2_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.Fig2_3.class);

	}
	
	@Test
	public void testFig3_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(     joana.api.testdata.demo.Fig3_1.class);

	}
	
	@Test
	public void testFig3_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(     joana.api.testdata.demo.Fig3_2.class);
	}
	
	@Test
	public void testFig3_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.Fig3_3.class);
	}
	
	@Test
	public void testLateSecretAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.LateSecretAccess.class);
	}
	
	@Test
	public void testNoSecret() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.NoSecret.class);
	}
	
	@Test
	public void testORLSOD1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.ORLSOD1.class);
	}
	
	@Test
	public void testORLSOD2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.ORLSOD2.class);
	}
	
	@Test
	public void testORLSOD3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.ORLSOD3.class);
	}
	
	@Test
	public void testORLSOD4() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.ORLSOD4.class);
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(joana.api.testdata.demo.xrlsod.ORLSOD5a.class);
	}
	
	@Test
	public void testORLSOD5b() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class);
	}
	
	@Test
	public void testORLSOD5Secure() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(       joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class);
	}
	
	@Test
	public void testORLSODImprecise() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		testDomTree(joana.api.testdata.demo.xrlsod.ORLSODImprecise.class);
	}
}
