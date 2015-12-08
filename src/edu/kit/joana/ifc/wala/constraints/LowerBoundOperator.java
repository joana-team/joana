package edu.kit.joana.ifc.wala.constraints;
import com.ibm.wala.fixedpoint.impl.NullaryOperator;

import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;

public class LowerBoundOperator<T> extends NullaryOperator<SecLevelVariable<T>> {
	private IStaticLattice<T> secLattice;
	private T lowerBound;

	public LowerBoundOperator(IStaticLattice<T> secLattice, T lowerBound) {
		this.secLattice = secLattice;
		this.lowerBound = lowerBound;
	}

	@Override
	public byte evaluate(SecLevelVariable<T> lhs) {
		boolean changed = lhs.join(lowerBound, secLattice);
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
		return 31 * this.lowerBound.hashCode() + 1;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof LowerBoundOperator)) {
			return false;
		} else {
			@SuppressWarnings("unchecked")
			LowerBoundOperator<T> lbo = (LowerBoundOperator<T>) o;
			return lbo.lowerBound.equals(this.lowerBound);
		}
	}

	@Override
	public String toString() {
		return "≥" + lowerBound;
	}

}
