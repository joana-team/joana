/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.tests;

public class ProgressCounter {
	private int base;
	private int ctr;

	public ProgressCounter(int base) {
		if (base < 1) throw new IllegalArgumentException("`base' must be at least 1.");
		this.base = base;
		this.ctr = 0;
	}

	public void tick() {
		ctr++;

		if (ctr % (base*10) == 0) {
			System.out.print(ctr);

		} else if (ctr % base == 0) {
			System.out.print(".");
		}

	    if (ctr % (base*100) == 0) {
			System.out.println();
		}
	}
}
