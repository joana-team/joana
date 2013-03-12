/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.threadviewer.view.view;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.AbstractZoomableViewer;
import org.eclipse.zest.core.viewers.EntityConnectionData;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.viewers.IZoomableWorkbenchPart;
import org.eclipse.zest.core.viewers.ZoomContributionViewItem;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.CompositeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.DirectedGraphLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalShift;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ui.ifc.sdg.textual.highlight.highlight.HighlightPlugin;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.AlphabeticalThreadRegionSorterAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.FilterHideInterproceduralEdgesAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.FilterHideNotInterferingThreadRegionAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.FilterHideSourcecodeThreadRegionAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.SourcecodeThreadRegionSorterAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_CenterInterferingRegionsAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_CenterParallelRegionAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_FollowInterferedNodesAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_FollowInterferenceAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_FollowNodeAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_FollowParallelRegionsAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_FollowThreadAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewContextMenu_FollowThreadRegionAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_CenterInterferingRegionsAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_CenterParallelRegionAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_FollowInterferedNodesAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_FollowNodeAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_FollowParallelRegionsAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_FollowThreadAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.actions.ViewToolbar_FollowThreadRegionAction;
import edu.kit.joana.ui.ifc.sdg.threadviewer.controller.Controller;
import edu.kit.joana.ui.ifc.sdg.threadviewer.model.SDGWrapper;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.Activator;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.filter.HideNotInterferingThreadRegionFilter;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.filter.HideSourceCodeThreadRegionFilter;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.filter.HideThreadFilter;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.listener.ViewDoubleClickListener;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.listener.ViewSelectionChangedListener;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider.GraphContentProvider;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider.GraphLabelProvider;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider.TreeContentProvider;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.provider.TreeLabelProvider;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.sorter.AlphabeticalThreadRegionSorter;
import edu.kit.joana.ui.ifc.sdg.threadviewer.view.sorter.SourcecodeThreadRegionSorter;


/**
 * The ThreadViewer class handles the plug-in edu.kit.joana.ui.ifc.sdg.threadviewer.view.
 *
 * @author Le-Huan Stefan Tran
 */
public class ThreadViewer extends ViewPart implements IZoomableWorkbenchPart {

	/**
	 * The ID of the edu.kit.joana.ui.ifc.sdg.threadviewer.view as specified by the extension.
	 */
	public static final String THREADVIEWER_ID = "ThreadViewer.View";

	// Captions
	private static final String HEADER_TITLE = "Thread Analysis";

	// HighlightPlugin to highlight code in the Java-editor
	private HighlightPlugin highlightPlugin;

	// 2 - Blue
	// 24 - Green
	// 40 - Dark Yellow
	// HighlightPlugin.LAST_LEVEL - Orange
	public static final int COLOR_THREAD = 40;
	public static final int COLOR_REMAINING_THREAD = HighlightPlugin.FIRST_LEVEL;
	public static final int COLOR_REGION = 40;
	public static final int COLOR_REMAINING_REGION = HighlightPlugin.FIRST_LEVEL;
	public static final int COLOR_PARALLEL = 40;
	public static final int COLOR_NODE = 40;
	public static final int COLOR_INTERFERING = HighlightPlugin.FIRST_LEVEL;
	public static final int COLOR_INTERFERED = 40;

	public static final int COLOR_THREAD_BLUE = 2;
	public static final int COLOR_THREAD_GREEN = 24;
	public static final int COLOR_THREAD_ORANGE = HighlightPlugin.LAST_LEVEL;
	public static final int COLOR_THREAD_YELLOW = 40;

	public static final int[] COLOR_THREADS = new int[] { COLOR_THREAD_BLUE,
		COLOR_THREAD_GREEN, COLOR_THREAD_ORANGE, COLOR_THREAD_YELLOW };

	// Tree Expansions
	private static final int TREE_LEVEL_THREAD_REGIONS = 2;
	private static final int TREE_LEVEL_INTERFERING_NODES = 4;

	private static ThreadViewer instance;

	// Tabs
	private TabFolder tabs;
	private static final int TREE_TAB = 1;
	private static final int GRAPH_TAB = 0;

	// Stores all currently selected threads of graph
	private Collection<ThreadInstance> activeThreads;

	// TreeViewer
	private TreeViewer treeViewer;
	private TreeContentProvider treeContentProvider;
	private ViewSelectionChangedListener viewSelectionChangedListener;
	private TreeLabelProvider treeLabelProvider;
	private ViewDoubleClickListener viewDoubleClickListener;

	// GraphViewer
	private FormToolkit toolKit = null;
	private Form form = null;
	private SashForm mainSash;

	private GraphViewer graphViewer;
	private GraphContentProvider graphContentProvider;
	private GraphLabelProvider graphLabelProvider;

	private ZoomContributionViewItem contextmenuZoomItem;
	private ZoomContributionViewItem toolbarZoomItem;

	// Options
	private Composite optionsComposite;
	private Button hideNonSourceCodeButton;
	private Button hideNotInterferingButton;
	private Button hideInterproceduralEdgesButton;

	private Button alphabeticalSorterButton;
	private Button sourcecodeSorterButton;

	private Button[] showThreadButtons;

	// Context menu
	private IMenuManager rootTriangleMenuManager;

	private MenuManager menuMgrView;
	private MenuManager menuMgrGraph;
	private ViewContextMenu_FollowThreadRegionAction contextmenuFollowThreadRegionAction;
	private ViewContextMenu_FollowParallelRegionsAction contextmenuFollowParallelRegionsAction;
	private ViewContextMenu_FollowThreadAction contextmenuFollowThreadAction;
	private ViewContextMenu_CenterParallelRegionAction contextmenuCenterParallelRegionAction;

	private ViewContextMenu_FollowNodeAction contextmenuFollowNodeAction;
	private ViewContextMenu_FollowInterferedNodesAction contextmenuFollowInterferedNodesAction;
	private ViewContextMenu_CenterInterferingRegionsAction contextmenuCenterInterferedNodeAction;
	private ViewContextMenu_FollowInterferenceAction contextmenuFollowInterference;

	// Tool bar
	private IToolBarManager toolbarManager;
	private ViewToolbar_FollowThreadAction toolbarFollowThreadAction;

