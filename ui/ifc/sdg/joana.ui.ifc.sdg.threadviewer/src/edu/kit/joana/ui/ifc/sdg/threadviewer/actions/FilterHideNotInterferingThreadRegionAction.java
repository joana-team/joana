/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.actions;

import org.eclipse.jface.action.Action;

import edu.kit.joana.ui.ifc.sdg.threadviewer.view.view.ThreadViewer;


public class FilterHideNotInterferingThreadRegionAction extends Action {
	public FilterHideNotInterferingThreadRegionAction() {
		super("Hide thread regions without interference");
		setChecked(false);
	}

	@Override
	public void run() {
		ThreadViewer.getInstance().updateNotInterferingFilter();
	}
}
