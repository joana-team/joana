/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import edu.kit.joana.ifc.sdg.qifc.nildumu.interproc.MethodInvocationHandler;

/**
 * Runs the program on the command line
 */
@Parameters(commandDescription="Basic quantitative information flow analysis")
public class Main {

	@Parameter(names="--handler", description="Method invocation handler configuration, "
			+ "run `--interprodhelp` to get more information")
	private String handler = "all";
	
	@Parameter(names="--classpath", description="Class path, by default uses the path from classpaths.properties. "
			+ "The classpath should contain the folder of the class to be analysed.")
	private String classPath = Builder.TEST_DATA_CLASSPATH;
	
	@Parameter(names="--dumppath", description="Dump path, by default uses the path from classpaths.properties") 
	private String dumpPath = Builder.TEST_DATA_GRAPHS;
	
	@Parameter(names="--dump", description="Dump graphs")
	private boolean dump = false;
	
	@Parameter(description="class name, class has to contain a 'program' method that is called in the main method", required=true)
	private String className;	
	
	@Parameter(names="--help", help=true)
	private boolean help;
	
	@Parameter(names="--interprodhelp", description="Print information on the method invocation handlers")
	private boolean interprodhelp;
	
	private static void printInterprodHelp() {
		System.out.println("Example handler configurations: ");
		for (String conf : MethodInvocationHandler.getExamplePropLines()) {
			System.out.println("   " + conf);
		}
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		JCommander com = JCommander.newBuilder().addObject(main).build();
		try {
			com.parse(args);
		} catch (ParameterException ex) {
			com.usage();
			return;
		}
		if (main.help) {
			com.usage();
			return;
		}
		if (main.interprodhelp) {
			printInterprodHelp();
			return;
		}
		Builder builder = new Builder().classpath(main.classPath)
				.methodInvocationHandler(main.handler)
				.entry(main.className).dumpDir(main.dumpPath);
		if (main.dump) {
			builder.enableDumpAfterBuild();
			BasicLogger.enable();
		} else {
			BasicLogger.disable();
		}
		try {
			Program program = builder.buildProgramOrDie();
			Context context = program.analyze();
			context.printLeakages();
		} finally {
			if (main.dump) {
				builder.dumpDotGraphs();
			}
		}
	}
}
