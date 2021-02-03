package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;

import java.io.IOException;

public class TestUtils {

	private static final String JARPATH = "../../../../dist/joana.ifc.sdg.qifc.qif_interpreter.jar";
	private static final String testFilePath = "testResources/";

	private static String path(String className) {
		return testFilePath + className+ ".class";
	}

	private static void compile(String file) throws IOException, InterruptedException {
		String cmd = String.format("javac -target 1.8 -source 1.8  %s -classpath %s", file, JARPATH);
		Process compilation = Runtime.getRuntime().exec(cmd);
		compilation.waitFor();
	}

	public static Program build(String fileName) throws IOException, InterruptedException {
		compile(testFilePath+fileName+".java");

		// create SDG
		IRBuilder builder = new IRBuilder(path(fileName), fileName);
		builder.createBaseSDGConfig();
		try {
			builder.buildAndKeepBuilder();
		} catch (IOException | CancelException | ClassHierarchyException | GraphIntegrity.UnsoundGraphException e) {
			e.printStackTrace();
		}
		return builder.getProgram();
	}

}
