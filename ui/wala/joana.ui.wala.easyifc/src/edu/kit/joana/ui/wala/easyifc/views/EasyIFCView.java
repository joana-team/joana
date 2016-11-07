/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.views;


import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
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

import edu.kit.joana.api.IFCType;
import edu.kit.joana.ui.wala.easyifc.Activator;
import edu.kit.joana.ui.wala.easyifc.actions.CollapseNodesAction;
import edu.kit.joana.ui.wala.easyifc.actions.ExpandNodesAction;
import edu.kit.joana.ui.wala.easyifc.actions.HighlightIFCResultAction;
import edu.kit.joana.ui.wala.easyifc.actions.IFCAction;
import edu.kit.joana.ui.wala.easyifc.actions.IFCRunnable;
import edu.kit.joana.ui.wala.easyifc.actions.SelectIFCTypeAction;
import edu.kit.joana.ui.wala.easyifc.actions.IFCAction.ProjectConf;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer;
import edu.kit.joana.ui.wala.easyifc.util.EntryPointSearch.EntryPointConfiguration;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.IFCInfoNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.LeakInfoNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.NotRunYetNode;
import edu.kit.joana.ui.wala.easyifc.views.IFCTreeContentProvider.TreeNode;


/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class EasyIFCView extends ViewPart {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "joana.ui.wala.easyifc.views.EasyIFCView";

	private IFCTreeViewer tree;
	private IFCAction checkIFCAction;
	private HighlightIFCResultAction markCriticalAction;
	private ExpandNodesAction expandNodesAction;
	private CollapseNodesAction collapseNodesAction;
	private Action doubleClickAction;
	private SingleIFCAction runIFCforSelectedEntryPoint;
	private SelectIFCTypeAction selectIFCTypeAction;

	public IFCType getSelectedIFCType() {
		return selectIFCTypeAction.getSelectedIFCtype();
	}

	public IFCTreeViewer getTree() {
		return tree;
	}

	private class SingleIFCAction extends Action {
		private EntryPointConfiguration entryPoint;
		private final IFCCheckResultConsumer resultConsumer;
		private final EasyIFCView view;
		public SingleIFCAction(IFCCheckResultConsumer resultConsumer, EasyIFCView view) {
			super();
			this.resultConsumer = resultConsumer;
			this.view = view;
			this.setText("Check selected entry point");
			this.setDescription("Check the information flow of the selected entry point.");
			this.setId("joana.ui.wala.easyifc.runSingleIFCAction");
			this.setImageDescriptor(Activator.getImageDescriptor("icons/run_ifc_action.png"));
		}

		@Override
		public void run() {
			try {
				final ProjectConf pconf = ProjectConf.create(view.getCurrentProject());
				PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						final IFCRunnable ifcrun = new IFCRunnable(pconf, resultConsumer, entryPoint);
						ifcrun.run(monitor);
					}
				});
			} catch (InvocationTargetException e) {
				view.showMessage(e.getClass().getCanonicalName());
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (InterruptedException e) {
				view.showMessage(e.getClass().getCanonicalName());
				e.printStackTrace();
				throw new RuntimeException(e);
			} catch (JavaModelException e) {
				view.showMessage(e.getClass().getCanonicalName());
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}

	private class ViewLabelProvider extends LabelProvider {
		public Image getImage(Object obj) {
			if (obj instanceof TreeNode) {
				final TreeNode<?,?,?> tn = (TreeNode<?,?,?>) obj;

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
		tree = new IFCTreeViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setContentProvider(new IFCTreeContentProvider(this));
		tree.setLabelProvider(new ViewLabelProvider());
		tree.setSorter(IFCTreeContentProvider.makeSorter());
		tree.setInput(null);

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
				EasyIFCView.this.fillContextMenu(manager);
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
		manager.add(markCriticalAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(runIFCforSelectedEntryPoint);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(expandNodesAction);
		manager.add(collapseNodesAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(expandNodesAction);
		manager.add(collapseNodesAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(selectIFCTypeAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(checkIFCAction);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private class UpdateTreeViewerConsumer implements IFCCheckResultConsumer {

		@Override
		public void consume(final IFCResult result) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					tree.setInput(result);
					tree.refresh();
				}
			});
		}
		@Override
		public void inform(final EntryPointConfiguration discovered) {
			Display.getDefault().asyncExec(new Runnable() {

				@Override
				public void run() {
					tree.setInput(discovered);
					tree.refresh();
				}
			});
			
		}

	}

	private void makeActions() {
		final UpdateTreeViewerConsumer consumer = new UpdateTreeViewerConsumer();
		checkIFCAction = new IFCAction(this, consumer);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().addSelectionListener(checkIFCAction);

		markCriticalAction = new HighlightIFCResultAction(this);
		markCriticalAction.setEnabled(false);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					final Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();

					if (obj instanceof TreeNode) {
						markCriticalAction.setEnabled(obj instanceof LeakInfoNode || (obj instanceof IFCInfoNode && !(obj instanceof NotRunYetNode)));
					}
				}
			}
		});

		runIFCforSelectedEntryPoint = new SingleIFCAction(consumer,this);
		runIFCforSelectedEntryPoint.setEnabled(false);
		tree.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(final SelectionChangedEvent event) {
				if (event.getSelection() instanceof IStructuredSelection) {
					final Object obj = ((IStructuredSelection) event.getSelection()).getFirstElement();
					if (obj instanceof TreeNode) {
						runIFCforSelectedEntryPoint.setEnabled(obj instanceof IFCInfoNode);
					}
					if (obj instanceof IFCInfoNode) {
						IFCInfoNode node = (IFCInfoNode) obj;
						runIFCforSelectedEntryPoint.entryPoint = node.getResult().getEntryPointConfiguration();
						runIFCforSelectedEntryPoint.setEnabled(runIFCforSelectedEntryPoint.entryPoint.getErrors().isEmpty());
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
						final TreeNode<?,?,?> tn = (TreeNode<?,?,?>) obj;
						final SourceRefElement sref = tn.getSourceRef();
						if (sref != null) {
							try {
								JavaUI.openInEditor(sref);
							} catch (PartInitException e) {
							} catch (JavaModelException e) {}
						} else {
							final IMarker[] side = tn.getSideMarker();
							if (side != null && side.length > 0 && side[0] != null) {
								final IMarker im = side[0];
								try {
									IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), im);
								} catch (PartInitException e) {}
							}
						}
					}
				}
			}
		};

		expandNodesAction = new ExpandNodesAction(tree);
		collapseNodesAction = new CollapseNodesAction(tree);
		selectIFCTypeAction = new SelectIFCTypeAction();
	}

	public void showMessage(String message) {
		MessageDialog.openInformation(tree.getControl().getShell(), "Information Flow Control Analysis", message);
	}

	private void hookDoubleClickAction() {
		tree.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	public IJavaProject getCurrentProject() {
		return checkIFCAction.getCurrentProject();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		tree.getControl().setFocus();
	}

	public void dispose() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().removeSelectionListener(checkIFCAction);
		super.dispose();
	}
}
