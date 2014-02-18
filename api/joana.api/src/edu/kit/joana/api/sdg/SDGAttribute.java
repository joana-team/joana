/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.util.JavaType;

public class SDGAttribute implements SDGProgramPart {

	private final SDGClass owningClass;
	private final String name;
	private final JavaType type;

	SDGAttribute(SDGClass declaringClass, String name, JavaType type) {
		this.owningClass = declaringClass;
		this.name = name;
		this.type = type;
	}

	public String getName() {
		int offset = this.name.lastIndexOf('.');
		return name.substring(offset + 1);
	}

	public JavaType getDeclaringType() {
		return owningClass.getTypeName();
	}

	public String getType() {
		return type.toHRString();
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitAttribute(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return null;
	}

	@Override
	public String toString() {
		return owningClass.toString() + "." + getName();// + srcNodes + "/" + snkNodes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owningClass == null) ? 0 : owningClass.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SDGAttribute)) {
			return false;
		}
		SDGAttribute other = (SDGAttribute) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (owningClass == null) {
			if (other.owningClass != null) {
				return false;
			}
		} else if (!owningClass.equals(other.owningClass)) {
			return false;
		}
		return true;
	}
}
