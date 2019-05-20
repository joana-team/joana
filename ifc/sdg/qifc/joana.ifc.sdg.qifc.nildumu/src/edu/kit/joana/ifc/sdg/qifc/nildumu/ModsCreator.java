/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Bit;

@FunctionalInterface
public interface ModsCreator {
	public Mods apply(Context context, Bit bit, Bit assumedValue);
}
