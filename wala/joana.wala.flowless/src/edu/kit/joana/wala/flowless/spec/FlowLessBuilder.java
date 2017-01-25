/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.kit.joana.wala.flowless.spec.FlowLessSimpleSemantic.SemanticException;
import edu.kit.joana.wala.flowless.spec.ast.FlowAstVisitor.FlowAstException;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.LightweightParser;
import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo.Comment;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class FlowLessBuilder {

	public static final String JML_START = "//@";
	public static final String IFC_START = "ifc:";

	private static final boolean PRINT_DEBUG = false;

	private static List<FlowError> errors = new LinkedList<FlowError>();

	public static class FlowSyntaxException extends FlowAstException {

		private static final long serialVersionUID = 554497169466692954L;

		private final List<FlowError> errors;

		public FlowSyntaxException(String msg, List<FlowError> errs) {
			super(msg);
			this.errors = errs;
		}

		public List<FlowError> getErrors() {
			return errors;
		}

	}

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class FlowError {

		public final Exception exc;
		public final MethodInfo method;
		public final int lineNr;

		public FlowError(MethodInfo method, RecognitionException exc) {
			this.exc = exc;
			this.method = method;
			this.lineNr = method.getLine();
		}

		public FlowError(MethodInfo method, SemanticException exc) {
			this.exc = exc;
			this.method = method;
			this.lineNr = exc.getLineNr();
		}

		public FlowError(MethodInfo method, FlowAstException exc) {
			this.exc = exc;
			this.method = method;
			this.lineNr = method.getLine();
		}

		public String toString() {
			return "ERROR(" + method.toString() + "): " + exc.toString()
				+ (exc.getCause() != null ? " - " + exc.getCause().toString() : "");
		}
	}

	public static boolean hadErrors() {
		return errors.size() > 0;
	}

	public static int numberOfErrors() {
		return errors.size();
	}

	public static List<FlowError> getAllErrors() {
		return Collections.unmodifiableList(errors);
	}

	public static void clearErrors() {
		errors = new LinkedList<FlowError>();
	}

	public static List<ClassInfo> parseJavaFile(String file) throws IOException {
		List<ClassInfo> classes = LightweightParser.parseFile(file);
		clearErrors();
		checkForFlowStatements(classes);
		simpleSemanticCheck(classes);
		return classes;
	}

	public static List<ClassInfo> parseJavaStream(InputStream stream) throws IOException {
		List<ClassInfo> classes = LightweightParser.parseFile(stream);
		clearErrors();
		checkForFlowStatements(classes);
		simpleSemanticCheck(classes);
		return classes;
	}

	public static List<ClassInfo> parseJavaString(String input) {
		List<ClassInfo> classes = LightweightParser.parseString(input);
		clearErrors();
		checkForFlowStatements(classes);
		simpleSemanticCheck(classes);
		return classes;
	}

	/**
	 * Checks ifc statements for simple semantic errors, like non-matched parameter names.
	 * Does not check if referenced fields exists or types match.
	 * @param classes List of classes to check
	 * @return Number of errors found. Look them up through getAllErrors()
	 */
	public static int simpleSemanticCheck(List<ClassInfo> classes) {
		int numErrs = 0;

		for (ClassInfo cls : classes) {
			for (MethodInfo m : cls.getMethods()) {
				try {
					FlowLessSimpleSemantic.check(m);
				} catch (SemanticException e) {
					FlowError err = new FlowError(m, e);
					errors.add(err);
					numErrs++;
				} catch (FlowAstException e) {
					throw new IllegalStateException("Exception not of type SemanticException", e);
				}
			}
		}

		return numErrs;
	}

	/**
	 * Searches all methods of a list of classes for methods with ifc statements and adds
	 * the parsed statements to the corresponding MethodInfo objects.
	 * @param cls The classes to search in.
	 * @return If at least one method has a (syntactical) legal ifc statement defined.
	 */
	public static boolean checkForFlowStatements(List<ClassInfo> cls) {
		boolean ifcFound = false;

		for (ClassInfo cinfo : cls) {
			ifcFound |= checkForFlowStatements(cinfo);
		}

		return ifcFound;
	}

	/**
	 * Searches all methods of a certain class for methods with ifc statements.
	 * @param cls The class to search in.
	 * @return If at least one method has a (syntactical) legal ifc statement defined.
	 */
	public static boolean checkForFlowStatements(ClassInfo cls) {
		boolean flowFound = false;

		for (MethodInfo m : cls.getMethods()) {
			flowFound |= checkForFlowStatements(m);
		}

		return flowFound;
	}

	/**
	 * Searches a method for ifc statements.
	 * @param m The method to search in.
	 * @return If the method has at least one (syntactical) legal ifc statement defined.
	 */
	public static boolean checkForFlowStatements(MethodInfo m) {
		boolean flowFound = false;

		if (PRINT_DEBUG) {
			System.out.println("Checking " + m.toString());
		}

		for (final Comment comment : m.getComments()) {
			String cur = comment.str;
			int lineNr = comment.lineNrStart;
			if (cur.startsWith(JML_START) && cur.contains(IFC_START)) {
				final int startIndex = cur.indexOf(IFC_START) + IFC_START.length();
				lineNr += countNewLines(cur, startIndex);
				cur = cur.substring(startIndex, cur.length());
				try {
					IFCStmt ifc = parseIFCString(cur);
					ifc.setLineNr(lineNr);
					if (PRINT_DEBUG) {
						System.out.println("Found ifc stmt: " + ifc.toString() + " at line " + ifc.getLineNr());
					}
					flowFound = true;
					m.addIFCStmt(ifc);
				} catch (RecognitionException e) {
					FlowError ferr = new FlowError(m, e);
					m.addError(ferr);
					errors.add(ferr);
				}
			}
		}

		return flowFound;
	}

	private static int countNewLines(final String str, final int pos) {
		int count = 0;
		final char[] ch = str.toCharArray();

		for (int i = 0; i < pos; i++) {
			if (ch.length <= i) break;
			switch (ch[i]) {
			case '\r':
				if (ch.length > i+1 && ch[i+1] == '\n') {
					count++;
					i++;
				} else {
					count++;
				}
			case '\n':
				count++;
			}
		}

		return count;
	}

	private static IFCStmt parseIFCStream(ANTLRStringStream stream) throws RecognitionException {
		FlowLessLexer lexer = new FlowLessLexer(stream);
		CommonTokenStream cts = new CommonTokenStream(lexer);
		FlowLessParser parser = new FlowLessParser(cts);

		return parser.ifc_stmt();
	}


	public static IFCStmt parseIFCString(String stmt) throws RecognitionException {
		return parseIFCStream(new ANTLRStringStream(stmt));
	}

}
