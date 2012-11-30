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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;
import edu.kit.joana.ui.ifc.sdg.gui.ActiveResourceChangeListener;
import edu.kit.joana.ui.ifc.sdg.gui.CAction;
import edu.kit.joana.ui.ifc.sdg.gui.ConfigurationException;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.actions.RemoveAllMarkersAction;
import edu.kit.joana.ui.ifc.sdg.gui.actions.RemoveAnnotationAction;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigReader;
import edu.kit.joana.ui.ifc.sdg.gui.launching.ConfigurationAttributes;
import edu.kit.joana.ui.ifc.sdg.gui.launching.LaunchConfigurationTools;
import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerConstants;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerListener;
import edu.kit.joana.ui.ifc.sdg.gui.views.mapping.MarkerSelectionBroker;

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

public class AnnotationView extends ViewPart implements NJSecMarkerListener, ActiveResourceChangeListener {
	private TableViewer viewer;
	private Action removeMarker, reAssignSDGNode;
	private CAction doubleClickAction;
	private CAction selectionAction;
	private ViewContentProvider vcp;
	private ViewLabelProvider vlp;
	private AnnotationView av;
	private NameSorter ns;
	private Action save;
	private Action load;
	private Action removeAllMarkers;
	private Action editClasses;
	private Action addPair;
	private Action removePair;
	private Action toggleActive;

	/*
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content
	 * (like Task List, for example).
	 */

	class ViewContentProvider implements IStructuredContentProvider {
		Object[] content;

		public ViewContentProvider() {
			String[] as = {"No Editor Selected!"};
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
	}

	class ViewLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (obj instanceof String) {
				if (index==3) {
					return (String) obj;
				} else {
					return "";
				}
			}

			IMarker m = (IMarker) obj;

