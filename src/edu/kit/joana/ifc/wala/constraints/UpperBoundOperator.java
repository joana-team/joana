package edu.kit.joana.ifc.wala.constraints;
import com.ibm.wala.fixedpoint.impl.NullaryOperator;

import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;

public class UpperBoundOperator<T> extends NullaryOperator<SecLevelVariable<T>> {
	private IStaticLattice<T> secLattice;
	private T upperBound;

	public UpperBoundOperator(IStaticLattice<T> secLattice, T upperBound) {
		this.secLattice = secLattice;
		this.upperBound = upperBound;
	}

	@Override
	public byte evaluate(SecLevelVariable<T> lhs) {
		boolean changed = lhs.meet(upperBound, secLattice);
		if (changed) {
			if (secLattice.getBottom().equals(lhs.getSecLevel())) {
				return CHANGED_AND_FIXED;
			} else {
				return CHANGED;
			}
		} else {
			if (secLattice.getBottom().equals(lhs.getSecLevel())) {
				return NOT_CHANGED_AND_FIXED;
			} else {
				return NOT_CHANGED;
			}
		}
	}

	@Override
	public int hashCode() {
		return 31 * this.upperBound.hashCode() + 1;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UpperBoundOperator)) {
			return false;
		} else {
			@SuppressWarnings("unchecked")
			UpperBoundOperator<T> lbo = (UpperBoundOperator<T>) o;
			return lbo.upperBound.equals(this.upperBound);
		}
	}

	@Override
	public String toString() {
		return "≤" + upperBound;
	}

}
