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

/**
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ClassInfo implements Iterable<MethodInfo> {

	private final String name;
	private final List<String> pack;
	private final List<String> enclosingClasses;
	private final List<MethodInfo> methods;
	private final Type type;

	public static enum Type { CLASS, INTERFACE }

	public ClassInfo(Type type, String name) {
		this(type, name, null, null);
	}

	public ClassInfo(Type type, String name, List<String> pack) {
		this(type, name, pack, null);
	}

	public ClassInfo(Type type, String name, List<String> pack, List<String> enclosingClasses) {
		if (name.equals("if")) {
			throw new IllegalArgumentException("This is a reserved keyword: " + name);
		}

		this.type = type;
		this.name = name;
		this.pack = (pack == null || pack.isEmpty() ? null : new LinkedList<String>(pack));
		this.enclosingClasses = (enclosingClasses == null || enclosingClasses.isEmpty() ? null : new LinkedList<String>(enclosingClasses));
		this.methods = new LinkedList<MethodInfo>();
	}

	void addMethod(MethodInfo m) {
		methods.add(m);
	}

	public List<MethodInfo> getMethods() {
		return Collections.unmodifiableList(methods);
	}

	public Type getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public String getPackage() {
		if (pack != null) {
			StringBuffer buf = new StringBuffer();

			for (String p : pack) {
				buf.append(p);
				buf.append('.');
			}

			return buf.toString();
		} else {
			return "";
		}
	}

	public String getEnclosingClasses() {
		if (enclosingClasses != null) {
			StringBuffer buf = new StringBuffer();

			for (String p : enclosingClasses) {
				buf.append(p);
				buf.append('$');
			}

			return buf.toString();
		} else {
			return "";
		}
	}

	public String toString() {
		return getPackage() + getEnclosingClasses() + getName();
	}

	public int countFlowStmts() {
		int count = 0;

		for (MethodInfo m : methods) {
			count += m.countIFCStmts();
		}

		return count;
	}

	public String getBytecodeName() {
		StringBuffer buf = new StringBuffer("L");

		if (pack != null) {
			for (String p : pack) {
				buf.append(p);
				buf.append('/');
			}
		}

		if (enclosingClasses != null) {
			for (String cls : enclosingClasses) {
				buf.append(cls);
				buf.append('$');
			}
		}

		buf.append(name);
		buf.append(';');

		return buf.toString();
	}

	public String getWalaBytecodeName() {
		StringBuffer buf = new StringBuffer("L");

		if (pack != null) {
			for (String p : pack) {
				buf.append(p);
				buf.append('/');
			}
		}

		if (enclosingClasses != null) {
			for (String cls : enclosingClasses) {
				buf.append(cls);
				buf.append('$');
			}
		}

		buf.append(name);

		return buf.toString();
	}

	@Override
	public Iterator<MethodInfo> iterator() {
		return getMethods().iterator();
	}

}
