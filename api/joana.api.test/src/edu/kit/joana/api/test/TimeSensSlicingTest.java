/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.test.util.BuildSDG;
import edu.kit.joana.api.test.util.JoanaPath;
import edu.kit.joana.ifc.sdg.core.SecurityNode.SecurityNodeFactory;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaForward;

/**
 * @author Martin Mohr
 */
public class TimeSensSlicingTest {
	
	private static class TestData {
		final String mainClass;
		final String sdgFile;
		TestData(final String mainClass, final String sdgFile) {
			this.mainClass = mainClass;
			this.sdgFile = outputDir + File.separator + sdgFile;
		}
	}
	
	private static Map<String, TestData> testData = new HashMap<String, TestData>();
	
	static {
		testData.put("thread_hierarchy",
				new TestData("joana.api.testdata.conc.ThreadHierarchy", "thread_hierarchy.pdg"));
	}
	
	private static final boolean FORCE_REBUILD = true;
	private static final String outputDir = "out";
	
	@BeforeClass
	public static void setUp() {
		/** test failure will only be visible if assertions are enabled */
		TimeSensSlicingTest.class.getClassLoader().setDefaultAssertionStatus(true);
		for (final TestData td : testData.values()) {
			if (FORCE_REBUILD) {
				final BuildSDG b = BuildSDG.standardConcSetup(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, td.mainClass, td.sdgFile);
				b.run();
			} else {
				final File f = new File(td.sdgFile);
				if (!f.exists() || !f.canRead()) {
					final BuildSDG b = BuildSDG.standardConcSetup(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, td.mainClass, td.sdgFile);
					b.run();
				}
			}
		}
	}
	
	@AfterClass
	public static void tearDown() {
		/** turn off assertions again to not influence later tests unintentionally */
		TimeSensSlicingTest.class.getClassLoader().setDefaultAssertionStatus(false);
	}
	
	public void doTestAPIStyle(String key) throws IOException {
		final TestData t = testData.get(key);
		if (t == null) {
			Assert.fail("wrong test key: " + key);
		}
		
		SDGProgram p = SDGProgram.loadSDG(t.sdgFile);
		IFCAnalysis ifcAnalysis = new IFCAnalysis(p);
		ifcAnalysis.setLattice(BuiltinLattices.getBinaryLattice());
		ifcAnalysis.setTimesensitivity(true);
		
		
		for (SDGMethod m : p.getMethods("joana.api.testdata.conc.Thread2.<init>()V")) {
			ifcAnalysis.addSourceAnnotation(m, BuiltinLattices.STD_SECLEVEL_HIGH);
		}
		
		for (SDGMethod m : p.getMethods("joana.api.testdata.conc.Thread2.run()V")) {
			ifcAnalysis.addSinkAnnotation(m, BuiltinLattices.STD_SECLEVEL_LOW);
		}
		
		
		ifcAnalysis.doIFC(IFCType.RLSOD);
	
	}
	
	public void doTestGoodOldFashioned(String key) throws IOException {
		final TestData t = testData.get(key);
		if (t == null) {
			Assert.fail("wrong test key: " + key);
		}
		
		SDG sdg = SDG.readFrom(t.sdgFile, new SecurityNodeFactory());
		
		Nanda tsfwSlicer = new Nanda(sdg, new NandaForward());
		for (SDGNode n : sdg.vertexSet()) {
			tsfwSlicer.slice(n);
		}
	}
	
	@Test
	public void doThreadHierarchyTestAPIStyle() throws IOException {
		doTestAPIStyle("thread_hierarchy");
	}
	
	@Test
	public void doThreadHierarchyTestGoodOldFashioned() throws IOException {
		doTestGoodOldFashioned("thread_hierarchy");
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
