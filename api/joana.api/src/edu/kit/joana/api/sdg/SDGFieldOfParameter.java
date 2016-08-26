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
			accessPath.addFirst(((SDGFieldOfParameter) current).getFieldName());
			current = ((SDGFieldOfParameter) current).getParent();
		}
		return accessPath;
	}
	public SDGProgramPart getRoot() {
		SDGProgramPart current = this;
		LinkedList<String> accessPath = new LinkedList<String>();
		while (current instanceof SDGFieldOfParameter) {
			accessPath.addFirst(((SDGFieldOfParameter) current).getDeclaringClass() + "." + ((SDGFieldOfParameter) current).getFieldName());
			current = ((SDGFieldOfParameter) current).getParent();
		}
		return current;
	}
	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#acceptVisitor(edu.kit.joana.api.sdg.SDGProgramPartVisitor, java.lang.Object)
	 */
	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.api.sdg.SDGProgramPart#getOwningMethod()
	 */
	@Override
	public SDGMethod getOwningMethod() {
		return parent.getOwningMethod();
	}

}
