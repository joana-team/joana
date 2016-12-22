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
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.function.BiFunction;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.test.util.ApiTestException;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.DumpTestSDG;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.irlsod.ClassicCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.DomTree;
import edu.kit.joana.ifc.sdg.irlsod.ICDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.RegionBasedCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.RegionClusterBasedCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.sdg.irlsod.VeryConservativeCDomOracle;
import edu.kit.joana.ifc.sdg.util.graph.ThreadInformationUtil;
import edu.kit.joana.ifc.sdg.util.sdg.GraphModifier;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class DomTreeTests {

	static enum Result { CYCLIC, ACYCLIC };

	static final boolean outputPDGFiles = true;
	static final boolean outputGraphMLFiles = false;
	static final boolean outputDotFiles = true;
	
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
		IFCAnalysis ana = BuildSDG.build(clazz, BuildSDG.top_concurrent, false);
		result.sdg = ana.getProgram().getSDG();
		result.mhp = PreciseMHPAnalysis.analyze(result.sdg);
		result.tct = ThreadInformationUtil.buildThreadCreationTree(result.sdg.getThreadsInfo());

		if (outputPDGFiles) {
			DumpTestSDG.dumpSDG(ana.getProgram().getSDG(), result.classname + ".pdg");
		}
		if (outputGraphMLFiles) {
			DumpTestSDG.dumpGraphML(ana.getProgram().getSDG(), result.classname + ".pdg");
		}
		if (outputDotFiles) {
			DumpTestSDG.dumpDotTCT(result.tct, result.classname + ".tct.dot");	
		}
		
		return result;
	}
	
	private static <T> void testDomTree(Common common, BiFunction<SDG, PreciseMHPAnalysis, ICDomOracle> newOracle, Result result)
			throws ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		final ICDomOracle oracle = newOracle.apply(common.sdg,common.mhp);
		final DomTree tree = new DomTree(common.sdg, oracle , common.mhp);

		if (outputDotFiles) {
			DumpTestSDG.dumpDotCDomTree(tree,
			    common.classname + "." + oracle.getClass().getSimpleName() +".cdom.dot"
			);
		}
		
		boolean acyclic = tree.reduce();
		
		if (outputDotFiles) {
			DumpTestSDG.dumpDotCDomTree(tree,
			    common.classname + "." + oracle.getClass().getSimpleName() +".cdom.reduced.dot"
			);
		}
		
		
		switch (result) {
			case CYCLIC : assertFalse(acyclic); break;
			case ACYCLIC: assertTrue( acyclic); break;
		}
	}
	
	private void testDomGuarantees(Common common) throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final SDG sdg = common.sdg;
		final CFG icfg = ICFGBuilder.extractICFG(sdg);
		GraphModifier.removeCallCallRetEdges(icfg);
		final VirtualNode root = new VirtualNode(sdg.getRoot(), 0);
		
		ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
		RegionBasedCDomOracle rbdo = 
				new RegionBasedCDomOracle(sdg, PreciseMHPAnalysis.analyze(sdg));
		rbdo.buildRegionGraph();
		ClassicCDomOracle cldo = new ClassicCDomOracle(sdg,common.mhp);
		VeryConservativeCDomOracle vcdo = new VeryConservativeCDomOracle(icfg);

		ThreadRegion[] regions = common.mhp.getThreadRegions().toArray(new ThreadRegion[0]);
		for (int i = 0; i < regions.length; i++) {
			ThreadRegion r1 = regions[i];
			int threadN = r1.getThread();
			for (int j = i; j < regions.length; j++) {
				ThreadRegion r2 = regions[j];
				if (common.mhp.isParallel(r1,r2)) {
					int threadM = r2.getThread();
					for (SDGNode n : r1.getNodes()) {
						VirtualNode vn = new VirtualNode(n, threadN);
						for (SDGNode m : r2.getNodes()) {
							VirtualNode vm = new VirtualNode(m, threadM);
							
							VirtualNode vt = tmdo.cdom(n, threadN, m, threadM);
							VirtualNode vt_s = tmdo.cdom(m, threadM, n, threadN);
							VirtualNode vr = rbdo.cdom(n, threadN, m, threadM);
							VirtualNode vr_s = rbdo.cdom(m, threadM, n, threadN);
							VirtualNode vc = cldo.cdom(n, threadN, m, threadM);
							VirtualNode vc_s = cldo.cdom(m, threadM, n, threadN);
							VirtualNode vv = vcdo.cdom(n, threadN, m, threadM);
							VirtualNode vv_s = vcdo.cdom(m, threadM, n, threadN);
							
							assertEquals(vt, vt_s);
							assertEquals(vr, vr_s);
							assertEquals(vc, vc_s);
							assertEquals(vv, vv_s);
							
							assertEquals(root, vv);
							
							assertFalse(common.mhp.isParallel(vt,vn));
							assertFalse(common.mhp.isParallel(vt,vm));
							assertFalse(common.mhp.isParallel(vr,vn));
							assertFalse(common.mhp.isParallel(vr,vm));
							assertFalse(common.mhp.isParallel(vc,vn));
							assertFalse(common.mhp.isParallel(vc,vm));
							assertFalse(common.mhp.isParallel(vv,vn));
							assertFalse(common.mhp.isParallel(vv,vm));
						}
					}
				}
			}
		}
	}

	public static void main(String[] args) throws
			ClassHierarchyException, ApiTestException, IOException, UnsoundGraphException, CancelException {
		new DomTreeTests().testPossibilisticLeaks();
	}
	
	
	@Test
	public void testDe_uni_trier_infsec_core_Setup() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       de.uni.trier.infsec.core.Setup.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testDe_uni_trier_infsec_core_SetupNoLeak() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(de.uni.trier.infsec.core.SetupNoLeak.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testPossibilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.PossibilisticLeaks.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);

	}
	
	@Test
	public void testProbabilisticOKDueToJoin() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(     joana.api.testdata.demo.ProbabilisticOKDueToJoin.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testProbabilisticLeaks() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.ProbabilisticLeaks.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testProbabilisticOK() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.ProbabilisticOK.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC); // see comment in test data class
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC); // see comment in test data class
		testDomGuarantees(common);
	}
	
	@Test
	public void testProbabilisticSmall() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.Prob_Small.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);

	}
	
	@Test
	public void testFig2_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig2_1.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testFig2_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig2_2.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testFig2_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig2_3.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);

	}
	
	@Test
	public void testFig3_1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(     joana.api.testdata.demo.Fig3_1.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);

	}
	
	@Test
	public void testFig3_2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(     joana.api.testdata.demo.Fig3_2.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testFig3_3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.Fig3_3.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testLateSecretAccess() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.LateSecretAccess.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testNoSecret() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.NoSecret.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD1() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD1.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD2() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD2.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD3() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD3.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD4() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD4.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD5a() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.xrlsod.ORLSOD5a.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD5b() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD5b.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSOD5Secure() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(       joana.api.testdata.demo.xrlsod.ORLSOD5Secure.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testORLSODImprecise() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.demo.xrlsod.ORLSODImprecise.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
	
	@Test
	public void testLotsOfDominationInMainThread() throws ClassHierarchyException, ApiTestException, IOException,
			UnsoundGraphException, CancelException {
		final Common common = getCommon(joana.api.testdata.conc.LotsOfDominationInMainThread.class);
		testDomTree(common, newRegionBasedCDomOracle,   Result.ACYCLIC);
		testDomTree(common, newThreadModularCDomOracle, Result.ACYCLIC);
		testDomTree(common, newClassicCDomOracle      , Result.ACYCLIC);
		testDomGuarantees(common);
	}
}


