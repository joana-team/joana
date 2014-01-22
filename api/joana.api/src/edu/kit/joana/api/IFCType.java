/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

public enum IFCType {
	POSSIBILISTIC, PROBABILISTIC_WITH_SIMPLE_MHP, PROBABILISTIC_WITH_PRECISE_MHP;

	public String toString() {
		switch (this) {
		case POSSIBILISTIC:
			return "possibilistic";
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
