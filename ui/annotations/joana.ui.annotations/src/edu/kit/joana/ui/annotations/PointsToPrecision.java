/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.annotations;

/**
 * see {@code edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision}
 */
public enum PointsToPrecision {
	RTA(false, "rapid type analysis"),
	TYPE_BASED(true, "type-based (0-CFA)"),
	INSTANCE_BASED(true, "instance-based (0-1-CFA)"),
	OBJECT_SENSITIVE(true, "object-sensitive + 1-level call-stack"),
	N1_OBJECT_SENSITIVE(true, "1-level object-sensitive + 1-level call-stack"),
	UNLIMITED_OBJECT_SENSITIVE(true, "unlimited object-sensitive + 1-level call-stack"),
	N1_CALL_STACK(true, "1-level call-stack (1-CFA)"),
	N2_CALL_STACK(true, "2-level call-stack (2-CFA)"),
	N3_CALL_STACK(true, "3-level call-stack (3-CFA)");

	public final String desc;         // short textual description of the option - can be used for gui
	public final boolean recommended; // option can make sense aside for academic evaluation

	private PointsToPrecision(final boolean recommended, final String desc) {
		this.recommended = recommended;
		this.desc = desc;
	}
}