/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

public class Pathedge {
	public SecurityNode source;
	public SecurityNode target;

	/** security level */
	public String in;

	/** security level */
	public String out;

	/** path without declassification */
	public boolean fp;

	public Pathedge(SecurityNode so, SecurityNode ta, String in , String out, boolean fp) {
		this.source = so;
		this.target= ta;
		this.in = in;
		this.out = out;
		this.fp = fp;
	}

	public int hashCode() {
		int hash = source.getId();
		hash = hash * 31 + target.getId();
		return  hash;
	}

	public boolean equals(Object comp) {
		if ((comp instanceof Pathedge)) {
			Pathedge pcomp = (Pathedge) comp;
			return pcomp.source.getId() == this.source.getId() &&
				pcomp.target.getId() == this.target.getId();
		}
		return false;
	}

	public boolean equalsExactly(Object comp) {
		if (!this.equals(comp)) return false;
		Pathedge pcomp = (Pathedge)comp;
		return pcomp.fp == this.fp && pcomp.in.equals(this.in) &&
			pcomp.out.equals(this.out);
	}

	public String toString() {
		return "Pathedge: " + source + "->" + target;
	}
}
