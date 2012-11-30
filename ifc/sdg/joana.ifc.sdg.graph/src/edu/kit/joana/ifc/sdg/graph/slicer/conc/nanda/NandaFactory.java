/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

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
	 * Creates a Nanda forward slicer with all optimizations.
	 *
	 * @param g
	 * @return
	 */
	public static Nanda createNandaForward(SDG g) {
		NandaMode mode = new NandaForward();
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
	 * Creates a Nanda forward slicer with all optimizations, but imprecise MHP information.
	 * All threads are assumed to happen in parallel.
	 *
	 * @param g
	 * @return
	 */
	public static Nanda createNandaFlatForward(SDG g) {
		NandaMode mode = new NandaForward();
		NandaFlat nanda = new NandaFlat(g, mode);
		return nanda;
	}

	/**
	 * Creates a Nanda backward slicer, which uses reachability analysis after each edge traversal.
	 * All threads are assumed to happen in parallel.
	 *
	 * @param g
	 * @return
	 */
	public static Nanda createNandaReachBackward(SDG g) {
		NandaMode mode = new NandaBackward();
		NandaReach nanda = new NandaReach(g, mode);
		return nanda;
	}
}
