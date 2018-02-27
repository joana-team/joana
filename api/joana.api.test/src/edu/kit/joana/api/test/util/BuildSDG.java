/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * Utility class to build SDGs for tests.
 *
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public final class BuildSDG {

	private static final Stubs STUBS = Stubs.JRE_15;

	public static final SDGConfig top_sequential = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			false, // no interference
			MHPType.NONE);

	public static final SDGConfig bottom_sequential = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS,
			ExceptionAnalysis.ALL_NO_ANALYSIS, FieldPropagation.OBJ_GRAPH, PointsToPrecision.TYPE_BASED, false, // no
																											// access
																											// paths
			false, // no interference
			MHPType.NONE);

	public static final SDGConfig top_concurrent = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS, ExceptionAnalysis.INTERPROC,
			FieldPropagation.OBJ_GRAPH, PointsToPrecision.OBJECT_SENSITIVE, false, // no
																					// access
																					// paths
			true, // interference
			MHPType.PRECISE);

	public static final SDGConfig bottom_concurrent = new SDGConfig(JoanaPath.JOANA_API_TEST_DATA_CLASSPATH, null, STUBS,
			ExceptionAnalysis.ALL_NO_ANALYSIS, FieldPropagation.OBJ_GRAPH, PointsToPrecision.TYPE_BASED, false, // no
																											// access
																											// paths
			true, // interference
			MHPType.SIMPLE);

	private BuildSDG() {

	}

	public static void saveSDGProgram(SDG sdg, String path) throws FileNotFoundException {
		SDGSerializer.toPDGFormat(sdg, new BufferedOutputStream(new FileOutputStream(path)));
	}

	public static SDGProgram standardConcBuild(String classPath, JavaMethodSignature entryMethod, String saveAs, PointsToPrecision ptsPrec) {
		SDGConfig cfg = new SDGConfig(classPath, entryMethod.toBCString(), STUBS);
		cfg.setComputeInterferences(true);
		cfg.setExceptionAnalysis(ExceptionAnalysis.IGNORE_ALL);
		cfg.setMhpType(MHPType.NONE);
		cfg.setFieldPropagation(FieldPropagation.OBJ_GRAPH);
		cfg.setPointsToPrecision(ptsPrec);
		cfg.setParallel(false);
		SDGProgram p;
		try {
			p = SDGProgram.createSDGProgram(cfg, new PrintStream(new ByteArrayOutputStream()),
					NullProgressMonitor.INSTANCE);
			SDG sdg = p.getSDG();
			PruneInterferences.preprocessAndPruneCSDG(sdg, MHPType.PRECISE);
			saveSDGProgram(sdg, saveAs);
			return p;
		} catch (ClassHierarchyException | IOException | UnsoundGraphException
					| CancelException e) {
			throw new RuntimeException(e);
		}
	}

	public static void standardConcBuild(String classPath, String mainClass, String saveAs) {
		standardConcBuild(classPath, JavaMethodSignature.mainMethodOfClass(mainClass), saveAs, PointsToPrecision.INSTANCE_BASED);
	}

	public static <T> IFCAnalysis build(Class<T> clazz, SDGConfig config, boolean ignore) throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		final String className = clazz.getName();
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

		return new IFCAnalysis(prog);
	}

	public static <T> IFCAnalysis buldAndUseJavaAnnotations(Class<T> clazz, SDGConfig config, boolean ignore)
				throws ApiTestException, ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
			IFCAnalysis ana = build(clazz,config,ignore);
			ana.addAllJavaSourceAnnotations();
			return ana;
	}

	public static <T> IFCAnalysis buldAndUseJavaAnnotations(Class<T> clazz, SDGConfig config, boolean ignore, IStaticLattice<String> l)
				throws ApiTestException, ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
			IFCAnalysis ana = build(clazz,config,ignore);
			ana.setLattice(l);
			ana.addAllJavaSourceAnnotations(l);
			return ana;
	}
}
