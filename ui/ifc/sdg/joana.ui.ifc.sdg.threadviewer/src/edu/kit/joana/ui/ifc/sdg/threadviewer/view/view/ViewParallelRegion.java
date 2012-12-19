/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.view;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;


public class ViewParallelRegion {
	private ViewCategory parent;
	private ThreadRegion item;

	public ViewParallelRegion(ViewCategory parent, ThreadRegion item) {
		this.parent = parent;
		this.item = item;
	}

	public ViewCategory getParent() {
		return this.parent;
	}

	public ThreadRegion getItem() {
		return this.item;
	}
}
