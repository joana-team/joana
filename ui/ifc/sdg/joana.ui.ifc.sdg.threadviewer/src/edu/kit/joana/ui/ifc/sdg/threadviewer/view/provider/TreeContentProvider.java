/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import edu.kit.joana.ui.ifc.sdg.threadviewer.controller.Controller;


/*
 * The content provider class is responsible for
 * providing objects to the edu.kit.joana.ui.ifc.sdg.threadviewer.view. It can wrap
 * existing objects in adapters or simply return
 * objects as-is. These objects may be sensitive
 * to the current input of the edu.kit.joana.ui.ifc.sdg.threadviewer.view, or ignore
 * it and always show the same content
 * (like Task List, for example).
 */

public class TreeContentProvider implements ITreeContentProvider {
	private Controller controller;

	public TreeContentProvider() {
		controller = Controller.getInstance();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return controller.getTreeChildren(parentElement);
	}

	@Override
	public Object getParent(Object element) {
		return controller.getTreeParent(element);
	}

	@Override
	public boolean hasChildren(Object element) {
		return controller.hasTreeChildren(element);
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return controller.getTreeRoots();
	}

	@Override
	public void dispose() { }

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {	}
}