	private ViewToolbar_CenterParallelRegionAction toolbarCenterParallelRegionAction;
	private ViewToolbar_FollowThreadRegionAction toolbarFollowThreadRegionAction;
	private ViewToolbar_FollowParallelRegionsAction toolbarFollowParallelRegionsAction;

	private ViewToolbar_CenterInterferingRegionsAction toolbarCenterInterferedNodeAction;
	private ViewToolbar_FollowNodeAction toolbarFollowNodeAction;
	private ViewToolbar_FollowInterferedNodesAction toolbarFollowInterferedNodesAction;

	// Filters & sorters
	private ViewerFilter hideSourceCodeThreadRegionFilter;
	private Action hideSourcecodeThreadRegionAction;
	private ViewerFilter hideNotInterferingThreadRegionFilter;
	private Action hideNotInterferingThreadRegionAction;

	private Action hideInterproceduralEdgesAction;
	private ViewerFilter hideThreadFilter;

	private ViewerSorter sourcecodeThreadRegionSorter;
	private Action sourcecodeThreadRegionAction;
	private ViewerSorter alphabeticalSorter;
	private Action alphabeticalAction;

	/**
	 * The public constructor of the ThreadViewer class.
	 */
	private ThreadViewer() {
		highlightPlugin = HighlightPlugin.getDefault();
		activeThreads = new HashSet<ThreadInstance>();

		// TreeViewer
		treeContentProvider = new TreeContentProvider();
		treeLabelProvider = new TreeLabelProvider();
		viewSelectionChangedListener = new ViewSelectionChangedListener();
		viewDoubleClickListener = new ViewDoubleClickListener();

		// GraphViewer
		graphContentProvider = new GraphContentProvider();
 		graphLabelProvider = new GraphLabelProvider();

		// Create handles for menu and toolbar
		contextmenuFollowThreadRegionAction = new ViewContextMenu_FollowThreadRegionAction();
		contextmenuFollowParallelRegionsAction = new ViewContextMenu_FollowParallelRegionsAction();
		contextmenuFollowThreadAction = new ViewContextMenu_FollowThreadAction();
		contextmenuCenterParallelRegionAction = new ViewContextMenu_CenterParallelRegionAction();

		contextmenuFollowNodeAction = new ViewContextMenu_FollowNodeAction();
		contextmenuFollowInterferedNodesAction = new ViewContextMenu_FollowInterferedNodesAction();
		contextmenuCenterInterferedNodeAction = new ViewContextMenu_CenterInterferingRegionsAction();
		contextmenuFollowInterference = new ViewContextMenu_FollowInterferenceAction();

		toolbarFollowThreadRegionAction = new ViewToolbar_FollowThreadRegionAction();

		toolbarCenterParallelRegionAction = new ViewToolbar_CenterParallelRegionAction();
		toolbarFollowParallelRegionsAction = new ViewToolbar_FollowParallelRegionsAction();
		toolbarFollowThreadAction = new ViewToolbar_FollowThreadAction();

		toolbarCenterInterferedNodeAction = new ViewToolbar_CenterInterferingRegionsAction();
		toolbarFollowNodeAction = new ViewToolbar_FollowNodeAction();
		toolbarFollowInterferedNodesAction = new ViewToolbar_FollowInterferedNodesAction();

		// Create filters and sorters
		hideSourceCodeThreadRegionFilter = new HideSourceCodeThreadRegionFilter();
		hideSourcecodeThreadRegionAction = new FilterHideSourcecodeThreadRegionAction();

		hideNotInterferingThreadRegionFilter = new HideNotInterferingThreadRegionFilter();
		hideNotInterferingThreadRegionAction = new FilterHideNotInterferingThreadRegionAction();

		hideInterproceduralEdgesAction = new FilterHideInterproceduralEdgesAction();
		hideThreadFilter = new HideThreadFilter();

		sourcecodeThreadRegionSorter = new SourcecodeThreadRegionSorter();
		sourcecodeThreadRegionAction = new SourcecodeThreadRegionSorterAction();

		alphabeticalSorter = new AlphabeticalThreadRegionSorter();
		alphabeticalAction = new AlphabeticalThreadRegionSorterAction();
	}


	/* View methods */

	/**
	 * Returns the current instance of the thread viewer.
	 */
	public static ThreadViewer getInstance() {
		if (instance == null) {
			instance = new ThreadViewer();
		}
		return instance;
	}

