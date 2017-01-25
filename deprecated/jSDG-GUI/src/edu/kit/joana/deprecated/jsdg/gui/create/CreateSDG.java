/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.gui.create;

import java.io.IOException;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import edu.kit.joana.deprecated.jsdg.gui.Activator;

public class CreateSDG implements IObjectActionDelegate, DisposeListener {

	private ISelection selected;


	public CreateSDG() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	private Shell shell = null;

	public void run(IAction action) {
		if (shell != null && !shell.isDisposed()) {
			shell.setActive();
			return;
		}

		try {
			Object obj = ((IStructuredSelection)selected).getFirstElement();
			ICompilationUnit file = (ICompilationUnit) obj;

			Display disp = Activator.getDefault().getDisplay();
			shell = new Shell(disp);
			shell.addDisposeListener(this);
			shell.setText("jSDG: Create System Dependency Graph");

			SDGCreator inst = new SDGCreator(file, shell, SWT.NULL);
			inst.show();

			Point size = inst.getSize();
			shell.setLayout(new FillLayout());
			shell.layout();
			if(size.x == 0 && size.y == 0) {
				inst.pack();
				shell.pack();
			} else {
				Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
				shell.setSize(shellBounds.width, shellBounds.height);
			}

			Point oldPos = disp.getActiveShell().getLocation();
			shell.setBounds(oldPos.x + 100, oldPos.y + 100, shell.getSize().x, shell.getSize().y);
			shell.open();
		} catch (JavaModelException e) {
			Activator.getDefault().showError(e, "Could not read properties of selected java project (JavaModelException)");
		} catch (IOException e) {
			Activator.getDefault().showError(e, "Could not access default configuration (IOException)");
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		selected = selection;
	}

	public void widgetDisposed(DisposeEvent e) {
		shell = null;
	}

}
