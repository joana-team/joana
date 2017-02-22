/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)OpenMethodAction.java
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

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class OpenMethodAction extends AbstractGVAction {
	private static final long serialVersionUID = 5977248876910666801L;

	protected int proc = -1;
    private final GraphViewerModel model;
    private Graph g;

    /**
     * Constructs a new <code>OpenMethodAction</code> object.
     */
    public OpenMethodAction(GraphViewerModel model) {
        super("openMethod.name", "openMethod.description");
        this.model = model;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
    	model.openPDG(g, proc);
//    	return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//                new Resource(COMMANDS_BUNDLE, "openMethod.success.status"));
    }

    public void setProc(int proc) {
        this.proc = proc;
    }

    public void setGraph(Graph g) {
    	this.g = g;
    }
}
