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
package edu.kit.joana.ui.ifc.sdg.viewer.view.analysisview;

import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ui.ifc.sdg.textual.highlight.graphviewer.ShowGraphViewer;
import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;
import edu.kit.joana.ui.ifc.sdg.viewer.Activator;
import edu.kit.joana.ui.ifc.sdg.viewer.algorithms.Algorithm;
import edu.kit.joana.ui.ifc.sdg.viewer.model.AlgorithmFactory;
import edu.kit.joana.ui.ifc.sdg.viewer.model.ChosenAlgorithm;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Criteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.CriteriaCategory;
import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationCriteria;
import edu.kit.joana.ui.ifc.sdg.viewer.model.EvaluationRun;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.viewer.model.Run;
import edu.kit.joana.ui.ifc.sdg.viewer.view.AlgorithmBrowserNeu;
import edu.kit.joana.ui.ifc.sdg.viewer.view.CompareRunDialog;
import edu.kit.joana.ui.ifc.sdg.viewer.view.CriteriaDialog;
import edu.kit.joana.ui.ifc.sdg.viewer.view.SelectCriterionDialogNeu;


/** This is the central class of the viewer plugin.
 * It contains all active SDGs and all chosen and configured algorithms.
 * All active analysis are shown in a tree.
 * This tree has the following structure:
 *
 *              Root
 *             /   \
 *           SDG   SDG                 // contains the active SDGs
 *           /
 *     Algorithms   ...                // contains the active algorithms
 *      /
 *    Runs   ...                       // contains concrete runs of the algorithms
 *
 *
 * The AnalysisView observes all active analysis runs.
 *
 * @author giffhorn
 *
 */

public class AnalysisView extends ViewPart implements Observer {

	// SINGLETON PATTERN
	private static AnalysisView singleton;

