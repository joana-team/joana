/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ui.ifc.sdg.gui.CAction;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.marker.MarkerManager;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerConstants;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerListener;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.SDGFactory;
import edu.kit.joana.ui.ifc.sdg.gui.sdgworks.SecurityNodeRater;
import edu.kit.joana.ui.ifc.sdg.gui.views.mapping.MarkerSelectionBroker;
import edu.kit.joana.ui.ifc.sdg.gui.views.mapping.MarkerSelectionListener;
import edu.kit.joana.ui.ifc.sdg.textual.highlight.graphviewer.ShowGraphViewer;

/**
 *
 */
public class MarkerSecurityNodeView extends ViewPart implements MarkerSelectionListener, NJSecMarkerListener {
	private TableViewer fViewer;
	private Action doubleClickAction;
	private ViewContentProvider vcp;
	private ViewLabelProvider vlp;
	private NameSorter ns;
	private SDG g;
	private HashMap<SecurityNode, Integer> rating;

	private IMarker im;
	private IMarker last;
	private Set<String> mapping = new HashSet<String>();
	private Set<String> completeMapping = new HashSet<String>();

	private SecurityNodeRater rater = new SecurityNodeRater();
	private Spinner showRow;
	private Spinner showHysteresis;
	private Button showAllButton;
	private Button showGraphButton;
	private Button showMethodButton;

	class ViewContentProvider implements IStructuredContentProvider {
		Object[] content;

		public ViewContentProvider() {
			String[] as = {"No Marker Selected!"};
			content = as;
			rating = new HashMap<SecurityNode, Integer>();
		}

		public void inputChanged(Viewer v, Object oldInput, Object newInput) {}

		public void dispose() {}

		public Object[] getElements(Object parent) {
			return content;
		}

		public void setContent(Object[] lines) {
			content = lines;
			ns.sortByColumn(1,0);
			fViewer.refresh();
			Resizer.resize(fViewer.getTable(), new int[] {30,5,5,5,5,5,5,5,5,5});
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

			SecurityNode m = (SecurityNode) obj;

			//rating,id,source,row,label,kind
			if (index==0) {
				String id = ""+m.getId();
				int status = mappingStatus(id);

				if (status == MAPPED_HERE) {
					return "X";   // gehoert zu diesem marker

				} else if (status == OTHER_MAPPED) {
					return "";    // gehoert zu einem anderen marker

				} else {
					return "O";   // unmarkiert
				}

			} else if (index==1) {
				return "" + rating.get(m);

			} else if (index==2) {
				return "" + m.getId();

			} else if (index==3) {
				return m.getOperation().toString();

			} else if (index==4) {
				return m.getLabel();

			} else if (index==5) {
				return m.getSource();

			} else if (index==6) {
				return "" + m.getSr();

			} else if (index==7) {
				return m.getKind().toString();

			} else if (index==8) {
				return m.getType();

			} else if (index==9) {
				return miniconv(m.getProc());
			}

			return getText(obj);
		}

		public Comparable<?> getColumnComp(Object obj, int index) {
			if (obj instanceof String) return (String) obj;

			SecurityNode m = (SecurityNode) obj;

			if (index == 1) {
				return rating.get(m);

			} else if (index == 2) {
				return m.getId();
			}

			return getColumnText(obj, index);
		}

		public String miniconv(int i) {
			if (i == -1) {
				return "n/a";
			} else {
				return "" + i;
			}
		}

		public Image getColumnImage(Object obj, int index) {
			return getImage(obj);
		}

