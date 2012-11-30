/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

public class RefinementResult {
	public final int removed;
	public final int suspected;
	public final int cached;

	public RefinementResult(int rem, int sus, int cach) {
		removed = rem;
		suspected = sus;
		cached = cach;
	}
	public RefinementResult(int rem, int sus) {
		this(rem,sus,0);
	}

	@Override public String toString() {
		return "RR(removed=" + removed + ", suspected=" + suspected + ", cached=" + cached + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cached;
		result = prime * result + removed;
		result = prime * result + suspected;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RefinementResult other = (RefinementResult) obj;
		if (cached != other.cached)
			return false;
		if (removed != other.removed)
			return false;
		if (suspected != other.suspected)
			return false;
		return true;
	}


 }
