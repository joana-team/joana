package tests;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.ifc.orlsod.ClassicCDomOracle;
import edu.kit.joana.ifc.orlsod.ORLSODChecker;
import edu.kit.joana.ifc.orlsod.PathBasedORLSODChecker;
import edu.kit.joana.ifc.orlsod.PredecessorMethod;
import edu.kit.joana.ifc.orlsod.ProbInfComputer;
import edu.kit.joana.ifc.orlsod.ThreadModularCDomOracle;
import edu.kit.joana.ifc.orlsod.TimimgClassificationChecker;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.ifc.sdg.util.sdg.ReducedCFGBuilder;
import edu.kit.joana.util.Util;
import tests.ORLSODExperiment.StandardTestConfig;

public class ORLSODExperimentTiming {

	@Test
	public void doORLSOD1() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/ORLSOD1", "orlsod1", 1, 2, 4));
	}

	@Test
	public void doORLSOD2() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/ORLSOD2", "orlsod2", 1, 2, 0));
	}

	@Test
	public void doORLSOD3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/ORLSOD3", "orlsod3", 1, 2, 0));
	}
	@Test
	public void doNoSecret() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/NoSecret", "noSecret", 0, 2, 0));
	}

	@Test
	public void doLateSecretAccess() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/LateSecretAccess", "lateSecAccess", 1, 2, 0));
	}

	private static void doConfigTiming(ORLSODExperiment.TestConfig cfg) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		SDG sdg = JoanaRunner.buildSDG(cfg.progDesc.classPath, cfg.progDesc.mainClass);
		CSDGPreprocessor.preprocessSDG(sdg);
		CFG redCFG = ReducedCFGBuilder.extractReducedCFG(sdg);
		Util.removeCallCallRetEdges(redCFG);
		DomExperiment.export(redCFG, DomExperiment.joanaGraphExporter(), cfg.outputFiles.dotFile);
		PreciseMHPAnalysis mhp = PreciseMHPAnalysis.analyze(sdg);
		PruneInterferences.pruneInterferences(sdg, mhp);
		PrintWriter pw = new PrintWriter(cfg.outputFiles.pdgFile);
		SDGSerializer.toPDGFormat(sdg, pw);
		pw.close();
		Map<SDGNode, String> userAnn = new HashMap<SDGNode, String>();
		int noHighThings = 0;
		for (SDGNode src : cfg.srcSelector.select(sdg)) {
			userAnn.put(src, BuiltinLattices.STD_SECLEVEL_HIGH);
			System.out.println(String.format("userAnn(%s) = %s", src, BuiltinLattices.STD_SECLEVEL_HIGH));
			noHighThings++;
		}
		Assert.assertEquals(cfg.expectedNoHighThings, noHighThings);
		int noLowThings = 0;
		for (SDGNode snk : cfg.snkSelector.select(sdg)) {
			userAnn.put(snk, BuiltinLattices.STD_SECLEVEL_LOW);
			System.out.println(String.format("userAnn(%s) = %s", snk, BuiltinLattices.STD_SECLEVEL_LOW));
			noLowThings++;
		}
		Assert.assertEquals(cfg.expectedNoLowThings, noLowThings);
		ThreadModularCDomOracle tmdo = new ThreadModularCDomOracle(sdg);
		TimimgClassificationChecker<String> checkerSlice = new TimimgClassificationChecker<>(sdg, BuiltinLattices.getBinaryLattice(), userAnn, mhp, tmdo, PredecessorMethod.SLICE);
		int noVios = checkerSlice.check();
		Assert.assertEquals(cfg.expectedNoViolations, noVios);
		
		TimimgClassificationChecker<String> checkerEdge = new TimimgClassificationChecker<>(sdg, BuiltinLattices.getBinaryLattice(), userAnn, mhp, tmdo, PredecessorMethod.EDGE);
		checkerEdge.check();
		Assert.assertEquals(checkerSlice.getCL(), checkerEdge.getCL());
		Assert.assertEquals(checkerSlice.getCLT(), checkerEdge.getCLT());
		
	}

	@Test
	public void testORLSOD5a() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/ORLSOD5a", "orlsod5a", 1, 2, 6));
	}

	@Test
	public void testORLSODSecure() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lorlsod/ORLSOD5Secure", "orlsod5secure", 1, 2, 0));
	}

	
	@Test
	public void testPost_Fig2_3() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		doConfigTiming(new ORLSODExperiment.StandardTestConfig("example/bin", "Lpost16/Fig2_3", "post_fig2_3", 1, 2, 4));
	}

	@Test
	public void testORLSOD_imprecise() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		/**
		 * NOTE: The program is actually secure AND TimingClassification does detect this
		 * RLSOD and LSOD deem this program secure (no "normal" flows and o low-observable conflict).
		 * TODO: add test code which proves this silly claim!
		 */
		doConfigTiming(new StandardTestConfig("example/bin", "Lorlsod/ORLSODImprecise", "orlsod_imprecise", 1, 1, 0));
	}
}