			if (index==0) {
				return m.getAttribute(IMarker.MESSAGE, "");

			} else if (index==2) {
				return m.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "").replaceAll("\n", "; ");

			} else if (index==1) {
				return m.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "").replaceAll("\n", "; ");

			} else if (index==3) {
				return m.getResource().getName();

			} else if (index==4) {
				return miniconv(m.getAttribute(IMarker.LINE_NUMBER, -1));

			} else if (index==5) {
				return miniconv(m.getAttribute(IMarker.CHAR_START, -1));

			} else if (index==6) {
				return miniconv(m.getAttribute(IMarker.CHAR_END, -1));

			} else if (index==7) {
				return (m.getAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, ""));
			}

			return getText(obj);
		}

		public String miniconv(int i) {
			if (i == -1) {
				return "n/a";
			} else {
				return "" + i;
			}
		}


		public Image getColumnImage(Object obj, int index) {
			if (index == 0 && obj instanceof IMarker) {
				if (((IMarker) obj).getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true)) {
					return NJSecPlugin.singleton().getImageRegistry().get("activated");
				} else {
					return NJSecPlugin.singleton().getImageRegistry().get("deactivated");
				}
			}
			return null;
		}
	}

	class NameSorter extends ViewerSorter {
		int col = 0;
		int multi = 1;

		NameSorter(int column) {
			col = column;
		}

		public void sortByColumn(int column) {
			if (col == column) multi *= -1;
			col = column;
		}

		public int compare(Viewer viewer, Object e1, Object e2) {
			String s1 = vlp.getColumnText(e1, col);
			String s2 = vlp.getColumnText(e2, col);
			return s1.compareTo(s2)*multi;
		}
	}



	/**
	 * The constructor.
	 */
	public AnnotationView() {
		av = this;
		MarkerManager.singleton().addListener(this);
	}

	public void dispose() {
		NJSecPlugin.singleton().getSelectionStore().removeActiveResourceChangeListener(this);
	}

	private void setContent(Object[] newc) {
		if (vcp != null) {
			vcp.setContent(newc);
			viewer.refresh();
			Resizer.resize(viewer.getTable(), new int[] {30,5,5,5,5,5,5,5});
		}
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	@SuppressWarnings("unchecked")
	public void createPartControl(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION); //SWT.MULTI |
		TableColumn c0 = new TableColumn(viewer.getTable(), SWT.LEFT, 0);
		TableColumn c1 = new TableColumn(viewer.getTable(), SWT.LEFT, 1);
		TableColumn c2 = new TableColumn(viewer.getTable(), SWT.LEFT, 2);
		TableColumn c3 = new TableColumn(viewer.getTable(), SWT.LEFT, 3);
		TableColumn c4 = new TableColumn(viewer.getTable(), SWT.RIGHT, 4);
		TableColumn c5 = new TableColumn(viewer.getTable(), SWT.RIGHT, 5);
		TableColumn c6 = new TableColumn(viewer.getTable(), SWT.RIGHT, 6);
		TableColumn c7 = new TableColumn(viewer.getTable(), SWT.RIGHT, 7);

		c0.setText("Message");
		c0.setWidth(300);
		c0.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(0);
				viewer.refresh();
			}
		});

		c2.setText("Required Security Level");
		c2.setWidth(100);
		c2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(1);
				viewer.refresh();
			}
		});

		c1.setText("Provided Security Level");
		c1.setWidth(100);
		c1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(2);
				viewer.refresh();
			}
		});

		c3.setText("File");
		c3.setWidth(100);
		c3.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(3);
				viewer.refresh();
			}
		});

		c4.setText("Line");
		c4.setWidth(50);
		c4.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(4);
				viewer.refresh();
			}
		});

		c5.setText("Charstart");
		c5.setWidth(50);
		c5.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(5);
				viewer.refresh();
			}
		});

		c6.setText("Charend");
		c6.setWidth(50);
		c6.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(6);
				viewer.refresh();
			}
		});

		c7.setText("SDGNodes");
		c7.setWidth(50);
		c7.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(7);
				viewer.refresh();
			}
		});

		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		vcp = new ViewContentProvider();
		viewer.setContentProvider(vcp);
		viewer.setLabelProvider(vlp=new ViewLabelProvider());
		viewer.setSorter(ns = new NameSorter(0));
		viewer.setInput(getViewSite());

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		hookSelectionAction();
		contributeToActionBars();

		NJSecPlugin.singleton().getSelectionStore().addActiveResourceChangeListener(this);
		IProject activeResource = NJSecPlugin.singleton().getActiveProject();

		if (activeResource != null) {
			markersChanged(activeResource);
			try {
				if (NJSecPlugin.singleton().getStandardLaunchConfiguration(activeResource) != null) {
					MarkerManager.singleton().changeProject(activeResource,
							NJSecPlugin.singleton().getStandardLaunchConfiguration(activeResource).
							getAttribute(ConfigurationAttributes.MARKER, new ArrayList<String>()));
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");

		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				AnnotationView.this.fillContextMenu(manager);
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
		manager.add(removeMarker);
		manager.add(reAssignSDGNode);
		manager.add(editClasses);
		manager.add(addPair);
		manager.add(removePair);
		manager.add(toggleActive);

		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(load);
		manager.add(save);
		manager.add(removeAllMarkers);
		manager.add(removeMarker);
		manager.add(reAssignSDGNode);
		manager.add(editClasses);
		manager.add(addPair);
		manager.add(removePair);
		manager.add(toggleActive);
	}

	private void makeActions() {
		save = new Action() {
			public void run() {
				FileDialog dialog = new FileDialog(NJSecPlugin.singleton().getActiveShell(), SWT.SAVE);
				String file = dialog.open();
				if (file==null) return;
				NJSecPlugin.singleton().getMarkerFactory().saveMarker(new File(file), (IMarker[])vcp.getElements(null));
			}
		};
		save.setText("Save edu.kit.joana.ifc.sdg Markers");
		save.setToolTipText("Save edu.kit.joana.ifc.sdg Markers");
		save.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("save"));

		load = new Action() {
			public void run() {
				FileDialog dialog = new FileDialog(NJSecPlugin.singleton().getActiveShell(), SWT.OPEN);
				String file = dialog.open();
				if (file==null) return;
				NJSecPlugin.singleton().getMarkerFactory().loadMarkerIntoActiveProject(new File(file));
			}
		};
		load.setText("Load edu.kit.joana.ifc.sdg Markers");
		load.setToolTipText("Load edu.kit.joana.ifc.sdg Markers");
		load.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("open"));

		removeMarker = new RemoveAnnotationAction(this);
		removeMarker.setText("Delete");
		removeMarker.setToolTipText("Delete Selected NJSec Marker");
		removeMarker.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));


		removeAllMarkers = new RemoveAllMarkersAction();
		removeAllMarkers.setText("Delete all Markers");
		removeAllMarkers.setToolTipText("Delete all NJSec Markers in view");
		removeAllMarkers.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("removeall"));

		reAssignSDGNode = new Action() {
			public void run() {
				IProject p = NJSecPlugin.singleton().getActiveProject();
				ISelection selection = viewer.getSelection();
				List<?> objs = ((IStructuredSelection)selection).toList();
				for (int i = 0; i < objs.size(); i++) {
					IMarker im = (IMarker) objs.get(i);

					try {
						im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, null);
						IMarker[] tim = new IMarker[1];
						tim[0] = im;
				        MarkerManager.singleton().updateMarker(p);

					} catch (CoreException e) {
					    NJSecPlugin.singleton().showError("Problem while annotating SDG by Markers", null, e);
					}
				}

			}
		};
		reAssignSDGNode.setText("Remove SDG-Node-Matching From This Marker");
		reAssignSDGNode.setToolTipText("Remove SDG-Node-Matching from Marker. You'll have to reassign a SDG-Node to this marker.");
		reAssignSDGNode.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_REDO));


		doubleClickAction = new CAction() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof IMarker) {
					IMarker im = (IMarker) obj;

					/**
					 * notifyAnnotationSelectionListeners
					 */
					MarkerSelectionBroker.getInstance().notifyListeners(im);
					/**
					 * Reveal Annotation in JavaEditor
					 */
					IWorkbenchPage page = NJSecPlugin.singleton().getActivePage();
					try {
						IDE.openEditor(page, im);
					} catch (PartInitException e) {
					    NJSecPlugin.singleton().showError("Problem while jumping to NJSec Annotation Marker", null, e);
					}
				}

			}

		};
		doubleClickAction.setContext(this);

		selectionAction = new CAction() {
			public void run() {
				IMarker im = getSelectedMarker();
				if (im != null) {
					// notifyAnnotationSelectionListeners
					MarkerSelectionBroker.getInstance().notifyListeners(im);

					// Reveal Annotation in JavaEditor
					// then reactivate view
					IWorkbenchPage page = NJSecPlugin.singleton().getActivePage();
					try {
						IDE.openEditor(page, im);
						NJSecPlugin.singleton().getActivePage().showView("edu.kit.joana.ifc.sdg.gui.views.AnnotationView");
					} catch (PartInitException e) {
					    NJSecPlugin.singleton().showError("Problem while jumping to NJSec Annotation Marker", null, e);
					}

					//check redefining
					try {
						if (NJSecMarkerConstants.MARKER_TYPE_REDEFINE.equals(im.getType())) {
							addPair.setEnabled(true);
							removePair.setEnabled(true);
						} else {
							addPair.setEnabled(false);
							removePair.setEnabled(false);
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}

					//check active
					if (im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true)) {
						toggleActive.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("deactivated"));
					} else {
						toggleActive.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("activated"));
					}
				}
			}
		};
		selectionAction.setContext(this);

		editClasses = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				List<?> objs = ((IStructuredSelection)selection).toList();

				for (int i = 0; i < objs.size(); i++) {
					try {
						IMarker im = (IMarker) objs.get(i);

						if (im.getType().equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
							int declassRules = NJSecPlugin.singleton().getMarkerFactory().getNumberOfDeclassRules(im);

							if (declassRules > 0) {
								// change is only possible if we have at least one existing rule
								int pos = 1;

							    if (declassRules > 1) {
							    	InputDialog idlg =
							    			new InputDialog(NJSecPlugin.singleton().getActiveShell(),
							    			"Position of the rule to change:", "Type the position of the rule to change", "1", null);

							    	idlg.open();

									try {
										pos = Integer.parseInt(idlg.getValue());

										if (pos < 1 || pos > declassRules) {
											NJSecPlugin.singleton().showError("Please type a number between 1 and "+declassRules , null, null);
											continue;// jump to the next marker in the selection queue
										}

									} catch (NumberFormatException ne) {
										NJSecPlugin.singleton().showError("Please type a number", null, ne);
										continue;// jump to the next marker in the selection queue
									}
							    }

							    // now change the selected declass rule
							    String requiredOld = im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "").split(";")[pos - 1];
							    String providedOld = im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "").split(";")[pos - 1];

								//Dialog erstellen
								DoubleLatticeDialog dlg = new DoubleLatticeDialog(NJSecPlugin.singleton().getShell());
							    IProject p = NJSecPlugin.singleton().getActiveProject();
						        dlg.setLattice(openLattice(p));
						        dlg.setAddCancelButton(true);
						        dlg.setMessage1("Select Security Class Allowed To Flow Into Selected Code\n");
						        dlg.setMessage2("Select Security Class For Information Flowing Out Of Selected Code\n");
						        dlg.setTitle("Select Security Class");
						        dlg.open();

						        //Dialog auswerten
						        Object[] secclasses = dlg.getResult();
						        if (secclasses == null || secclasses.length < 2) continue;
						        String required = secclasses[0].toString();
						        if (required == null) continue;
						        String provided = secclasses[1].toString();
						        if (provided == null) continue;

						        NJSecPlugin.singleton().getMarkerFactory().removePairFromRedefiningMarker(im, requiredOld, providedOld);
						        NJSecPlugin.singleton().getMarkerFactory().addPairToRedefiningMarker(im, required, provided);
						        MarkerManager.singleton().updateMarker(p);
//						        System.out.println(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, ""));
//						        System.out.println(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, ""));
							}

						} else { // not a declassification
							//Dialog erstellen
							LatticeDialog dlg = new LatticeDialog(NJSecPlugin.singleton().getShell());
							IProject p = NJSecPlugin.singleton().getActiveProject();
							dlg.setLattice(openLattice(p));
					        dlg.setAddCancelButton(true);
					        dlg.setMessage("Select Security Class for Selected Code");
					        dlg.setTitle("Select Security Class");
					        dlg.open();

					        //Auswerten
					        Object[] secclasses = dlg.getResult();
					        if (secclasses == null) return;
					        String secclass = secclasses[0].toString();

							if (im.getType().equals(NJSecMarkerConstants.MARKER_TYPE_INPUT)) {
								im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, secclass);
							} else if (im.getType().equals(NJSecMarkerConstants.MARKER_TYPE_OUTPUT)) {
								im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, secclass);
							}

					        MarkerManager.singleton().updateMarker(p);
						}

					} catch (CoreException e) {
						NJSecPlugin.singleton().showError("Problem while editing NJSec Annotation Marker", null, e);
					}
				}
			}
		};
		editClasses.setText("Edit the Security-Classes");
		editClasses.setToolTipText("Edits the Classes for the selected Marker.");
		editClasses.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_CUT));

		addPair = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				List<?> objs = ((IStructuredSelection)selection).toList();

				for (int i = 0; i < objs.size(); i++) {
					IMarker im = (IMarker) objs.get(i);

					//marker ueberpruefen
					try {
						if (!NJSecMarkerConstants.MARKER_TYPE_REDEFINE.equals(im.getType())) {
							continue;
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}

					//Dialog erstellen
					DoubleLatticeDialog dlg = new DoubleLatticeDialog(NJSecPlugin.singleton().getShell());
				    IProject p = NJSecPlugin.singleton().getActiveProject();
			        dlg.setLattice(openLattice(p));
			        dlg.setAddCancelButton(true);
			        dlg.setMessage1("Select Security Class Allowed To Flow Into Selected Code\n");
			        dlg.setMessage2("Select Security Class For Information Flowing Out Of Selected Code\n");
			        dlg.setTitle("Select Security Class to add");
			        dlg.open();

			        //Dialog auswerten
			        Object[] secclasses = dlg.getResult();
			        if (secclasses == null || secclasses.length < 2) return;
			        String secclass = secclasses[0].toString();
			        if (secclass == null) return;
			        String secclassout = secclasses[1].toString();
			        if (secclassout == null) return;

			        try {
						NJSecPlugin.singleton().getMarkerFactory().addPairToRedefiningMarker(im, secclass, secclassout);
				        MarkerManager.singleton().updateMarker(p);
//				        System.out.println(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, ""));
//				        System.out.println(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, ""));

			        } catch (CoreException e) {
						NJSecPlugin.singleton().showError("Problem while adding pair", null, e);
					}
				}
			}
		};
		addPair.setText("Add a pair of classes to the redefining marker");
		addPair.setToolTipText("");
		addPair.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD));

		removePair = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				List<?> objs = ((IStructuredSelection)selection).toList();
				IProject p = NJSecPlugin.singleton().getActiveProject();

				for (int i = 0; i < objs.size(); i++) {
					IMarker im = (IMarker) objs.get(i);

					//marker ueberpruefen
					try {
						if (!NJSecMarkerConstants.MARKER_TYPE_REDEFINE.equals(im.getType())
								|| NJSecPlugin.singleton().getMarkerFactory().getNumberOfDeclassRules(im) < 2) {
								NJSecPlugin.singleton().showError("Can't remove if Marker has only one pair!", null, null);
							return;
						}
					} catch (CoreException e) {
						e.printStackTrace();
					}

//					//Dialog erstellen
//					DoubleLatticeDialog dlg = new DoubleLatticeDialog(NJSecPlugin.singleton().getShell());
//				    dlg.setLattice(openLattice(p));
//			        dlg.setAddCancelButton(true);
//			        dlg.setMessage1("Select Security Class Allowed To Flow Into Selected Code\n");
//			        dlg.setMessage2("Select Security Class For Information Flowing Out Of Selected Code\n");
//			        dlg.setTitle("Select Security Classpair to remove");
//			        dlg.open();

					LatticeListDialog dlg = new LatticeListDialog(NJSecPlugin.singleton().getShell());
					dlg.setLattice(openLattice(p));
					dlg.setAddCancelButton(true);
					dlg.setMessage("Select SecurityClass pair to remove.\n");
					dlg.setTitle("Select Security Classpair to remove");
					String[] input = im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "").split(";");
					String[] input2 = im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "").split(";");
					for (int i1 = 0; i1 < input.length; ++i1) {
						input[i1] += " - " + input2[i1];
					}
					dlg.setInput(input);
					dlg.open();

