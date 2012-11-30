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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.ObjTreeType;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.PointsToType;
import edu.kit.joana.deprecated.jsdg.sdg.nodes.JDependencyGraph.PDGFormatException;
import edu.kit.joana.deprecated.jsdg.slicing.LineNrSlicer;
import edu.kit.joana.deprecated.jsdg.slicing.LineNrSlicer.Line;
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
public class SimpleSlicer {

	private static void printUsage() {
		System.out.println("Simple Slicer (for evalutation purposes) - Juergen Graf <graf@kit.edu>");
		System.out.println();
		System.out.println("This program creates a system dependence graph for a java program ");
		System.out.println("or computes a backward slice for a given edu.kit.joana.deprecated.jsdg.slicing criterion.");
		System.out.println("THIS IS WORK IN PROGRESS. It most likely contains errors.");
		System.out.println();
		System.out.println("You may run this program in two different modi: SDG or SLICE.");
		System.out.println("Mode SDG creates a system dependence graph for a given class that must");
		System.out.println("contain a main method.");
		System.out.println("Mode SLICE creates a backward slice for a given system dependence graph and");
		System.out.println("a set of edu.kit.joana.deprecated.jsdg.slicing criteria.");
		System.out.println();
		System.out.println("Usage Mode SDG:");
		System.out.println("\tjava -jar simple-slicer.jar sdg <path> <classname>");
		System.out.println();
		System.out.println("\t<path>\n\t\tPath to class files or jar file");
		System.out.println("\t<classname>\n\t\tBytecode name of the class containing the main method");
		System.out.println();
		System.out.println("Usage Mode SLICE:");
		System.out.println("\tjava -jar simple-slicer.jar slice <sdgfile> <edu.kit.joana.deprecated.jsdg.slicing criterion>+");
		System.out.println();
		System.out.println("\t<sdgfile>\n\t\tFilename of sdg created with SDG mode. (<classname>.pdg)");
		System.out.println("\t<edu.kit.joana.deprecated.jsdg.slicing criterion>\n\t\tFilename and linenumber that should be used as");
		System.out.println("\t\tslicing criterion. E.g. MyClass.java:32");
		System.out.println();
		System.out.println("Examples:");
		System.out.println("\tjava -jar simple-slice.jar sdg MyProject/bin Ledu/kit/ipd/Main");
		System.out.println("\tjava -jar simple-slice.jar slice Main.pdg edu/kit/Main.java:123");
		System.out.println("\tjava -jar simple-slice.jar slice Main.pdg \\");
		System.out.println("\t\tpath/to/SomeClass.java:123 path/to/SomeOtherClass.java:143");
	}

	public static void main(String[] args) throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		if (args.length < 1) {
			printUsage();
			return;
		}

		final String mode = args[0].trim().toLowerCase();

