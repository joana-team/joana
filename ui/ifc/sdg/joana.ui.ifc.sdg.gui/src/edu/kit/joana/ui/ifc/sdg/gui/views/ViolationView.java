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
package edu.kit.joana.ui.ifc.sdg.gui.views;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.Conflict;
import edu.kit.joana.ifc.sdg.core.violations.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.core.violations.Violation.Chop;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPath;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;
import edu.kit.joana.ui.ifc.sdg.gui.ActiveResourceChangeListener;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.actions.ChopAction;
import edu.kit.joana.ui.ifc.sdg.gui.contextmenu.AnnotateRedefiningActionDelegate;
import edu.kit.joana.ui.ifc.sdg.gui.contextmenu.MarkerActionDelegate;
import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.SliceHighlighter;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.ViolationChangeListener;
import edu.kit.joana.ui.ifc.sdg.gui.violations.IChopper;

/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class ViolationView extends ViewPart implements ActiveResourceChangeListener, ViolationChangeListener {
	private static final boolean DEBUG = false;

	private TreeViewer viewer;
	private Action showSDG;
	private Action selectionAction;
	private Action declassAction;
	private TreeViewContentProvider vcp;
	private ViolationView av;
	private SliceHighlighter sl = new SliceHighlighter();
	private IAction hideSliceHighlight;
	private IProject oldProject;
	private Collection<Violation> contentCopy;
	private IChopper[] extChoppers;
	private ChopAction[] extChopActions;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content
	 * (like Task List, for example).
	 */

	class TreeViewContentProvider implements ITreeContentProvider {
		Object[] content;

		public TreeViewContentProvider() {
			String[] as = {"no slicing data available"};
			content = as;
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}

		public void dispose() {}

		public Object[] getElements(Object parent) {
			return content;
		}

		public void setContent(Object[] lines) {
			content = lines;
		}

		public Object[] getChildren(Object parent) {
			if (parent instanceof edu.kit.joana.ifc.sdg.core.violations.Violation) {
				return ((edu.kit.joana.ifc.sdg.core.violations.Violation) parent).getChops().toArray();
			} else if (parent instanceof Chop) {
				return ((Chop) parent).getViolationPathes().getPathesList().toArray();
			} else if (parent instanceof ViolationPath) {
				LinkedList<SecurityNode> tpl = ((ViolationPath) parent).getPathList();
				ArrayList<prettySecureNode> out = new ArrayList<prettySecureNode>(tpl.size());
				for (SecurityNode now : tpl) {
					out.add(new prettySecureNode(now));
				}
				return out.toArray();

			} else {
				return new Object[0];
			}
		}

		public Object getParent(Object element) {
			return null;
		}

		public boolean hasChildren(Object element) {
			if (element instanceof edu.kit.joana.ifc.sdg.core.violations.Violation) return true;
			if (element instanceof Chop) return true;
			if (element instanceof ViolationPath) return true;
			return false;
		}
	}

	class TreeViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			return getText(obj);
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public String getText(Object obj) {
			// first tree level
			if (obj instanceof OrderConflict) {
				OrderConflict c = (OrderConflict) obj;

				return "Probabilistic Order Channel between Lines " + c.getSink().getSr() +
						" and " + c.getConflicting().getSr() + ", leaking Line " + c.getSource().getSr() +
						", visible for " + c.getAttackerLevel();

			} else if (obj instanceof Conflict) {
				Conflict c = (Conflict) obj;

				return "Probabilistic Data Channel from Line " + c.getSource().getSr() +
						" to Line " + c.getSink().getSr() + ", visible for " + c.getAttackerLevel();

			} else if (obj instanceof Violation) {
				Violation c = (Violation) obj;

				return "Illicit Flow from Line " + c.getSource().getSr() +
						" to Line " + c.getSink().getSr() + ", visible for " + c.getAttackerLevel();

			} else if (obj instanceof String) {
				return (String) obj;

			}

			// second tree level
			else if (obj instanceof Chop) {
				return ((Chop) obj).getName();
			} else if (obj instanceof ViolationPath) {
				ViolationPath v = (ViolationPath) obj;
				int[] linePath = v.reduceToStatements();
				StringBuffer b = new StringBuffer().append("Lines "+linePath[0]);

				for (int i = 1; i < linePath.length; i++) {
					b.append(" -> ").append(linePath[i]);
				}

				return b.toString();

			} else if (obj instanceof prettySecureNode) {
				return obj.toString();

			} else {
				return "not implemented; extend ViolationView.TreeViewLabelProvider.getText(Object obj)";
			}
		}

		public Image getImage(Object obj) {
			if (obj instanceof edu.kit.joana.ifc.sdg.core.violations.Violation) {
				return NJSecPlugin.singleton().getImageRegistry().get("violation");

			} else if (obj instanceof ViolationPath) {
				return NJSecPlugin.singleton().getImageRegistry().get("viopath");

			} else if (obj instanceof prettySecureNode) {
				if (((prettySecureNode) obj).getSecureNode().isInformationSource()) {
					return NJSecPlugin.singleton().getImageRegistry().get("vionodeann");

				} else if (((prettySecureNode) obj).getSecureNode().isInformationSink()) {
					return NJSecPlugin.singleton().getImageRegistry().get("vionodeout");

				} else if (((prettySecureNode) obj).getSecureNode().isDeclassification()) {
					return NJSecPlugin.singleton().getImageRegistry().get("vionodedeclass");

				} else {
					return NJSecPlugin.singleton().getImageRegistry().get("vionodeunann");
				}

			} else if (obj instanceof String) {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);

			} else {
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);
			}
		}
	}

	class TreeNameSorter extends ViewerSorter {}

	class prettySecureNode {
		SecurityNode n;

		public prettySecureNode(SecurityNode n) {
			this.n = n;
		}

		public SecurityNode getSecureNode() {
			return n;
		}

		public String toString() {
			String str = "SDGNode "+n.getId()+" ["+n.getKind()+ " " + n.getLabel() + ", " + n.getSource()+ " " + n.getSr() + "]";
			return str;
		}
	}


	/**
	 * The constructor.
	 */
	public ViolationView() {
		av = this;
		NJSecPlugin.singleton().getSDGFactory().addViolationChangeListener(this);
	}

	public void dispose() {
		NJSecPlugin.singleton().getSelectionStore().removeActiveResourceChangeListener(this);
	}

	public void setContent(Object[] newc) {
		if (vcp != null) vcp.setContent(newc);

		Runnable body = new Runnable() {
			public void run() {
				viewer.refresh();
			}
		};

		NJSecPlugin.singleton().getWorkbench().getDisplay().asyncExec(body);
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		SashForm sash = new SashForm(parent, SWT.VERTICAL);
		viewer = new TreeViewer(sash, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		vcp = new TreeViewContentProvider();
		viewer.setContentProvider(vcp);
		viewer.setLabelProvider(new TreeViewLabelProvider());
		viewer.setInput(getViewSite());

		try {
			readChopExtensions();

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError(e.getMessage(), null, e);
		}

		makeActions();
		hookContextMenu();
		hookSelectionAction();
		contributeToActionBars();
		NJSecPlugin.singleton().getSelectionStore().addActiveResourceChangeListener(this);
	}

	private void readChopExtensions() throws CoreException {
		ArrayList<IChopper> ret = new ArrayList<IChopper>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point =
		registry.getExtensionPoint("edu.kit.joana.ifc.sdg.gui.ChopExtension");
		IExtension[] extensions = point.getExtensions();

		for (IExtension ext : extensions) {
			IConfigurationElement[] ces = ext.getConfigurationElements();

			for (IConfigurationElement ce : ces) {
				if (ce.getName().equals("chopper")) {
					Object impl = ce.createExecutableExtension("class");

					if (impl instanceof IChopper) {
						IChopper newChopper = (IChopper) impl;
						ret.add(newChopper);
					}
				}
			}
		}

		extChoppers = ret.toArray(new IChopper[0]);
	}

	private void makeChopActions() {
		extChopActions = new ChopAction[extChoppers.length];
		int i = 0;

		for (IChopper cho : extChoppers) {
			ChopAction newca = new ChopAction(cho, this);
			cho.initializeGUI(newca);
			extChopActions[i++] = newca;
		}
	}

	private void hookExtChopActions(IMenuManager manager) {
		for (ChopAction ca : extChopActions) {
			manager.add(ca);
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				ViolationView.this.fillContextMenu(manager);
			}
		});

		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
