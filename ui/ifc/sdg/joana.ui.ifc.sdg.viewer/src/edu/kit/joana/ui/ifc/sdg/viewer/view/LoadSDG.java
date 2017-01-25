/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view;

import java.io.IOException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.viewer.model.GraphFactory;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;


/**
 * LoadSDG implements a PopupMenu contribution for Java source files.
 * It is used to load a saved PDG from harddisc.
 */
public class LoadSDG implements IObjectActionDelegate {
	IWorkbenchPart part;
	ISelection selection ;

	/**
	 * Constructor for LoadSDG.
	 */
	public LoadSDG() {
		super();
	}

	/**
	 * This inherited method is used to set the currently active workbench part.
	 *
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		part = targetPart;
	}

	/**
	 * This method loads a saved PDG and propagates it to the AnalysisView.
	 * It is triggered by the eclipse framework.
	 *
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		// the current shell and the selected Java file
		Shell shell = this.part.getSite().getShell();
		ICompilationUnit file = (ICompilationUnit)((IStructuredSelection)selection).getFirstElement();

		// open a FileDialog to choose a saved PDG
		FileDialog fd = new FileDialog(shell, SWT.OPEN);
        fd.setText("Open");
        String[] filterExt = { "*.pdg"};
        fd.setFilterExtensions(filterExt);
        String selected = fd.open();

		try {
	        // load the chosen PDG
	        Graph g = GraphFactory.loadGraph(file, selected);

	        // give it to the AnalysisView
	        AnalysisView.getInstance().setNewGraph(g);
		} catch (IOException e) {
			Activator.getDefault().showError(e, "Could not load graph from file " + selected);
		}
	}

	/**
	 * This inherited method is used to set the currently selected Java source file.
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

}
