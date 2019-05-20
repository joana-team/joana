/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;

/**
 * Important: the {@link StructuredModsCreator#apply(Context, Bit, Bit)} adds
 * the default bit modification for the own bit to all computed modifications,
 * except for the unused case.
 */
public interface StructuredModsCreator extends ModsCreator {

	default Mods apply(Context c, Bit r, Bit a) {
		if (r.isConstant()) {
			return Mods.empty();
		}
		Mods mods = null;
		switch (a.val()) {
		case ONE:
			mods = assumeOne(c, r, a);
			break;
		case ZERO:
			mods = assumeZero(c, r, a);
			break;
		case U:
			mods = assumeUnknown(c, r, a);
			break;
		case X:
			return assumeUnused(c, r, a);
		}
		return mods.add(defaultOwnBitMod(c, r, a));
	}

	/**
	 * Assume that <code>a</code> is one
	 */
	Mods assumeOne(Context c, Bit r, Bit a);

	/**
	 * Assume that <code>a</code> is zero
	 */
	Mods assumeZero(Context c, Bit r, Bit a);

	/**
	 * Assume that <code>a</code> is u
	 */
	default Mods assumeUnknown(Context c, Bit r, Bit a) {
		return Mods.empty();
	}

	default Mods defaultOwnBitMod(Context c, Bit r, Bit a) {
		if (r.isConstant() || c.choose(r, a) == r) {
			return Mods.empty();
		}
		return new Mods(r, a);
	}

	default Mods assumeUnused(Context c, Bit r, Bit a) {
		return Mods.empty();
	}
}