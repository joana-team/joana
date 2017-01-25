/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)AboutAction.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 13.12.2004 at 17:44:21
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller;

import java.awt.event.ActionEvent;

import edu.kit.joana.ui.ifc.sdg.graphviewer.GraphViewer;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.BundleConstants;
import edu.kit.joana.ui.ifc.sdg.graphviewer.translation.Resource;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVFrame;
import edu.kit.joana.ui.ifc.sdg.graphviewer.view.component.GVOptionPane;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class AboutAction extends AbstractGVAction implements BundleConstants {
	private static final long serialVersionUID = -6525753514144256022L;
	private GVOptionPane optionPane;

    /**
     * Constructs a new <code>AboutAction</code> object.
     */
    public AboutAction(GVFrame frame) {
        super("about.name", "About.png", "about.description", "about");
        this.optionPane = new GVOptionPane(frame);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        this.optionPane.showInformationDialog(new Resource(ACTIONS_BUNDLE,
                "about.message", "\nLast changed on " + GraphViewer.CVS_ID));
    }
}
