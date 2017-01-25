/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)ToolBar.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 27.11.2004 at 17:09:59
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.FlowLayout;

import javax.swing.JToolBar;

import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AllPredsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.AllSuccsAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.OpenAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomInAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.ZoomOutAction;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVPanel;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVToolBar;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class ToolBar extends GVPanel implements BundleConstants {
	private static final long serialVersionUID = 1432023647651820941L;
	protected MainFrame owner = null;

	/**
	 * Constructs a new <code>ToolBar</code> object.
	 */
	public ToolBar(MainFrame owner) {
		super(owner.getTranslator(), new FlowLayout(FlowLayout.LEFT));
		this.owner = owner;
		this.initToolBar();
	}

	private void initToolBar() {

		JToolBar fileBar = new GVToolBar(this.translator, new Resource(
				MAIN_FRAME_BUNDLE, "file.menu"));
		this.add(fileBar);
		fileBar.add(this.owner.getActions().get(OpenAction.class));

		JToolBar viewBar = new GVToolBar(this.translator, new Resource(
				MAIN_FRAME_BUNDLE, "view.menu"));
		this.add(viewBar);
		viewBar.add(this.owner.getActions().get(ZoomInAction.class));
		viewBar.add(this.owner.getActions().get(ZoomOutAction.class));

		JToolBar travBar = new GVToolBar(this.translator, new Resource(
				MAIN_FRAME_BUNDLE, "trav.menu"));
		this.add(travBar);
		travBar.add(this.owner.getActions().get(AllPredsAction.class));
		travBar.add(this.owner.getActions().get(AllSuccsAction.class));

	}

}
