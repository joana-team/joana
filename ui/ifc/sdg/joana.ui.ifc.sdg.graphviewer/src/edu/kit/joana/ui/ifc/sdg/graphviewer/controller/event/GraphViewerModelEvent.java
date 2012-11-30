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
/*
 * @(c)GraphViewerModelEvent.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 26.11.2004 at 16:29:40
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event;

import edu.kit.joana.ui.ifc.sdg.graphviewer.model.Graph;
import edu.kit.joana.ui.ifc.sdg.graphviewer.model.GraphViewerModel;

import java.util.EventObject;

/**
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>
 * @version 1.0
 */
public class GraphViewerModelEvent extends EventObject {
	private static final long serialVersionUID = 7667342460809913248L;

	public static final int UNDEFINED = 0;
    public static final int ALL_GRAPHS_REMOVED = 1;
    public static final int CALL_GRAPH_ADDED = 2;
//    public static final int CALL_GRAPH_REMOVED = 3;
    public static final int METHOD_GRAPH_ADDED = 4;
//    public static final int METHOD_GRAPH_REMOVED = 5;

    protected int id = UNDEFINED;
    protected Graph graph;

    public GraphViewerModelEvent(GraphViewerModel source, int id, Graph graph) {
        super(source);
        this.id = id;
        this.graph = graph;
    }

    public int getId() {
        return this.id;
    }

    public Graph getGraph() {
        return this.graph;
    }
}
