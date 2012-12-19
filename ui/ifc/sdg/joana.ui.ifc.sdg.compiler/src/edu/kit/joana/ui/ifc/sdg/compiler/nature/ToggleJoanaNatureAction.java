/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.compiler.nature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import edu.kit.joana.ui.ifc.sdg.compiler.util.PluginUtil;

/**
 * A eclipse ui <code>IObjectActionDelegate</code> toggling the JOANA nature
 * on selected JDT projects.
 *
 */
public class ToggleJoanaNatureAction implements IObjectActionDelegate {

	private List<IProject> projects = new ArrayList<IProject>();

	/**
	 * Constructor
	 */
	public ToggleJoanaNatureAction() {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {

	}

	public void run(IAction action) {
		try {
			for (IProject project : projects) {
				PluginUtil.toggleNature(project, JoanaNature.ID);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void selectionChanged(IAction action, ISelection selection) {
		assert selection != null;

		projects.clear();

		if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
			Iterator i = ((IStructuredSelection) selection).iterator();
			while (i.hasNext()) {
				Object current = i.next();
				if (current instanceof IJavaProject)
					projects.add(((IJavaProject) current).getProject());
			}
		}
	}
}
