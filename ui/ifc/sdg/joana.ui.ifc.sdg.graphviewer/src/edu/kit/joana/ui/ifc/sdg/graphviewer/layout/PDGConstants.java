/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * PDGConstants.java
 *
 * Created on 29. August 2005, 10:13
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.Edge;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.Port;

/**
 * This class sets and gets the attributes of JGraph elements.
 * @author Siegfried Weber
 */
public class PDGConstants extends GraphConstants {

    /**
     * key for an ID
     */
    public static final String ID = "id";
    /**
     * key for the kind of a vertex
     */
    public static final String KIND = "kind";
	/**
	 * key for the edgeColors
	 */
	public static final String EDGE_COLOR = "edgeColor";
    /**
     * key for loop edges
     */
    public static final String LOOP_EDGES = "loopEdges";
    /**
     * key for the ports at the bottom of a node with a data dependant edge
     */
    public static final String LOWER_ORTHO_PORTS = "lowerOrthoPorts";

    /**
     * key for the ports at the top of a node with a data dependant edge
     */
    public static final String UPPER_ORTHO_PORTS = "upperOrthoPorts";

    /**
     * key for the opposite node
     */
    public static final String OPPOSITE_NODE = "oppositeNode";

    /**
     * key for a link node
     */
    public static final String LINK_NODE = "linkNode";

    /**
     * key for a tool tip
     */
    public static final String TOOL_TIP = "toolTip";

    /**
     * key for a proc number
     */
    public static final String PROC = "proc";

    /**
     * key for a loop target
     */
    public static final String LOOP_TARGET = "loopTarget";

    /**
     * key for a turned around edge
     */
    public static final String TURNED = "turned";

    /**
     * Returns an ID.
     * @param map an attribute map
     * @return an ID
     */
    public static final int getID(Map<?, ?> map) {
        Integer id = (Integer) map.get(ID);
        if(id != null)
            return id;
        return -1;
    }

    /**
     * Sets an ID.
     * @param map an attribute map
     * @param id an ID
     */
    public static final void setID(Map<String, Integer> map, int id) {
        map.put(ID, id);
    }

    /**
     * Gets the kind of an edge.
     * @param map an attribute map
     * @return the kind of an edge
     */
    public static final String getKind(Map<?, ?> map) {
        return (String) map.get(KIND);
    }

    /**
     * Sets the kind of an edge.
     * @param map an attribute map
     * @param kind the kind of an edge
     */
    public static final void setKind(Map<String, String> map, String kind) {
        map.put(KIND, kind);
    }

    /**
     * Gets the loop edges.
     * @param map an attribute map
     * @return the loop edges
     */
    public static final List<Edge> getLoopEdges(Map<String, List<Edge>> map) {
        List<Edge> edges = map.get(LOOP_EDGES);
        if(edges == null)
            return new LinkedList<Edge>();
        return edges;
    }

    /**
     * Adds a loop edge.
     * @param map an attribute map
     * @param edge a loop edge
     */
    public static final void addLoopEdge(Map<String, List<Edge>> map, Edge edge) {
        List<Edge> edges = map.get(LOOP_EDGES);
        if(edges == null) {
            edges = new LinkedList<Edge>();
            map.put(LOOP_EDGES, edges);
        }
        edges.add(edge);
    }

    /**
     * Gets the ports at the bottom of a node with a data dependant edge.
     * @param map an attribute map
     * @return a list of ports
     */
    public static final List<Port> getLowerOrthoPorts(Map<String, List<Port>> map) {
        List<Port> ports = map.get(LOWER_ORTHO_PORTS);
        if(ports == null)
            return new LinkedList<Port>();
        return ports;
    }

    /**
     * Adds a port at the bottom of a node with a data dependant edge.
     * @param map an attribute map
     * @param port a port
     */
    public static final void addLowerOrthoPorts(Map<String, List<Port>> map, Port port) {
        List<Port> ports = map.get(LOWER_ORTHO_PORTS);
        if(ports == null) {
            ports = new LinkedList<Port>();
            map.put(LOWER_ORTHO_PORTS, ports);
        }
        ports.add(port);
    }

    /**
     * Gets the ports at the top of a node with a data dependant edge.
     * @param map an attribute map
     * @return a list of ports
     */
    public static final List<Port> getUpperOrthoPorts(Map<String, List<Port>> map) {
        List<Port> ports = map.get(UPPER_ORTHO_PORTS);
        if(ports == null)
            return new LinkedList<Port>();
        return ports;
    }

    /**
     * Adds a port at the top of a node with a data dependant edge.
     * @param map an attribute map
     * @param port a port
     */
    public static final void addUpperOrthoPorts(Map<String, List<Port>> map, Port port) {
        List<Port> ports = map.get(UPPER_ORTHO_PORTS);
        if(ports == null) {
            ports = new LinkedList<Port>();
            map.put(UPPER_ORTHO_PORTS, ports);
        }
        ports.add(port);
    }

    /**
     * Gets the opposite node of the port.
     * @param map an attribute map
     * @return the opposite node of the port
     */
    public static final Node getOppositeNode(Map<?, ?> map) {
        return (Node) map.get(OPPOSITE_NODE);
    }

    /**
     * Sets the opposite node of the port.
     * @param map an attribute map
     * @param node the opposite node of the port
     */
    public static final void setOppositeNode(Map<String, Node> map, Node node) {
        map.put(OPPOSITE_NODE, node);
    }

    /**
     * Returns whether the node is a link node.
     * @param map an attribute map
     * @return true if the node is a link node
     */
    public static final boolean isLinkNode(Map<?, ?> map) {
        Object link = map.get(LINK_NODE);
        if(link == null)
            return false;
        return (Boolean) link;
    }

    /**
     * Sets whether the node is a link node.
     * @param map an attribute map
     * @param link true if the node is a link node
     */
    public static final void setLinkNode(Map<String, Boolean> map, boolean link) {
        map.put(LINK_NODE, link);
    }

    /**
     * Returns the tool tip of the node.
     * @param map an attribute map
     * @return the tool tip
     */
    public static final String getToolTip(Map<?, ?> map) {
        return (String) map.get(TOOL_TIP);
    }

    /**
     * Sets the tool tip of the node.
     * @param map an attribute map
     * @param toolTip a tool tip
     */
    public static final void setToolTip(Map<String, String> map, String toolTip) {
        map.put(TOOL_TIP, toolTip);
    }

    /**
     * Returns the proc number.
     * @param map an attribute map
     * @return the proc number
     */
    public static final int getProc(Map<?, ?> map) {
        Integer proc = (Integer) map.get(PROC);
        if(proc != null)
            return proc;
        return -1;
    }

    /**
     * Sets the proc number.
     * @param map an attribute map
     * @param proc the proc number
     */
    public static final void setProc(Map<String, Integer> map, int proc) {
        map.put(PROC, proc);
    }

    /**
     * Returns the loop target port.
     * @param map an attribute map
     * @return the loop target port
     */
    public static final DefaultPort getLoopTarget(Map<?, ?> map) {
        return (DefaultPort) map.get(LOOP_TARGET);
    }

    /**
     * Sets loop target port.
     * @param map an attribute map
     * @param target the loop target port
     */
    public static final void setLoopTarget(Map<String, DefaultPort> map, DefaultPort target) {
        map.put(LOOP_TARGET, target);
    }
}
