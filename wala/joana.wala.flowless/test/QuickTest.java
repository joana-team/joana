/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.kit.joana.wala.flowless.spec.FlowLessBuilder;
import edu.kit.joana.wala.flowless.spec.FlowLessLexer;
import edu.kit.joana.wala.flowless.spec.FlowLessParser;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.LightweightJava;
import edu.kit.joana.wala.flowless.spec.java.LightweightParser;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;


public class QuickTest {

	public static void main(String[] args) throws IOException, RecognitionException, IllegalArgumentException, IllegalAccessException {
//		test("examples/ok1.flo");
//		test("examples/ok2.flo");
//		test("examples/ok3.flo");
//		test("examples/ok4.flo");
//		testJLexer("test/QuickTest.java");
//		testJLexer("src/edu/kit/pp/mojo/spec/FLowLessLexer.java");
//		testJLexer("src/edu/kit/pp/mojo/spec/FLowLessParser.java");
		FlowLessBuilder.clearErrors();
		FlowLessBuilder.parseJavaFile("test/QuickTest.java");
		if (FlowLessBuilder.hadErrors()) {
			for (FlowError err : FlowLessBuilder.getAllErrors()) {
				System.err.println(err.toString());
			}
		}
//		List<ClassInfo> classes = LightweightParser.parseFile("test/QuickTest.java");
//		LightweightParser.parseFile("src/edu/kit/pp/mojo/spec/FLowLessLexer.java");
//		LightweightParser.parseFile("src/edu/kit/pp/mojo/spec/FLowLessParser.java");
//		parseJavaFilesInDir("test");
///		parseJavaFilesInDir("../../workspace-jSDG/com.ibm.wala.core/src/com/ibm/wala/cfg");
//		parseJavaFilesInAllSubDirs("../../workspace-jSDG/com.ibm.wala.core/src");
//		parseJavaFilesInAllSubDirs("../../workspace-jSDG/com.ibm.wala.cast/source/java");
//		parseJavaFilesInAllSubDirs("../../workspace-jSDG/com.ibm.wala.shrike/src");
//		parseJavaFilesInAllSubDirs("../../workspace-jSDG/");
//		parseJavaFilesInAllSubDirs("../../workspace-Praktomat/");
//		parseJavaFilesInAllSubDirs("../../workspace-Compiler/");
//		LightweightParser.parseFile("../../workspace-jSDG/jSDG-Stubs-JRE1.5/src/java/lang/StrictMath.java");
//		LightweightParser.parseFile("../../workspace-jSDG/jSDG-Stubs-Android_2.1_r2/src-tagsuop/org/ccil/cowan/tagsoup/AttributesImpl.java");
//		LightweightParser.parseFile("../../workspace-jSDG/jSDG-Stubs-Android_2.1_r2/src-java/javax/crypto/EncryptedPrivateKeyInfo.java");
//		LightweightParser.parseFile("../../workspace-jSDG/jSDG-Stubs-Android_2.1_r2/src/android/view/View.java");
//		LightweightParser.parseFile("../../workspace-jSDG/jSDG-Stubs-Android_2.1_r2/src/android/opengl/GLWrapperBase.java");
//		LightweightParser.parseFile("../../workspace-jSDG/com.ibm.wala.core/src/com/ibm/wala/util/heapTrace/HeapTracer.java");
//		LightweightParser.parseFile("../../workspace-jSDG/com.ibm.wala.core/src/com/ibm/wala/model/SyntheticFactory.java");
//		LightweightParser.parseFile("../../workspace-jSDG/com.ibm.wala.core/src/com/ibm/wala/cfg/ControlFlowGraph.java");
//		LightweightParser.parseFile("../../workspace-jSDG/com.ibm.wala.core/src/com/ibm/wala/cfg/InducedCFG.java");
//		for (ClassInfo cls : classes) {
//			FlowLessBuilder.checkForFlowStatements(cls);
//		}
	}

	QuickTest() {}

