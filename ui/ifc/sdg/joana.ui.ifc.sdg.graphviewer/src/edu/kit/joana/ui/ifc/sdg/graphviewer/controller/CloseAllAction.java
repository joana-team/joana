/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)CloseAllAction.java
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

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;

import java.awt.event.ActionEvent;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class CloseAllAction extends AbstractGVAction {
	private static final long serialVersionUID = 6835865443223781755L;

	private final GraphViewerModel gvm;

	/**
     * Constructs a new <code>CloseAllAction</code> object.
     */
    public CloseAllAction(GraphViewerModel gvm) {
        super("closeAll.name", "closeAll.description", "closeAll");
		this.gvm = gvm;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
        gvm.removeAllGraphs();
//        return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//                new Resource(COMMANDS_BUNDLE, "closeAll.success.status"));
    }
}
