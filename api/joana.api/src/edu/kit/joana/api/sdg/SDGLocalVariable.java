/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;

public class SDGLocalVariable implements SDGProgramPart {

	private final SDGMethod owningMethod;
	private String name;
	
	SDGLocalVariable(SDGMethod owner, String name) {
		this.owningMethod = owner;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "local variable " +  getName() + " of method "
				+ owningMethod.getSignature().toHRString();
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitLocalVariable(this, data);
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owningMethod;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((owningMethod == null) ? 0 : owningMethod.hashCode());
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
		if (!(obj instanceof SDGLocalVariable)) {
			return false;
		}
		SDGLocalVariable other = (SDGLocalVariable) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (owningMethod == null) {
			if (other.owningMethod != null) {
				return false;
			}
		} else if (!owningMethod.equals(other.owningMethod)) {
			return false;
		}
		return true;
	}
}