	/**
	 * Returns the reference to the graph viewer.
	 *
	 * @return	The reference to the graph viewer
	 */
	public GraphViewer getGraphViewer() {
		return this.graphViewer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dispose() {
		super.dispose();

		// Clears all highlights before disposing
		this.clearAllHighlights();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public AbstractZoomableViewer getZoomableViewer() {
		return graphViewer;
	}


	/* Main method of creating the edu.kit.joana.ui.ifc.sdg.threadviewer.view */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void createPartControl(Composite parent) {
	    toolKit = new FormToolkit(parent.getDisplay());
		form = this.toolKit.createForm(parent);
		mainSash = new SashForm(form.getBody(), SWT.NONE);

		// Create header
		this.createHeader();
		this.decorateHeader();

		// Create the tab folder
	    tabs = new TabFolder(mainSash, SWT.TOP);

	    // Create tabs: GraphView and TreeView
	    this.createGraphTab();
	    this.createTreeTab();

	    // Create options section
	    this.createOptionsSection();

		// Set children windows proportions
		mainSash.setWeights(new int[] { 10, 3 });

	    // Create zoom functions
		toolbarZoomItem = new ZoomContributionViewItem(this);
		contextmenuZoomItem = new ZoomContributionViewItem(this);

		this.hookContextMenuTree();
		this.hookContextMenuGraph();
		this.contributeToToolBar();
		this.createTriangleMenu();
	}

	private void createHeader() {
		Composite headClient = new Composite(form.getHead(), SWT.NULL);

		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.numColumns = 3;
		headClient.setLayout(layout);

		headClient.setBackgroundMode(SWT.INHERIT_DEFAULT);
		form.setText(HEADER_TITLE);
		form.setImage(Activator.getDefault().getImageRegistry().get(Activator.IMAGE_LOGO));
	}

	private void decorateHeader() {
		FillLayout layout = new FillLayout();
		layout.marginHeight = 8;
		layout.marginWidth = 4;
		form.getBody().setLayout(layout);

		this.toolKit.decorateFormHeading(form);
		this.toolKit.paintBordersFor(form.getBody());
	}

	private void createGraphTab() {
	    TabItem graphTab = new TabItem(tabs, SWT.NONE);
	    graphTab.setText("GraphViewer");
	    graphTab.setToolTipText("Show threads using a graph.");

		Composite composite = new Composite(tabs, SWT.NONE);
	    composite.setLayout(new FillLayout(SWT.VERTICAL));

		graphViewer = new GraphViewer(composite, SWT.V_SCROLL | SWT.H_SCROLL);

		graphViewer.setContentProvider(graphContentProvider);
		graphViewer.setLabelProvider(graphLabelProvider);

		graphViewer.setLayoutAlgorithm(new CompositeLayoutAlgorithm(
				LayoutStyles.NO_LAYOUT_NODE_RESIZING, new LayoutAlgorithm[] {
						new DirectedGraphLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING),
						new HorizontalShift(LayoutStyles.NO_LAYOUT_NODE_RESIZING) }));
		graphViewer.applyLayout();

		graphViewer.addDoubleClickListener(viewDoubleClickListener);
		graphViewer.addSelectionChangedListener(viewSelectionChangedListener);

		graphViewer.addFilter(hideSourceCodeThreadRegionFilter);
		graphViewer.addFilter(hideThreadFilter);
		// Adding a sorter has no effect

	    graphTab.setControl(composite);
	}

	private void createTreeTab() {
	    TabItem treeTab = new TabItem(tabs, SWT.NONE);
	    treeTab.setText("TreeViewer");
	    treeTab.setToolTipText("Show threads using a tree.");

	    tabs.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (ThreadViewer.this.tabs != null) {
					if (ThreadViewer.this.tabs.getSelectionIndex() == ThreadViewer.GRAPH_TAB) {
						ThreadViewer.this.selectionChanged(ThreadViewer.this.graphViewer.getSelection());
						alphabeticalSorterButton.setEnabled(false);
						sourcecodeSorterButton.setEnabled(false);
					} else {
						ThreadViewer.this.selectionChanged(ThreadViewer.this.treeViewer.getSelection());

						if (!isSourceCodeFilterSet()) {
							alphabeticalSorterButton.setEnabled(true);
							sourcecodeSorterButton.setEnabled(true);
						}
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) { }
		});

		Composite composite = new Composite(tabs, SWT.NONE);
	    composite.setLayout(new FillLayout(SWT.VERTICAL));

		treeViewer = new TreeViewer(composite, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);

		treeViewer.setContentProvider(treeContentProvider);
		treeViewer.setLabelProvider(treeLabelProvider);

		treeViewer.addDoubleClickListener(viewDoubleClickListener);
		treeViewer.addSelectionChangedListener(viewSelectionChangedListener);

		treeViewer.setSorter(sourcecodeThreadRegionSorter);
		treeViewer.addFilter(hideSourceCodeThreadRegionFilter);
		treeViewer.addFilter(hideThreadFilter);

	    treeTab.setControl(composite);
	}

