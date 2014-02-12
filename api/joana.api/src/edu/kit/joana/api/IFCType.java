/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

public enum IFCType {
	CLASSICAL_NI, PROBABILISTIC_WITH_SIMPLE_MHP, PROBABILISTIC_WITH_PRECISE_MHP;

	@Override
	public String toString() {
		switch (this) {
		case CLASSICAL_NI:
			return "classical non-interference (extended to threads)";
		case PROBABILISTIC_WITH_PRECISE_MHP:
			return "probabilistic (with precise mhp)";
		case PROBABILISTIC_WITH_SIMPLE_MHP:
			return "probabilistic (with simple mhp)";
		default:
			throw new IllegalStateException("not all values of this enumeration are handeled!");
		}
	}
	
	public static IFCType fromString(String s) {
		for (IFCType t : IFCType.values()) {
			if (t.toString().equals(s)) {
				return t;
			}
		}
		throw new IllegalArgumentException("given string must match the return value of toString() of one of the enum values, but does not: " + s);
	}
}
