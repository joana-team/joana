package edu.kit.joana.ifc.wala.constraints;
import com.ibm.wala.fixpoint.UnaryOperator;

import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


/**
 * This operator implements constraints of the form "lhs ≥ rhs". The "≥" comes from
 * a given (security) lattice.
 * @author Martin Mohr
 *
 * @param <T> type of elements in the given lattice
 */
public class SimpleGeqPropagator<T> extends UnaryOperator<SecLevelVariable<T>> {

	private IStaticLattice<T> secLattice;

	public SimpleGeqPropagator(IStaticLattice<T> secLattice) {
		this.secLattice = secLattice;
	}

	@Override
	public byte evaluate(SecLevelVariable<T> lhs, SecLevelVariable<T> rhs) {
		boolean changed = lhs.join(rhs.getSecLevel(), secLattice);
		if (changed) {
			if (secLattice.getTop().equals(lhs.getSecLevel())) {
				return CHANGED_AND_FIXED;
			} else {
				return CHANGED;
			}
		} else {
			if (secLattice.getTop().equals(lhs.getSecLevel())) {
				return NOT_CHANGED_AND_FIXED;
			} else {
				return NOT_CHANGED;
			}
		}
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	@Override
	public String toString() {
		return "≥";
	}
}
