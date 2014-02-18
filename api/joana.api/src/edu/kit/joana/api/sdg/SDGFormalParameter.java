/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

import edu.kit.joana.ifc.sdg.util.JavaType;

public class SDGFormalParameter implements SDGProgramPart {

	private final SDGMethod owner;
	private final int index;
	private String label;
	private JavaType type = null;

	SDGFormalParameter(SDGMethod owner, int index, String label, JavaType type) {
		this.owner = owner;
		this.index = index;
		this.label = label;
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String newLabel) {
		this.label = newLabel;
	}

	public JavaType getType() {
		return type;
	}

	public String getName() {
		return label;
	}

	@Override
	public String toString() {
		return "parameter " +  getName() + " of method "
				+ owner.getSignature().toHRString();
	}

	public int getIndex() {
		return index;
	}

	public SDGMethod getOwner() {
		return owner;
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitParameter(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owner;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		if (!(obj instanceof SDGFormalParameter)) {
			return false;
		}
		SDGFormalParameter other = (SDGFormalParameter) obj;
		if (index != other.index) {
			return false;
		}
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		if (owner == null) {
			if (other.owner != null) {
				return false;
			}
		} else if (!owner.equals(other.owner)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
			return false;
		}
		return true;
	}
}