		public Image getImage(Object obj) {
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

		public void sortByColumn(int column, int asc) {
			if (asc==1) multi = 1; else multi=-1;
			col = column;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public int compare(Viewer viewer, Object e1, Object e2) {
			Comparable s1 = vlp.getColumnComp(e1, col);
			Comparable s2 = vlp.getColumnComp(e2, col);

			if (s1 == null) return -1;
			if (s2 == null) return 1;

			return s1.compareTo(s2) * multi;
		}
	}



	/**
	 * The constructor.
	 */
	public MarkerSecurityNodeView() { }

	public void dispose() {
		MarkerSelectionBroker.getInstance().removeListener(this);
		MarkerManager.singleton().removeListener(this);
	}

	public void setContent(Object[] newc) {
		if (vcp != null && newc != null) {
			vcp.setContent(newc);
		}

		if (fViewer != null) {
			fViewer.refresh();
		}
	}

	protected void createColumn(TableViewer tviewer, int style, int column, String header, int width) {
		TableColumn c = new TableColumn(tviewer.getTable(), style, column);
		c.setText(header);
		c.setWidth(width);

		class MySelectionAdapter extends SelectionAdapter{
			int column;
			TableViewer viewer;
			public void widgetSelected(SelectionEvent e) {
				ns.sortByColumn(column);
				viewer.refresh();
			}
			public void setColumn(int column) {
				this.column = column;
			}
			public void setViewer(TableViewer viewer) {
				this.viewer = viewer;
			}
		};

		MySelectionAdapter sa = new MySelectionAdapter();
		sa.setColumn(column);
		sa.setViewer(tviewer);
		c.addSelectionListener(sa);
	}

	/**
	 * This is a callback that will allow us
	 * to create the fViewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		Composite maingroup = new Composite(parent, SWT.FILL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.verticalSpacing = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		maingroup.setLayout(layout);

		Composite widgetgroup = new Composite(maingroup, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 10;
		widgetgroup.setLayout(layout);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
		gd.horizontalSpan = 1;
		gd.heightHint = 24;
		widgetgroup.setLayoutData(gd);

		setShowGraphButton(new Button(widgetgroup, SWT.NONE));
		getShowGraphButton().setText("Show Graph");
		getShowGraphButton().addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent e) {
				final String graph = NJSecPlugin.singleton().getSDGFactory().getSDGPath(g);
				ISelection selection = fViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (!(obj instanceof SecurityNode) || graph == null) {
					NJSecPlugin.singleton().showError("You have to select a node.", null, null);
					return;
				}
				final int n = ((SecurityNode) obj).getId();

				ShowGraphViewer.showGraphViewer(graph, n);
			}

			public void widgetDefaultSelected(SelectionEvent e) { }

		});
		setShowAllButton(new Button(widgetgroup, SWT.NONE));
		getShowAllButton().setText("Show *all* SecureNodes");
		getShowAllButton().addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				activateFor(0, getShowHysteresis().getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) { }
		});
		setShowMethodButton(new Button(widgetgroup, SWT.NONE));
		getShowMethodButton().setText("Show SecureNodes of Method");
		getShowMethodButton().addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				activateForMethod(im);
			}

			public void widgetDefaultSelected(SelectionEvent e) { }
		});

		new Label(widgetgroup, SWT.NONE).setText("Line:");

		setShowRow(new Spinner(widgetgroup, SWT.SINGLE | SWT.BORDER));
		gd = new GridData();
		gd.widthHint = 50;
		getShowRow().setLayoutData(gd);
		getShowRow().setMaximum(Integer.MAX_VALUE);

		getShowRow().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try {
					activateFor(getShowRow().getSelection(), getShowHysteresis().getSelection());
				} catch(NumberFormatException nfe){}
			}
		});
		new Label(widgetgroup, SWT.NONE).setText("Radius:");

		setShowHysteresis(new Spinner(widgetgroup, SWT.SINGLE | SWT.BORDER));
		gd = new GridData();
		gd.widthHint = 50;
		getShowHysteresis().setLayoutData(gd);
		getShowHysteresis().setMaximum(Integer.MAX_VALUE);
		getShowHysteresis().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				try {
					activateFor(getShowRow().getSelection(), getShowHysteresis().getSelection());
				} catch(NumberFormatException nfe){}
			}
		});

		Composite viewergroup = new Composite(maingroup, SWT.NONE);

		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 1;
		gd.grabExcessHorizontalSpace = true;
		gd.grabExcessVerticalSpace = true;
		viewergroup.setLayoutData(gd);
		layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		viewergroup.setLayout(layout);
		fViewer = new TableViewer(viewergroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 1;
		viewergroup.setLayoutData(gd);
		fViewer.getControl().setLayoutData(gd);

		createColumn(fViewer, SWT.LEFT, 0, "Sel", 50);
		createColumn(fViewer, SWT.LEFT, 1, "Rating", 50);
		createColumn(fViewer, SWT.LEFT, 2, "ID", 50);
		createColumn(fViewer, SWT.LEFT, 3, "Operation", 50);
		createColumn(fViewer, SWT.LEFT, 4, "Label", 300);
		createColumn(fViewer, SWT.LEFT, 5, "Source", 100);
		createColumn(fViewer, SWT.RIGHT, 6, "Row", 50);
		createColumn(fViewer, SWT.RIGHT, 7, "Kind", 50);
		createColumn(fViewer, SWT.RIGHT, 8, "Type", 50);
		createColumn(fViewer, SWT.RIGHT, 9, "Proc", 50);

		fViewer.getTable().setHeaderVisible(true);
		fViewer.getTable().setLinesVisible(true);
		vcp = new ViewContentProvider();
		fViewer.setContentProvider(vcp);
		fViewer.setLabelProvider(vlp=new ViewLabelProvider());
		fViewer.setSorter(ns = new NameSorter(0));
		fViewer.setInput(getViewSite());
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();

		MarkerSelectionBroker.getInstance().addListener(this);
		MarkerManager.singleton().addListener(this);
	}

	private Button getShowAllButton() {
		return this.showAllButton;
	}

	private void setShowAllButton(Button button) {
		this.showAllButton = button;
	}

	private Button getShowGraphButton() {
		return this.showGraphButton;
	}

	private Button getShowMethodButton() {
		return this.showMethodButton;
	}

	private void setShowMethodButton(Button button) {
		this.showMethodButton = button;
	}

	private void setShowGraphButton(Button button) {
		this.showGraphButton = button;
	}

	private Spinner getShowRow() {
		return this.showRow ;
	}

	private void setShowRow(Spinner spinner) {
		this.showRow = spinner;
	}

	private Spinner getShowHysteresis() {
		return this.showHysteresis ;
	}

	private void setShowHysteresis(Spinner spinner) {
		this.showHysteresis = spinner;
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				MarkerSecurityNodeView.this.fillContextMenu(manager);
			}
		});

		Menu menu = menuMgr.createContextMenu(fViewer.getControl());
		fViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, fViewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
//		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

//	private void fillLocalPullDown(IMenuManager manager) {}

	private void fillContextMenu(IMenuManager manager) {
		// Other plug-ins can contribute their actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager) {
		Action loadSdg = new Action() {
			public void run() {
				FileDialog dialog = new FileDialog(NJSecPlugin.singleton().getActiveShell(), SWT.OPEN);
				dialog.setFilterExtensions(new String[] {"*.pdg", "*"});
				dialog.setText("Load SDG from file");
				String file = dialog.open();
				if (file == null) return;

		        SDGFactory sf = NJSecPlugin.singleton().getSDGFactory();
	            // read it from a file
	            try {
	            	IPath ws = ResourcesPlugin.getWorkspace().getRoot().getRawLocation();
	            	IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

	            	IProject project = null;
	            	for (int i = 0; i < projects.length; i++) {
	            		IProject cur = projects[i];
	            		IPath projectPath = ws.append(cur.getFullPath());
	            		String osPath = projectPath.toOSString();
	            		if (file.startsWith(osPath)) {
	            			project = cur;
	            			break;
	            		}
	            	}

	            	if (project != null) {
	            		if (im == null || im.getResource().getProject() == project) {
	            			g = sf.loadSDG(file, project);
	            			activateFor(im);
	            		} else {
			            	NJSecPlugin.singleton().showError("Mismatch: Current annotations are for project "
			            			+ im.getResource().getProject()
			            			+ " and SDG is of project " + project,
			            			null, new NullPointerException());
            			}
	            	} else {
	            		NJSecPlugin.singleton().showError("Couldn't find matching IProject for SDG " + file,
		            			null, new NullPointerException());
	            	}
	            } catch (Exception e) {
	            	NJSecPlugin.singleton().showError("Couldn't read/parse SDG File "+ file, null, e);
				}
			}
		};

		loadSdg.setText("Load SDG from file");
		loadSdg.setToolTipText("Load SDG from file");
		loadSdg.setImageDescriptor(NJSecPlugin.singleton().getImageRegistry().getDescriptor("open"));

		manager.add(loadSdg);
	}

	private void makeActions() {
		doubleClickAction = new CAction() {
			public void run() {
				ISelection selection = fViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof SecurityNode) {
					SecurityNode node = (SecurityNode) obj;

					String id = "" + node.getId();
					int status = mappingStatus(id);
					if (status == MAPPED_HERE) {
						// remove node from mapping
						mapping.remove(id);
						completeMapping.remove(id);

					} else if (status == OTHER_MAPPED) {
						// do nothing

					} else {
						// add node to mapping
						mapping.add(id);
						completeMapping.add(id);
					}

					// update marker
					updateMarker();
				}
			}
		};
		((CAction)doubleClickAction).setContext(this);
	}

	private void  updateMarker() {
		String nodes = "";
		for (String str : mapping) {
			nodes += str + ";";
		}
		try {
			im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, nodes);
			// update annotation views
			MarkerManager.singleton().updateMarker(im.getResource().getProject());

		} catch (CoreException e) {
		    NJSecPlugin.singleton().showError("Problem while adding an SDG-Node to a Marker", null, e);
		}
		fViewer.refresh();
	}

	public void setDoubleClickCAction(Action ca) {
		doubleClickAction = ca;
	}

	private void hookDoubleClickAction() {
		fViewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the fViewer's control.
	 */
	public void setFocus() {
		fViewer.getControl().setFocus();
	}

	private void activateForMethod(IMarker im) {
		int line = im.getAttribute(IMarker.LINE_NUMBER, -1);

		ArrayList<SecurityNode> candidates = NJSecPlugin.singleton().getSDGFactory().
        findSecurityNodes(line, 0, im.getResource().getProjectRelativePath().toString(), g);

		LinkedList<Integer> procs = new LinkedList<Integer>();
		for (SecurityNode sn : candidates) {
			if (!procs.contains(sn.getProc())) {
				procs.add(sn.getProc());
			}
		}

		candidates = (NJSecPlugin.singleton().getSDGFactory().findSecurityNodesProc(procs, im.getResource().getProjectRelativePath().toString(), g));

		rating = rater.getRating(im, candidates);
		setContent(candidates.toArray());
	}

	private void activateFor(int line, int width) {
		ArrayList<SecurityNode> candidates = NJSecPlugin.singleton().getSDGFactory().
		        findSecurityNodes(line, width, im.getResource().getProjectRelativePath().toString(), g);

		rating = rater.getRating(im, candidates);
		setContent(candidates.toArray());

		if (getShowHysteresis().getSelection() != width) getShowHysteresis().setSelection(width);
		if (getShowRow().getSelection() != line) getShowRow().setSelection(line);
	}

	private void activateFor(IMarker im) {
		rater = new SecurityNodeRater();
		this.im = im;

		// No Marker selected: set values to null and return
		if (im == null) {
			this.mapping.clear();
			this.g = null;
			setContent(new String[]{"No Marker Selected!"});
			return;
		}

		if (last != null && im.equals(last)) {
			return;
		}
		last = im;

		// retrieve the SDG
		g = NJSecPlugin.singleton().getSDGFactory().getCachedSDG(im.getResource().getProject());

		// if the SDG has not been loaded yet, set values to null and return
		if (g == null) {
			this.mapping.clear();
			setContent(new String[]{"No SDG Available! (Launch-Configuration existent?)"});
			return;
		}

		int radius = 0;
		int line = im.getAttribute(IMarker.LINE_NUMBER, -1);

		recomputeCompleteMapping(im.getResource().getProject());

		this.mapping.clear();
		String[] nodes = im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, "").split(";");
		for (String node : nodes) {
			if (!"".equals(node))
				mapping.add(node);
		}

		try {
			radius = this.getShowHysteresis().getSelection();

		} catch (NumberFormatException nfe) {}

		activateFor(line, radius);
	}

