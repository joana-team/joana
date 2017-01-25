/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)MainFrame.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 28.10.2004 at 15:35:14
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.ActionMap;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AboutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AdjustmentsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AllPredsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AllSuccsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CloseAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CloseAllAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CollapseAllCallNodesAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CollapseCallNodeAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CombiAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ControlDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ControllFlussAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.DataDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ExitAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ExportAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.HeapDataDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.HideNodeAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.HighlightMainAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.InterferenceDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.LookupAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ManualAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OpenAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OpenMethodAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OrderedTreeLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ParamStructDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.PredAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SearchAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SpringLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SuccAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SugiyamaLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.TreeLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomInAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomOutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Translator;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.Debug;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.GVUtilities;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFrame;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.pdg.PDGViewFactory;

/**
 * This class constitutes the main frame of the graph viewer.
 *
 * It is derived from JFrame. A Frame is a top-level window with a titlebar and
 * a border.
 *
 * The content pane, graphPane, contains all the non-menu components displayed
 * by the JFrame.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public class MainFrame extends GVFrame implements ContainerListener, BundleConstants {
	private static final long serialVersionUID = -3751361992525158750L;
	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;

	private MenuBar menuBar;
	private GraphPane graphPane;
	private StatusBar statusBar;

	/**
	 * a dialog where the user can make adjustments (concerning language and
	 * colour so far)
	 */
	private AdjustmentsDialog adjustmentDialog;
	/**
	 * a dialog where the user can search for vertex numbers,text and regular
	 * expreesions
	 */
	private SearchDialog searchDialog;
	private LookupDialog lookupDialog;
	private HideNodeDialog hideNodeDialog;
	/**
	 * <code>ActionMap</code> provides mappings from Objects (called keys or
	 * Action names) to Actions. <code>An ActionMap</code> is usually used with
	 * an <code>InputMap</code> to locate a particular action when a key is
	 * pressed. As with <code>InputMap</code>, an <code>ActionMap</code> can
	 * have a parent that is searched for keys not defined in the
	 * <code>ActionMap</code>.
	 */
	protected ActionMap actions = new ActionMap();

	/**
	 * Manages lists of all currently opened graphs.
	 */
	protected GraphViewerModel model;