//			        //Dialog auswerten
//			        Object[] secclasses = dlg.getResult();
//			        if (secclasses == null || secclasses.length < 2) return;
//			        String secclass = secclasses[0].toString();
//			        if (secclass == null) return;
//			        String secclassout = secclasses[1].toString();
//			        if (secclassout == null) return;

					Object[] result = dlg.getResult();
					if (result == null) return;
					String[] secclasses = ((String) result[0]).split(" - ");
					if (secclasses.length != 2) return;
					String secclass = secclasses[1];
					String secclassout = secclasses[0];

					try {
						NJSecPlugin.singleton().getMarkerFactory().removePairFromRedefiningMarker(im, secclass, secclassout);
				        MarkerManager.singleton().updateMarker(p);
//				        System.out.println(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, ""));
//				        System.out.println(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, ""));

					} catch(CoreException ce) {
						NJSecPlugin.singleton().showError("Problem while removing pair", null, ce);
					}
				}
			}
		};
		removePair.setText("Remove a pair of classes from the redefining marker");
		removePair.setToolTipText("");
		removePair.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_REMOVE));

		toggleActive = new Action() {
			public void run() {
				IMarker im = getSelectedMarker();
				if (im == null) {
					return;
				}
				boolean active = im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true);
				active = !active;

				try {
					im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, active);

				} catch (CoreException e) {
					NJSecPlugin.singleton().showError("Problem while changing active status", null, e);
				}

				if (active) {
					setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("deactivated"));

				} else {
					setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("activated"));
				}

				IProject p = NJSecPlugin.singleton().getActiveProject();

				try {
					MarkerManager.singleton().updateMarker(p);

				} catch (CoreException e) {
					e.printStackTrace();
				}

				viewer.refresh();
			}
		};
		toggleActive.setText("Activates or deactivates selected marker");
		toggleActive.setToolTipText("");
		toggleActive.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("activated"));
	}

	private IMarker getSelectedMarker() {
		ISelection selection = viewer.getSelection();
		Object obj = ((IStructuredSelection)selection).getFirstElement();

		if (obj instanceof IMarker) {
			return (IMarker) obj;
		}

		return null;
	}

	private void hookSelectionAction() {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				selectionAction.run();
			}
		});
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	public synchronized void select (IMarker marker) {
		StructuredSelection sel = new StructuredSelection(marker);
		viewer.setSelection(sel, true);
	}

	public void markersChanged(IProject project) {
		if (project == null) {
			String[] t = {"No Editor Selected!"};
			av.setContent(t);
			return;
		}

		//show always the markers of the whole project
		IMarker[] annmarkers = null;
		IMarker[] redmarkers = null;
		IMarker[] outmarkers = null;
		IMarker[] amarkers = null;

		try {
			annmarkers = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(project, NJSecMarkerConstants.MARKER_TYPE_INPUT);
			redmarkers = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(project, NJSecMarkerConstants.MARKER_TYPE_REDEFINE);
			outmarkers = NJSecPlugin.singleton().getMarkerFactory().findNJSecMarkers(project, NJSecMarkerConstants.MARKER_TYPE_OUTPUT);
			amarkers = new IMarker[annmarkers.length + redmarkers.length + outmarkers.length];

			System.arraycopy(annmarkers, 0, amarkers, 0, annmarkers.length);
			System.arraycopy(redmarkers, 0, amarkers, annmarkers.length, redmarkers.length);
			System.arraycopy(outmarkers, 0, amarkers, annmarkers.length+redmarkers.length, outmarkers.length);

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError("Problem while finding NJSec Annotation Markers", null, e);
		}

		av.setContent(amarkers);

		// notify MarkerSelectionListeners
		MarkerSelectionBroker.getInstance().notifyListeners(getSelectedMarker());
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.gui.ActiveResourceChangeListener#activeResourceChanged(org.eclipse.core.resources.IResource, org.eclipse.core.resources.IProject)
	 */
	public void activeResourceChanged(IResource activeResource, IProject activeProject) {
		markersChanged(activeProject);
	}

    private IEditableLattice<String> openLattice(IProject ip) {
        IEditableLattice<String> lattice = null;

        try {
            ConfigReader cr = new ConfigReader(LaunchConfigurationTools.getStandardLaunchConfiguration(ip));
            String loc = cr.getLatticeLocation();
            if (loc == null) throw new ConfigurationException("No Lattice available. Maybe no NJSec-Standard-Launch-Configuration available?");

            File latFile = new File(loc);
            if (!latFile.exists()) throw new ConfigurationException("No Lattice available. Maybe no NJSec-Standard-Launch-Configuration available?");

            lattice = LatticeUtil.loadLattice(new FileInputStream(latFile));

        } catch (CoreException ce) {
            NJSecPlugin.singleton().showError("Problem getting Lattice-File-Location", null, ce);

        } catch (IOException e1) {
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Couldn't read Projects Lattice", e1);
            NJSecPlugin.singleton().showError("Couldn't read Projects Lattice (defined in \"Project Properties -> NJSec\")", status, e1);

        } catch (WrongLatticeDefinitionException e) {
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, "Invalid Lattice Definition", e);
            NJSecPlugin.singleton().showError("Invalid Lattice Definition", status, e);

        } catch (ConfigurationException e) {
            IStatus status= new Status(IStatus.ERROR, NJSecPlugin.singleton().getSymbolicName(), 0, e.getMessage(), e);
            NJSecPlugin.singleton().showError(e.getMessage(), status, e);
        }

        return lattice;
    }

    @SuppressWarnings("unchecked")
	public Collection<IMarker> getSelectedMarkers() {
		ISelection selection = viewer.getSelection();
		List<IMarker> result = ((IStructuredSelection)selection).toList();
    	return result;
    }

    public Collection<IMarker> getInversedSelection() {
    	Collection<IMarker> all = getAllMarkers();
    	Collection<IMarker> selected = getSelectedMarkers();
    	all.removeAll(selected);
    	return all;
    }

    public Collection<IMarker> getAllMarkers() {
    	Object[] all = vcp.content;
    	LinkedList<IMarker> l = new LinkedList<IMarker>();
    	for (Object o : all) {
    		l.add(((IMarker)o));
    	}
    	return l;
    }
}
