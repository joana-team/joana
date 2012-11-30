/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.spec.java.ast;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class MethodInfo implements Iterable<IFCStmt> {

	private final ClassInfo parent;
	private final String name;
	private final String retType;
	private final List<ParamInfo> params = new LinkedList<ParamInfo>();
	private final List<Comment> comments = new LinkedList<Comment>();
	private final List<IFCStmt> ifcStmts = new LinkedList<IFCStmt>();
	private final List<FlowError> err = new LinkedList<FlowError>();
	private final int line;
	private final boolean isStatic;

	/**
	 *
	 * @author Juergen Graf <graf@kit.edu>
	 *
	 */
	public static class ParamInfo {

		public final String type;
		public final String name;

		private ParamInfo(String type, String name) {
			this.type = type;
			this.name = name;
		}

		public String toString() {
			return type + " " + name;
		}

	}

	public static class Comment {
		public final String str;
		public final int lineNrStart;
		public final int lineNrEnd;

		public Comment(final String str, final int lineNrStart, final int lineNrEnd) {
			this.str = str;
			this.lineNrStart = lineNrStart;
			this.lineNrEnd = lineNrEnd;
		}

		public String toString() {
			return str;
		}

	}

	public MethodInfo(ClassInfo parent, String name, String retType, boolean isStatic, int line) {
		this.parent = parent;
		parent.addMethod(this);
		this.name = name;
		this.retType = retType;
		this.line = line;
		this.isStatic = isStatic;

		if (!isStatic) {
			ParamInfo thisParam = new ParamInfo(parent.getBytecodeName(), "this");
			params.add(thisParam);
		}
	}

	public int getLine() {
		return line;
	}

	public void addParameter(String type, String name) {
		ParamInfo pi = new ParamInfo(type, name);
		params.add(pi);
	}

	/**
	 * Returns a list of all method parameters. Includes the this-pointer, iff the method is not static.
	 */
	public List<ParamInfo> getParameters() {
		return Collections.unmodifiableList(params);
	}

	public String getReturnType() {
		return retType;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		if (isStatic) {
			buf.append("static ");
		}
		buf.append(retType);
		buf.append(' ');
		buf.append(parent.toString());
		buf.append('.');
		buf.append(name);
		buf.append('(');

		for (Iterator<ParamInfo> it = params.iterator(); it.hasNext(); ) {
			ParamInfo pi = it.next();
			buf.append(pi.toString());

			if (it.hasNext()) {
				buf.append(", ");
			}
		}

		buf.append(')');

		return buf.toString();
	}

	public ClassInfo getClassInfo() {
		return parent;
	}

	public void addComment(Comment comment) {
		this.comments.add(comment);
	}

	public List<Comment> getComments() {
		return Collections.unmodifiableList(comments);
	}

	public void addIFCStmt(IFCStmt ifc) {
		ifcStmts.add(ifc);
	}

	public List<IFCStmt> getIFCStmts() {
		return Collections.unmodifiableList(ifcStmts);
	}

	public int countIFCStmts() {
		return ifcStmts.size();
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isVoid() {
		return retType.equals("void");
	}

	public boolean hasErrors() {
		return !err.isEmpty();
	}

	public List<FlowError> getErrors() {
		return err;
	}

	public void addError(FlowError err) {
		if (err == null) {
			throw new IllegalArgumentException("Argument may not be null.");
		}

		this.err.add(err);
	}

	public boolean hasIFCStmts() {
		return !ifcStmts.isEmpty();
	}

	public String getName() {
		return name;
	}

	@Override
	public Iterator<IFCStmt> iterator() {
		return getIFCStmts().iterator();
	}
}
