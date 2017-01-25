/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

/**
 * A viewer for SDGNodes. Displays a SDGNode with its neighbors
 * and provides basic navigation functions.
 * @author kai brueckner, university of passau
 */

package edu.kit.joana.ui.ifc.sdg.viewer.view.sdgview;

import java.util.ArrayList;
import java.util.Stack;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview.AnalysisView;

public class SDGView extends ViewPart implements ISelectionListener, ISelectionChangedListener {
    private boolean attached;
    private AnalysisView graphs;

    private Graph graph;
    private SDGNode selection;
    private boolean changed;

    private ViewContentProvider vcp;
    private TreeViewer viewer;
    private DrillDownAdapter drillDownAdapter;
    private Action doubleClickAction;
	private Spinner showRow;

    class DrillAdapter extends DrillDownAdapter {

		Stack<SDGNode> drillStack = new Stack<SDGNode>();

    	public DrillAdapter(TreeViewer tree) {
			super(tree);
		}

		@Override
		public boolean canGoBack() {
			return !drillStack.isEmpty();
		}
		@Override
		public boolean canGoHome() {
			return canGoBack();
		}
		@Override
	    public boolean canGoInto() {
	        IStructuredSelection oSelection = (IStructuredSelection) viewer
	                .getSelection();
	        if (oSelection == null || oSelection.size() != 1) {
				return false;
			}
	        Object anElement = oSelection.getFirstElement();
	        return anElement instanceof NodeObject && vcp.invisibleRoot.getChildren()[0] != anElement;
	    }
	    @Override
	    public void goInto(Object newInput) {
          SDGNode jumpTo = ((NodeObject)newInput).getSDGNode();
	    	if (jumpTo != selection) {
                drillStack.add(selection);

	            // Install the new state.
	            selection = jumpTo;
                vcp.initialize();
                viewer.refresh();
	            updateNavigationButtons();
            }
	    }
	    @Override
	    public void goBack() {
	        Object currentInput = viewer.getInput();
	        selection = drillStack.pop();
	        // if there was a selection, it should have been preserved,
	        // but if not, select the element that was drilled into
	        if (viewer.getSelection().isEmpty()) {
				viewer
	                    .setSelection(new StructuredSelection(currentInput), true);
			}
            vcp.initialize();
            viewer.refresh();
            updateNavigationButtons();
	    }
	    @Override
	    public void goHome() {
	        Object currentInput = viewer.getInput();
	        selection = drillStack.firstElement();
	        drillStack.clear();
	        // if there was a selection, it should have been preserved,
	        // but if not, select the element that was drilled into
	        if (viewer.getSelection().isEmpty()) {
				viewer
	                    .setSelection(new StructuredSelection(currentInput), true);
			}
            vcp.initialize();
            viewer.refresh();
            updateNavigationButtons();
	    }
	    @Override
	    public void reset() {
	    	drillStack.clear();
	    	super.reset();
	    }
    }

    static abstract class SuperObject implements IAdaptable {
        private SuperObject parent;
        private ArrayList<SuperObject> children = new ArrayList<SuperObject>();

        public void setParent(SuperObject n) {
            this.parent = n;
        }
        public SuperObject getParent() {
            return parent;
        }
        public void addChild(SuperObject child) {
            children.add(child);
            child.setParent(this);
        }
        public void removeChild(SuperObject child) {
            children.remove(child);
            child.setParent(null);
        }
        public SuperObject[] getChildren() {
            return children.toArray(new SuperObject[children.size()]);
        }
        public boolean hasChildren() {
            return children.size()>0;
        }
        public String toString() {
            return getName();
        }
		@SuppressWarnings("rawtypes")
		public Object getAdapter(Class key) {
            return null;
        }
        public abstract String getName();
    }

    /**
     * The Object for the distinction of incoming and outgoing edges.
     */
    static class TypeObject extends SuperObject {
        private String label;

        public TypeObject(String s) {
            this.label = s;
        }
        public String getName() {
            return label;
        }
    }

