/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.views;

import java.util.LinkedList;
import java.util.List;

public class ViolationSelection {
	private static ViolationSelection instance;

	public static ViolationSelection getInstance() {
		if (instance == null) {
			instance = new ViolationSelection();
		}
		return instance;
	}

	private List<ClassificationView> views;

	private ViolationSelection() {
		views = new LinkedList<ClassificationView>();
	}

	public void addCView(ClassificationView v) {
		views.add(v);
	}

	public void violationSelected(Object[] objects) {
		for (ClassificationView v : views) {
			v.fillTable(objects);
		}
	}

}
