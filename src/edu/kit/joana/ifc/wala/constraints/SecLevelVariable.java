package edu.kit.joana.ifc.wala.constraints;
import com.ibm.wala.fixpoint.AbstractVariable;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;


public class SecLevelVariable<T> extends AbstractVariable<SecLevelVariable<T>>{
	private SDGNode node;
	private T secLevel;

	public SecLevelVariable(SDGNode node) {
		this.node = node;
		this.secLevel = null;
	}

	public T getSecLevel() {
		return secLevel;
	}

	public boolean join(T otherLevel, IStaticLattice<T> lattice) {
		if (this.secLevel == null && otherLevel == null) {
			return false;
		} else if (this.secLevel == null && otherLevel != null) {
			secLevel = otherLevel;
			return true;
		} else if (this.secLevel != null && otherLevel == null) {
			return false;
		} else {
			T newLevel = lattice.leastUpperBound(this.secLevel, otherLevel);
			boolean ret = !newLevel.equals(secLevel);
			secLevel = newLevel;
			return ret;
		}
	}

	public boolean meet(T otherLevel, IStaticLattice<T> lattice) {
		if (this.secLevel == null && otherLevel == null) {
			return false;
		} else if (this.secLevel == null && otherLevel != null) {
			secLevel = otherLevel;
			return true;
		} else if (this.secLevel != null && otherLevel == null) {
			return false;
		} else {
			T newLevel = lattice.greatestLowerBound(this.secLevel, otherLevel);
			boolean ret = !newLevel.equals(secLevel);
			secLevel = newLevel;
			return ret;
		}
	}

	@Override
	public void copyState(SecLevelVariable<T> v) {
		this.secLevel = v.secLevel;
	}

	@Override
	public String toString() {
		return "S["+node+"]";
	}
}