//		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

//	private void fillLocalPullDown(IMenuManager manager) {}

	private void fillContextMenu(IMenuManager manager) {
		ISelection sel = viewer.getSelection();

		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			Object selobj = ssel.getFirstElement();

			if (selobj instanceof prettySecureNode) {
				manager.add(declassAction);
			} else if (selobj instanceof Violation) {
				hookExtChopActions(manager);
				// Other plug-ins can contribute their actions here
				manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			} else if (selobj instanceof Chop) {

			} else {
//				NullChopper ncho = new NullChopper();
//				ChopAction choa = new ChopAction(ncho, this);
//				ncho.initializeGUI(choa);
//				manager.add(choa);
			}
		}
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(hideSliceHighlight);
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void makeActions() {
		//Create Action for Extensions
		makeChopActions();

		showSDG = new Action() {
			public void run() {
				IProject p = NJSecPlugin.singleton().getActiveProject();
				String s = NJSecPlugin.singleton().getSDGFactory().getCachedSDG(p).toString();
				FileDialog dialog = new FileDialog (viewer.getControl().getShell(), SWT.SAVE);

				dialog.setFilterNames (new String [] {"SDG Files", "All Files (*.*)"});
				dialog.setFilterExtensions (new String [] {"*.pdg;*.sdg", "*.*"}); // wild cards
				dialog.open();

				String filename = dialog.getFileName();
				String pf = dialog.getFilterPath() + File.separator + filename;
				FileWriter fw = null;

				try {
					fw = new FileWriter(pf);
					fw.write(s);

				} catch (IOException e) {
					e.printStackTrace();

				} finally {
					if (fw != null) {
						try {
							fw.close();

						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}

				if (DEBUG) System.out.println(s);
			}
		};

		showSDG.setText("Save Unannotated SDG Source");
		showSDG.setToolTipText("Save Unannotated SDG Source");
		showSDG.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

		hideSliceHighlight = new Action() {
			public void run() {
				try {
                    IProject active = NJSecPlugin.singleton().getActiveProject();
					sl.removeAllSliceHighlighting(active);

				} catch (CoreException e) {
					NJSecPlugin.singleton().showError("Problem while hiding Highlighting", null, e);
				}
			}
		};
		hideSliceHighlight.setText("Hide All Visible Slice Highlights");
		hideSliceHighlight.setToolTipText("Hide Slice-Highlighting");
		hideSliceHighlight.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("hideann"));

		selectionAction = new Action() {
			public void run() {
				ISelection sel = viewer.getSelection();

				if (sel instanceof IStructuredSelection) {
					IStructuredSelection ssel = (IStructuredSelection) sel;
					Object selobj = ssel.getFirstElement();
					Collection<SecurityNode> involvedNodes = new LinkedList<SecurityNode>();

					if (selobj instanceof edu.kit.joana.ifc.sdg.core.violations.Violation) {
						involvedNodes = ((edu.kit.joana.ifc.sdg.core.violations.Violation) selobj).getAllInvolvedNodes();
						//Tabelle fuellen
						ViolationSelection.getInstance().violationSelected(((Violation) selobj).getClassifications().toArray());
					} else if (selobj instanceof Chop) {
						involvedNodes = ((Chop) selobj).getViolationPathes().getAllInvolvedNodes();
					} else if (selobj instanceof ViolationPathes) {
						involvedNodes = ((ViolationPathes) selobj).getAllInvolvedNodes();

					} else if (selobj instanceof ViolationPath) {
						involvedNodes = ((ViolationPath) selobj).getPathList();

					} else if (selobj instanceof prettySecureNode) {
						try {
	                        IProject active = NJSecPlugin.singleton().getActiveProject();
							sl.revealSliceNode(active,((prettySecureNode) selobj).getSecureNode());
							NJSecPlugin.singleton().showView("edu.kit.joana.ifc.sdg.gui.views.ViolationView");
							return;

						} catch (CoreException e) {
							String msg = "Problem While Creating IFlowMarker For The purpose of Displaying Slice" + e.toString();
							NJSecPlugin.singleton().showError(msg, null, e);

						} catch (IOException e) {
							IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0,
									"Couldn't open file for Line-Offset-Calculation: " + e.toString(), e);
							String msg = "Problem While Calculating Line-Offset While Creating IFlowMarker For Displaying Slice" + e.toString();
							NJSecPlugin.singleton().showError(msg, status, e);
						}
					}

					try {
					    IProject active = NJSecPlugin.singleton().getActiveProject();
						sl.removeAllSliceHighlighting(active);
						sl.addSliceNodes(active, involvedNodes);

					} catch (CoreException e) {
						NJSecPlugin.singleton().showError("Problem With Creating IFlowMarker For Displaying Slice" + e.toString(), null, e);

					} catch (IOException e) {
						IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0,
								"Couldn't open file for Line-Offset-Calculation: " + e.toString(), e);
						String msg = "Problem While Calculating Line-Offset While Creating IFlowMarker For Displaying Slice" + e.toString();
						NJSecPlugin.singleton().showError(msg, status, e);
					}
				}
			}
		};

		declassAction = new Action(){

			public void run() {
				ISelection s = viewer.getSelection();
				if (s instanceof IStructuredSelection) {
					Object o = ((IStructuredSelection) s).getFirstElement();
					if (o instanceof prettySecureNode) {
						SecurityNode n  = ((prettySecureNode) o).getSecureNode();
						MarkerActionDelegate d = new AnnotateRedefiningActionDelegate();
						IEditorPart part = NJSecPlugin.singleton().getActivePage().getActiveEditor();
						ITextEditor editor = (ITextEditor) part;
						IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
						TextSelection sel;
						try {
							sel = new TextSelection(doc, doc.getLineOffset(n.getSr()) + n.getSc(), doc.getLineOffset(n.getEr()) + n.getEc() - (doc.getLineOffset(n.getSr()) + n.getSc()));
						} catch (BadLocationException e) {
							sel = new TextSelection(doc, 0, 0);
						}
						d.selectionChanged(null, sel);
						d.run(null);
						IMarker im = d.getMarker();
						MarkerManager.singleton().map(NJSecPlugin.singleton().getActiveProject(), im, n);
					}
				}
			}
		};
		declassAction.setText("Declassificate Node");
		declassAction.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("pdg"));

	}

	private void hookSelectionAction() {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectionAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.gui.marker.NJSecMarkerListener#markersChanged()
	 */
	class ToStringMarker {
		public IMarker m = null;
		public String string = "";

		public String toString() {
			return string;
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.gui.ActiveResourceChangeListener#activeResourceChanged(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
	 */
	public void activeResourceChanged(IResource activeResource, IProject activeProject) {
		if (oldProject != activeProject) {
			Collection<Violation> violations = NJSecPlugin.singleton().getSDGFactory().getRemViolations(activeProject);

			if (violations == null) {
				String[] novio = {"No Data!"};
				av.setContent(novio);
				contentCopy = null;

			} else if (violations.size() == 0) {
				String[] novio = {"No Security Violations Found By Last Check!"};
				av.setContent(novio);
				contentCopy = new ArrayList<edu.kit.joana.ifc.sdg.core.violations.Violation>();

			} else {
				av.setContent(violations.toArray());
				contentCopy = violations;
			}
		}

		oldProject = activeProject;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.gui.sdgworks.ViolationChangeListener#violationsChanged(org.eclipse.core.resources.IProject, java.util.List)
	 */
	public void violationsChanged(IProject p, Collection<edu.kit.joana.ifc.sdg.core.violations.Violation> violations) {
		contentCopy = violations;

		if (DEBUG) System.out.println("Violations Changed ViolationView");

		if (violations.size() == 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
			String[] novio = {"No Security Violations Found By Last Check! (Time: " + sdf.format(new Date()) + ")"};
			av.setContent(novio);

		} else {
			av.setContent(violations.toArray());
		}
	}

	public void violationsChanged(IProject p) {
		av.setContent(contentCopy.toArray());
	}

	public Collection<Violation> getSelected() {
		LinkedList<Violation> ret = new LinkedList<Violation>();
		ISelection sel = viewer.getSelection();
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection stru = (IStructuredSelection) sel;
			for (Object o : stru.toList()) {
				if (o instanceof Violation) {
					ret.add((Violation) o);
				}
			}
		}
		return ret;
	}

	public Collection<edu.kit.joana.ifc.sdg.core.violations.Violation> getContentCache() {
		return this.contentCopy;
	}

}