	// ifc: {fileName, f2} & !{a, b, c} | {x, y} => y->x
	//@ ifc: alias(fileName, fileName) => fileName->\state
	private static void test(String fileName) throws IOException, RecognitionException {
		FlowLessLexer lexer = new FlowLessLexer(new ANTLRFileStream(fileName));
		FlowLessParser parser = new FlowLessParser(new CommonTokenStream(lexer));

		IFCStmt ifc = parser.ifc_stmt();
		if (ifc != null) {
			System.out.println(ifc);
		} else {
			System.out.println("No. of errors: " + parser.getNumberOfSyntaxErrors());
		}
	}

	private static void parseJavaFilesInAllSubDirs(String dir) throws IOException {
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory() | !f.canRead()) {
			throw new IllegalArgumentException(dir + " is not an existing and readable directory.");
		}

		parseJavaFilesInDir(dir);

		for (File fl : f.listFiles()) {
			if (fl.isDirectory()) {
				parseJavaFilesInAllSubDirs(fl.getAbsolutePath());
			}
		}
	}

	private static void parseJavaFilesInDir(String dir) throws IOException {
		File f = new File(dir);
		if (!f.exists() || !f.isDirectory() | !f.canRead()) {
			throw new IllegalArgumentException(dir + " is not an existing and readable directory.");
		}

		String[] files = f.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".java") && (new File(dir + File.separator + name)).isFile();
			}
		});

		for (String file : files) {
			System.out.println("<<<<<<<< Parsing " + f.getAbsolutePath() + File.separator + file);
			LightweightParser.parseFile(f.getAbsolutePath() + File.separator + file);
		}
	}

	/**
	 *
	 * 	  What a beautiful comment!
	 *
	 */


//
//	private static void testJdtLexer(String fileName) throws IOException, IllegalArgumentException, IllegalAccessException {
//		IErrorHandlingPolicy policy = new IErrorHandlingPolicy() {
//			@Override
//			public boolean stopOnFirstError() {
//				return true;
//			}
//
//			@Override
//			public boolean proceedOnErrors() {
//				return false;
//			}
//		};
//
//		CompilerOptions opt = new CompilerOptions();
//		opt.sourceLevel = ClassFileConstants.JDK1_6;
//		opt.docCommentSupport = true;
//		opt.ignoreMethodBodies = true;
//
//		IProblemFactory fact = new DefaultProblemFactory();
//
//		ProblemReporter reporter = new ProblemReporter(policy, opt, fact);
//
//		Parser p = new Parser(reporter, true);
//
//		String contents = "";
//
//		{
//			StringBuffer fileData = new StringBuffer(1000);
//	        BufferedReader reader = new BufferedReader(new FileReader(fileName));
//	        char[] buf = new char[1024];
//	        int numRead = 0;
//
//	        while((numRead = reader.read(buf)) != -1){
//	            fileData.append(buf, 0, numRead);
//	        }
//
//	        reader.close();
//	        contents = fileData.toString();
//		}
//
//		ICompilationUnit sourceUnit = new CompilationUnit(contents.toCharArray(), fileName.toCharArray());
//
//		CompilationResult compilationResult = new CompilationResult(sourceUnit, 0, 0, 0);
//
//		CompilationUnitDeclaration cdecl =  p.parse(sourceUnit, compilationResult);
//		for (TypeDeclaration td : cdecl.types) {
//			for (AbstractMethodDeclaration md : td.methods) {
//				System.out.println("Found method " + new String(md.selector) + " in " + new String(td.name));
//				Javadoc doc = md.javadoc;
//				if (doc != null) {
//					char[] comment = new char[doc.sourceEnd - doc.sourceStart];
//					System.arraycopy(sourceUnit.getContents(), doc.sourceStart, comment, 0, comment.length);
//					System.out.println("Comment:\n" + new String(comment));
//				}
//			}
//		}
//	}

	private static void testJLexer(String fileName) throws IOException, IllegalArgumentException, IllegalAccessException {
		LightweightJava lexer = new LightweightJava(new ANTLRFileStream(fileName));

//		for (Token tok = lexer.nextToken(); tok != null && tok.getType() != Token.EOF; tok = lexer.nextToken()) {
//			System.out.println(LightweightJava.getTokenName(tok.getType()) + ":" + tok.getLine());
//		}

//		Token tok = lexer.nextToken();
//		while (tok.getType() != Token.EOF) {
//			System.out.println(tok);
//			tok = lexer.nextToken();
//		}
	}

}