	/** Returns the AnalysisView singleton.
	 */
	public static AnalysisView getInstance() {
		if (singleton == null) {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("joana.project.slicer");
			} catch (PartInitException e) {
			}
		}
		return singleton;
	}
	// SINGLETON PATTERN

	// a tree viewer for the active SDGs
	private TreeViewer viewer;
	private DrillDownAdapter drillDownAdapter;
	private ViewContentProvider viewContent;

	// choose an algorithm
	private Action choose;
	// remove an element
	private Action remove;
	// execute an analysis
	private Action slice;
	// show the result of an analysis
	private Action showResult;
	private Action clearResult;
	// create a new run configuration
	private Action newRun;
	// create a new Evaluation run configuration
	private Action newEvalRun;
	// define double click actions
	private Action doubleClickAction;
	// compare Runs
	private Action compareRun;
	//New Algorithm with same Run
	private Action chooseRun;
	//Add Algorithm to EvalRun
	private Action addAlgo;
	//Show SDG
	private Action showGraph;
	//add criteria
	private Action addCrit;

	/**
	 * The content provider class is responsible for
	 * providing objects to the view. It can wrap
	 * existing objects in adapters or simply return
	 * objects as-is. These objects may be sensitive
	 * to the current input of the view, or ignore
	 * it and always show the same content
	 * (like Task List, for example).
	 */
	class ViewContentProvider implements IStructuredContentProvider,
										   ITreeContentProvider {
		// the invisible root
		private Root invisibleRoot;

		/** Empty inherited method.
		 */
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
		}

		/** Empty inherited method.
		 */
		public void dispose() {
		}

		/** Returns the children of a parent.
		 *
		 */
		public Object[] getElements(Object parent) {
			if (parent.equals(getViewSite())) {
				if (invisibleRoot==null) initialize();
				return getChildren(invisibleRoot);
			}
			return getChildren(parent);
		}

		/** Returns a parent of a TreeNode.
		 * If child is not a TreeNode, it returns null.
		 */
		public Object getParent(Object child) {
			if (child instanceof TreeNode) {
				return ((TreeNode)child).getParent();
			}
			return null;
		}

		/** Returns the children of a parent.
		 * If parent is not a Parent, it returns an empty Object array.
		 */
		public Object [] getChildren(Object parent) {
			if (parent instanceof Parent) {
				return ((Parent)parent).getChildren();
			}
			return new Object[0];
		}

		/** Returns true if this Object is a Parent and has children.
		 */
		public boolean hasChildren(Object parent) {
			if (parent instanceof Parent)
				return ((Parent)parent).hasChildren();
			return false;
		}

		/** Create a new Root.
		 *
		 */
		private void initialize() {
			invisibleRoot = new Root();
		}

		/** Adds a new Subtree to the Root.
		 * Creates a new SDG from the given graph and adds it to the Root.
		 * @param g  The new Graph.
		 */
		private void addNewTree(GraphView g) {
			invisibleRoot.addChild(g);
		}

		/** Removes a tree element.
		 *
		 * @param node  The tree element.
		 */
		private void remove(TreeNode node) {
			Parent p = node.getParent();
			p.removeChild(node);
		}
	}

	/** Provides icons and text for tree elements.
	 *
	 * @author giffhorn
	 *
	 */
	class ViewLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			return obj.toString();
		}

		public Image getImage(Object obj) {
			if (obj instanceof Parent) {
				if (obj instanceof EvaluationRunView && ((EvaluationRunView) obj).isExecuting()
						|| obj instanceof EvaluationAlgorithmView && ((EvaluationAlgorithmView) obj).isExecuting()) {
					return Activator.getDefault().getImageRegistry().getDescriptor("sanduhr").createImage();
				} else if (obj instanceof EvaluationRunView && ((EvaluationRunView) obj).hasExecuted()
						|| obj instanceof EvaluationAlgorithmView && ((EvaluationAlgorithmView) obj).isFinished()) {
					return Activator.getDefault().getImageRegistry().getDescriptor("haken").createImage();
				} else if (obj instanceof EvaluationRunView || obj instanceof EvaluationAlgorithmView) {
					return Activator.getDefault().getImageRegistry().getDescriptor("todo").createImage();
				} else {
					String imageKey = ISharedImages.IMG_OBJ_FOLDER;
					return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
				}
			} else {
				return Activator.getDefault().getImageRegistry().getDescriptor("criterion").createImage();
			}
		}
	}

	/** A implementation for a ViewerSorter.
	 *
	 * @author Martin Seidel
	 *
	 */
	class NameSorter extends ViewerComparator {

		@Override
		public int category(Object element) {
	        if (element instanceof EvaluationCriteriaView) { //Evaluation
	        	return 0;
	        } else if (element instanceof EvaluationAlgorithmView) {
				return 1;
			} else {
				return 0;
			}
	    }

	}

	public class PossibleActionsListener implements ISelectionChangedListener {
	    public void selectionChanged(SelectionChangedEvent event) {
	    	ISelection selection = event.getSelection();
			Object obj = ((IStructuredSelection)selection).getFirstElement();

			if (!(obj instanceof TreeNode)) {
				return;
			}

			boolean[] enabled = ((TreeNode)obj).getPossibleActions();
			int i = 0;
			remove.setEnabled(enabled[i++]);
    		choose.setEnabled(enabled[i++]);
    		newEvalRun.setEnabled(enabled[i++]);
    		chooseRun.setEnabled(enabled[i++]);
    		newRun.setEnabled(enabled[i++]);
    		slice.setEnabled(enabled[i++]);
    		showResult.setEnabled(enabled[i++]);
    		clearResult.setEnabled(enabled[i++]);
    		compareRun.setEnabled(enabled[i++]);
    		showGraph.setEnabled(enabled[i++]);
    		addAlgo.setEnabled(enabled[i++]);
    		addCrit.setEnabled(enabled[i++]);

    		/*
	    	if (obj instanceof GraphView) {
	    		remove.setEnabled(true);
	    		choose.setEnabled(true);
	    		newEvalRun.setEnabled(true);
	    		chooseRun.setEnabled(false);
	    		newRun.setEnabled(false);
	    		slice.setEnabled(false);
	    		showResult.setEnabled(false);
	    		clearResult.setEnabled(true);
	    		compareRun.setEnabled(false);
	    		showGraph.setEnabled(true);

	    	} else if (obj instanceof ChosenAlgorithmView) {
	    		remove.setEnabled(true);
	    		choose.setEnabled(false);
	    		newEvalRun.setEnabled(false);
	    		chooseRun.setEnabled(false);
	    		newRun.setEnabled(true);
	    		slice.setEnabled(false);
	    		showResult.setEnabled(false);
	    		clearResult.setEnabled(true);
	    		compareRun.setEnabled(false);
	    		showGraph.setEnabled(false);

	    	} else if (obj instanceof EvaluationRunView) {
	    		remove.setEnabled(true);
	    		choose.setEnabled(false);
	    		newEvalRun.setEnabled(false);
	    		chooseRun.setEnabled(false);
	    		newRun.setEnabled(false);
	    		slice.setEnabled(false);
	    		showResult.setEnabled(false);
	    		clearResult.setEnabled(true);
	    		compareRun.setEnabled(false);
	    		showGraph.setEnabled(false);

	    	} else if (obj instanceof RunView) {
	    		remove.setEnabled(true);
	    		choose.setEnabled(false);
	    		newEvalRun.setEnabled(false);
	    		chooseRun.setEnabled(true);
	    		newRun.setEnabled(false);
	    		slice.setEnabled(true);
	    		showResult.setEnabled(true);
	    		clearResult.setEnabled(true);
	    		compareRun.setEnabled(true);
	    		showGraph.setEnabled(false);

	    	} else if (obj instanceof CriteriaCategoryView) {
	    		remove.setEnabled(false);
	    		choose.setEnabled(false);
	    		newEvalRun.setEnabled(false);
	    		chooseRun.setEnabled(false);
	    		newRun.setEnabled(false);
	    		slice.setEnabled(false);
	    		showResult.setEnabled(false);
	    		clearResult.setEnabled(false);
	    		compareRun.setEnabled(false);
	    		showGraph.setEnabled(false);

	    	} else if (obj instanceof CriteriaView) {
	    		remove.setEnabled(false);
	    		choose.setEnabled(false);
	    		newEvalRun.setEnabled(false);
	    		chooseRun.setEnabled(false);
	    		newRun.setEnabled(false);
	    		slice.setEnabled(false);
	    		showResult.setEnabled(false);
	    		clearResult.setEnabled(false);
	    		compareRun.setEnabled(false);
	    		showGraph.setEnabled(false);

	    	} else if (obj instanceof NodeView) {
	    		remove.setEnabled(true);
	    		choose.setEnabled(false);
	    		newEvalRun.setEnabled(false);
	    		chooseRun.setEnabled(false);
	    		newRun.setEnabled(false);
	    		slice.setEnabled(false);
	    		showResult.setEnabled(false);
	    		clearResult.setEnabled(false);
	    		compareRun.setEnabled(false);
	    		showGraph.setEnabled(false);

	    	}
	    	*/
	    }
	}

	/**
	 * The constructor.
	 * Initializes the singleton pattern.
	 */
	public AnalysisView() {
		singleton = this;
	}

	/*** PREDEFINED METHODS ***/

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(viewer);
		viewContent = new ViewContentProvider();
		viewer.setContentProvider(viewContent);
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setComparator(new NameSorter());
		viewer.setInput(getViewSite());
		viewer.addSelectionChangedListener(new PossibleActionsListener());

		// listen to selections in other views
		//getSite().getPage().addSelectionListener(listener);

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		hookSelectionListener();
		contributeToActionBars();

	}

	// popup-menue
	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);

		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				AnalysisView.this.fillContextMenu(manager);
			}
		});

		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	// inhalt des pulldown-menues
	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(choose);
		manager.add(chooseRun);
		manager.add(newEvalRun);
		manager.add(addAlgo);
		manager.add(addCrit);
		manager.add(newRun);
		manager.add(slice);
		manager.add(showResult);
		manager.add(clearResult);
		manager.add(compareRun);
		manager.add(remove);
		manager.add(showGraph);
		manager.add(new Separator());
	}

	// inhalt des popup-menues
	private void fillContextMenu(IMenuManager manager) {
		manager.add(choose);
		manager.add(chooseRun);
		manager.add(newEvalRun);
		manager.add(addAlgo);
		manager.add(addCrit);
		manager.add(newRun);
		manager.add(slice);
		manager.add(showResult);
		manager.add(clearResult);
		manager.add(compareRun);
		manager.add(remove);
		manager.add(showGraph);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	// inhalt der toolbar der view
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(choose);
		manager.add(chooseRun);
		manager.add(newEvalRun);
		manager.add(addAlgo);
		manager.add(addCrit);
		manager.add(newRun);
		manager.add(slice);
		manager.add(showResult);
		manager.add(clearResult);
		manager.add(compareRun);
		manager.add(remove);
		manager.add(showGraph);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	/** Creates the actions.
	 */
	private void makeActions() {
		// the choose-algorithm action
		choose = new Action() {
			// creates an AlgorithmBRowser
			public void run() {
				//AlgorithmBrowser.showGUI();
				Display display = Activator.getDefault().getDisplay();
				Shell shell = new Shell(display);
				AlgorithmBrowserNeu ab = new AlgorithmBrowserNeu(shell);
				ab.setTitle("Choose Algorithm");
				ab.open();
				if (ab.getResult() != null) {
					setAlgorithm((Algorithm) ab.getResult()[0]);
				}
			}
		};
		choose.setText("Choose Algorithm");
		choose.setToolTipText("choose the desired algorithm");
		choose.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("choose"));
		choose.setEnabled(false);

		// the choose-algorithm action
		chooseRun = new Action() {
			// creates an AlgorithmBRowser
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof RunView) {
					Display display = Activator.getDefault().getDisplay();
					Shell shell = new Shell(display);
					AlgorithmBrowserNeu ab = new AlgorithmBrowserNeu(shell);
					ab.setTitle("Choose Algorithm");
					ab.open();
					setAlgorithm((Algorithm) ab.getResult()[0], (RunView) obj);
				}
			}
		};
		chooseRun.setText("Choose Algorithm with same Run");
		chooseRun.setToolTipText("choose the desired algorithm with the same Run");
		chooseRun.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("choose"));
		chooseRun.setEnabled(false);

		newEvalRun = new Action() {
			// creates an AlgorithmBRowser
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof GraphView) {
					EvaluationRunView evView = new EvaluationRunView(new EvaluationRun());
					((GraphView) obj).addChild(evView);
					evView.setParent((GraphView) obj);
					viewer.refresh();
				}
			}
		};
		newEvalRun.setText("New EvaluationRun");
		newEvalRun.setToolTipText("creates a new EvaluationRun");
		newEvalRun.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("configure"));
		newEvalRun.setEnabled(false);

		// action to remove tree elements
		remove = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof TreeNode) {
					viewContent.remove((TreeNode)obj);
					viewer.refresh();
					if (obj instanceof NodeView) {
						NodeView nv = (NodeView) obj;
						nv.getParent().getCriteria().removeNode(nv.getNode());
					}
				}
			}
		};
		remove.setText("Remove");
		remove.setToolTipText("remove the chosen entity");
		remove.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("remove"));
		remove.setEnabled(false);


		// starts a chosen algorithm
		slice = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof RunView) {
					((RunView)obj).execute();
					viewer.refresh();
				}  else if (obj instanceof EvaluationRunView) {
					((EvaluationRunView)obj).execute();
					viewer.refresh();
				}
			}
		};
		slice.setText("Run Analysis");
		slice.setToolTipText("runs the chosen analysis");
		slice.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("slicer"));
		slice.setEnabled(false);


		// shows the result of an algorithm
		showResult = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof RunView) {
					try {
						((RunView)obj).showResult();
						viewer.refresh();
					} catch (NullPointerException e) {
					}
				} else if (obj instanceof EvaluationRunView) {
					if (((EvaluationRunView) obj).hasExecuted()) {
						((EvaluationRunView) obj).showResult();
					} else {
						AnalysisView.getInstance().showMessage("Execute first pls");
					}
				}
			}
		};
		showResult.setText("Show Result");
		showResult.setToolTipText("shows the result of the chosen analysis");
		showResult.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("results"));
		showResult.setEnabled(false);


		// shows the result of an algorithm
		clearResult = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof TreeNode) {
				    try {
				        TreeNode tn = (TreeNode) obj;
			            HighlightPlugin high = HighlightPlugin.getDefault();

			            // get the Java project and clean it
			            IProject p = tn.getProject();
			            high.clearAll(p);

			        } catch(Exception e) {
			            e.printStackTrace();
			        }
				}
			}
		};
		clearResult.setText("Clear Result");
		clearResult.setToolTipText("clears the currently shown result");
		clearResult.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("clear"));
		clearResult.setEnabled(false);


		// creates a new run for an algorithm
		newRun = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof ChosenAlgorithmView) {
					ChosenAlgorithmView a = (ChosenAlgorithmView)obj;
					//GraphView g = a.getParent().getGraph();
					Run run = AlgorithmFactory.createAlgorithm(a.getAlgorithm().getAlgorithm(), a.getParent().getGraph());
					RunView r = new RunView(run);

					run.addObserver(getInstance());
					a.addChild(r);

					// create CriteriaCategoryViews
					for (Enum<?> e : run.getKindsOfCriteria()) {
						CriteriaCategory cc = new CriteriaCategory(e.toString());
						CriteriaCategoryView ccv = new CriteriaCategoryView(cc);

						run.addChild(cc);
						r.addChild(ccv);
					}

					viewer.refresh();
				}
			}
		};
		newRun.setText("New Run");
		newRun.setToolTipText("configures a new run of the chosen analysis");
		newRun.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("configure"));
		newRun.setEnabled(false);

		//adds an Algorithm to an evalRun
		addAlgo = new Action() {
			public void run() {
				try {
					ISelection selection = viewer.getSelection();
					Object obj = ((IStructuredSelection)selection).getFirstElement();

					if (obj instanceof EvaluationRunView) {
						EvaluationRunView a = (EvaluationRunView)obj;

						Display display = Activator.getDefault().getDisplay();
						Shell shell = new Shell(display);
						AlgorithmBrowserNeu ab = new AlgorithmBrowserNeu(shell);
						ab.setTitle("Choose Algorithm");
						ab.open();
						Algorithm alg = (Algorithm) ab.getResult()[0];
						Graph g = getCurrentGraph();
						Run r = AlgorithmFactory.createAlgorithm(alg, g);
						ChosenAlgorithm c = new ChosenAlgorithm(alg);
						c.addChild(r);
						c.setParent(g);
						EvaluationAlgorithmView evAlgoView = new EvaluationAlgorithmView(r);
						r.addObserver(getInstance());
						a.addChild(evAlgoView);

						//create CriteriaCategoryViews
						for (Enum<?> e : r.getKindsOfCriteria()) {
							CriteriaCategory cc = new CriteriaCategory(e.toString());
							CriteriaCategoryView ccv = new CriteriaCategoryView(cc);

							r.addChild(cc);
							evAlgoView.addChild(ccv);
						}

						viewer.refresh();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		addAlgo.setText("Add Algorithm");
		addAlgo.setToolTipText("Adds an Algorithm to an EvaluationRun");
		addAlgo.setImageDescriptor(Activator.getDefault().getImageRegistry().getDescriptor("configure"));
		addAlgo.setEnabled(false);

		// captures double-clicks
		doubleClickAction = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof TreeNode) {
					((TreeNode)obj).doubleClicked();

				} else {
					showMessage("Double-click detected on "+obj.toString());
				}
			}
		};

		compareRun = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof RunView) {
					((RunView)obj).compareRuns();
					viewer.refresh();
				}
			}
		};
		compareRun.setText("Compare Run");
		compareRun.setToolTipText("compares a run with another run");
		compareRun.setEnabled(false);

		showGraph = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof GraphView) {
					ShowGraphViewer.showGraphViewer(((GraphView) obj).getGraph().getSdgPath());
				}
			}
		};
		showGraph.setText("Open SDG");
		showGraph.setToolTipText("opens sdg in graphviewer");
		showGraph.setEnabled(false);

		addCrit = new Action() {
			public void run() {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof EvaluationRunView) {
					CriteriaDialog dlg = new CriteriaDialog(new Shell(Activator.getDefault().getDisplay()));
					dlg.open();
					if (dlg.getResult() != null) {
						EvaluationCriteria ec = (EvaluationCriteria) dlg.getResult()[0];
						EvaluationCriteriaView ecv = new EvaluationCriteriaView((EvaluationRunView) obj, ec);
						((EvaluationRunView) obj).addChild(ecv);

						viewer.refresh();
					}
				}
			}
		};
		addCrit.setText("Add Criteria");
		addCrit.setToolTipText("adds a criteria to an evaluation run");
		addCrit.setEnabled(false);
	}

	private void hookDoubleClickAction() {
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				doubleClickAction.run();
			}
		});
	}

	private void hookSelectionListener() {
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				ISelection selection = viewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();

				if (obj instanceof RunView) {
					if (CompareRunDialog.getInst() != null && !CompareRunDialog.getInst().isDisposed()) {
						CompareRunDialog.getInst().setSecond((RunView) obj);
					}
				}
			}
		});
	}

	void showMessage(String message) {
		MessageDialog.openInformation(
			viewer.getControl().getShell(),
			"Sample View",
			message);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}



	/*** CUSTOM METHODS ***/

	public void attachSelectionChangedListener(ISelectionChangedListener o) {
	    viewer.addSelectionChangedListener(o);
	}

	public void detachSelectionChangedListener(ISelectionChangedListener o) {
	    viewer.removeSelectionChangedListener(o);
	}

	/** Adds a new SDG to this view.
	 *
	 * @param g  The new Graph.
	 */
	public void setNewGraph(Graph g) {
		GraphView gv = new GraphView(g);

		viewContent.addNewTree(gv);
		viewer.refresh();
	}

	/** Adds a new algorithm to a SDG.
	 *
	 * @param alg  The new algorithm
	 */
	public void setAlgorithm(Algorithm alg) {
		ISelection selection = viewer.getSelection();
		Object obj = ((IStructuredSelection)selection).getFirstElement();

		// search the surrounding SDG object and add the algorithm as a new Algorithms object
		if (obj instanceof GraphView) {
			setAlgorithm(alg, (GraphView)obj);

		} else if (obj instanceof ChosenAlgorithmView) {
			ChosenAlgorithmView f = (ChosenAlgorithmView)obj;
			setAlgorithm(alg, f.getParent());

		} else if (obj instanceof RunView) {
			RunView f = (RunView)obj;
			setAlgorithm(alg, f.getParent().getParent());
		}
	}

	public void setAlgorithm(Algorithm alg, RunView info) {
		ISelection selection = viewer.getSelection();
		Object obj = ((IStructuredSelection)selection).getFirstElement();

		// search the surrounding SDG object and add the algorithm as a new Algorithms object
		if (obj instanceof GraphView) {
			setAlgorithm(alg, (GraphView)obj, info);

		} else if (obj instanceof ChosenAlgorithmView) {
			ChosenAlgorithmView f = (ChosenAlgorithmView)obj;
			setAlgorithm(alg, f.getParent(), info);

		} else if (obj instanceof RunView) {
			RunView f = (RunView)obj;
			setAlgorithm(alg, f.getParent().getParent(), info);
		}
	}

	// helper method for setAlgorithm.
	private void setAlgorithm(Algorithm alg, GraphView sdg){
		if (!sdg.contains(alg)) {
			ChosenAlgorithm ca = new ChosenAlgorithm(alg);
			ChosenAlgorithmView cav = new ChosenAlgorithmView(ca);

			sdg.addChild(cav);
			viewer.refresh();
		}
	}

	private void setAlgorithm(Algorithm alg, GraphView sdg, RunView info){
		if (!sdg.contains(alg)) {
			ChosenAlgorithm ca = new ChosenAlgorithm(alg);
			ChosenAlgorithmView cav = new ChosenAlgorithmView(ca);
			sdg.addChild(cav);

			Run run = AlgorithmFactory.createAlgorithm(cav.getAlgorithm().getAlgorithm(), cav.getParent().getGraph());
			RunView r = new RunView(run);

			run.addObserver(getInstance());
			cav.addChild(r);

			// create CriteriaCategoryViews
			for (Enum<?> e : run.getKindsOfCriteria()) {
				CriteriaCategory cc = new CriteriaCategory(e.toString());
				CriteriaCategoryView ccv = new CriteriaCategoryView(cc);

				run.addChild(cc);
				r.addChild(ccv);
			}

			for (TreeNode t : info.getChildren()) {
				CriteriaCategoryView critv = (CriteriaCategoryView) t;
				for (Enum<?> e : run.getKindsOfCriteria()) {
					if (e.toString().equals(critv.getName())) {
						for (TreeNode t2 : critv.getChildren()) {
							r.setCriteria(((CriteriaView) t2).getCriteria(), e);
						}
						break;
					}
				}
			}

			viewer.refresh();
		}
	}

	public void askCriterion(ITextSelection selected, String filename) {
		ISelection selection = viewer.getSelection();
		Object obj = ((IStructuredSelection)selection).getFirstElement();
		Run r = null;
		boolean valid = true;
		CriteriaCategoryView[] children = null;

		if (obj instanceof RunView) {
			r = ((RunView)obj).getRun();
			children = ((RunView) obj).getChildren();

		} else if (obj instanceof EvaluationAlgorithmView) {
			r = ((EvaluationAlgorithmView)obj).getRun();
			children = ((EvaluationAlgorithmView) obj).getChildren();

		} else if (obj instanceof CriteriaCategoryView) {
			CriteriaCategoryView par = (CriteriaCategoryView) obj;
			if (par.getParent() instanceof RunView) {
				r = ((RunView)par.getParent()).getRun();
				children = par.getParent().getChildren();
			} else if (par.getParent() instanceof EvaluationAlgorithmView) {
				r = ((EvaluationAlgorithmView)par.getParent()).getRun();
				children = par.getParent().getChildren();
			}
		} else if (obj instanceof CriteriaView) {
			CriteriaCategoryView par = ((CriteriaView) obj).getParent();
			if (par.getParent() instanceof RunView) {
				r = ((RunView)par.getParent()).getRun();
				children = par.getParent().getChildren();
			} else if (par.getParent() instanceof EvaluationAlgorithmView) {
				r = ((EvaluationAlgorithmView)par.getParent()).getRun();
				children = par.getParent().getChildren();
			}
		} else {
			valid = false;
		}

		if (valid) {
			TreeSet<SDGNode> nodes = new TreeSet<SDGNode>(SDGNode.getIDComparator());
	        Collection<SDGNode> allNodes = getCurrentGraph().getNodes();

	        filename = filename.replaceAll("\\\\", "/");

	        // get all nodes that lie between start and and line
	        for (SDGNode n : allNodes) {
	            if (n.getSource() == null) continue;

	            if (filename.equals(n.getSource())) {
	                if (n.getSr() >= selected.getStartLine() +1 && n.getEr() <= selected.getEndLine() +1) {
	                    nodes.add(n);
	                }
	            }
	        }

            Criteria crit = new Criteria(selected.getText(), nodes);
            SelectCriterionDialogNeu dlg = new SelectCriterionDialogNeu(Activator.getDefault().getDisplay().getActiveShell(), r);
            dlg.open();
			//SelectCriterionDialog.create(r, crit);

            if (dlg.getResult() != null) {
            	Enum<?> kind = (Enum<?>) dlg.getResult()[0];
	            setCriteria(r, crit, kind);
	            // insert into view
	    		for (CriteriaCategoryView cc : children) {
	    			if (cc.getName().equals(kind.toString())) {
	    				CriteriaView cv = new CriteriaView(crit);
	    				cc.addChild(cv);
	    			}
	    		}
	            viewer.refresh();
            }

		} else {
			showMessage("you must first select a run in the Analysis View");
		}
	}

	//sets Criteria in model
	public void setCriteria(Run run, Criteria crit, Enum<?> category) {
		run.setCriteria(crit, category);
	}

	public void update(Observable o, Object arg) {
		// the Observable is a Run-Object
		final Run run = (Run) o;

		Activator.getDefault().getDisplay().asyncExec (new Runnable () {
			public void run () {
				showMessage(run.getName()+" has finished computation");
			}
		});
		if (CompareRunDialog.getInst() != null && !CompareRunDialog.getInst().isDisposed()) {
			CompareRunDialog.getInst().update();
		}
	}

	public Graph getCurrentGraph() {
	    ISelection selection = viewer.getSelection();
        Object obj = ((IStructuredSelection)selection).getFirstElement();

        // search the surrounding Graph and return it
        /*if (obj instanceof GraphView) {
            return ((GraphView)obj).getGraph();

        } else if (obj instanceof ChosenAlgorithmView) {
            ChosenAlgorithmView f = (ChosenAlgorithmView)obj;
            return f.getParent().getGraph();

        } else if (obj instanceof RunView) {
            RunView f = (RunView)obj;
            return f.getParent().getParent().getGraph();

        } else if (obj instanceof CriteriaCategoryView) {
            CriteriaCategoryView f = (CriteriaCategoryView)obj;
            return f.getParent().getParent().getParent().getGraph();

        } else if (obj instanceof CriteriaView) {
            CriteriaView f = (CriteriaView)obj;
            return f.getParent().getParent().getParent().getParent().getGraph();

        } else if (obj instanceof NodeView) {
            NodeView f = (NodeView)obj;
            return f.getParent().getParent().getParent().getParent().getParent().getGraph();*/

        if (obj instanceof TreeNode) {
            TreeNode parent = (TreeNode) obj;
            while (!(parent instanceof GraphView)) {
            	parent = (TreeNode) parent.getParent();
            }
            return ((GraphView)parent).getGraph();
        } else {
            return null;
        }
	}

	public SDGNode getCurrentNode() {
        ISelection selection = viewer.getSelection();
        Object obj = ((IStructuredSelection)selection).getFirstElement();

        // search the surrounding Graph and return it
        if (obj instanceof NodeView) {
            return ((NodeView)obj).getNode();

        } else {
            return null;
        }
    }

	public void refresh() {
		viewer.refresh();
	}
}
