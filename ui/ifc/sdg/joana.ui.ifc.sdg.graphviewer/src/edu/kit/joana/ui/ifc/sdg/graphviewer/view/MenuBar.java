/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)MenuBar.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 27.11.2004 at 17:12:57
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import javax.swing.JMenuBar;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AboutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AdjustmentsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AllPredsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AllSuccsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CloseAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CloseAllAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CollapseAllCallNodesAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.CombiAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ControlDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ControlFlowAction;
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
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OrderedTreeLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ParamStructDependencyAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SpringLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.SugiyamaLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.TreeLayoutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomInAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomOutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVMenu;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class MenuBar extends JMenuBar implements BundleConstants {
	private static final long serialVersionUID = 7502433280562398430L;
	protected MainFrame owner = null;

    /**
     * Constructs a new <code>MenuBar</code> object.
     */
    public MenuBar(MainFrame owner) {
        super();
        this.owner = owner;
        this.setLocale(owner.getLocale());
        this.initMenuBar();
    }

    private void initMenuBar() {
    	/* Files */
		GVMenu fileMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "file.menu"));
		this.add(fileMenu);
		fileMenu.add(this.owner.getActions().get(OpenAction.class));
//		OpenRecentFiles openRecentMenu = new OpenRecentFiles(owner, this.owner
//				.getTranslator(), new Resource(MAIN_FRAME_BUNDLE,
//				"openRecentFiles.menu"), this);
//		fileMenu.add(openRecentMenu);
		fileMenu.addSeparator();
		fileMenu.add(this.owner.getActions().get(ExportAction.class));
		fileMenu.addSeparator();
		fileMenu.add(this.owner.getActions().get(CloseAction.class));
		fileMenu.add(this.owner.getActions().get(CloseAllAction.class));
		fileMenu.addSeparator();
		fileMenu.add(this.owner.getActions().get(ExitAction.class));

		/* Configuration */
		GVMenu configMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "config.menu"));
		this.add(configMenu);
		configMenu.add(this.owner.getActions().get(AdjustmentsAction.class));


		/* View */
		GVMenu viewMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "view.menu"));
		this.add(viewMenu);
		viewMenu.add(this.owner.getActions().get(ZoomInAction.class));
		viewMenu.add(this.owner.getActions().get(ZoomOutAction.class));
		viewMenu.addSeparator();
		viewMenu.add(this.owner.getActions().get(ControlFlowAction.class));
		viewMenu.add(this.owner.getActions().get(ControlDependencyAction.class));
		viewMenu.add(this.owner.getActions().get(DataDependencyAction.class));
		viewMenu.add(this.owner.getActions().get(HeapDataDependencyAction.class));
		viewMenu.add(this.owner.getActions().get(ParamStructDependencyAction.class));
		viewMenu.add(this.owner.getActions().get(InterferenceDependencyAction.class));
		viewMenu.add(this.owner.getActions().get(HideNodeAction.class));
		viewMenu.add(this.owner.getActions().get(HighlightMainAction.class));
		viewMenu.add(this.owner.getActions().get(CombiAction.class));
		viewMenu.add(this.owner.getActions().get(CollapseAllCallNodesAction.class));

		/* Traversal */
		GVMenu travMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "trav.menu"));
		this.add(travMenu);
		travMenu.add(this.owner.getActions().get(AllPredsAction.class));
		travMenu.add(this.owner.getActions().get(AllSuccsAction.class));
		ChoppingMenu choppingMenu = new ChoppingMenu(owner, this.owner.getTranslator(),
				new Resource(MAIN_FRAME_BUNDLE, "chopping.menu"), this);
		travMenu.add(choppingMenu);
		SlicingMenu slicingMenu=new SlicingMenu(owner, this.owner.getTranslator(),
				new Resource(MAIN_FRAME_BUNDLE, "slicing.menu"), this);
		travMenu.add(slicingMenu);

		/* Layout */
		GVMenu layoutMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "lay.menu"));
		this.add(layoutMenu);
		layoutMenu.add(this.owner.getActions().get(SugiyamaLayoutAction.class));
		layoutMenu.add(this.owner.getActions().get(OrderedTreeLayoutAction.class));
		layoutMenu.add(this.owner.getActions().get(TreeLayoutAction.class));
		layoutMenu.add(this.owner.getActions().get(SpringLayoutAction.class));

		/* Lookup */
		GVMenu lookupMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "lookup.menu"));
		this.add(lookupMenu);
		lookupMenu.add(this.owner.getActions().get(LookupAction.class));

//		GVMenu hideNodeMenu = new GVMenu(this.owner.getTranslator(), new Resource(
//				MAIN_FRAME_BUNDLE, "hidenode.menu"));
//		this.add(hideNodeMenu);
////		hideNodeMenu.add(this.owner.getActions().get(CloseAction.class));
//		hideNodeMenu.add(this.owner.getActions().get(HideNodeAction.class));
//
//		GVMenu combiMenu=new GVMenu(this.owner.getTranslator(),new Resource(MAIN_FRAME_BUNDLE,"combi.menu"));
//		this.add(combiMenu);
//		combiMenu.add(this.owner.getActions().get(CombiAction.class));

		/* Help */
		GVMenu helpMenu = new GVMenu(this.owner.getTranslator(), new Resource(
				MAIN_FRAME_BUNDLE, "help.menu"));
		this.add(helpMenu);
		helpMenu.add(this.owner.getActions().get(ManualAction.class));
		helpMenu.addSeparator();
		helpMenu.add(this.owner.getActions().get(AboutAction.class));

	}
}
