/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO: @author Add your name here.
 */
public class SDGFieldOfParameter implements SDGProgramPart {
	private final SDGProgramPart parent;
	private final String declaringClass;
	private final String fieldName;
	
	public SDGFieldOfParameter(SDGProgramPart parent, String declaringClass, String fieldName) {
		// TODO: refactor SDGProgramPart code such that SDGFormalParameter, SDGActualParameter and SDGFieldOfParameter
		// have a common super-class / interface
		if (parent == null || declaringClass == null || fieldName == null) {
			throw new IllegalArgumentException(String.format("%s %s %s", parent, declaringClass, fieldName));
		}
		boolean invalid = true;
		if (parent instanceof SDGFormalParameter) {
			invalid = false;
		} else if (parent instanceof SDGMethodExitNode) {
			invalid = false;
		} else if (parent instanceof SDGMethodExceptionNode) {
			invalid = false;
		} else if (parent instanceof SDGActualParameter) {
			invalid = false;
		} else if (parent instanceof SDGCallReturnNode) {
			invalid = false;
		} else if (parent instanceof SDGCallExceptionNode) {
			invalid = false;
		} else if (parent instanceof SDGFieldOfParameter) {
			invalid = false;
		}
		if (invalid) {
			throw new IllegalArgumentException("invalid type of parent for a field of parameter: " + parent.getClass());
		}
		this.parent = parent;
		this.declaringClass = declaringClass;
		this.fieldName = fieldName;
	}
	public SDGProgramPart getParent() {
		return parent;
	}
	public String getDeclaringClass() {
		return declaringClass;
	}
	public String getFieldName() {
		return fieldName;
	}
	public List<String> getAccessPath() {
		SDGProgramPart current = this;
		LinkedList<String> accessPath = new LinkedList<String>();
		while (current instanceof SDGFieldOfParameter) {
			accessPath.addFirst(((SDGFieldOfParameter) current).getDeclaringClass() + "." + ((SDGFieldOfParameter) current).getFieldName());
			current = ((SDGFieldOfParameter) current).getParent();
		}
		return accessPath;
	}
	public SDGProgramPart getRoot() {
		SDGProgramPart current = this;
		while (current instanceof SDGFieldOfParameter) {
			current = ((SDGFieldOfParameter) current).getParent();
		}
		return current;
	}
	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitFieldOfParameter(this, data);
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return parent.getOwningMethod();
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given formal parameter and navigates
	 * along the given access path.<br>
	 * The items in the access path have the form '&lt;classname&gt;.&lt;fieldname&gt;, where
	 * &lt;classname&gt; is the name of the declaring class of the respective field in bytecode notation
	 * (without ;) and &lt;fieldname&gt; is the name of the respective field.
	 *
	 * Example:
	 * package foo;
	 * class A {int x;}
	 * package bar;
	 * class B {A f;}
	 * ...
	 * public void m(A a, B b)
	 * ...
	 * To get the field a.x of the first parameter of method m, 'root' is acquired in the usual way and
	 * the accessPath has one single item 'Lfoo/A.x'.
	 * To get the field b.f.x, 'root' denotes the second parameter of m and 'accessPath' has two items:
	 * 'Lbar/B.f', 'Lfoo/A.x'
	 * @param root the parameter from which navigation starts
	 * @param accessPath the access path to navigate along
	 * @return the parameter field which is obtained by starting at the given parameter and then navigating
	 * along the given accessPath
	 */
	public static SDGFieldOfParameter make(SDGFormalParameter root, List<String> accessPath) {
		return makePP(root, accessPath);
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given method exit node (return) and navigates
	 * along the given access path.<br>
	 * For details of usage, see {@link SDGFieldOfParameter#make(SDGFormalParameter, List)}.
	 */
	public static SDGFieldOfParameter make(SDGMethodExitNode root, List<String> accessPath) {
		return makePP(root, accessPath);
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given method exception node and navigates
	 * along the given access path.<br>
	 * For details of usage, see {@link SDGFieldOfParameter#make(SDGFormalParameter, List)}.
	 */
	public static SDGFieldOfParameter make(SDGMethodExceptionNode root, List<String> accessPath) {
		return makePP(root, accessPath);
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given actual parameter node and navigates
	 * along the given access path.<br>
	 * For details of usage, see {@link SDGFieldOfParameter#make(SDGFormalParameter, List)}.
	 */
	public static SDGFieldOfParameter make(SDGActualParameter root, List<String> accessPath) {
		return makePP(root, accessPath);
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given return node of a method call and navigates
	 * along the given access path.<br>
	 * For details of usage, see {@link SDGFieldOfParameter#make(SDGFormalParameter, List)}.
	 */
	public static SDGFieldOfParameter make(SDGCallReturnNode root, List<String> accessPath) {
		return makePP(root, accessPath);
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given exception node of a method call and navigates
	 * along the given access path.<br>
	 * For details of usage, see {@link SDGFieldOfParameter#make(SDGFormalParameter, List)}.
	 */
	public static SDGFieldOfParameter make(SDGCallExceptionNode root, List<String> accessPath) {
		return makePP(root, accessPath);
	}

	/**
	 * Makes a field-of-parameter selector which starts at the given program part and navigates along the given access path.<br>
	 * For details of usage, see {@link SDGFieldOfParameter#make(SDGFormalParameter, List)}.
	 * Use this method only if you know what you are doing!
	 */
	public static SDGFieldOfParameter makePP(SDGProgramPart root, List<String> accessPath) {
		if (accessPath.isEmpty()) {
			throw new IllegalArgumentException("access path must be non-empty!");
		}
		SDGFieldOfParameter result = null;
		SDGProgramPart parent = root;
		Pattern pField = Pattern.compile("(.*?)\\.(.*)");
		for (int i = accessPath.size() - 1; i >= 0; i--) {
			String item = accessPath.get(i);
			Matcher m = pField.matcher(item);
			if (!m.matches()) throw new IllegalArgumentException("Illegal access path item: " + item);
			result = new SDGFieldOfParameter(parent, m.group(1), m.group(2));
			parent = result;
		}
		assert result != null; // loop iterates at least once because 'accessPath' is non-empty.
		return result;
	}
}
