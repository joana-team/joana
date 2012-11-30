/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.ObjTreeType;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.PointsToType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.util.Log;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.wala.util.VerboseProgressMonitor;

/**
 * Simple class to create SDGs that are not correct but can be used for metrics,
 * o. ae.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class SimpleSDG {

	public static void main(String[] args) throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		if (args.length != 2) {
			System.out.println("Simple SDG builder (for evalutation purposes) - Juergen Graf <graf@kit,edu>");
			System.out.println();
			System.out.println("This program creates a system depenence graph for a java program ");
			System.out.println("which does NOT COVER ALL DEPENENCIES.");
			System.out.println();
			System.out.println("Effects of exceptions as well as data flow through the heap are left out.");
			System.out.println();
			System.out.println("Usage: java -jar simple-sdg.jar <path> <name>");
			System.out.println("\t<path>\tPath to class files or jar file");
			System.out.println("\t<name>\tBytecode name ot the class containing the main method");
			System.out.println("Examples:");
			System.out.println("\tjava -jar simple-sdg.jar MyProject/bin Ledu/kit/ipd/Main");
			System.out.println("\tjava -jar simple-sdg.jar MyProject.jar Ledu/kit/ipd/Foo$InnerClass");
			return;
		}

		String j2seLibs = System.getenv("JAVA_LIB");
		if (j2seLibs == null) {
			System.out.println("Please set the environment variable JAVA_LIB to the path of the java runtime");
			System.out.println("libraries.");
			System.out.println("This will be $JAVA_HOME/lib or $JAVA_HOME/jre/lib in most cases.");
			return;
		}

		File libDir = new File(j2seLibs);
		if (!libDir.isDirectory()) {
			System.out.println("JAVA_LIB is set to '" + j2seLibs + "': This is not a directory.");
		}

		String mainClass = args[1].replace('/', '.').replace('$', '.').substring(1);

		SDGFactory.Config cfg = new SDGFactory.Config();
		cfg.addControlFlow = true;
		cfg.classpath = args[0];
		cfg.mainClass = args[1];
		cfg.computeInterference = false;
		cfg.computeSummaryEdges = false;
		cfg.ignoreExceptions = true;
		cfg.exclusions = new LinkedList<String>();
		cfg.exclusions.add("java/awt/.*");
		cfg.exclusions.add("java/security/.*");
		cfg.exclusions.add("javax/swing/.*");
		cfg.exclusions.add("sun/awt/.*");
		cfg.exclusions.add("sun/swing/.*");
		cfg.exclusions.add("com/sun/.*");
		cfg.exclusions.add("sun/.*");
		cfg.logFile = mainClass + ".log";
		cfg.outputDir = "./";
		cfg.outputSDGfile = mainClass + ".pdg";
		cfg.logLevel = Log.LogLevel.INFO;
		cfg.pointsTo = PointsToType.ZERO_CFA;
		cfg.objTree = ObjTreeType.ZERO;
		cfg.useWalaSdg = false;
		cfg.scopeData = new LinkedList<String>();
		String[] stdlibs = getJarsInDirectory(j2seLibs);
		for (String lib: stdlibs) {
			cfg.scopeData.add("Primordial,Java,jarFile," + lib);
			System.out.println("adding library to scope: " + lib);
		}
//		cfg.scopeData.add("Primordial,Java,stdlib,none");
//		cfg.scopeData.add("Primordial,Java,jarFile,lib/jSDG-stubs-jre1.4.jar");
		cfg.nativesXML = "lib/natives_empty.xml";
///		cfg.nativesXML = "lib/natives_orig_wala.xml";

		Analyzer.cfg = cfg;

		IProgressMonitor progress = new VerboseProgressMonitor(System.out);
		SDGFactory fact = new SDGFactory();
		SDG sdg = fact.getJoanaSDG(cfg, progress);

		progress.beginTask("Saving SDG to " + cfg.outputSDGfile, -1);
		BufferedOutputStream bOut = new BufferedOutputStream(
				new FileOutputStream(cfg.outputSDGfile));
		SDGSerializer.toPDGFormat(sdg, bOut);
		progress.done();
	}

	private static String[] getJarsInDirectory(String dir) {
		Collection<File> col = FileUtil.listFiles(dir, ".*\\.jar$", true);
		String[] result = new String[col.size()];
		int i = 0;
		for (File jarFile : col) {
			result[i++] = jarFile.getAbsolutePath();
		}
		return result;
	}

}
