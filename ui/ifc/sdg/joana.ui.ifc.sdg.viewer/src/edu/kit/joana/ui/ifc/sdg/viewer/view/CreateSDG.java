/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.view;
//package edu.kit.joana.ui.ifc.sdg.viewer.view;
//
//
//import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
//import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;
//
//import org.eclipse.jdt.internal.core.CompilationUnit;
//import org.eclipse.jface.action.IAction;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.jface.viewers.ISelection;
//import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.graphics.Point;
//import org.eclipse.swt.graphics.Rectangle;
//import org.eclipse.swt.layout.FillLayout;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.ui.IActionDelegate;
//import org.eclipse.ui.IObjectActionDelegate;
//import org.eclipse.ui.IWorkbenchPart;
//
///**
// * CreateSDG implements a PopupMenu contribution for Java source files.
// * It is used to compute SDGs for selected Java files.
// */
//public class CreateSDG implements IObjectActionDelegate {
//	private ISelection selected;
//
//	/**
//	 * Constructor for CreateSDG.
//	 */
//	public CreateSDG() {
//		super();
//	}
//
//	/**
//	 * This inherited method is used to set the currently active workbench part.
//	 *
//	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
//	 */
//	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
//	}
//
//	/** This method is used to create a SDG for a chosen Java file.
//	 * It triggers a SDGCreator object where the user can configure the analysis.
//	 *
//	 * @see IActionDelegate#run(IAction)
//	 */
//	public void run(IAction action) {
//		if (AnalysisView.getInstance() == null) {
//			MessageDialog.openInformation(
//					new Shell(),
//					"No Analysis View activated",
//					"Please activate an Analysis View first");
//
//		} else {
//			createSDGCreator(action);
//		}
//	}
//
//	private void createSDGCreator(IAction action) {
//		Object obj = ((IStructuredSelection)selected).getFirstElement();
//		CompilationUnit file = (CompilationUnit)obj;
//
//		Display disp = Activator.getDefault().getDisplay();
//		Shell shell = new Shell(disp);
//
//		SDGCreator inst = new SDGCreator(file, shell, SWT.NULL);
//		Point size = inst.getSize();
//		shell.setLayout(new FillLayout());
//		shell.layout();
//		if(size.x == 0 && size.y == 0) {
//			inst.pack();
//			shell.pack();
//		} else {
//			Rectangle shellBounds = shell.computeTrim(0, 0, size.x, size.y);
//			shell.setSize(shellBounds.width, shellBounds.height);
//		}
//
//		Point oldPos = disp.getActiveShell().getLocation();
//		shell.setBounds(oldPos.x + 100, oldPos.y + 100, shell.getSize().x, shell.getSize().y);
//		shell.open();
//	}
//
//	/**
//	 * This inherited method is used to set the currently selected Java source file.
//	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
//	 */
//	public void selectionChanged(IAction action, ISelection selection) {
//		selected = selection;
//	}
//
//}
//
