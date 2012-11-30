/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import edu.kit.joana.ifc.sdg.graph.SDG;

public class NandaFactory {

	/**
	 * Creates a Nanda backward slicer with all optimizations.
	 *
	 * @param g
	 * @return
	 */
	public static Nanda createNandaBackward(SDG g) {
		NandaMode mode = new NandaBackward();
		Nanda nanda = new Nanda(g, mode);
		return nanda;
	}

	/**
	 * Creates a Nanda backward slicer with all optimizations, but imprecise MHP information.
	 * All threads are assumed to happen in parallel.
	 *
	 * @param g
	 * @return
	 */
	public static Nanda createNandaFlatBackward(SDG g) {
		NandaMode mode = new NandaBackward();
		NandaFlat nanda = new NandaFlat(g, mode);
		return nanda;
	}

	/**
	 * Creates a Nanda backward slicer, which is close to the original algorithm.
	 * All threads are assumed to happen in parallel.
	 *
	 * @param g
	 * @return
	 */
	public static Nanda createNandaOriginalBackward(SDG g) {
		NandaMode mode = new NandaBackward();
		NandaOriginal nanda = new NandaOriginal(g, mode);
		return nanda;
	}
}
