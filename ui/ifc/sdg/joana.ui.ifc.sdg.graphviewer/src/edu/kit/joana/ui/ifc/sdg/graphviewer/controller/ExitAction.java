/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)ExitAction.java
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

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class ExitAction extends AbstractGVAction {
	private static final long serialVersionUID = 552566003241035414L;

    private final GraphViewerModel model;

	/**
     * Constructs a new <code>ExitAction</code> object.
     */
    public ExitAction(GraphViewerModel model) {
        super("exit.name", "exit.description", "exit");
        this.model = model;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
    	model.exit();
    }
}
