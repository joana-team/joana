/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;
import edu.kit.joana.wala.core.params.objgraph.candidates.ParameterCandidate;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public final class ModRefFieldCandidate extends ModRefCandidate implements Cloneable {

	public final ParameterCandidate pc;

	public ModRefFieldCandidate(final boolean isMod, final boolean isRef, final ParameterCandidate pc) {
		super(isMod, isRef);

		if (pc == null) {
			throw new IllegalArgumentException();
		}

		this.pc = pc;
	}

	public ModRefFieldCandidate clone() {
		final ModRefFieldCandidate clone = new ModRefFieldCandidate(isMod(), isRef(), pc);
		clone.flags = flags;
		
		return clone;
	}

	@Override
	public V isStatic() {
		return pc.isStatic();
	}

	@Override
	public V isRoot() {
		return pc.isRoot();
	}

	@Override
	public V isPrimitive() {
		return pc.isPrimitive();
	}

	@Override
	public boolean isPotentialParentOf(final ModRefFieldCandidate other) {
		return other.pc.isReachableFrom(pc);
		// it is not safe to propagate only fields reachable through referenced values
		// also written fields may be parents of visible side effects
//		return isRef() && other.pc.isReachableFrom(pc);
	}

	public boolean isBaseAliased(final OrdinalSet<InstanceKey> pts) {
		return pc.isBaseAliased(pts);
	}
	
	/**
	 * @return false iff for any ModRefFieldCandidate other, this.isMustAliased(other) == false
	 */
	public boolean canMustAlias() {
		return pc.canMustAlias();
	}
	
	public boolean isMustAliased(final ModRefFieldCandidate other) {
		return pc.isMustAliased(other.pc);
	}

	public boolean isMayAliased(final ModRefFieldCandidate other) {
		return pc.isMayAliased(other.pc);
	}

	@Override
	public int hashCode() {
		return pc.hashCode() * 13;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof ModRefFieldCandidate) {
			final ModRefFieldCandidate other = (ModRefFieldCandidate) obj;

			return isMod() == other.isMod() && isRef() == other.isRef() && pc.equals(other.pc);
		}

		return false;
	}

	public String toString() {
		final String prefix = super.toString();

		return prefix + "|" + flags + "|" + (pc == null ? "???" : pc.toString());
	}

	@Override
	public TypeReference getType() {
		return pc.getType();
	}

	@Override
	public int getBytecodeIndex() {
		return pc.getBytecodeIndex();
	}

	@Override
	public String getBytecodeName() {
		return pc.getBytecodeName();
	}

	public OrdinalSet<ParameterField> getFields() {
		return pc.getFields();
	}

	@Override
	public boolean isReachableFrom(final OrdinalSet<InstanceKey> reachable) {
		return pc.isBaseAliased(reachable);
	}

}
