/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import edu.kit.joana.wala.eval.util.EvalException;
import edu.kit.joana.wala.eval.util.EvalPaths;
import edu.kit.joana.wala.eval.util.EvalTimingStats;
import edu.kit.joana.wala.eval.util.EvalTimingStats.TaskInfo;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class TestObjGraphPerformance {

	public static boolean lazy = true;
	
	public static class ApiTestException extends Exception {

		private static final long serialVersionUID = 7000978878774124747L;

		public ApiTestException(Throwable t) {
			super(t);
		}
	}
	
	public static EvalTimingStats stats = new EvalTimingStats(); 
	
	public SDGConfig createConfig(final String testCase, final PointsToPrecision pts,
			final FieldPropagation fprop, final Stubs stubs, final String cp, final String className) {
		final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
		final SDGConfig config = new SDGConfig(cp, mainMethod.toBCString(), stubs);
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(ExceptionAnalysis.INTRAPROC);
		config.setFieldPropagation(fprop);
		config.setPointsToPrecision(pts);
		config.setComputeAllocationSites(false);
		config.setComputeAccessPaths(false);
		config.setNotifier(stats);
		config.setSkipSDGProgramPart(true);
		config.setComputeSummaryEdges(false);
		stats.setCurrentTask(testCase);
		
		postCreateConfigHook(config);
		
		return config;
	}
	
	protected void postCreateConfigHook(final SDGConfig cfg) {
		// overwrite this method if you want to add something to the default config
	}

	public SDG buildSDG(final SDGConfig config, final int numberOfRuns) throws ApiTestException {
		SDG sdg = null;
		
		for (int i = 0; i < numberOfRuns; i++) {
			sdg = buildSDG(config);
			stats.nextRun();
		}
		
		return sdg;
	}
	
	public SDG buildSDG(final SDGConfig config) throws ApiTestException {
		SDGProgram prog = null;
		
		try {
			prog = SDGProgram.createSDGProgram(config);
		} catch (ClassHierarchyException e) {
			throw new ApiTestException(e);
		} catch (IOException e) {
			throw new ApiTestException(e);
		} catch (UnsoundGraphException e) {
			throw new ApiTestException(e);
		} catch (CancelException e) {
			throw new ApiTestException(e);
		}
		
		return prog.getSDG();
	}

	public static String currentMethodName() {
		final Throwable t = new Throwable();
		final StackTraceElement e = t.getStackTrace()[1];
		return e.getMethodName();
	}
	
	public static boolean areWeLazy(final String testMethodName) {
		if (!lazy) {
			return false;
		}
		
		try {
			final boolean nonEmptyLogExists = checkExists(EvalPaths.getOutputPath(testMethodName + ".log"));
			final boolean nonEmptySdgExists = checkExists(EvalPaths.getOutputPath(testMethodName + ".pdg"));
			
			return nonEmptyLogExists && nonEmptySdgExists;
		} catch (final EvalException e) {
			e.printStackTrace();
		}
		
		return false;
	}
	
	private static boolean checkExists(final String fileName) {
		final File f = new File(fileName);
		return f.exists() && f.isFile() && f.length() > 0;
	}
	
	public static void outputStatistics(final SDG sdg, final SDGConfig cfg, final String testMethodName) {
		stats.readAdditionalStats(sdg, cfg);
		final TaskInfo ti = stats.getCurrent();
		System.out.println(ti);
		// write stats to file
		try {
			final String statsFileName = EvalPaths.getOutputPath(testMethodName + ".log");
			final PrintWriter pw = new PrintWriter(statsFileName);
			pw.print(ti.toString());
			pw.println();
			pw.print(cfg.toString());
			pw.flush();
			pw.close();
		} catch (final EvalException e) {
			System.err.println("Could not write statistics to file: " + e.getMessage());
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			System.err.println("Could not write statistics to file: " + e.getMessage());
			e.printStackTrace();
		}
		
		try {
			final String sdgFileName = EvalPaths.getOutputPath(testMethodName + ".pdg");
			System.out.println("Writing sdg to file '" + sdgFileName + "'");
			final FileOutputStream sdgOut = new FileOutputStream(sdgFileName); 
			SDGSerializer.toPDGFormat(sdg, sdgOut);
			sdgOut.flush();
		} catch (final EvalException e) {
			System.err.println("Could not write sdg to file: " + e.getMessage());
			e.printStackTrace();
		} catch (final FileNotFoundException e) {
			System.err.println("Could not write sdg to file: " + e.getMessage());
			e.printStackTrace();
		} catch (final IOException e) {
			System.err.println("Could not write sdg to file: " + e.getMessage());
			e.printStackTrace();
		} 
	}

}