    /**
     * The object for displaying the different <code>SDGNodes</code>
     */
    static class NodeObject extends SuperObject {
        private SDGNode node;

        public NodeObject(SDGNode n) {
            this.node = n;
        }
        public SDGNode getSDGNode() {
            return node;
        }
        public String getName() {
            if (node==null) return "";
            return (node.getId() + " " + node.getKind()+ " " + node.getLabel());
        }
    }

    /**
     * The object for displaying the different edges.
     */
    static class EdgeObject extends SuperObject {
        private SDGEdge edge;
        public EdgeObject(SDGEdge e) {
            this.edge = e;
        }
        public String getName() {
            if (edge==null) return "";
            return (edge.getKind().toString());
        }
    }

    class ViewContentProvider implements IStructuredContentProvider, ITreeContentProvider {
        private EdgeObject invisibleRoot;

        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
            disposeSDGView();
        }

        public Object[] getElements(Object parent) {
            if (parent.equals(getViewSite())) {
                initialize();
                return getChildren(invisibleRoot);
            }
            return getChildren(parent);
        }
        public Object getParent(Object child) {
        	return ((SuperObject)child).getParent();
        }
        public Object[] getChildren(Object parent) {
        	if (parent == null) return new Object[0];
        	return ((SuperObject)parent).getChildren();
        }
        public boolean hasChildren(Object parent) {
        	return ((SuperObject)parent).hasChildren();
        }


        /**
         * This function initializes the tree viewer.
         * It is called on every selection change.
         */
        private void initialize() {
            if (changed && graph != null){
                if (selection == null) {
                    selection = graph.getGraph().getNode(1);
                }

                NodeObject root = new NodeObject(selection);
                TypeObject out = new TypeObject("outgoing");
                root.addChild(out);
                for (SDGEdge e : graph.getGraph().outgoingEdgesOf(selection)) {
                    NodeObject nodeToAdd = new NodeObject(e.getTarget());
                    EdgeObject eo = new EdgeObject(e);
                    SuperObject edgeToAdd = getSuperObject(eo, out.getChildren());
                    edgeToAdd.addChild(nodeToAdd);
                    if (eo == edgeToAdd)
                        out.addChild(edgeToAdd);
                }
                TypeObject in = new TypeObject("incoming");
                root.addChild(in);
                for (SDGEdge e : graph.getGraph().incomingEdgesOf(selection)) {
                    NodeObject nodeToAdd = new NodeObject(e.getSource());
                    EdgeObject eo = new EdgeObject(e);
                    SuperObject edgeToAdd = getSuperObject(eo, in.getChildren());
                    edgeToAdd.addChild(nodeToAdd);
                    if (eo == edgeToAdd)
                        in.addChild(edgeToAdd);
                }
                TypeObject parent = new TypeObject("parent");
                root.addChild(parent);
                NodeObject nodeToAdd = new NodeObject(graph.getGraph().getEntry(selection));
	            parent.addChild(nodeToAdd);
	            if (selection.getKind() == SDGNode.Kind.ACTUAL_IN ||
	            		selection.getKind() == SDGNode.Kind.ACTUAL_OUT) {
	                nodeToAdd = new NodeObject(graph.getGraph().getCallSiteFor(selection));
		            parent.addChild(nodeToAdd);
	            }
                invisibleRoot = new EdgeObject(null);
                invisibleRoot.addChild(root);
            }
        }

