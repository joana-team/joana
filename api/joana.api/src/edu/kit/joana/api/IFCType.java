/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api;

public enum IFCType {
	CLASSICAL_NI, LSOD, RLSOD, ORLSOD;

	@Override
	public String toString() {
		switch (this) {
		case CLASSICAL_NI:
			return "classical non-interference (extended to threads)";
		case RLSOD:
			return "relaxed low-security observational determinism (RLSOD)";
		case ORLSOD:
			return "optimized relaxed low-security observational determinism (ORLSOD)";
		case LSOD:
			return "low-security observational determinism (LSOD)";
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
