/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.listener;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;

import edu.kit.joana.ui.ifc.sdg.threadviewer.controller.Controller;


public class ViewDoubleClickListener implements IDoubleClickListener {

	public void doubleClick(DoubleClickEvent event) {
		Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
		Controller.getInstance().runViewDoubleClick(obj);
	}
}
