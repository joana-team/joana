/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.ConstructionNotifier;
import edu.kit.joana.api.sdg.SDGBuildPreparation;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.qifc.nildumu.util.Pair;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * Fluent API for creating SDGConfigs and for loading SDGPrograms
 * 
 * Based on JoanaPath, {@link SDGBuilder} and
 * {@link SDGProgram} (code from these classes is copied almost directly)
 * 
 * Contract: entry(…) → build(…) → [dump(…)] → analyze() → [dumpDotGraphs()]
 */
public class Builder {

	public static final String PROPERTIES_FILE = "classpaths.properties";
	public static final String TEST_DATA_CLASSPATH;
	public static final String TEST_DATA_GRAPHS;
	
	private static final Map<String, BuildResult> cache = new HashMap<>();

	static {
		TEST_DATA_CLASSPATH = loadProperty("joana.api.testdata.classpath", "bin");
		TEST_DATA_GRAPHS = loadProperty("joana.api.testdata.graphs", "graphs");
		Stream.of(TEST_DATA_CLASSPATH, TEST_DATA_GRAPHS).forEach(p -> {
			try {
				Files.createDirectories(Paths.get(p));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}); 
	}

	private static String loadProperty(String key, String defaultValue) {
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(new File(PROPERTIES_FILE)));
		} catch (IOException e) {
			//e.printStackTrace();
		}
		if (p.containsKey(key)) {
			return p.getProperty(key);
		}
		return defaultValue;
	}

	/**
	 * Modified version of {@link SDGProgram#createSDGProgram(String, String, Stubs, boolean, MHPType, PrintStream, IProgressMonitor)}
	 */
	private static <T> Pair<SDGBuilder, SDGProgram> createSDGProgram(SDGConfig config) throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException{
		PrintStream out = IOFactory.createUTF8PrintStream(new ByteArrayOutputStream());
		IProgressMonitor monitor = NullProgressMonitor.INSTANCE;
		monitor.beginTask("build SDG", 20);
		ConstructionNotifier notifier = config.getNotifier();
		if (notifier != null) {
			notifier.sdgStarted();
		}
		final com.ibm.wala.util.collections.Pair<SDG, SDGBuilder> p =
				SDGBuildPreparation.computeAndKeepBuilder(out, SDGProgram.makeBuildPreparationConfig(config), monitor);
		final SDG sdg = p.fst;
		final SDGBuilder buildArtifacts = p.snd;

		if (config.computeInterferences()) {
			CSDGPreprocessor.preprocessSDG(sdg);
		}

		final MHPAnalysis mhpAnalysis = config.getMhpType().getMhpAnalysisConstructor().apply(sdg);
		assert (mhpAnalysis == null) == (config.getMhpType() == MHPType.NONE);

		if (config.computeInterferences()) {
			PruneInterferences.pruneInterferences(sdg, mhpAnalysis);
		}

		if (notifier != null) {
			notifier.sdgFinished();
			notifier.numberOfCGNodes(buildArtifacts.getNonPrunedWalaCallGraph().getNumberOfNodes(), buildArtifacts.getWalaCallGraph().getNumberOfNodes());
		}
		if (config.getIgnoreIndirectFlows()) {
			if (notifier != null) {
				notifier.stripControlDepsStarted();
			}
			SDGProgram.throwAwayControlDeps(sdg);
			if (notifier != null) {
				notifier.stripControlDepsFinished();
			}

		}
		final SDGProgram ret = new SDGProgram(sdg, mhpAnalysis);

		if (config.isSkipSDGProgramPart()) {
			return new Pair<>(buildArtifacts, ret);
		}


		final IClassHierarchy ch  = buildArtifacts.getClassHierarchy();
		final CallGraph callGraph = buildArtifacts.getWalaCallGraph(); 
		ret.fillWithAnnotations(ch, SDGProgram.findClassesRelevantForAnnotation(ch, callGraph));
		return new Pair<>(buildArtifacts, ret);
	}


	private SDGConfig config = new SDGConfig(TEST_DATA_CLASSPATH, true, null, Stubs.JRE_15,
			ExceptionAnalysis.IGNORE_ALL, FieldPropagation.OBJ_GRAPH, PointsToPrecision.TYPE_BASED, false, // no
			false, // no interference
			MHPType.NONE);

	private Path dumpDir = Paths.get(TEST_DATA_GRAPHS);

	private BuildResult res = null;

	private String className;
	
	private Method entryMethod = null;

	private boolean dumpAfterBuild = false;
	
	private String methodInvocationHandler = "basic";
	
	private boolean doCache = true;
	
	public Builder() {
		DotRegistry.get().disable();
	}
	
	/**
	 * Set the entry class
	 */
	public Builder entry(String className) {
		config.setEntryMethod(JavaMethodSignature.mainMethodOfClass(className).toBCString());
		this.className = className;
		return this;
	}

	public Builder entry(Class<?> clazz) {
		return entry(clazz.getName());
	}

	public Builder entryMethod(Method method) {
		this.entryMethod = method;
		return this;
	}
	
	public Builder classpath(String classpath) {
		config.setClassPath(classpath);
		return this;
	}

	/**
	 * Sets the directory where the PDGs and other graphs are dumped via {@code dump()}
	 */
	public Builder dumpDir(String dirName) {
		this.dumpDir = Paths.get(dirName);
		DotRegistry.get().setTmpDir(dirName);
		return this;
	}

	public BuildResult build() throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException {
		if (!cache.containsKey(className) || !doCache) {
			Pair<SDGBuilder, SDGProgram> pair = createSDGProgram(config);
			IFCAnalysis ana = new IFCAnalysis(pair.second);
			ana.addAllJavaSourceAnnotations();
			res = new BuildResult(pair.first, ana);
			if (dumpAfterBuild) {
				dump();
				dumpDotGraphs();
			}
			if (doCache) {
				cache.put(className, res);
			} else {
				return res;
			}
		}
		res = cache.get(className);
		return res;
	}

	public BuildResult buildOrDie() {
		try {
			return build();
		} catch (ClassHierarchyException | UnsoundGraphException | CancelException | IOException e) {
			e.printStackTrace();
			System.exit(1);
			return null;
		}
	}
	
	public Program buildProgram() throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException {
		build();
		return new Program(res, entryMethod).setMethodInvocationHandler(methodInvocationHandler);
	}

	public Program buildProgramOrDie() {
		buildOrDie();
		return new Program(res, entryMethod).setMethodInvocationHandler(methodInvocationHandler);
	}
	
	/**
	 * Call a build method before
	 */
	public Builder dump() {
		assert res != null;
		try {
			try {
				Files.createDirectories(dumpDir);
			} catch (IOException e) {
				e.printStackTrace();
			}
			BufferedOutputStream bOut = 
					new BufferedOutputStream(new FileOutputStream(dumpDir.resolve(className + ".pdg").toFile()));
			SDGSerializer.toPDGFormat(res.analysis.getProgram().getSDG(), bOut);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return this;
	}
	
	public Builder dumpDotGraphs() {
		DotRegistry.get().storeFiles();
		return this;
	}
	
	public Builder enableDumpAfterBuild() {
		dumpAfterBuild = true;
		DotRegistry.get().enable();
		return this;
	}
	
	public Builder methodInvocationHandler(String handler) {
		methodInvocationHandler = handler;
		return this;
	}
	
	/**
	 * Enable caching of the SDG creation
	 */
	public Builder cache() {
		this.doCache = true;
		return this;
	}

	public Builder omitSummaryEdges(){
		config.setComputeSummaryEdges(false);
		return this;
	}

}