//	protected CommandManager commandManager = null;

	/**
	 * Constructs a new <code>MainFrame</code> object. Initializes the menu bar,
	 * tool bar, status bar and prepares the adjustments dialog.
	 */
	public MainFrame(Translator translator, GraphViewerModel model) {
		// language support
		super(translator, new Resource(MAIN_FRAME_BUNDLE, "program.title"));

		Debug.print("MainFrame.MainFrame()");
		this.addWindowListener(new CloseWindows());
		this.model = model;

		this.adjustmentDialog = new AdjustmentsDialog(this);
		this.searchDialog = new SearchDialog(this);
		this.lookupDialog = new LookupDialog(this);
		this.hideNodeDialog = new HideNodeDialog(this);

		this.graphPane = new GraphPane(this, adjustmentDialog, searchDialog,
				lookupDialog, hideNodeDialog);

		this.initActions();
		this.initComponents();
		this.initFrame();

		searchDialog.setGraphPane(graphPane);
	}

	private void initComponents() {

		Debug.print("MainFrame.initComponents()");

		menuBar = new MenuBar(this);
		this.setJMenuBar(menuBar);

		this.getContentPane().add(new ToolBar(this), BorderLayout.NORTH);

		this.statusBar = new StatusBar(this);
		this.getContentPane().add(this.statusBar, BorderLayout.SOUTH);

		this.graphPane.addMouseListener(this.statusBar);
		this.getContentPane().add(this.graphPane, BorderLayout.CENTER);

		this.model.addGraphViewerModelListener(this.graphPane);

		// tab for language settings (will appear in adjustmentDialog)
		if (this.translator instanceof Adjustable) {
			this.adjustmentDialog.addAdjustable((Adjustable) this.translator);
		}
		// tab for adjustments of vertex colour (will appear in
		// adjustmentDialog)
		PDGViewFactory pdgTab = PDGViewFactory.getInstance();
		this.adjustmentDialog.addAdjustable(pdgTab);
		// listener to also refresh displayed graphs
		pdgTab.addRefreshViewListener(this.graphPane);
	}

	private void initFrame() {
		Debug.print("MainFrame.initFrame()");

		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
//		this.addWindowListener(new WindowAdapter() {
//			@Override
//			public void windowClosing(WindowEvent event) {
//				commandManager.invoke(new ExitCommand((MainFrame) event
//						.getWindow(), null));
//			}
//		});
		this.setIconImage(GVUtilities.getIcon("GraphViewer.png").getImage());
		this.setSize(new Dimension(WIDTH, HEIGHT));
		this.enableActions(false);
		this.addMouseListener(this.statusBar);

	}

	// fill the ActionMap
	private void initActions() {

		Debug.print("MainFrame.initActions()");

		OpenAction open = new OpenAction(model, this);
		actions.put(OpenAction.class, open);

		CloseAction close = new CloseAction(graphPane);
		actions.put(CloseAction.class, close);

		CloseAllAction closeAll = new CloseAllAction(model);
		actions.put(CloseAllAction.class, closeAll);

		ExitAction exit = new ExitAction(model);
		actions.put(ExitAction.class, exit);

		ZoomInAction zoomIn = new ZoomInAction(graphPane);
		actions.put(ZoomInAction.class, zoomIn);

		ZoomOutAction zoomOut = new ZoomOutAction(graphPane);
		actions.put(ZoomOutAction.class, zoomOut);

		AdjustmentsAction adjustments = new AdjustmentsAction(adjustmentDialog);
		actions.put(AdjustmentsAction.class, adjustments);

		ManualAction manual = new ManualAction(this);
		actions.put(ManualAction.class, manual);

		AboutAction about = new AboutAction(this);
		actions.put(AboutAction.class, about);

		OpenMethodAction openMethod = new OpenMethodAction(model);
		actions.put(OpenMethodAction.class, openMethod);

		ExportAction export = new ExportAction(this, graphPane);
		actions.put(ExportAction.class, export);

		SearchAction search = new SearchAction(searchDialog);
		actions.put(SearchAction.class, search);

		ControllFlussAction hide = new ControllFlussAction(graphPane);
		actions.put(ControllFlussAction.class, hide);

		ControlDependencyAction controlDep= new ControlDependencyAction(graphPane);
		actions.put(ControlDependencyAction.class, controlDep);

		DataDependencyAction datenDep= new DataDependencyAction(graphPane);
		actions.put(DataDependencyAction.class, datenDep);

		HeapDataDependencyAction heapDep= new HeapDataDependencyAction(graphPane);
		actions.put(HeapDataDependencyAction.class, heapDep);

		ParamStructDependencyAction pstructDep= new ParamStructDependencyAction(graphPane);
		actions.put(ParamStructDependencyAction.class, pstructDep);

		InterferenceDependencyAction interfereDep= new InterferenceDependencyAction(graphPane);
		actions.put(InterferenceDependencyAction.class, interfereDep);

		PredAction pred = new PredAction(searchDialog, graphPane);
		actions.put(PredAction.class, pred);

		SuccAction succ = new SuccAction(searchDialog, graphPane);
		actions.put(SuccAction.class, succ);

		AllPredsAction preds = new AllPredsAction(graphPane, pred);
		actions.put(AllPredsAction.class, preds);

		AllSuccsAction succs = new AllSuccsAction(graphPane, succ);
		actions.put(AllSuccsAction.class, succs);

		SugiyamaLayoutAction sugiyama = new SugiyamaLayoutAction(graphPane);
		actions.put(SugiyamaLayoutAction.class, sugiyama);

		OrderedTreeLayoutAction orderedTree = new OrderedTreeLayoutAction(graphPane);
		actions.put(OrderedTreeLayoutAction.class, orderedTree);

		TreeLayoutAction tree = new TreeLayoutAction(graphPane);
		actions.put(TreeLayoutAction.class, tree);

		SpringLayoutAction spring = new SpringLayoutAction(graphPane);
		actions.put(SpringLayoutAction.class, spring);

		LookupAction lookup = new LookupAction(lookupDialog, graphPane);
		actions.put(LookupAction.class, lookup);

		HideNodeAction hideNode = new HideNodeAction(hideNodeDialog, graphPane);
		actions.put(HideNodeAction.class, hideNode);

		CombiAction combi = new CombiAction(graphPane, model);
		actions.put(CombiAction.class, combi);

		HighlightMainAction hiMain = new HighlightMainAction(this, graphPane);
		actions.put(HighlightMainAction.class, hiMain);

		CollapseCallNodeAction coll = new CollapseCallNodeAction(searchDialog, graphPane, model);
		actions.put(CollapseCallNodeAction.class, coll);

		CollapseAllCallNodesAction collall = new CollapseAllCallNodesAction(searchDialog, graphPane, model);
		actions.put(CollapseAllCallNodesAction.class, collall);

		this.graphPane.initPopups();
	}

	/**
	 * @see java.awt.event.ContainerListener#componentAdded(java.awt.event.ContainerEvent)
	 */
	public void componentAdded(ContainerEvent event) {
		if (event.getContainer().getComponentCount() > 0) {
			this.enableActions(true);
		}
	}

	/**
	 * @see java.awt.event.ContainerListener#componentRemoved(java.awt.event.ContainerEvent)
	 */
	public void componentRemoved(ContainerEvent event) {
		if (event.getContainer().getComponentCount() == 0) {
			this.enableActions(false);
		}
	}

	private void enableActions(boolean newValue) {
		this.actions.get(CloseAction.class).setEnabled(newValue);
		this.actions.get(CloseAllAction.class).setEnabled(newValue);
		this.actions.get(ZoomInAction.class).setEnabled(newValue);
		this.actions.get(ZoomOutAction.class).setEnabled(newValue);
		this.actions.get(ExportAction.class).setEnabled(newValue);
		this.actions.get(SugiyamaLayoutAction.class).setEnabled(newValue);
		this.actions.get(OrderedTreeLayoutAction.class).setEnabled(newValue);
		this.actions.get(TreeLayoutAction.class).setEnabled(newValue);
		this.actions.get(SpringLayoutAction.class).setEnabled(newValue);
		this.actions.get(CollapseAllCallNodesAction.class).setEnabled(newValue);

	}

	/**
	 * @return the graph pane that displays the graphs
	 */
	public GraphPane getGraphPane() {
		return this.graphPane;
	}

	/**
	 * A dialog where the user can make adjustments (concerning language and
	 * colour so far).
	 *
	 * @return the adjustments dialog
	 */
	public AdjustmentsDialog getAdjustmentDialog() {

		return this.adjustmentDialog;
	}

	public SearchDialog getSearchDialog() {
		return this.searchDialog;
	}

	/**
	 * Gets the model. The model manages lists of all currently opened graphs.
	 *
	 * @return the current GraphViewerModel
	 */
	public GraphViewerModel getModel() {
		return this.model;
	}

	/**
	 * Sets the model. The model contains lists of all currently opened graphs.
	 *
	 * @param model
	 *            the current GraphViewerModel
	 */
	public void setModel(GraphViewerModel model) {
		this.model = model;
	}

	/**
	 * Gets actions. <code>ActionMap</code> provides mappings from Objects
	 * (called keys or Action names) to Actions.
	 *
	 * @return the action map
	 */
	public ActionMap getActions() {
		return this.actions;
	}

	/**
	 * Sets new actions. <code>ActionMap</code> provides mappings from Objects
	 * (called keys or Action names) to Actions.
	 */
	public void setActions(ActionMap actions) {
		this.actions = actions;
	}

	class CloseWindows extends WindowAdapter {

		@Override
		public void windowOpened(WindowEvent e) {
//			super.windowOpened(e);
////			System.out.println("opened win");
//			BufferedReader br = null;
//			String string=null;
//			try {
//				string = "./src/Recentfilelist.txt";
//				File f = new File(string);
//				br = new BufferedReader(new FileReader(f));
//				String inputLine;
//				LinkedList<String> files = new LinkedList<String>();
//				while (true) {
//					inputLine = br.readLine();
//					if (inputLine == null) {
//
//						break;
//					}
////					System.out.println("bf reader: " + inputLine);
//					files.add(inputLine);
//				}
//				model.setFiles_vec(files);
//
//			} catch (FileNotFoundException e1) {
//				System.out.println("noch keine Recentfilelist.txt");
//			} catch (IOException e1) {
//				System.out.println("keine Recentfilelist.txt, da kann nicht auslesen!");
//			}
		}

		@Override
		public void windowClosing(WindowEvent e) {
//			super.windowClosing(e);
//
//			File file = new File("./src", "Recentfilelist.txt");
//
//			BufferedWriter out = null;
//
//			try {
//				out = new BufferedWriter(new FileWriter(file));
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
//			LinkedList<String> files_vec = model.getFiles_vec();
//			for (String j : files_vec) {
//				try {
//					out.write(j + "\n");
//
//					out.flush();
//				} catch (IOException e1) {
//					e1.printStackTrace();
//				}
//			}
//
//			try {
//				out.close();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
		}
	}

	public void setLookupId(int id) {
		lookupDialog.setId(id);
	}
}
