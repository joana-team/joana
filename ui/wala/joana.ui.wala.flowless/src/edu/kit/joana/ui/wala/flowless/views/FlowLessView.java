/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.flowless.views;


import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ui.wala.flowless.actions.CheckFlowAction;
import edu.kit.joana.ui.wala.flowless.actions.CollapseNodesAction;
import edu.kit.joana.ui.wala.flowless.actions.ExpandNodesAction;
import edu.kit.joana.ui.wala.flowless.actions.SliceFlowResultAction;
import edu.kit.joana.ui.wala.flowless.views.FlowLessTreeContentProvider.StmtPartNode;
import edu.kit.joana.ui.wala.flowless.views.FlowLessTreeContentProvider.TreeNode;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.MethodResult;


/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class FlowLessView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "edu.kit.joana.ui.wala.flowless.views.FlowLessView";

	private CheckFlowTreeViewer tree;
	private CheckFlowAction checkFlowAction;
	private SliceFlowResultAction sliceAction;
	private ExpandNodesAction expandNodesAction;
	private CollapseNodesAction collapseNodesAction;
	private Action doubleClickAction;


	public CheckFlowTreeViewer getTree() {
		return tree;
	}

	private class ViewLabelProvider extends LabelProvider {
		public Image getImage(Object obj) {
			if (obj instanceof TreeNode) {
				final TreeNode tn = (TreeNode) obj;

				return tn.getImage();
			} else {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
			}
		}
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		tree = new CheckFlowTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setContentProvider(new FlowLessTreeContentProvider(this));
		tree.setLabelProvider(new ViewLabelProvider());
		tree.setSorter(FlowLessTreeContentProvider.makeSorter());
		tree.setInput(null);

		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(tree.getControl(), "MoJo-FlowLess-Plugin.viewer");
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				FlowLessView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(tree.getControl());
		tree.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, tree);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(sliceAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(expandNodesAction);
		manager.add(collapseNodesAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(expandNodesAction);
		manager.add(collapseNodesAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(checkFlowAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private class UpdateTreeViewerConsumer implements FlowCheckResultConsumer {

		@Override
		public void consume(final MethodResult result) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					tree.setInput(result);
					tree.refresh();
				}
			});
		}

	}

	private void makeActions() {
		checkFlowAction = new CheckFlowAction(this, new UpdateTreeViewerConsumer());
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(checkFlowAction);
		sliceAction = new SliceFlowResultAction(this);
		sliceAction.setEnabled(false);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					final Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();

					if (obj instanceof TreeNode) {
						if (obj instanceof StmtPartNode) {
							sliceAction.setEnabled(true);
						} else {
							sliceAction.setEnabled(false);
						}
					}
				}
			}
		});
		doubleClickAction = new Action() {
			public void run() {
				final ISelection sel = tree.getSelection();
				if (sel instanceof ITreeSelection) {
					final ITreeSelection tsel = (ITreeSelection) sel;
					final Object obj = tsel.getFirstElement();
					if (obj instanceof TreeNode) {
						final TreeNode tn = (TreeNode) obj;
						final IMarker m = tn.getMarker();
						if (m != null) {
							try {
								IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), m);
							} catch (PartInitException e) {
							}
						} else {
							final SourceRefElement sref = tn.getSourceRef();
							if (sref != null) {
								try {
									JavaUI.openInEditor(sref);
								} catch (PartInitException e) {
								} catch (JavaModelException e) {
								}
	//							System.out.println("Click click: " + tsel.getFirstElement());
							}
						}
					}
				}
			}
		};

		expandNodesAction = new ExpandNodesAction(tree);
		collapseNodesAction = new CollapseNodesAction(tree);
	}

	public void showMessage(String message) {
		MessageDialog.openInformation(tree.getControl().getShell(), "IFC Annotation Checker", message);
	}

	private void hookDoubleClickAction() {
		tree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	public IJavaProject getCurrentProject() {
		return checkFlowAction.getCurrentProject();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		tree.getControl().setFocus();
	}

	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().removeSelectionListener(checkFlowAction);
		super.dispose();
	}
}