        private SuperObject getSuperObject(SuperObject e, SuperObject[] c) {
            for (SuperObject eo : c) {
                if(eo.getName().equals(e.getName()))
                    return eo;
            }
            return e;
        }

    }

    static class ViewLabelProvider extends LabelProvider {

        public String getText(Object obj) {
            return obj.toString();
        }

        public Image getImage(Object obj) {
            String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
            if (obj instanceof EdgeObject)
                imageKey = ISharedImages.IMG_OBJ_FOLDER;
            return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
        }
    }

    /**
     * The constructor.
     */
    public SDGView() {
        attached = false;
    }

    private void attach() {
        graphs = AnalysisView.getInstance();
        if (graphs != null) {
            graphs.attachSelectionChangedListener(this);
            attached = true;
        }
    }

    private void disposeSDGView() {
        if (graphs != null) {
            graphs.detachSelectionChangedListener(this);
            attached = true;
        }
    }

	private Spinner getShowRow() {
		return this.showRow ;
	}

	private void setShowRow(Spinner spinner) {
		this.showRow = spinner;
	}

    /**
     * This is a callback that allows
     * to create the viewer and initialize it.
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

		new Label(widgetgroup, SWT.NONE).setText("Root:");

		setShowRow(new Spinner(widgetgroup, SWT.SINGLE | SWT.BORDER));
		gd = new GridData();
		gd.widthHint = 50;
		getShowRow().setLayoutData(gd);
		getShowRow().setMinimum(1);
		getShowRow().setMaximum(Integer.MAX_VALUE);

		getShowRow().addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				drillDownAdapter.reset();
                selection = graph.getGraph().getNode(showRow.getSelection());
	            vcp.initialize();
	            viewer.refresh();
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
        viewer = new TreeViewer(viewergroup, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 1;
		viewergroup.setLayoutData(gd);
		viewer.getControl().setLayoutData(gd);

        drillDownAdapter = new DrillAdapter(viewer);
        vcp = new ViewContentProvider();
        viewer.setContentProvider(vcp);
        viewer.setLabelProvider(new ViewLabelProvider());
        viewer.setSorter(new ViewerSorter());
        viewer.setInput(getViewSite());
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
                SDGView.this.fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
//        fillLocalPullDown(bars.getMenuManager());
        fillLocalToolBar(bars.getToolBarManager());
    }

//    private void fillLocalPullDown(IMenuManager manager) {
//    }

    private void fillContextMenu(IMenuManager manager) {
        manager.add(new Separator());
        drillDownAdapter.addNavigationActions(manager);
        // Other plug-ins can contribute there actions here
        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        drillDownAdapter.addNavigationActions(manager);
    }



    /**
     * Adding a double click action. The selected node is used as source
     * and a new tree is built according to the selection.
     */
    private void makeActions() {
        doubleClickAction = new Action() {
            public void run() {
                ISelection selection = viewer.getSelection();
                Object obj = ((IStructuredSelection)selection).getFirstElement();
                if (obj instanceof NodeObject) {
                   	drillDownAdapter.goInto(obj);
                    /*IMarker jumpMarker;
                    if(jumpTo.getSource() != null){
                        if (SDGUtilities.findProject(jumpTo.getSource())!=null) {
                            try {
                                if(jumpTo.getSr()!=-1) {
                                    IProject p = SDGUtilities.findProject(jumpTo.getSource());
                                    if (p!=null) {
                                        IResource r = p.getFile(jumpTo.getSource());
                                        jumpMarker = r.createMarker("org.eclipse.core.resources.textmarker");
                                        jumpMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(jumpTo.getSr()));
                                        IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(),jumpMarker);
                                        jumpMarker.delete();
                                    }
                                }
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            } catch (CoreException e) {
                                e.printStackTrace();
                            }
                        }
                    }*/
                }
            }
        };


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

        if (!attached) {
            attach();
        }
    }

    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection s = (IStructuredSelection) selection;
            Object o = s.getFirstElement();
            if (o instanceof SDGNode) {
                this.selection = (SDGNode) o;
                vcp.initialize();
                viewer.refresh();
            }
        }
    }

    /** Listening for changes in the AnalysisView.
     *
     */
    public void selectionChanged(SelectionChangedEvent event) {
        Graph tmp = graphs.getCurrentGraph();
        SDGNode selected = graphs.getCurrentNode();
        changed = false;

        if (tmp == graph && selected != null) {
            selection = selected;
            changed = true;
        }

        if (tmp != graph) {
            selection = selected;
            graph = tmp;
            changed = true;
        }

        viewer.refresh();
    }
}
