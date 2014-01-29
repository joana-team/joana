/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.sdg;



public class SDGInstruction implements SDGProgramPart,
Comparable<SDGInstruction> {

	//private final SDGNode rootNode;
	private final SDGMethod owner;
	private final int bcIndex;
	private final String label;
	private final String type;
	private final String op;

	SDGInstruction(SDGMethod owner, int bcIndex, String label, String type, String op) {
		this.owner = owner;
		this.bcIndex = bcIndex;
		this.label = label;
		this.type = type;
		this.op = op;
	}

	public SDGMethod getOwner() {
		return owner;
	}

	public int getBytecodeIndex() {
		return bcIndex;
	}

	public String getLabel() {
		return label;
	}

	public String getType() {
		return type;
	}

	public String getOperation() {
		return op;
	}

	@Override
	public String toString() {
		return "(" + owner.getSignature().toHRString() + ":"
		+ bcIndex + ") " + label;
	}

	@Override
	public <R, D> R acceptVisitor(SDGProgramPartVisitor<R, D> v, D data) {
		return v.visitInstruction(this, data);
	}

	@Override
	public int compareTo(SDGInstruction arg0) {
		return getBytecodeIndex() - arg0.getBytecodeIndex();
	}

	@Override
	public SDGMethod getOwningMethod() {
		return owner;
	}

	/**
	 * Returns whether this instruction is a call instruction.
	 * @return {@code true}, if this instruction is a call instruction, {@code false} otherwise
	 */
	public boolean isCall() {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bcIndex;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((owner == null) ? 0 : owner.getSignature().hashCode());
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
		if (!(obj instanceof SDGInstruction)) {
			return false;
		}
		SDGInstruction other = (SDGInstruction) obj;
		if (bcIndex != other.bcIndex) {
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
		return true;
	}

}