	private void createOptionsSection() {
	    Section optionsSection = this.toolKit.createSection(mainSash, Section.TITLE_BAR | Section.EXPANDED);
		optionsSection.setText("Options");

		// Create main composite
		optionsComposite = new Composite(optionsSection, SWT.NONE) {
			public Point computeSize(int hint, int hint2, boolean changed) {
				return new Point(0, 0);
			}
		};
		this.toolKit.adapt(optionsComposite);
		optionsComposite.setLayout(new GridLayout());
		optionsSection.setClient(optionsComposite);

		// Create filters
		Section filterSection = this.toolKit.createSection(optionsComposite, Section.EXPANDED);
		filterSection.setText("Filters");
		Composite filterComposite = this.toolKit.createComposite(filterSection);
		filterComposite.setLayout(new TableWrapLayout());
		filterSection.setClient(filterComposite);

		hideNonSourceCodeButton = this.toolKit.createButton(optionsComposite, "Hide non-source code regions", SWT.CHECK);
		hideNonSourceCodeButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		hideNonSourceCodeButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hideSourcecodeThreadRegionAction.setChecked(!hideSourcecodeThreadRegionAction.isChecked());
				hideSourcecodeThreadRegionAction.run();
			}
		});
		hideNonSourceCodeButton.setEnabled(false);
		hideNonSourceCodeButton.setSelection(true);


		hideNotInterferingButton = this.toolKit.createButton(optionsComposite, "Hide not interfering regions", SWT.CHECK);
		hideNotInterferingButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		hideNotInterferingButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hideNotInterferingThreadRegionAction.setChecked(!hideNotInterferingThreadRegionAction.isChecked());
				hideNotInterferingThreadRegionAction.run();
			}
		});
		hideNotInterferingButton.setEnabled(false);
		hideNotInterferingButton.setSelection(false);


		hideInterproceduralEdgesButton = this.toolKit.createButton(optionsComposite,
				"Hide interprocedural edges", SWT.CHECK);
		hideInterproceduralEdgesButton.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
		hideInterproceduralEdgesButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				hideInterproceduralEdgesAction.setChecked(!hideInterproceduralEdgesAction.isChecked());
				hideInterproceduralEdgesAction.run();
			}
		});
		hideInterproceduralEdgesButton.setEnabled(false);
		hideInterproceduralEdgesButton.setSelection(true);

		// Create sorters
		Section sorterSection = this.toolKit.createSection(optionsComposite, Section.EXPANDED);
		sorterSection.setText("Sorters");
		Composite sorterComposite = this.toolKit.createComposite(sorterSection);
		sorterComposite.setLayout(new TableWrapLayout());
		sorterSection.setClient(sorterComposite);

		alphabeticalSorterButton = this.toolKit.createButton(sorterComposite, "Alphabetically", SWT.RADIO);
		alphabeticalSorterButton.setLayoutData(new TableWrapData(TableWrapData.FILL));
		alphabeticalSorterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				alphabeticalAction.setChecked(true);
				alphabeticalAction.run();
			}
		});
		alphabeticalSorterButton.setEnabled(false);

		sourcecodeSorterButton = this.toolKit.createButton(sorterComposite, "Source code first", SWT.RADIO);
		sourcecodeSorterButton.setLayoutData(new TableWrapData(TableWrapData.FILL));
		sourcecodeSorterButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				sourcecodeThreadRegionAction.setChecked(true);
				sourcecodeThreadRegionAction.run();
			}
		});
		sourcecodeSorterButton.setEnabled(false);
		sourcecodeSorterButton.setSelection(true);

		// Create thread filters
		Section threadSection = this.toolKit.createSection(optionsComposite, Section.EXPANDED);
		threadSection.setText("View Threads");
		Composite threadComposite = this.toolKit.createComposite(threadSection);
		threadComposite.setLayout(new TableWrapLayout());
		threadSection.setClient(threadComposite);

		// Check boxes for threads will be created when .pdg-file is loaded
	}

	/**
	 * Sets the initial input to the plug-in.
	 *
	 * @param fullPath	The path to the loaded .pdg-file.
	 */
	public void setInitialInput(IPath fullPath) {
		setInitialInput(fullPath.toString());
	}

	public void setInitialInput(String label) {
		// Set tree and graph
		treeViewer.setInput(new Object());
		treeViewer.expandToLevel(TREE_LEVEL_THREAD_REGIONS);

		graphViewer.setInput(new Object());
		graphLabelProvider.setCurrentSelection(null);

		// Set up check boxes for threads
		activeThreads.clear();
		this.createShowThreadButtons();

		// Workaround to make new check boxes visible
		mainSash.setWeights(new int[] {10, 4});
		mainSash.setWeights(new int[] {10, 3});

		hideNonSourceCodeButton.setEnabled(true);
		hideNotInterferingButton.setEnabled(true);
		hideInterproceduralEdgesButton.setEnabled(true);
		sourcecodeSorterButton.setEnabled(false);
		alphabeticalSorterButton.setEnabled(false);

		// Set header title
		label = HEADER_TITLE + ": " + label.substring(label.lastIndexOf("/") + 1, label.length());
		form.setText(label);
	}

	private void createShowThreadButtons() {
		// Dispose old buttons
		if (showThreadButtons != null) {
			for (int i = 0; i < showThreadButtons.length; i++) {
				showThreadButtons[i].dispose();
			}
		}

		// Create new buttons
		Collection<ThreadInstance> threads = SDGWrapper.getInstance().getThreads();
		showThreadButtons = new Button[threads.size()];

		// Use thread IDs for ascending order
		for (int threadID = 0; threadID < threads.size(); threadID++) {
			ThreadInstance thread = SDGWrapper.getInstance().getThread(threadID);
			showThreadButtons[threadID] = this.toolKit.createButton(optionsComposite,
					SDGWrapper.getInstance().getShortLabel(thread), SWT.CHECK);
			showThreadButtons[threadID].setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));
			showThreadButtons[threadID].addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					Object obj = e.getSource();
					Object data = null;
					if (obj instanceof Button) {
						Button button = (Button) obj;
						data = button.getData();
					}
					setActiveThread(data, !isActiveThread(data));

					// Update tree
					treeViewer.refresh();

					// Redraw graph
					graphViewer.refresh();
					ISelection saveSelection = graphViewer.getSelection();
					graphViewer.setInput(new Object());
					graphViewer.setSelection(saveSelection);
				}
			});
			showThreadButtons[threadID].setEnabled(true);
			showThreadButtons[threadID].setData(thread);
			showThreadButtons[threadID].setSelection(true);

			setActiveThread(thread, true);
		}
	}

	/**
	 * Sets the state of a given thread.
	 * Needed for displaying / hiding threads in the plug-in edu.kit.joana.ui.ifc.sdg.threadviewer.view.
	 *
	 * @param obj		The thread to be saved
	 * @param active    State of thread
	 */
	public void setActiveThread(Object obj, boolean active) {
		if (obj != null) {
			if (obj instanceof ThreadInstance) {
				ThreadInstance thread = (ThreadInstance) obj;

				if (active) {
					this.activeThreads.add(thread);
				} else {
					this.activeThreads.remove(thread);
				}
			}
		}
	}


	/**
	 * Checks the state of a given thread.
	 * Needed for displaying / hiding threads in the plug-in edu.kit.joana.ui.ifc.sdg.threadviewer.view.
	 *
	 * @param obj	The thread to be checked
	 * @return		True if thread is active
	 */
	public boolean isActiveThread(Object obj) {
		boolean isActive = false;

		if (obj instanceof ThreadInstance) {
			ThreadInstance thread = (ThreadInstance) obj;

			if (this.activeThreads.contains(thread)) {
				isActive = true;
			}
		} else if (obj instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) obj;
			if (this.activeThreads.contains(SDGWrapper.getInstance().getThread(region))) {
				isActive = true;
			}
		} else if (obj instanceof EntityConnectionData) {
			isActive = true;
		} else if (obj instanceof ViewCategory) {
			isActive = true;
		} else if (obj instanceof ViewParallelRegion) {
			isActive = true;
		} else if (obj instanceof SDGNode) {
			isActive = true;
		} else if (obj instanceof ViewInterferedNode) {
			isActive = true;
		}

		return isActive;
	}

	/**
	 * Refreshes the plug-in's edu.kit.joana.ui.ifc.sdg.threadviewer.view.
	 */
	public void refresh() {
		treeViewer.refresh();
		graphViewer.refresh();

		this.setFocus();

		// Bring edu.kit.joana.ui.ifc.sdg.threadviewer.view to front
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			page.showView(THREADVIEWER_ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
	}


	/**
	 *  Highlights the given region in the Editor View.
	 */
	public void highlightRegion(ThreadRegion region, int lvl) {
		Collection<SDGNode> nodes = region.getNodes();

		// Highlight nodes and set cursor
		this.highlight(nodes, lvl);
	}

	/**
	 *  Highlights the given node in the Editor View.
	 */
	public void highlight(SDGNode node, int lvl) {
		IProject project = SDGWrapper.getInstance().getProject();

		if (project != null) {
			highlightPlugin.highlight(project, node, lvl);
		}
	}

	/**
	 * Highlights the given nodes in the Editor View
	 */
	public void highlight(Collection<SDGNode> nodes, int lvl) {
		IProject project = SDGWrapper.getInstance().getProject();

		if (project != null) {
			// Choose color for all nodes
			Map<SDGNode, Integer> allNodesMap = new HashMap<SDGNode, Integer>();

			for (SDGNode node : nodes) {
				allNodesMap.put(node, lvl);
			}

			highlightPlugin.highlightJC(project, allNodesMap);
		}
	}


	/**
	 * Highlight all regions of the same thread except the given region
	 */
	public void highlightRemainingThread(ThreadRegion mainRegion) {
		Collection<ThreadRegion> regionCandidates = new HashSet<ThreadRegion>();

		for (ThreadRegion region : SDGWrapper.getInstance().getRegions(mainRegion.getThread())) {
			if (!region.equals(mainRegion)) {
				regionCandidates.add(region);
			}
		}

		// Delete nodes of same line of the region not to be highlighted
		Collection<SDGNode> nodesToHighlight = new HashSet<SDGNode>();

		for (ThreadRegion region : regionCandidates) {
			for (SDGNode node : region.getNodes()) {
				Iterator<SDGNode> iterMainNodes = mainRegion.getNodes().iterator();

				boolean isOutside = true;
				while (iterMainNodes.hasNext() && isOutside) {
					SDGNode mainNode = iterMainNodes.next();

					if (checkSameFile(mainNode, node)) {
						if (mainNode.getSr() <= node.getSr() && node.getSr() <= mainNode.getEr()){
							isOutside = false;
						}
					}
				}

				if (isOutside == true) {
					nodesToHighlight.add(node);
				}
			}
		}

		// Highlight remaining nodes
		this.highlight(nodesToHighlight, COLOR_REMAINING_THREAD);
	}


	/**
	 * Highlight all regions of the same thread except the given region
	 */
	public void highlightRemainingRegion(SDGNode mainNode) {
		Collection<SDGNode> nodes = new HashSet<SDGNode>();

		for (ThreadRegion region : SDGWrapper.getInstance().getRegions(mainNode)) {
			for (SDGNode node : region.getNodes()) {
				if (!node.equals(mainNode)) {
					if (checkSameFile(mainNode, node)) {
						if (!(mainNode.getSr() <= node.getSr() && node.getSr() <= mainNode.getEr())){
							nodes.add(node);
						}
					}
				}
			}
		}

		// Highlight remaining nodes
		this.highlight(nodes, COLOR_REMAINING_REGION);
	}

	/**
	 * Highlight all nodes being interfered by the given node
	 */
	public void highlightInterferedNodes(SDGNode node, int lvl) {
		Collection<SDGNode> nodes = SDGWrapper.getInstance().getInterferedNodes(node);
		this.highlight(nodes, lvl);
	}


	/**
	 * Clears all highlight markers in source code
	 */
	public void clearAllHighlights() {
		IProject project = SDGWrapper.getInstance().getProject();

		if (project != null) {
			// Clear all highlights
			try {
				highlightPlugin.clearAll(project);
			} catch (CoreException e) {
				Controller.getInstance().updateStatusBar("Clearing highlights failed.");
				e.printStackTrace();
			}
		}
	}


	/* Checks if two given nodes are located in the same file */
	private boolean checkSameFile(SDGNode n1, SDGNode n2) {
		boolean sameFile = false;

		if (n1 != null & n2 != null) {
			String s1 = n1.getSource();
			String s2 = n2.getSource();

			if (s1 != null && s2 != null) {
				if (s1.equals(s2)) {
					sameFile = true;
				}
			}
		}

		return sameFile;
	}


	/**
	 * Handles the double click on a tree item.
	 *
	 * @param obj	The tree item being clicked on
	 */
	public void doubleclickNode(Object obj) {
		if (obj != null) {
			if (obj instanceof ViewCategory) {
				ViewCategory category = (ViewCategory) obj;

				if (treeViewer.getExpandedState(category)) {
					treeViewer.collapseToLevel(category, 1);
				} else {
					treeViewer.expandToLevel(category, 1);
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFocus() {
		int currentTab = tabs.getSelectionIndex();

		if (currentTab == TREE_TAB) {
			treeViewer.getControl().setFocus();
		} else if (currentTab == GRAPH_TAB) {
			graphViewer.getControl().setFocus();
		}
	}

	/**
	 * Handles the change of selection in the edu.kit.joana.ui.ifc.sdg.threadviewer.view.
	 *
	 * @param selection		The new selection
	 */
	public void selectionChanged(ISelection selection) {
		if (selection != null) {
			if (!selection.isEmpty()) {
				Object obj = null;
				boolean correctSelection = false;

				if (selection instanceof ITreeSelection) {
					ITreeSelection treeSelection = (ITreeSelection) selection;
					obj = treeSelection.getFirstElement();
					correctSelection = true;

					// Set corresponding selection in graph
					graphViewer.setSelection(treeSelection);
				} else if (selection instanceof IStructuredSelection) {
					IStructuredSelection graphSelection = (IStructuredSelection) selection;
					obj = graphSelection.getFirstElement();
					correctSelection = true;

					// Update current selection in graph
					graphLabelProvider.setCurrentSelection(obj);
					graphViewer.refresh();

					// Set corresponding selection in tree
					if (graphSelection.getFirstElement() instanceof EntityConnectionData) {
						// Set TreeViewer to nothing
					} else {
						this.treeViewer.setSelection(graphSelection, true);
					}
				}

				if (correctSelection) {
					// Activate the appropriate tool bar items according to the selected object
					if (obj instanceof ThreadInstance) {
						ThreadViewer.this.updateToolBarToThread();
					} else if (obj instanceof ThreadRegion) {
						ThreadRegion region = (ThreadRegion) obj;
						if (SDGWrapper.getInstance().isInSourceCode(region)) {
							if (SDGWrapper.getInstance().getParallelRegions(region).size() > 0) {
								this.updateToolBarToThreadRegionWithParallel();
							} else {
								this.updateToolBarToThreadRegionWithoutParallel();
							}
						} else {
							this.disableToolBar();
						}
					}  else if (obj instanceof ViewParallelRegion) {
						ThreadRegion region = ((ViewParallelRegion) obj).getItem();

						if (SDGWrapper.getInstance().isInSourceCode(region)) {
							this.updateToolBarToParallelRegion();
						} else {
							this.disableToolBar();
						}
					} else if (obj instanceof SDGNode) {
						SDGNode node = (SDGNode) obj;

						if (SDGWrapper.getInstance().isInSourceCode(node)) {
							this.updateToolBarToNode();
						} else {
							this.disableToolBar();
						}
					} else if (obj instanceof ViewInterferedNode) {
						SDGNode node = ((ViewInterferedNode) obj).getItem();

						if (SDGWrapper.getInstance().isInSourceCode(node)) {
							this.updateToolBarToInterferedNode();
						} else {
							this.disableToolBar();
						}
					} else if (obj instanceof EntityConnectionData) {
						EntityConnectionData connection = (EntityConnectionData) obj;
						Object tmpSource = connection.source;
						Object tmpTarget = connection.dest;

						if (tmpSource instanceof ThreadRegion && tmpTarget instanceof ThreadRegion) {
							ThreadRegion source = (ThreadRegion) tmpSource;
							ThreadRegion target = (ThreadRegion) tmpTarget;

							if (SDGWrapper.getInstance().getInterferedRegions(source).
									contains(target)) {
								this.updateToolBarToInterference();
							}
						}
					} else {
						this.disableToolBar();
					}
				}
			} else {
				// Clears selection
				this.disableToolBar();
				graphLabelProvider.setCurrentSelection(null);

				graphViewer.setSelection(new StructuredSelection(new Object()));
				// Set TreeViewer to nothing
				graphViewer.refresh();
			}
		}
	}

	/**
	 * Centers the plug-in's edu.kit.joana.ui.ifc.sdg.threadviewer.view to the given object.
	 *
	 * @param obj	The object to be centered on
	 */
	public void centerView(Object obj) {
		ISelection selection = null;

		if (obj instanceof ViewParallelRegion) {
			ThreadRegion region = ((ViewParallelRegion) obj).getItem();
			selection = new StructuredSelection(region);
		} else if (obj instanceof ViewInterferedNode) {
			SDGNode node = ((ViewInterferedNode) obj).getItem();
			Object[] regions = SDGWrapper.getInstance().getRegions(node).toArray();
			selection = new StructuredSelection(regions);
		}

	    treeViewer.setSelection(selection, true);
	    graphViewer.setSelection(selection, true);
		graphViewer.refresh();
		setFocus();
	}

	/**
	 * Sets the ThreadViewer to the given item.
	 *
	 * @param obj	The item to be set to
	 */
	public void setSelectedObject(Object obj) {
		ISelection selection = new StructuredSelection(obj);

		if (obj instanceof SDGNode) {
			tabs.setSelection(TREE_TAB);
			treeViewer.expandToLevel(TREE_LEVEL_INTERFERING_NODES);
		}

	    treeViewer.setSelection(selection, true);
	    graphViewer.setSelection(selection, true);
	    selectionChanged(selection);
		setFocus();
	}

	/**
	 * Sets ThreadViewer to the items parallel to the given item.
	 *
	 * @param obj	The item whose parallel items are to be set to
	 */
	public void setParallelObjects(Object obj) {
		ISelection selection = null;

		if (obj instanceof ThreadRegion) {
			ThreadRegion region = (ThreadRegion) obj;
			selection = new StructuredSelection(SDGWrapper.getInstance().getParallelRegions(region).toArray());
		} else if (obj instanceof ViewParallelRegion) {
			ThreadRegion region = ((ViewParallelRegion) obj).getItem();
			selection = new StructuredSelection(SDGWrapper.getInstance().getParallelRegions(region).toArray());
		} else if (obj instanceof SDGNode) {
			SDGNode node = (SDGNode) obj;
			Object[] nodes = SDGWrapper.getInstance().getInterferedNodes(node).toArray();
			selection = new StructuredSelection(nodes);

			// Set tree viewer
			treeViewer.expandToLevel(TREE_LEVEL_INTERFERING_NODES);
			tabs.setSelection(TREE_TAB);
		} else if (obj instanceof ViewInterferedNode) {
			SDGNode node = ((ViewInterferedNode) obj).getItem();
			Object[] nodes = SDGWrapper.getInstance().getInterferedNodes(node).toArray();
			selection = new StructuredSelection(nodes);

			// Set tree viewer
			treeViewer.expandToLevel(TREE_LEVEL_INTERFERING_NODES);
		}

	    treeViewer.setSelection(selection, true);
	    graphViewer.setSelection(selection, true);
		graphViewer.refresh();
		setFocus();
	}

	/**
	 * Gets the currently selected object.
	 *
	 * @return	The selected object
	 */
	public Object getSelectedObject() {
	    int currentTab = tabs.getSelectionIndex();

	    if (currentTab == TREE_TAB) {
	    	ISelection selection = treeViewer.getSelection();

			if (selection instanceof ITreeSelection) {
				ITreeSelection treeSelection = (ITreeSelection) selection;
				Object obj = treeSelection.getFirstElement();
				return obj;
			}
	    } else if (currentTab == GRAPH_TAB) {
			ISelection selection = graphViewer.getSelection();

			if (selection instanceof IStructuredSelection) {
				IStructuredSelection graphSelection = (IStructuredSelection) selection;
				Object obj = graphSelection.getFirstElement();
				return obj;
			}
	    }

		return null;
	}

	/**
	 * Updates the "source code" filter.
	 */
	public void updateSourceCodeFilter() {
		if (hideSourcecodeThreadRegionAction.isChecked()) {
			treeViewer.addFilter(hideSourceCodeThreadRegionFilter);
			graphViewer.addFilter(hideSourceCodeThreadRegionFilter);

			hideNonSourceCodeButton.setSelection(true);
			sourcecodeSorterButton.setEnabled(false);
			alphabeticalSorterButton.setEnabled(false);
		} else {
			treeViewer.removeFilter(hideSourceCodeThreadRegionFilter);
			graphViewer.removeFilter(hideSourceCodeThreadRegionFilter);

			hideNonSourceCodeButton.setSelection(false);

			if (tabs.getSelectionIndex() == TREE_TAB) {
				sourcecodeSorterButton.setEnabled(true);
				alphabeticalSorterButton.setEnabled(true);
			}
		}

		treeViewer.refresh();
		graphViewer.refresh();

		// Redraw graph
		ISelection saveSelection = graphViewer.getSelection();
		graphViewer.setInput(new Object());
		graphViewer.setSelection(saveSelection);
	}

	/**
	 * Updates the "not interfering" filter.
	 */
	public void updateNotInterferingFilter() {
		if (hideNotInterferingThreadRegionAction.isChecked()) {
			treeViewer.addFilter(hideNotInterferingThreadRegionFilter);
			graphViewer.addFilter(hideNotInterferingThreadRegionFilter);

			hideNotInterferingButton.setSelection(true);
		} else {
			treeViewer.removeFilter(hideNotInterferingThreadRegionFilter);
			graphViewer.removeFilter(hideNotInterferingThreadRegionFilter);

			hideNotInterferingButton.setSelection(false);
		}

		treeViewer.refresh();
		graphViewer.refresh();

		// Redraw graph
		ISelection saveSelection = graphViewer.getSelection();
		graphViewer.setInput(new Object());
		graphViewer.setSelection(saveSelection);

	}

	/**
	 * Updates the "hide interprocedural edges" filter.
	 */
	public void updateInterproceduralEdgesFilter() {
		if (hideInterproceduralEdgesAction.isChecked()) {
			hideInterproceduralEdgesButton.setSelection(true);
		} else {
			hideInterproceduralEdgesButton.setSelection(false);
		}

		treeViewer.refresh();
		graphViewer.refresh();

		// Redraw graph
		ISelection saveSelection = graphViewer.getSelection();
		graphViewer.setInput(new Object());
		graphViewer.setSelection(saveSelection);
	}

	/**
	 * Return the current state of the "hide interprocedural edges" filter.
	 */
	public boolean isFilterHideInterproceduralEdgesSet() {
		return hideInterproceduralEdgesAction.isChecked();
	}

	/**
	 * Checks if the "source code" filter is set.
	 *
	 * @return True if the filter is set
	 */
	public boolean isSourceCodeFilterSet() {
		return hideSourcecodeThreadRegionAction.isChecked();
	}

	/**
	 * Checks if the "not interfering" filter is set.
	 *
	 * @return True if the filter is set
	 */
	public boolean isInterferingFilterSet() {
		return hideNotInterferingThreadRegionAction.isChecked();
	}

	/**
	 * Updates the tree sorter.
	 *
	 * @param action	The action causing the update
	 */
	public void updateTreeSorter(Action action) {
		if (action == alphabeticalAction) {
			sourcecodeThreadRegionAction.setChecked(!alphabeticalAction.isChecked());
			if (action.isChecked()) {
				treeViewer.setSorter(alphabeticalSorter);
			} else {
				treeViewer.setSorter(sourcecodeThreadRegionSorter);
			}
		} else {
			alphabeticalAction.setChecked(!sourcecodeThreadRegionAction.isChecked());
			if (action.isChecked()) {
				treeViewer.setSorter(sourcecodeThreadRegionSorter);
			} else {
				treeViewer.setSorter(alphabeticalSorter);
			}
		}

		sourcecodeSorterButton.setSelection(alphabeticalSorterButton.getSelection());
		alphabeticalSorterButton.setSelection(!sourcecodeSorterButton.getSelection());
		treeViewer.refresh();
	}


	/* Helper methods */

	private void hookContextMenuTree() {
		menuMgrView = new MenuManager("#PopupMenu");
		menuMgrView.setRemoveAllWhenShown(true);

		menuMgrView.add(contextmenuFollowParallelRegionsAction);
		menuMgrView.add(contextmenuFollowThreadRegionAction);
		menuMgrView.add(contextmenuFollowThreadAction);
		menuMgrView.add(contextmenuCenterParallelRegionAction);
		menuMgrView.add(contextmenuFollowNodeAction);
		menuMgrView.add(contextmenuFollowInterferedNodesAction);
		menuMgrView.add(contextmenuCenterInterferedNodeAction);
		menuMgrView.add(new Separator());
		menuMgrView.add(contextmenuZoomItem);

		// Other plug-ins can contribute there edu.kit.joana.ui.ifc.sdg.threadviewer.actions here
		menuMgrView.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuMgrView.update(true);

		menuMgrView.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				Object obj = ThreadViewer.this.getSelectedObject();

				// Show the appropriate menu items according to the selected object
				if (obj != null) {
					if (obj instanceof ThreadInstance) {
						manager.add(ThreadViewer.this.contextmenuFollowThreadAction);
					} else if (obj instanceof ThreadRegion) {
						ThreadRegion region = (ThreadRegion) obj;

						if (SDGWrapper.getInstance().isInSourceCode(region)) {
							manager.add(ThreadViewer.this.contextmenuFollowThreadRegionAction);

							if (SDGWrapper.getInstance().getParallelRegions(region).size() > 0) {
								manager.add(ThreadViewer.this.contextmenuFollowParallelRegionsAction);
							}
						}
					} else if (obj instanceof ViewParallelRegion) {
						ThreadRegion region = (ThreadRegion) (((ViewParallelRegion) obj).getItem());

						if (SDGWrapper.getInstance().isInSourceCode(region)) {
							manager.add(ThreadViewer.this.contextmenuCenterParallelRegionAction);
							manager.add(ThreadViewer.this.contextmenuFollowThreadRegionAction);
							manager.add(ThreadViewer.this.contextmenuFollowParallelRegionsAction);
						}
					} else if (obj instanceof SDGNode) {
						SDGNode node = (SDGNode) obj;

						if (SDGWrapper.getInstance().isInSourceCode(node)) {
							manager.add(ThreadViewer.this.contextmenuFollowNodeAction);
							manager.add(ThreadViewer.this.contextmenuFollowInterferedNodesAction);
						}
					} else if (obj instanceof ViewInterferedNode) {
						SDGNode node = ((ViewInterferedNode) obj).getItem();

						if (SDGWrapper.getInstance().isInSourceCode(node)) {
							manager.add(ThreadViewer.this.contextmenuCenterInterferedNodeAction);
							manager.add(ThreadViewer.this.contextmenuFollowNodeAction);
							manager.add(ThreadViewer.this.contextmenuFollowInterferedNodesAction);
						}
					}
				}
			}
		});

		Menu menuTreeViewer = menuMgrView.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menuTreeViewer);
		getSite().registerContextMenu(menuMgrView, treeViewer);
	}

	private void hookContextMenuGraph() {
		menuMgrGraph = new MenuManager("#PopupMenu");
		menuMgrGraph.setRemoveAllWhenShown(true);

		menuMgrGraph.add(contextmenuFollowParallelRegionsAction);
		menuMgrGraph.add(contextmenuFollowThreadRegionAction);
		menuMgrGraph.add(contextmenuFollowThreadAction);
		menuMgrGraph.add(contextmenuFollowInterference);

		// Other plug-ins can contribute there edu.kit.joana.ui.ifc.sdg.threadviewer.actions here
		menuMgrGraph.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menuMgrGraph.update(true);

		menuMgrGraph.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				Object obj = ThreadViewer.this.getSelectedObject();

				// Show the appropriate menu items according to the selected object
				if (obj != null) {
					if (obj instanceof ThreadInstance) {
						manager.add(ThreadViewer.this.contextmenuFollowThreadAction);
					} else if (obj instanceof ThreadRegion) {
						ThreadRegion region = (ThreadRegion) obj;

						if (SDGWrapper.getInstance().isInSourceCode(region)) {
							manager.add(ThreadViewer.this.contextmenuFollowThreadRegionAction);

							if (SDGWrapper.getInstance().getParallelRegions(region).size() > 0) {
								manager.add(ThreadViewer.this.contextmenuFollowParallelRegionsAction);
							}
						} else {
							Controller.getInstance().updateStatusBar("Selection is not part of loaded .pdg-file.");
						}
					} else if (obj instanceof EntityConnectionData) {
						EntityConnectionData connection = (EntityConnectionData) obj;
						Object tmpSource = connection.source;
						Object tmpTarget = connection.dest;

						if (tmpSource instanceof ThreadRegion && tmpTarget instanceof ThreadRegion) {
							ThreadRegion source = (ThreadRegion) tmpSource;
							ThreadRegion target = (ThreadRegion) tmpTarget;

							if (SDGWrapper.getInstance().getInterferedRegions(source).
									contains(target)) {
								manager.add(ThreadViewer.this.contextmenuFollowInterference);
							}
						}
					}
				}

				// Add zoom
				manager.add(new Separator());
				manager.add(contextmenuZoomItem);
			}
		});

		Menu menuGraphViewer = menuMgrGraph.createContextMenu(graphViewer.getControl());
		graphViewer.getControl().setMenu(menuGraphViewer);
		getSite().registerContextMenu(menuMgrGraph, graphViewer);
	}

	private void contributeToToolBar() {
		IActionBars bars = getViewSite().getActionBars();
		toolbarManager = bars.getToolBarManager();

		toolbarManager.add(toolbarFollowThreadAction);
		toolbarManager.add(new Separator());
		toolbarManager.add(toolbarCenterParallelRegionAction);
		toolbarManager.add(toolbarFollowThreadRegionAction);
		toolbarManager.add(toolbarFollowParallelRegionsAction);
		toolbarManager.add(new Separator());
		toolbarManager.add(toolbarCenterInterferedNodeAction);
		toolbarManager.add(toolbarFollowNodeAction);
		toolbarManager.add(toolbarFollowInterferedNodesAction);

		this.disableToolBar();

		// Other plug-ins can contribute there edu.kit.joana.ui.ifc.sdg.threadviewer.actions here
		toolbarManager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void updateToolBarToThread() {
		this.disableToolBar();
		toolbarFollowThreadAction.setEnabled(true);
	}

	private void updateToolBarToThreadRegionWithParallel() {
		this.disableToolBar();
		toolbarFollowParallelRegionsAction.setEnabled(true);
		toolbarFollowThreadRegionAction.setEnabled(true);
	}

	private void updateToolBarToThreadRegionWithoutParallel() {
		this.disableToolBar();
		toolbarFollowThreadRegionAction.setEnabled(true);
	}

	private void updateToolBarToParallelRegion() {
		this.disableToolBar();
		toolbarCenterParallelRegionAction.setEnabled(true);
		toolbarFollowParallelRegionsAction.setEnabled(true);
		toolbarFollowThreadRegionAction.setEnabled(true);
	}

	private void updateToolBarToNode() {
		this.disableToolBar();
		toolbarFollowNodeAction.setEnabled(true);
		toolbarFollowInterferedNodesAction.setEnabled(true);
	}

	private void updateToolBarToInterferedNode() {
		this.disableToolBar();
		toolbarCenterInterferedNodeAction.setEnabled(true);
		toolbarFollowNodeAction.setEnabled(true);
		toolbarFollowInterferedNodesAction.setEnabled(true);
	}

	private void updateToolBarToInterference() {
		this.disableToolBar();
		toolbarFollowInterferedNodesAction.setEnabled(true);
	}

	private void disableToolBar() {
		toolbarFollowThreadAction.setEnabled(false);

		toolbarCenterParallelRegionAction.setEnabled(false);
		toolbarFollowParallelRegionsAction.setEnabled(false);
		toolbarFollowThreadRegionAction.setEnabled(false);

		toolbarCenterInterferedNodeAction.setEnabled(false);
		toolbarFollowNodeAction.setEnabled(false);
		toolbarFollowInterferedNodesAction.setEnabled(false);
	}

	private void createTriangleMenu() {
		rootTriangleMenuManager = getViewSite().getActionBars().getMenuManager();
		rootTriangleMenuManager.setRemoveAllWhenShown(true);
		rootTriangleMenuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager mgr) {
				mgr.add(toolbarZoomItem);
			}
		});
		rootTriangleMenuManager.add(toolbarZoomItem);
	}
}