	public void markerSelectionChanged(IMarker marker) {
		activateFor(marker);
	}

	public void markersChanged(IProject p) {
		markerSelectionChanged(im);
	}


	/* SDGNode mapping */

	private static final int MAPPED_HERE = 1;
	private static final int OTHER_MAPPED = 2;
	private static final int FREE = 3;

	private int mappingStatus(String id) {
		if (completeMapping.contains(id)) {
			// knoten ist ausgewaehlt: gehoert er zu diesem marker?
			if (mapping.contains(id)) {
				return MAPPED_HERE;  // gehoert zu diesem marker

			} else {
				return OTHER_MAPPED; // gehoert zu einem anderen marker
			}

		} else {
			return FREE;       // unmarkiert
		}
	}

	private void recomputeCompleteMapping(IProject p) {
		completeMapping.clear();
		try {
			IMarker[] marker = p.findMarkers(NJSecMarkerConstants.MARKER_TYPE_NJSEC, true, IResource.DEPTH_INFINITE);

			for (IMarker m : marker) {
				String[] nodes = m.getAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, "").split(";");
				for (String n : nodes) {
					if (!"".equals(n)) {
						completeMapping.add(n);
					}
				}
			}

		} catch(CoreException ex) {
			ex.printStackTrace();
		}
	}
}
