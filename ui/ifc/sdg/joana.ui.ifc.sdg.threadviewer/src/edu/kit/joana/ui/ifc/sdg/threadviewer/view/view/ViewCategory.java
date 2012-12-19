/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.view;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;

public abstract class ViewCategory {
	private ThreadRegion parent;
	private String label;

	public ViewCategory(ThreadRegion parent, String label) {
		this.parent = parent;
		this.label = label;
	}

	public ThreadRegion getParent() {
		return this.parent;
	}

	public String getLabel() {
		return this.label;
	}
}
