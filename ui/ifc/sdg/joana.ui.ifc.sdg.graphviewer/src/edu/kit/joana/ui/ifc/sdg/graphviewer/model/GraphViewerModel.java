/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)GraphViewerModel.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 26.11.2004 at 16:13:27
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.model;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.event.EventListenerList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.GraphViewerModelEvent;
import edu.kit.joana.ui.ifc.sdg.graphviewer.controller.event.GraphViewerModelListener;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.SDGUtils;


/**
 * This class manages lists of all currently opened graphs. It distinguishes
 * between graphs of methods and graphs of a whole program (that have only nodes
 * for each contained method). Here graphs are represented still in JGraphT
 * format.
 *
 * An instance of this class is included in the instance of the main class
 * <code>GraphViewer</code>.
 *
 * @author <a href="mailto:wellner@fmi.uni-passau.de">Tobias Wellner </a>, <a
 *         href="mailto:westerhe@fmi.uni-passau.de">Marieke Westerheide </a>
 * @version 1.1
 */
public class GraphViewerModel {
	private final transient EventListenerList listenersList = new EventListenerList();

	public void removeAllGraphs() {
		GraphViewerModelEvent evt = new GraphViewerModelEvent(this, GraphViewerModelEvent.ALL_GRAPHS_REMOVED, null);
		fireGraphModelChanged(evt);
	}

	public void addGraphViewerModelListener(GraphViewerModelListener listener) {
		listenersList.add(GraphViewerModelListener.class, listener);
	}

	public void removeGraphViewerModelListener(GraphViewerModelListener listener) {
		listenersList.remove(GraphViewerModelListener.class, listener);
	}

	/**
	 * Notify all listeners that have registered interest for notification on
	 * this event type. Calls
	 * {@link edu.kit.joana.ui.ifc.sdg.graphviewer.view.GraphPane#graphViewerModelChanged(GraphViewerModelEvent event)}
	 * which displays the graph on the screen.
	 *
	 * @see javax.swing.event.EventListenerList
	 *      javax.swing.event.EventListenerList
	 */
	protected void fireGraphModelChanged(GraphViewerModelEvent evt) {// TODO: listeners untersuchen
		Object[] listeners = this.listenersList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == GraphViewerModelListener.class) {
				((GraphViewerModelListener) listeners[i + 1]).graphViewerModelChanged(evt);
			}
		}
	}



	/* exit */ // TODO: dispose auf allen views aufrufen
	public void exit() {
		System.exit(0);
	}

	/* parametergraphen kollabieren */
	public void createPDG(MethodGraph methodSDG) {
		try {
			MethodGraph newMethodSDG = CollapseParameterGraphs.collapse(methodSDG);
			GraphViewerModelEvent evt = new GraphViewerModelEvent(this, GraphViewerModelEvent.METHOD_GRAPH_ADDED, newMethodSDG);
			fireGraphModelChanged(evt);

		} catch (Exception e) { // TODO fehlerbehandlung
			System.out.println("createPDG failed!!");
			e.printStackTrace();
		}
	}

	/* knoten ausblenden */
	/**
	 * Parses the file to construct an SDG graph instance. This graph is
	 * iterated and ENTRY nodes and their relations are filtered out. Finally
	 * the graph and the filtered graph are included in the GraphViewerModel
	 * instance.
	 */
	public void hideNode(CallGraph callGraph, String regexp) {
		SDG call = callGraph.getSDG();
		SDG truncatedCallGraph = SDGUtils.truncatedCallGraph(call, regexp);
		CallGraph cg = new CallGraph(truncatedCallGraph, callGraph.getCompleteSDG());
		GraphViewerModelEvent evt = new GraphViewerModelEvent(this, GraphViewerModelEvent.CALL_GRAPH_ADDED, cg);
		fireGraphModelChanged(evt);
	}

	/* open PDG */
    public void openPDG(Graph g, int proc) {
        SDG completeSDG = g.getCompleteSDG();
        MethodGraph mg = new MethodGraph(completeSDG, proc);
//        addMethodGraph(methodGraph);
        GraphViewerModelEvent evt = new GraphViewerModelEvent(this, GraphViewerModelEvent.METHOD_GRAPH_ADDED, mg);
		fireGraphModelChanged(evt);
    }

    /* open SDG */
    /**
	 * Parses the file to construct an SDG graph instance. This graph is
	 * iterated and ENTRY nodes and their relations are filtered out. Finally
	 * the graph and the filtered graph are included in the GraphViewerModel
	 * instance.
	 */
	public void openSDG(File file) {
		try {
			// this SDG shall contain the filtered graph
			SDG sdg = SDG.readFrom(new FileReader(file));
			SDG callGraph = SDGUtils.callGraph(sdg);
//			setPdgAbsolutePath(file.getParent());
			CallGraph cg = new CallGraph(callGraph, sdg);
			GraphViewerModelEvent evt = new GraphViewerModelEvent(this, GraphViewerModelEvent.CALL_GRAPH_ADDED, cg);
			fireGraphModelChanged(evt);

		} catch (IOException e) {
			e.printStackTrace(); // TODO fehlerbehandlung
		}
	}
}
