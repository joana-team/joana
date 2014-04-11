/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import com.ibm.wala.types.TypeReference;

import edu.kit.joana.wala.core.params.objgraph.TVL.V;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public abstract class ModRefCandidate {

	private boolean isMod;
	private boolean isRef;

	public ModRefCandidate(final boolean isMod, final boolean isRef) {
		assert isMod || isRef;
		this.isMod = isMod;
		this.isRef = isRef;
	}

	public abstract V isStatic();
	public abstract V isRoot();
	public abstract V isPrimitive();
	public abstract TypeReference getType();
	public abstract int getBytecodeIndex();
	public abstract String getBytecodeName();
	public abstract boolean isPotentialParentOf(final ModRefFieldCandidate other);
	public abstract int hashCode();
	public abstract boolean equals(Object obj);

	public boolean isMod() {
		return isMod;
	}

	public void setMod() {
		isMod = true;
	}

	public boolean isRef() {
		return isRef;
	}

	public void setRef() {
		isRef = true;
	}

	public String toString() {
		String prefix = "[?]";

		if (isMod() && isRef()) {
			prefix = "[MR] ";
		} else if (isRef()) {
			prefix = "[R] ";
		} else if (isMod()) {
			prefix = "[M] ";
		}

		return prefix;
	}

}
