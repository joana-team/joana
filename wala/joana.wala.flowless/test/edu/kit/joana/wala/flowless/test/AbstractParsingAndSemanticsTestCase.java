/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.util.config.AnalysisScopeReader;

import edu.kit.joana.wala.flowless.spec.FlowLessBuilder;
import edu.kit.joana.wala.flowless.spec.FlowLessSimpleSemantic;
import edu.kit.joana.wala.flowless.spec.FlowLessSimpleSemantic.SemanticException;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.java.LightweightParser;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import junit.framework.TestCase;

public abstract class AbstractParsingAndSemanticsTestCase extends TestCase {

	private static class CacheCha {
		private final String projectLocation;
		private final ClassHierarchy cha;
		private final List<ClassInfo> classes;

		private CacheCha(String projectLocation, ClassHierarchy cha, List<ClassInfo> classes) {
			this.projectLocation = projectLocation;
			this.cha = cha;
			this.classes = classes;
		}
	}

	private static CacheCha cache = null;

	private final String projectLocation;

	private List<ClassInfo> classes;
	private ClassHierarchy cha;

	public AbstractParsingAndSemanticsTestCase(String projectLocation) {
		this.projectLocation = projectLocation;
	}

	@Override
	public void setUp() {
		if (cache != null && cache.projectLocation.equals(projectLocation)) {
			// do not recompute...
			cha = cache.cha;
			classes = cache.classes;
			System.out.println("Using cached version for project '" + projectLocation + "'");

			return;
		}

		try {
			System.out.print("Parsing for IFC statements... ");
			classes = parseJavaFilesInAllSubDirs(projectLocation);
			FlowLessBuilder.checkForFlowStatements(classes);
			System.out.print("Running class hierarchy analysis... ");
			AnalysisScope scope = AnalysisScopeReader.makeJavaBinaryAnalysisScope("bin/", null);
			cha = ClassHierarchyFactory.make(scope);
			System.out.println("done. - " + cha.getNumberOfClasses() + " classes found.");
			cache = new CacheCha(projectLocation, cha, classes);
		} catch (IOException e) {
			e.printStackTrace();
			assertFalse(e.getMessage(), true);
		} catch (ClassHierarchyException e) {
			e.printStackTrace();
			assertFalse(e.getMessage(), true);
		}
	}

	protected void runExpectedError(String className, String methodName, String failMessage) {
		System.out.println("Analyzing " + className + "." + methodName + " - expect error message.");

		ClassInfo lib2 = findClass(className);
		MethodInfo method = findMethod(lib2, methodName);
		boolean expectedException = false;
		try {
			FlowLessSimpleSemantic.check(method);
		} catch (SemanticException e) {
			System.out.println("Catched expected exception: " + e.getMessage());
			expectedException = true;
		} catch (FlowAstException e) {
			System.out.println("Catched unexpected exception: " + e.getMessage());
		}

		assertTrue(failMessage, expectedException);
	}

	protected void runExpectedOk(String className, String methodName, String failMessage) {
		System.out.println("Analyzing " + className + "." + methodName + " - expect all ok.");

		ClassInfo lib2 = findClass(className);
		MethodInfo method = findMethod(lib2, methodName);
		boolean expectedException = false;
		try {
			FlowLessSimpleSemantic.check(method);
		} catch (SemanticException e) {
			System.out.println("Catched unexpected exception: " + e.getMessage());
			expectedException = true;
		} catch (FlowAstException e) {
			System.out.println("Catched unexpected exception: " + e.getMessage());
		}

		assertFalse(failMessage, expectedException);
	}

	private MethodInfo findMethod(ClassInfo info, String name) {
		for (MethodInfo method : info.getMethods()) {
			if (method.getName().contains(name)) {
				return method;
			}
		}

		return null;
	}

	private ClassInfo findClass(String name) {
		for (ClassInfo info : classes) {
			if (name.equals(info.getName())) {
				return info;
			}
		}

		return null;
	}

	private static List<ClassInfo> parseJavaFilesInAllSubDirs(String dir) throws IOException {
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory() | !f.canRead()) {
			throw new IllegalArgumentException(dir + " is not an existing and readable directory.");
		}

		List<ClassInfo> result = new LinkedList<ClassInfo>();

		result.addAll(parseJavaFilesInDir(dir));

		for (File fl : f.listFiles()) {
			if (fl.isDirectory()) {
				result.addAll(parseJavaFilesInAllSubDirs(fl.getAbsolutePath()));
			}
		}

		return result;
	}

	private static List<ClassInfo> parseJavaFilesInDir(String dir) throws IOException {
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory() | !f.canRead()) {
			throw new IllegalArgumentException(dir + " is not an existing and readable directory.");
		}

		String[] files = f.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				boolean ok = name.endsWith(".java") && (new File(dir + File.separator + name)).isFile();
				return ok;
			}
		});

		List<ClassInfo> result = new LinkedList<ClassInfo>();

		for (String file : files) {
			System.out.println("<<<<<<<< Parsing " + f.getAbsolutePath() + File.separator + file);
			result.addAll(LightweightParser.parseFile(f.getAbsolutePath() + File.separator + file));
		}

		return result;
	}

}
