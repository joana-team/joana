/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)ZoomOutAction.java
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jgraph.JGraph;

import edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class ZoomOutAction extends AbstractGVAction implements PropertyChangeListener {
	private static final long serialVersionUID = -4715163256504706376L;
	private static final double ZOOM_FACTOR = 0.5;
    protected static final Double MIN_SCALE = Double.valueOf(0.25);

    private final GraphPane pane;

    /**
     * Constructs a new <code>ZoomOutAction</code> object.
     */
    public ZoomOutAction(GraphPane pane) {
        super("zoomOut.name", "ZoomOut.png", "zoomOut.description", "zoomOut");
        this.pane = pane;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {
    	JGraph graph = pane.getSelectedJGraph();
    	double oldScale = graph.getScale();
        double newScale = oldScale * ZOOM_FACTOR;
        graph.setScale(newScale);
        if (graph.getSelectionCell() != null) {
            graph.scrollCellToVisible(graph.getSelectionCell());
        }
//    	return new CommandStatusEvent(this, CommandStatusEvent.SUCCESS,
//                new Resource(COMMANDS_BUNDLE,
//                        (oldScale > newScale) ? "zoomOut.success.status"
//                                : "zoomOut.success.status"));
    }
    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(JGraph.SCALE_PROPERTY)) {
            setEnabled(((Double) event.getNewValue()).compareTo(MIN_SCALE) > 0);
        }
    }

}
