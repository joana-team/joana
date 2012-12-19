/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * DFSDecycling.java
 *
 * Created on 13. Dezember 2005, 15:06
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jgraph.JGraph;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;

/**
 * This class implements the depth first search algorithm for decycling of
 * graphs.
 * @author Siegfried Weber
 */
public class DFSDecycling extends DecyclingStrategy {

    /**
     * estimated time
     */
    private static final int TIME = 113;
    /**
     * the attribute name for the dfsnum variable
     */
    private static final String DFSNUM = "dfsnum";
    /**
     * the attribute name for the compnum variable
     */
    private static final String COMPNUM = "compnum";

    /**
     * the time a node is entered
     */
    private int dfsnum = 0;
    /**
     * the time a node is leaved
     */
    private int compnum = 0;

    /**
     * Returns the maximum time for this graph.
     * @param graph a PDG
     * @return the maximum time
     */
    @Override
	int getMaxTime(PDG graph) {
        return TIME;
    }

    /**
     * Decycles the graph
     * @param graph a PDG
     * @return the turned around edges
     */
    @Override
	public List<DefaultEdge> decycle(PDG graph) {
        List<Node[]> fbcEdges = new LinkedList<Node[]>();
        Node root = graph.getEntryNode();
        searchCycles(graph, root, fbcEdges);
        return turnEdges(fbcEdges, graph);
    }

    /**
     * Search for cycles with the depth first search.
     * @param graph a PDG
     * @param node the current node
     * @param fbcEdges a list with forward, backward and cross edges
     */
    private void searchCycles(PDG graph, Node node, List<Node[]> fbcEdges) {
        node.setAttribute(DFSNUM, ++dfsnum);
        for(Node child : graph.getChildren(node)) {
            if(child.getAttribute(DFSNUM) == null) {
                searchCycles(graph, child, fbcEdges);
            }
            else {
                fbcEdges.add(new Node[] {node, child});
            }
        }
        node.setAttribute(COMPNUM, ++compnum);
    }

    /**
     * Turns edges around.
     * @param fbcEdges a list with forward, backward and cross edges
     * @param graph a PDG
     * @return the turned around edges
     */
    private List<DefaultEdge> turnEdges(List<Node[]> fbcEdges, PDG graph) {
        JGraph jGraph = graph.getJGraph();
        GraphLayoutCache view = jGraph.getGraphLayoutCache();
        GraphModel model = jGraph.getModel();
        List<DefaultEdge> turnedEdges = new LinkedList<DefaultEdge>();
        ConnectionSet conn = new ConnectionSet();
        for(Node[] edge : fbcEdges) {
            int v_dfsnum = (Integer) edge[0].getAttribute(DFSNUM);
            int w_dfsnum = (Integer) edge[1].getAttribute(DFSNUM);
            if(v_dfsnum > w_dfsnum) {
                int v_compnum = (Integer) edge[0].getAttribute(COMPNUM);
                int w_compnum = (Integer) edge[1].getAttribute(COMPNUM);
                if(v_compnum < w_compnum) {
                    // backward edge
                    Object[] edges = DefaultGraphModel.getEdgesBetween(model,
                            edge[0].getCell(), edge[1].getCell(), true);
                    for(Object o : edges) {
                        DefaultEdge e = (DefaultEdge) o;
                        conn.connect(e, e.getTarget(), e.getSource());
                        turnedEdges.add(e);
                    }
                }
            }
        }
        view.edit(null, conn, null, null);
        return turnedEdges;
    }

    /**
     * Recreates the cycles.
     * @param graph a PDG
     * @param turnedEdges the turned around edges
     */
    @Override
	public void undo(PDG graph, List<DefaultEdge> turnedEdges) {
        GraphLayoutCache view = graph.getJGraph().getGraphLayoutCache();
        Map<DefaultEdge, Map<?, ?>> nested = new Hashtable<DefaultEdge, Map<?, ?>>();
        ConnectionSet conn = new ConnectionSet();
        for(DefaultEdge edge : turnedEdges) {
            turnPointList(view, edge, nested);
            conn.connect(edge, edge.getTarget(), edge.getSource());
        }
        view.edit(nested, conn, null, null);
    }

    /**
     * Turns around the bend points of a edge.
     * @param view the JGraph view
     * @param edge an edge
     * @param nested a map with changes
     */
    private void turnPointList(GraphLayoutCache view, DefaultEdge edge, Map<DefaultEdge, Map<?, ?>> nested) {
        EdgeView edgeView = (EdgeView) view.getMapping(edge, false);
        List<?> oldPoints = edgeView.getPoints();
        LinkedList<Object> newPoints = new LinkedList<Object>();
        for(Object point : oldPoints)
            newPoints.addFirst(point);
        Map<?, ?> map = new Hashtable<Object, Object>();
        GraphConstants.setPoints(map, newPoints);
        nested.put(edge, map);
    }
}
