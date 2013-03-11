/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.java;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.Lexer;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;

import edu.kit.joana.wala.flowless.spec.java.ast.ClassInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo.Comment;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class LightweightParser {

	public static final boolean PRINT_INFO = false;
	public static final boolean PRINT_SKIPS = false;


	public static List<ClassInfo> parseFile(String javaFile) throws IOException {
		return parseFile(new ANTLRFileStream(javaFile), javaFile);
	}

	public static List<ClassInfo> parseFile(InputStream stream) throws IOException {
		return parseFile(new ANTLRInputStream(stream), "<InputStream>");
	}

	public static List<ClassInfo> parseString(String input) {
		return parseFile(new ANTLRStringStream(input), "<StringStream>");
	}


	private static List<ClassInfo> parseFile(ANTLRStringStream stream, String fileName) {
		LightweightJava lexer = new LightweightJava(stream);
		CommonTokenStream cts = new CommonTokenStream(lexer);
		LightweightParser parser = new LightweightParser(cts, fileName);

		return parser.parse();
	}


	private static class TokenStream {

		@SuppressWarnings("unused")
		private final CommonTokenStream cts;
		@SuppressWarnings("unused")
		private final String fileName;
		private List<Token> toks;
		private int index;

		private boolean ignoreComment = true;

		@SuppressWarnings("unchecked")
		private TokenStream(CommonTokenStream cts, String fileName) {
			this.cts = cts;
			this.fileName = fileName;

			try {
				index = -1;
				toks = (List<Token>) cts.getTokens();
			} catch (Exception exc) {
				if (exc instanceof RecognitionException) {
					RecognitionException rexc = (RecognitionException) exc;
					System.err.println(fileName + ": " + rexc.line + ":" + rexc.charPositionInLine);
				}

				throw new IllegalStateException(exc);
			}
		}

		public void setIgnoreComment() {
			ignoreComment = true;
		}

		public void unsetIgnoreComment() {
			ignoreComment = false;
		}

		public boolean isIgnoreComment() {
			return ignoreComment;
		}

		public Token current() {
			return toks.get(index);
		}

		public Token peekNext() {
			if (hasNext()) {
				int next = nextIndex();

				return toks.get(next);
			} else {
				return null;
			}
		}

		public Token next() {
			if (hasNext()) {
				int next = nextIndex();
				index = next;

				return toks.get(index);
			} else {
				return null;
			}
		}

		@SuppressWarnings("unused")
		public Token peekPrevious() {
			if (hasPrevious()) {
				int prev = prevIndex();

				return toks.get(prev);
			} else {
				return null;
			}
		}

		public Token previous() {
			if (hasPrevious()) {
				int prev = prevIndex();
				index = prev;

				return toks.get(index);
			} else {
				return null;
			}
		}

		@SuppressWarnings("unused")
		public void skip(int tokCount) {
			while (tokCount > 0 && hasNext()) {
				int next = nextIndex();
				index = next;
				tokCount--;
			}
		}

		public void rewind(int tokCount) {
			while (tokCount > 0 /* && hasPrevious() */) {
				int prev = prevIndex();
				index = prev;
				tokCount--;
			}
		}

		private static boolean isComment(Token tok) {
			return tok.getType() == LightweightJava.BLOCK_COMMENT || tok.getType() == LightweightJava.LINE_COMMENT;
		}

		public int nextIndex() {
			int index = this.index;

			while (index < toks.size() - 1) {
				index++;
				final Token t = toks.get(index);
				if (t.getChannel() != Lexer.HIDDEN) {
					if (!ignoreComment || !isComment(t)) {
						return index;
					}
				}
			}

			return toks.size();
		}

		public int prevIndex() {
			int index = this.index;

			while (index > 0) {
				index--;
				final Token t = toks.get(index);
				if (t.getChannel() != Lexer.HIDDEN) {
					if (!ignoreComment || !isComment(t)) {
						return index;
					}
				}
			}

			return -1;
		}

		public boolean hasNext() {
			final int next = nextIndex();

			return next >= 0 && next < toks.size();
		}

		public boolean hasPrevious() {
			final int prev = prevIndex();

			return prev >= 0 && prev < toks.size();
		}

		public String toString() {
			StringBuffer sb = new StringBuffer();

			for (Token t : toks) {
				if (t.getChannel() != LightweightJava.HIDDEN) {
					sb.append(LightweightJava.getTokenName(t.getType()));
					sb.append('(');
					sb.append(t.getLine());
					sb.append(", \'");
					sb.append((t.getText().length() > 60 ? t.getText().substring(0, 56) + "..." : t.getText()));
					sb.append("\')\n");
				}
			}

			return sb.toString();
		}

	}

	private final TokenStream stream;

	private LightweightParser(CommonTokenStream cts, String fileName) {
		this.stream = new TokenStream(cts, fileName);
	}

	private List<ClassInfo> info = null;

	private List<ClassInfo> parse() {
//		System.out.println(stream);

		info = new LinkedList<ClassInfo>();

		List<String> pkgs = parseForPackage();

		Stack<String> currentClass = new Stack<String>();

		stream.setIgnoreComment();

		while (stream.hasNext()) {
			Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.CLASS: {
				ClassInfo cls = parseForClass(ClassInfo.Type.CLASS, pkgs, currentClass);
				info.add(cls);
			} break;

			case LightweightJava.INTERFACE: {
				ClassInfo cls = parseForClass(ClassInfo.Type.INTERFACE, pkgs, currentClass);
				info.add(cls);
			} break;

//			default: {
//				System.out.println("Skipping " + LightweightJava.getTokenName(tok.getType()));
//			} break;

			}
		}

		return info;
	}

	private ClassInfo parseForClass(ClassInfo.Type type, List<String> packs, List<String> enclosingClasses) {
		assert stream.isIgnoreComment();

		ClassInfo cls = null;

		// get name (ignore comment)
		String name = null;
		while (name == null && stream.hasNext()) {
			final Token tok = stream.next();
			switch (tok.getType()) {
			case LightweightJava.IDENT: {
				name = tok.getText();
				cls = new ClassInfo(type, name, packs, enclosingClasses);
			} break;
			}
		}

		// look for methods and inner classes
		boolean parseDone = false;
//		int prevIdent = 0;
		List<String> lastIdents = new LinkedList<String>();
		int countArrays = 0;
		boolean isStatic = false;

		// skip implements & co
		skipTill(LightweightJava.BR_OPEN);

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.INTERFACE:
			case LightweightJava.CLASS: {
				// skip inner classes atm

				List<String> clss = new LinkedList<String>(enclosingClasses);
				clss.add(name);
				ClassInfo.Type t = (tok.getType() == LightweightJava.INTERFACE ? ClassInfo.Type.INTERFACE : ClassInfo.Type.CLASS);
				ClassInfo enclCls = parseForClass(t, packs, clss);
				info.add(enclCls);

				lastIdents.clear();
				countArrays = 0;
				isStatic = false;
			} break;

			case LightweightJava.ANNOTATION: {
				Token peek = stream.peekNext();
				if (peek.getType() == LightweightJava.DOT) {
					skipDottedIds();
					peek = stream.current();
				}

				if (peek.getType() == LightweightJava.PARAM_START) {
					// skip optional annotation parameter
					skipParamBlock();
				}
			} break;

			case LightweightJava.ASSIGN: {
				lastIdents.clear();
				countArrays = 0;
				skipCodeBlock();
				isStatic = false;
			} break;

			case LightweightJava.STATIC: {
				lastIdents.clear();
				countArrays = 0;
				if (stream.hasNext() && stream.peekNext().getType() == LightweightJava.BR_OPEN) {
					// skip static initializer
					skipCodeBlock();
					isStatic = false;
				} else {
					isStatic = true;
				}
			} break;

			case LightweightJava.BR_OPEN: {
				lastIdents.clear();
				countArrays = 0;
				// skip code blocks when we are inside the class block.
				// do not skip before, as we would skip the class block itself.
				stream.rewind(1);
				skipCodeBlock();
				isStatic = false;
			} break;

//			case LightweightJava.DOT: {
//			} break;

			case LightweightJava.IDENT: {
//				if (lastIdents.size() > 0 && stream.hasPrevious() && stream.peekPrevious().getType() == LightweightJava.DOT) {
//					String lastId = lastIdents.remove(lastIdents.size() - 1);
//					lastIdents.add(lastId + "." + tok.getText());
//				} else {
					lastIdents.add(tok.getText());
//				}
			} break;

			case LightweightJava.ARRAY_DEF: {
				countArrays++;
			} break;

			case LightweightJava.LT: {
				stream.rewind(1);
				skipGenerics();
			} break;

			case LightweightJava.PARAM_START: {
				if (lastIdents.size() == 2) {
					final String methodName = lastIdents.get(1);
					String methodType = lastIdents.get(0);
					for (int i = 0; i < countArrays; i++) {
						methodType += "[]";
					}

					stream.rewind(1);
					parseForMethod(methodType, methodName, isStatic, cls);
				} else if (lastIdents.size() == 1) {
					final String methodName = lastIdents.get(0);
					if (methodName .equals(name)) {
						// we have a constructor..
						stream.rewind(1);
						parseForMethod(null, methodName, isStatic, cls);
					} else {
						error(tok, "Only 1 preceeding identifier ('" + methodName + "') but no constructor...");
						skipTill(LightweightJava.PARAM_END);
						skipCodeBlock();
					}
				} else if (lastIdents.size() == 0) {
					error(tok, "No preceeding identifiers...");
					skipTill(LightweightJava.PARAM_END);
					skipCodeBlock();
				} else {
					error(tok, "More then 2 (" + lastIdents.size() + ") preceeding identifiers...");
					skipTill(LightweightJava.PARAM_END);
					skipCodeBlock();
				}
				countArrays = 0;
				lastIdents.clear();
				isStatic = false;
			} break;

			case LightweightJava.BR_CLOSE: {
				lastIdents.clear();
				countArrays = 0;
				isStatic = false;
				parseDone = true;
			} break;

			case LightweightJava.DOT: {
				lastIdents.clear();
				countArrays = 0;
			} break;

			default: {
				lastIdents.clear();
				countArrays = 0;
				isStatic = false;
				// skip till ';'
				if (tok.getType() != LightweightJava.SC) {
					skipTill(LightweightJava.SC);
				}
			} break;
			}
		}


		return cls;
	}

	private void parseForMethod(final String methodType, final String methodName, final boolean isStatic, final ClassInfo cls) {
		assert stream.isIgnoreComment();

		final int startPos = stream.index;

		final String typeName = (methodType == null ? "void" : methodType);

		final int lineNr = stream.current().getLine();

		{
			Token tokParamStart = stream.next();
			assert tokParamStart.getType() == LightweightJava.PARAM_START;
		}

		MethodInfo method = new MethodInfo(cls, methodName, typeName, isStatic, lineNr);

		boolean parseDone = false;

		List<String> currentParam = new LinkedList<String>();
		int arrayCount = 0;

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.IDENT: {
				currentParam.add(tok.getText());
			} break;

			case LightweightJava.ARRAY_DEF: {
				arrayCount++;
			} break;

			case LightweightJava.ANNOTATION: {
				if (stream.peekNext().getType() == LightweightJava.PARAM_START) {
					// skip optional annotation parameter
					skipParamBlock();
				}
			} break;

			case LightweightJava.LT: {
				stream.rewind(1);
				skipGenerics();
			} break;

			case LightweightJava.COMMA: {
				// new param;
				if (currentParam.size() >= 2) {
					final String paramName = currentParam.get(currentParam.size() - 1);
					String paramType = currentParam.get(currentParam.size() - 2);

					for (int i = 0; i < arrayCount; i++) {
						paramType += "[]";
					}

					method.addParameter(paramType, paramName);
				} else {
					error(tok, "Weird, no 2 identifiers before potential method decl.");
				}

				arrayCount = 0;
				currentParam.clear();
			} break;

			case LightweightJava.PARAM_END: {
				if (currentParam.size() > 0) {
					// param found in declaration.
					if (currentParam.size() >= 2) {
						final String paramName = currentParam.get(currentParam.size() - 1);
						String paramType = currentParam.get(currentParam.size() - 2);

						for (int i = 0; i < arrayCount; i++) {
							paramType += "[]";
						}

						method.addParameter(paramType, paramName);
					} else {
						error(tok, "Weird, no 2 identifiers before potential method decl.");
					}

					arrayCount = 0;
					currentParam.clear();
				}

				parseDone = true;
			} break;

			}
		}

		List<Comment> comments = searchForCommentsBackwards(startPos);

		for (Comment comment : comments) {
			if (PRINT_INFO) {
				System.out.println(comment.str);
			}
			method.addComment(comment);
		}

		if (PRINT_INFO) {
			System.out.println(method.toString() + " in line " + method.getLine());
		}

		skipCodeBlock();
	}

	private List<Comment> searchForCommentsBackwards(final int startIndex) {
		boolean wasIgnoreComment = stream.isIgnoreComment();
		stream.unsetIgnoreComment();

		final int currentIndex = stream.index;
		LinkedList<Comment> comments = new LinkedList<Comment>();

		stream.index = startIndex;
		boolean parseDone = false;

		while (!parseDone && stream.hasPrevious()) {
			final Token tok = stream.previous();

			switch (tok.getType()) {

			case LightweightJava.BR_CLOSE:
			case LightweightJava.BR_OPEN:
			case LightweightJava.SC: {
				parseDone = true;
			} break;

			case LightweightJava.BLOCK_COMMENT: {
				final Comment co = new Comment(tok.getText(), tok.getLine(), tok.getLine());
				comments.addFirst(co);
			} break;
			case LightweightJava.LINE_COMMENT: {
				String text = tok.getText();
				if (text.endsWith("\n")) {
					// remove trailing newline
					text = text.substring(0, text.length() - 1);
				}
				final Comment co = new Comment(text, tok.getLine(), tok.getLine());
				comments.addFirst(co);
			} break;
			}
		}

		stream.index = currentIndex;

		if (wasIgnoreComment) {
			stream.setIgnoreComment();
		}

		return comments;
	}

	private void skipCodeBlock() {
		assert stream.isIgnoreComment();

		final int from = stream.current().getLine();

		boolean parseDone = false;
		int brackCount = -1;

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.BR_OPEN: {
				brackCount++;
			} break;

			case LightweightJava.SC: {
				parseDone = (brackCount < 0);
			} break;

			case LightweightJava.PARAM_START: {
				stream.rewind(1);
				skipParamBlock();
			} break;

			case LightweightJava.BR_CLOSE: {
				brackCount--;
				parseDone = (brackCount < 0);
			} break;

			}
		}

		final int to = stream.current().getLine();

		if (PRINT_SKIPS) {
			System.out.println("Skipping code block line " + from + " - " + to);
		}
	}

	private void skipGenerics() {
		boolean parseDone = false;
		int brackCount = -1;

		final int from = stream.current().getLine();

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.LT: {
				brackCount++;
			} break;

			case LightweightJava.GT: {
				brackCount--;
				parseDone = (brackCount < 0);
			} break;

			}
		}

		final int to = stream.current().getLine();

		if (PRINT_SKIPS) {
			System.out.println("Skipping  generics line " + from + " - " + to);
		}
	}

	private void skipParamBlock() {
		boolean parseDone = false;
		int brackCount = -1;

		final int from = stream.current().getLine();

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.PARAM_START: {
				brackCount++;
			} break;

			case LightweightJava.PARAM_END: {
				brackCount--;
				parseDone = (brackCount < 0);
			} break;

			}
		}

		final int to = stream.current().getLine();

		if (PRINT_SKIPS) {
			System.out.println("Skipping  generics line " + from + " - " + to);
		}
	}

	private void skipDottedIds() {
		boolean parseDone = false;

		final int from = stream.current().getLine();

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.DOT:
			case LightweightJava.IDENT: {
			} break;

			default: {
				parseDone = true;
			} break;


			}
		}

		final int to = stream.current().getLine();

		if (PRINT_SKIPS) {
			System.out.println("Skipping  generics line " + from + " - " + to);
		}
	}

	private void skipTill(final int tokenType) {
		assert stream.isIgnoreComment();

		final int from = stream.current().getLine();

		boolean parseDone = false;

		while (!parseDone && stream.hasNext()) {
			final Token tok = stream.next();

			parseDone = (tok.getType() == tokenType);
		}

		final int to = stream.current().getLine();

		if (PRINT_SKIPS) {
			System.out.println("Skipping  till '" + LightweightJava.getTokenName(tokenType) + "' line " + from + " - " + to);
		}
	}

	private List<String> parseForPackage() {
		assert stream.isIgnoreComment();

		List<String> pkgs = new LinkedList<String>();
		boolean parseDone = false;

		while (!parseDone && stream.hasNext()) {
			Token tok = stream.next();

			switch (tok.getType()) {

			case LightweightJava.PACKAGE: {
				while (stream.hasNext()) {
					tok = stream.next();

					if (tok.getType() == LightweightJava.SC) {
						break;
					} else if (tok.getType() == LightweightJava.IDENT) {
						pkgs.add(tok.getText());
					}
				}
			} break;

			case LightweightJava.INTERFACE:
			case LightweightJava.CLASS: {
				parseDone = true;
				stream.rewind(1);
			} break;

			}
		}

		return (pkgs.isEmpty() ? null : pkgs);
	}

	private static void error(Token tok, String message) {
		System.err.println(tok.getLine() + " : " + LightweightJava.getTokenName(tok.getType())
				+ "(\"" + tok.getText() + "\") - " + message);
		throw new IllegalStateException();
	}

}