		if ("sdg".equals(mode)) {
			if (args.length != 3) {
				printUsage();
				return;
			}

			final String j2seLibs = System.getenv("JAVA_LIB");
			if (j2seLibs == null) {
				System.out.println("Please set the environment variable JAVA_LIB to the path of the java runtime");
				System.out.println("libraries.");
				System.out.println("This will be $JAVA_HOME/lib or $JAVA_HOME/jre/lib in most cases.");
				return;
			}

			File libDir = new File(j2seLibs);
			if (!libDir.isDirectory()) {
				System.out.println("JAVA_LIB is set to '" + j2seLibs + "': This is not a directory.");
				return;
			}

			final String classpath = args[1];
			final String bcMainClass = args[2];
			final String mainClass = bcMainClass.replace('/', '.').replace('$', '.').substring(1);
			final String sdgOutputFile = mainClass + ".pdg";
			final String sdgLogFile = mainClass + ".log";

			// compute SDG
			computeSDG(j2seLibs, classpath, bcMainClass, sdgOutputFile, sdgLogFile);
		} else if ("slice".equals(mode)) {
			if (args.length < 3) {
				printUsage();
				return;
			}

			final String sdgFile = args[1];
			File test = new File(sdgFile);
			if (!test.exists() || !test.isFile() || !test.canRead()) {
				System.err.println("ERR: No valid file. Can not read sdg from file \"" + sdgFile + "\".");
				return;
			}

			final Set<LineNrSlicer.Line> criterion = new HashSet<LineNrSlicer.Line>();
			for (int i = 2; i < args.length; i++) {
				String[] crit = args[i].split(":");
				if (crit != null && crit.length == 2) {
					String filename = crit[0];
					int lineNr = -1;
					try {
						lineNr = Integer.parseInt(crit[1]);
					} catch (NumberFormatException exc) {
						System.err.println("ERR: Line number no int: Ignoring edu.kit.joana.deprecated.jsdg.slicing criterion no. "
								+ (i - 1) + " \"" + args[i] + "\"");
					}

					if (lineNr >= 0) {
						LineNrSlicer.Line line = new LineNrSlicer.Line(filename, lineNr);
						criterion.add(line);
					}
				} else {
					System.err.println("ERR: Ignoring edu.kit.joana.deprecated.jsdg.slicing criterion no. " + (i - 1) + " \"" + args[i] + "\"");
				}
			}

			if (!criterion.isEmpty()) {
				System.out.println("Slicing criterion are: ");
				for (LineNrSlicer.Line line : criterion) {
					System.out.println("\t" + line);
				}
			} else {
				System.err.println("ERR: No edu.kit.joana.deprecated.jsdg.slicing criterion provided. Only creating SDG now.");
			}

			// begin edu.kit.joana.deprecated.jsdg.slicing
			runSlicing(sdgFile, criterion);
		} else {
			printUsage();
		}
	}

	/**
	 * @param sdgOutputFile
	 * @param criterion
	 * @throws IOException
	 */
	private static void runSlicing(String sdgOutputFile, Set<Line> criterion) throws IOException {
		System.out.print("Initializing slicer - loading SDG... ");
		LineNrSlicer slicer = new LineNrSlicer(sdgOutputFile);
		slicer.readIn();
		System.out.println("done.");
		System.out.println("Slice contains:");
		Set<Line> slice = slicer.sliceBackward(criterion);
		for (Line line : slice) {
			System.out.println(line);
		}
	}

	/**
	 * Run the SDG generator and create an SDG.
	 * @param classpath
	 * @param bcMainClass
	 * @param sdgOutputFile
	 * @throws WalaException
	 * @throws IOException
	 * @throws PDGFormatException
	 * @throws CancelException
	 * @throws IllegalArgumentException
	 * @throws InvalidClassFileException
	 */
	private static void computeSDG(String j2seLibs, String classpath, String bcMainClass,
			String sdgOutputFile, String sdgLogFile) throws IllegalArgumentException, CancelException, PDGFormatException, IOException, WalaException, InvalidClassFileException {
		SDGFactory.Config cfg = new SDGFactory.Config();
		cfg.addControlFlow = true;
		cfg.classpath = classpath;
		cfg.mainClass = bcMainClass;
		cfg.computeInterference = false;
		cfg.computeSummaryEdges = true;
		cfg.ignoreExceptions = false;
		cfg.exclusions = new LinkedList<String>();
		cfg.exclusions.add("java/awt/.*");
		cfg.exclusions.add("java/security/.*");
		cfg.exclusions.add("javax/swing/.*");
		cfg.exclusions.add("sun/awt/.*");
		cfg.exclusions.add("sun/swing/.*");
		cfg.exclusions.add("com/sun/.*");
		cfg.exclusions.add("sun/.*");
		cfg.logFile = sdgLogFile;
		cfg.outputDir = "./";
		cfg.outputSDGfile = sdgOutputFile;
		cfg.logLevel = Log.LogLevel.INFO;
		cfg.pointsTo = PointsToType.ZERO_CFA;
		cfg.objTree = ObjTreeType.PTS_GRAPH;
		cfg.useWalaSdg = false;
		cfg.scopeData = new LinkedList<String>();
		String[] stdlibs = getJarsInDirectory(j2seLibs);
		for (String lib: stdlibs) {
			cfg.scopeData.add("Primordial,Java,jarFile," + lib);
			System.out.println("adding library to scope: " + lib);
		}
//		cfg.scopeData.add("Primordial,Java,stdlib,none");
//		cfg.scopeData.add("Primordial,Java,jarFile,lib/jSDG-stubs-jre1.4.jar");
//		cfg.nativesXML = "lib/natives_empty.xml";
		cfg.nativesXML = "lib/natives_orig_wala.xml";

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
