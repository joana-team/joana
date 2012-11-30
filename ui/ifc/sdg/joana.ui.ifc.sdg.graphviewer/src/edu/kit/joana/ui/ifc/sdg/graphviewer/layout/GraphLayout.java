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
 * GraphLayout.java
 *
 * Created on 29. September 2005, 12:39
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.ConnectionSet;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.Edge;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.Port;
import org.jgraph.graph.PortView;

/**
 * This class implements the standard post processing algorithm.
 * JGraph does not always return the correct coordinate values.
 * To prevent confusion: x and y coordinates are read from the JGraph view
 * and the width and height of nodes are read from the JGraph model.
 * Multiple loops are supported but in real PDGs a node can have only one loop.
 * @author Siegfried Weber
 */
public class GraphLayout extends LayoutStrategy {

    /**
     * the preferred distance of ports
     */
    private static final int PREFERRED_PORT_DISTANCE = 5;

    // The estimation bases on javacard.framework.JCMain.pdg: 3310.

    /**
     * estimated time to create ports
     */
    private static final int TIME_CREATE_PORTS = 444;
    /**
     * estimated time until the first commitment to JGraph
     */
    private static final int TIME_EDIT1 = 476;
    /**
     * estimated time until the second commitment to JGraph
     */
    private static final int TIME_EDIT2 = (266 + 608 + 670 + 942 + 851 + 731 +
            459 + 288 + 203 + 128) / 10;
    /**
     * estimated time until the third commitment to JGraph
     */
    private static final int TIME_EDIT3 = (261 + 88 + 108 + 107 + 97 + 173 +
            118 + 98 + 67 + 317) / 10;
    /**
     * estimated time to remove unnecessary bend points
     */
    private static final int TIME_REMOVE_UNNECESSARY_POINTS = 141;
    /**
     * estimated time until the forth commitment to JGraph
     */
    private static final int TIME_EDIT4 = 946;

    /**
     * Returns the maximum time for this graph.
     * @param graph a PDG
     * @return the maximum time
     */
    @Override
	int getMaxTime(PDG graph) {
        return TIME_CREATE_PORTS +
                TIME_EDIT1 +
                TIME_REMOVE_UNNECESSARY_POINTS +
                TIME_EDIT4 +
                graph.getLevelCount() * (TIME_EDIT2 + TIME_EDIT3);
    }

    /**
     * Starts the post processing algorithm.
     * @param graph a PDG
     * @param levelSpacing the minimum space between the nodes and the horizontal edge segments
     */
    @Override
	public void layout(PDG graph, int levelSpacing) {
        start();
        GraphLayoutCache view = graph.getJGraph().getGraphLayoutCache();

        Map<GraphCell, Map> nested = new Hashtable<GraphCell, Map>();
        ConnectionSet conn = new ConnectionSet();
        for(Level level : graph.getLevels()) {
            for(Node node : level) {
                createPorts(node, graph, nested, conn);
            }
        }

        check("createPorts");
        go(TIME_CREATE_PORTS);

        view.edit(nested, conn, null, null);

        check("edit 1");
        go(TIME_EDIT1);

        int y = 0;
        int levelCounter = 0;
        nested = new Hashtable<GraphCell, Map>();
        conn = new ConnectionSet();

        for(Level level : graph.getLevels()) {
            Map<Object, Map<?, ?>> lNested = new Hashtable<Object, Map<?, ?>>();
            setCoordinates(level, y, view, lNested);

            if(levelCounter < graph.getLevelCount() - 1) {
                setVertexXCoordinates(graph.getLevel(levelCounter + 1), lNested);
            }
            view.edit(lNested);

            check("edit 2");
            go(TIME_EDIT2);

            y += setLoopEdges(level, view, nested);

            y += setLinkNodes(graph, level, view, nested, conn);

            y += graph.getMaxVertexHeight(level) + levelSpacing / 2;

            lNested = new Hashtable<Object, Map<?, ?>>();
            y += setOrthoEdges(graph, y, levelCounter, lNested);
            view.edit(lNested);

            check("edit 3");
            go(TIME_EDIT3);

            y += levelSpacing / 2;
            levelCounter++;
        }
        removeUnnecessaryPoints(graph);

        check("removeUnnecessaryPoints");
        go(TIME_REMOVE_UNNECESSARY_POINTS);

        view.edit(nested, conn, null, null);

        check("edit 4");
        go(TIME_EDIT4);
        complete(graph);
    }

    /**
     * Searches the neighbors of the vertex and calls setPorts(...).
     * @param node a vertex
     * @param graph a PDG
     * @param nested a map with changes
     * @param conn the connections of the edges to the ports
     */
    private void createPorts(Node node, PDG graph, Map<GraphCell, Map> nested,  ConnectionSet conn) {
        if(node.isVertex()) {
            JGraph jGraph = graph.getJGraph();
            GraphModel model = jGraph.getModel();
            DefaultGraphCell cell = (DefaultGraphCell) node.getCell();
            Rectangle2D cellBounds = GraphConstants.getBounds(cell.getAttributes());
            int nodeHeight = (int) cellBounds.getHeight();
            List<Port> upperPorts =
            	setPorts(cell, Arrays.asList(node.getUpperNeighbors()), model, false, conn);

            List<Node> neighbors = new LinkedList<Node>();
            SortedSet<Node> intraLevelNeighbors = graph.getIntraLevelNeighbors(node);
            neighbors.addAll(intraLevelNeighbors.headSet(node));
            neighbors.addAll(Arrays.asList(node.getLowerNeighbors()));
            List<Node> tail = new LinkedList<Node>(intraLevelNeighbors.tailSet(node));
            Collections.reverse(tail);
            neighbors.addAll(tail);
            neighbors.add(node);
            List<Port> lowerPorts = setPorts(cell, neighbors, model, true, conn);

            setPortCoordinates(node, upperPorts, 0, nested);
            setPortCoordinates(node, lowerPorts, nodeHeight, nested);
        }
    }

    /**
     * Creates the ports and connects the edges to the ports.
     * @param cell the vertex
     * @param neighbors the neighbors of the vertex
     * @param model the JGraph model
     * @param lower true if the ports to the lower neighbors shall be created
     * @param conn the connections of the edges to the ports
     * @return the created ports
     */
    private List<Port> setPorts(DefaultGraphCell cell, List<Node> neighbors,
            GraphModel model, boolean lower, ConnectionSet conn) {
        List<Port> ports = new LinkedList<Port>();
        for(Node neighbor : neighbors) {
            Object neighborCell = neighbor.getCell();
            if(neighbor.isVertex()) {
                Object[] edges = DefaultGraphModel.getEdgesBetween(model,
                        cell, neighborCell, false);
                for(Object o : edges) {
                    Edge edge = (Edge) o;
                    // is it a loop?
                    if(edge.getSource() == edge.getTarget()) {
                        DefaultPort sourcePort = new DefaultPort();
                        DefaultPort targetPort = new DefaultPort();
                        // add only sourcePort!
                        ports.add(sourcePort);
                        PDGConstants.setLoopTarget(sourcePort.getAttributes(),
                                targetPort);
                        cell.add(sourcePort);
                        cell.add(targetPort);
                        conn.connect(edge, sourcePort, targetPort);
                        PDGConstants.addLoopEdge(cell.getAttributes(),
                                edge);
                    } else {
                        DefaultPort port = new DefaultPort();
                        ports.add(port);
                        cell.add(port);
                        setEdgeToPort(edge, port, model, conn);
                        addOrthoPort(cell, port, edge, lower, neighbor);
                    }
                }
            } else {
                DefaultPort port = new DefaultPort();
                ports.add(port);
                cell.add(port);
                setEdgeToPort(neighborCell, port, model, conn);
                addOrthoPort(cell, port, (Edge) neighborCell, lower, neighbor);
            }
        }
        return ports;
    }

    /**
     * Stores a port of a data dependant edge in an attribute of the node.
     * @param cell the vertex
     * @param port the port
     * @param edge the data dependant edge
     * @param lower true if it is a port at the bottom of the node
     * @param neighbor the other node of the edge
     */
    private void addOrthoPort(GraphCell cell, Port port, Edge edge,
            boolean lower, Node neighbor) {
        if(!PDG.isDAGEdge(edge)) {
            AttributeMap attributeMap = cell.getAttributes();
            if(lower)
                PDGConstants.addLowerOrthoPorts(attributeMap, port);
            else
                PDGConstants.addUpperOrthoPorts(attributeMap, port);
            PDGConstants.setOppositeNode(port.getAttributes(), neighbor);
        }
    }

    /**
     * Sets the coordinates of the ports.
     * @param node a node
     * @param ports a list of ports of this node
     * @param y an y-coordinate
     * @param nested a map with changes
     */
    private void setPortCoordinates(Node node, List<Port> ports, int y,
            Map<GraphCell, Map> nested) {
        DefaultGraphCell cell = (DefaultGraphCell) node.getCell();
        // x, y not set in GraphModel!
        Rectangle2D cellBounds =
                GraphConstants.getBounds(cell.getAttributes());
        int nodeWidth = (int) cellBounds.getWidth();
        int nPorts = ports.size();
        float portDistance;
        if(nPorts * PREFERRED_PORT_DISTANCE <= nodeWidth)
            portDistance = PREFERRED_PORT_DISTANCE;
        else
            portDistance = nodeWidth / nPorts;
        int x = (nodeWidth - (int) (nPorts * portDistance)) / 2;
        int spaceAtEnd = x;
        List<Port> loops = new LinkedList<Port>();
        for(Port port : ports) {
            if(PDGConstants.getLoopTarget(port.getAttributes()) != null) {
                loops.add(port);
                x += spaceAtEnd;
                spaceAtEnd = 0;
            }
            Map<?, ?> attributeMap = new Hashtable<Object, Object>();
            GraphConstants.setAbsolute(attributeMap, true);
            GraphConstants.setOffset(attributeMap, new Point(x, y));
            nested.put(port, attributeMap);
            x += portDistance;
        }
        setLoopPortCoordinates(node, loops, nested);
    }

    /**
     * Sets the ports of loops.
     * @param node a vertex
     * @param loops a list of loops
     * @param nested a map with changes
     */
    private void setLoopPortCoordinates(Node node, List<Port> loops,
            Map<GraphCell, Map> nested) {
        DefaultGraphCell cell = (DefaultGraphCell) node.getCell();
        // x, y not set in GraphModel!
        Rectangle2D cellBounds =
                GraphConstants.getBounds(cell.getAttributes());
        int nodeWidth = (int) cellBounds.getWidth();
        int nodeHeight = (int) cellBounds.getHeight();
        int nPorts = loops.size();
        float portDistance;
        if(nPorts * PREFERRED_PORT_DISTANCE <= nodeHeight)
            portDistance = PREFERRED_PORT_DISTANCE;
        else
            portDistance = nodeHeight / nPorts;
        int y = nodeHeight - (int) (nPorts * portDistance);
        for(Port sourcePort : loops) {
            Port targetPort =
                    PDGConstants.getLoopTarget(sourcePort.getAttributes());
            Map<?, ?> attributeMap = new Hashtable<Object, Object>();
            GraphConstants.setAbsolute(attributeMap, true);
            GraphConstants.setOffset(attributeMap, new Point(nodeWidth, y));
            nested.put(targetPort, attributeMap);
            y += portDistance;
        }
    }

    /**
     * Connects edges to ports.
     * Prefers the source of loops.
     * @param edge an edge
     * @param port a port
     * @param model a JGraph model
     * @param conn a set of connections
     */
    private void setEdgeToPort(Object edge, Object port, GraphModel model,
            ConnectionSet conn) {
        Object cell = model.getParent(port);
        if(DefaultGraphModel.getSourceVertex(model, edge) == cell) {
            Object target = conn.getPort(edge, false);
            if(target == null)
                target = ((Edge) edge).getTarget();
            conn.connect(edge, port, target);
        } else {
            Object source = conn.getPort(edge, true);
            if(source == null)
                source = ((Edge) edge).getSource();
            conn.connect(edge, source, port);
        }
    }

    /**
     * Sets the coordinates of all nodes and bend points in a level.
     * @param level a level
     * @param y an y-coordinate
     * @param view a JGraph view
     * @param nested a map with changes
     */
    private void setCoordinates(Level level, int y, GraphLayoutCache view,
            Map<Object, Map<?, ?>> nested) {
        for(Node node : level) {
            GraphCell cell = node.getCell();
            int x = node.getXCoord();
            Map<?, ?> map = new Hashtable<Object, Object>();
            if(node.isVertex()) {
                Rectangle2D bounds =
                        GraphConstants.getBounds(cell.getAttributes());
                GraphConstants.setBounds(map, new Rectangle(x, y,
                        (int) bounds.getWidth(), (int) bounds.getHeight()));
            } else {
                EdgeView edgeView = (EdgeView) view.getMapping(cell, false);
                List<?> oldPoints = edgeView.getPoints();
                List<Object> newPoints = new LinkedList<Object>();
                int index = 1;
                if(node.pointsDown())
                    index = oldPoints.size() - 1;
                int i = 0;
                for(Object point : oldPoints) {
                    if(i == index)
                        newPoints.add(new Point(x, y));
                    newPoints.add(point);
                    i++;
                }
                GraphConstants.setPoints(map, newPoints);
            }
            nested.put(cell, map);
        }
    }

    /**
     * Sets the x-coordinates of all nodes in a level.
     * @param level a level
     * @param nested a map with changes
     */
    private void setVertexXCoordinates(Level level, Map<Object, Map<?, ?>> nested) {
        for(Node node : level) {
            if(node.isVertex()) {
                int x = node.getXCoord();
                Map<?, ?> map = new Hashtable<Object, Object>();
                Rectangle2D bounds = GraphConstants.getBounds(
                        node.getCell().getAttributes());
                GraphConstants.setBounds(map, new Rectangle(x, 0,
                        (int) bounds.getWidth(), (int) bounds.getHeight()));
                nested.put(node.getCell(), map);
            }
        }
    }

    /**
     * Sets the loop edges of all nodes of a level.
     * Adds bend points to align them in Manhattan Layout.
     * @param level a level
     * @param view a JGraph view
     * @param nested a map with changes
     * @return the vertical size of the loop
     */
    private int setLoopEdges(Level level, GraphLayoutCache view, Map<GraphCell, Map> nested) {
        int maxSize = 0;
        for(Node node : level) {
            if(node.isVertex()) {
                DefaultGraphCell cell = (DefaultGraphCell) node.getCell();
                CellView cellView = view.getMapping(cell, false);
                Rectangle2D cellBounds = cellView.getBounds();
                int nodeX = (int) cellBounds.getX();
                int nodeY = (int) cellBounds.getY();
                int nodeWidth = (int) cellBounds.getWidth();
                int nodeHeight = (int) cellBounds.getHeight();
                List<Edge> edges =
                        PDGConstants.getLoopEdges(cell.getAttributes());
                int loopsSize = (edges.size() + 1) * PREFERRED_PORT_DISTANCE;
                int x = nodeX + nodeWidth + loopsSize;
                int y = nodeY + nodeHeight + loopsSize;
                maxSize = Math.max(maxSize, loopsSize);
                for(Edge edge : edges) {
                    Port sourcePort = (Port) edge.getSource();
                    Point2D sourcePoint = GraphConstants.getOffset(
                            sourcePort.getAttributes());
                    Port targetPort = (Port) edge.getTarget();
                    Point2D targetPoint = GraphConstants.getOffset(
                            targetPort.getAttributes());
                    EdgeView edgeView = (EdgeView) view.getMapping(edge, false);
                    List<Object> points = new ArrayList<Object>(edgeView.getPoints());
                    points.add(1,
                            new Point(nodeX + (int) sourcePoint.getX(), y));
                    points.add(2, new Point(x, y));
                    points.add(3,
                            new Point(x, nodeY + (int) targetPoint.getY()));
                    Map<?, ?> map = new Hashtable<Object, Object>();
                    GraphConstants.setPoints(map, points);
                    nested.put(edge, map);
                    x -= PREFERRED_PORT_DISTANCE;
                    y -= PREFERRED_PORT_DISTANCE;
                }
            }
        }
        return maxSize;
    }

    /**
     * Sets the edges between <CODE>upperLevel</CODE> and
     * <CODE>upperLevel + 1</CODE> in Manhattan layout.
     * If the x-coordinate of the opposite ports is greater than the
     * x-coordinate of this port then the order in which the ports are processed
     * is changed.
     * @param graph a PDG
     * @param y an y-coordinate
     * @param upperLevel the upper level
     * @param nested a map with changes
     * @return the size needed for the horizontal edge segments
     */
    private int setOrthoEdges(PDG graph, int y, int upperLevel, Map<Object, Map<?, ?>> nested) {
        GraphLayoutCache view = graph.getJGraph().getGraphLayoutCache();
        Level l0 = graph.getLevel(upperLevel);
        TreeMap<Integer, BitSet> bitmap = new TreeMap<Integer, BitSet>();
        setPortBitmap(l0, bitmap, true);
        if(upperLevel < graph.getLevelCount() - 1) {
            Level l1 = graph.getLevel(upperLevel + 1);
            setPortBitmap(l1, bitmap, false);
        }
        int maxY = y;
        for(Node node : l0) {
            if(node.isVertex()) {
                GraphCell cell = node.getCell();
                AttributeMap attributeMap = cell.getAttributes();
                List<Port> ports =
                        PDGConstants.getLowerOrthoPorts(attributeMap);
                LinkedList<Object[]> leftOrthoJobs = new LinkedList<Object[]>();
                LinkedList<Object[]> rightOrthoJobs =
                        new LinkedList<Object[]>();
                for(Port port : ports) {
                    Node oppositeNode = PDGConstants.getOppositeNode(
                            port.getAttributes());
                    Edge edge = (Edge) port.edges().next();
                    if(node.getLevel() != oppositeNode.getLevel() ||
                            edge.getSource() == port) {
                        Point2D offset = GraphConstants.getOffset(
                                port.getAttributes());
                        int x1 = node.getXCoord() + (int) offset.getX();
                        int x2 = oppositeNode.getXCoord();
                        boolean pointsDown;
                        if(oppositeNode.isVertex()) {
                            Port sourcePort = (Port) edge.getSource();
                            Port targetPort = (Port) edge.getTarget();
                            Port oppositePort = port == sourcePort ?
                                targetPort : sourcePort;
                            Point2D offset2 = GraphConstants.getOffset(
                                    oppositePort.getAttributes());
                            x2 += offset2.getX();
                            pointsDown = port == sourcePort;
                        } else
                            pointsDown = oppositeNode.pointsDown();
                        Object[] job = new Object[] {edge, x1, x2, pointsDown};
                        if(x2 <= x1)
                            leftOrthoJobs.add(job);
                        else
                            rightOrthoJobs.addFirst(job);
                    }
                }
                leftOrthoJobs.addAll(rightOrthoJobs);
                for(Object[] job : leftOrthoJobs) {
                    Edge edge = (Edge) job[0];
                    int x1 = (Integer) job[1];
                    int x2 = (Integer) job[2];
                    boolean pointsDown = (Boolean) job[3];
                    maxY = Math.max(maxY, setEdgeOrtho(edge, view, bitmap,
                            x1, x2, y, pointsDown, nested));
                }
            } else if(!PDG.isDAGEdge((Edge) node.getCell())) {
                int x1 = node.getXCoord();
                Node lower = node.getLowerNeighbors()[0];
                int x2 = lower.getXCoord();
                Edge edge = (Edge) node.getCell();
                if(lower.isVertex()) {
                    GraphCell lowerCell = lower.getCell();
                    DefaultPort sourcePort = (DefaultPort) edge.getSource();
                    DefaultPort targetPort = (DefaultPort) edge.getTarget();
                    Port port = sourcePort.getParent() == lowerCell ?
                        sourcePort : targetPort;
                    Point2D offset =
                            GraphConstants.getOffset(port.getAttributes());
                    x2 += offset.getX();
                }
                maxY = Math.max(maxY, setEdgeOrtho(edge, view, bitmap, x1, x2,
                        y, node.pointsDown(), nested));
            }
        }
        return maxY - y;
    }

    /**
     * Creates for every port in a level a bitset.
     * The bitset saves where on the x-coordinate of the port are horizontal
     * edge segments.
     * @param level a level
     * @param bitmap a bitset
     * @param lower true if the ports at the bottom of the nodes are processed
     */
    private void setPortBitmap(Level level, TreeMap<Integer, BitSet> bitmap,
            boolean lower) {
        for(Node node : level) {
            if(node.isVertex()) {
                GraphCell cell = node.getCell();
                AttributeMap attributeMap = cell.getAttributes();
                List<Port> ports;
                if(lower)
                    ports = PDGConstants.getLowerOrthoPorts(attributeMap);
                else
                    ports = PDGConstants.getUpperOrthoPorts(attributeMap);
                for(Port port : ports) {
                    Point2D offset =
                            GraphConstants.getOffset(port.getAttributes());
                    int x = node.getXCoord() + (int) offset.getX();
                    bitmap.put(x, new BitSet());
                }
            } else {
                bitmap.put(node.getXCoord(), new BitSet());
            }
        }
    }

    /**
     * Sets an edge into Manhattan layout while taking on consideration of the
     * bitsets of the ports.
     * Additional bend points are added.
     * @param edge an edge
     * @param view a JGraph view
     * @param bitmap the bitsets of the ports
     * @param x1 the x-coordinate of one port
     * @param x2 the x-coordinate of the other port
     * @param startY the y-coordinate where to set the first edge segment
     * @param pointsDown true if the edge points down
     * @param nested a map with changes
     * @return the vertical size of the edge segments
     */
    private int setEdgeOrtho(Edge edge, GraphLayoutCache view,
            SortedMap<Integer, BitSet> bitmap, int x1, int x2, int startY,
            boolean pointsDown, Map<Object, Map<?, ?>> nested) {
        int startX = Math.min(x1, x2);
        int endX = Math.max(x1, x2);
        SortedMap<Integer, BitSet> submap = bitmap.subMap(startX, endX);
        BitSet endBitSet = bitmap.get(endX);
        BitSet free = (BitSet) endBitSet.clone();
        for(BitSet bitSet : submap.values())
            free.or(bitSet);
        int freeLine = free.nextClearBit(0);
        for(BitSet bitSet : submap.values())
            bitSet.set(freeLine);
        endBitSet.set(freeLine);

        int y = startY + freeLine * PREFERRED_PORT_DISTANCE;

        EdgeView edgeView = (EdgeView) view.getMapping(edge, false);
        List<Object> points = new ArrayList<Object>(edgeView.getPoints());
        if(pointsDown) {
            int i = points.size() - 1;
            points.add(i, new Point(x2, y));
            points.add(i, new Point(x1, y));
        } else {
            points.add(1, new Point(x2, y));
            points.add(2, new Point(x1, y));
        }
        Map<?, ?> map = new Hashtable<Object, Object>();
        GraphConstants.setPoints(map, points);
        nested.put(edge, map);

        return y;
    }


    /**
     * Places the nodes of other PDGs.
     * @param graph a PDG
     * @param level a level
     * @param view a JGraph view
     * @param nested a map with changes
     * @param conn a set of connections
     * @return the size of all link nodes
     */
    private float setLinkNodes(PDG graph, Level level, GraphLayoutCache view,
            Map<GraphCell, Map> nested, ConnectionSet conn) {
        GraphModel model = graph.getJGraph().getModel();
        int maxY = 0;
        for(Node node : level) {
            if(node.isVertex()) {
                DefaultGraphCell cell = (DefaultGraphCell) node.getCell();
                CellView vertexView = view.getMapping(cell, false);
                Rectangle2D bounds = vertexView.getBounds();
                int cellX = (int) bounds.getX();
                int cellY = (int) bounds.getY();
                bounds = GraphConstants.getBounds(cell.getAttributes());
                int cellWidth = (int) bounds.getWidth();
                int x = cellX + cellWidth + 20;
                int y = cellY;
                int portY = 4;
                Object[] incoming = DefaultGraphModel.getEdges(model, cell,
                        true);
                DefaultPort incomingPort = null;
                for(Object edge : incoming) {
                    DefaultGraphCell neighborCell =
                            (DefaultGraphCell) DefaultGraphModel.
                            getSourceVertex(model, edge);
                    if(PDGConstants.isLinkNode(neighborCell.getAttributes())) {
                        if(incomingPort == null) {
                            incomingPort = createLinkPort(cell, nested,
                                    cellWidth, portY);
                            portY += 5;
                        }
                        y += setLinkNode(nested, conn, edge, incomingPort, true,
                                x, y, neighborCell);
                    }
                }
                Object[] outgoing = DefaultGraphModel.getEdges(model, cell,
                        false);
                DefaultPort outgoingPort = null;
                for(Object edge : outgoing) {
                    DefaultGraphCell neighborCell =
                            (DefaultGraphCell) DefaultGraphModel.
                            getTargetVertex(model, edge);
                    if(PDGConstants.isLinkNode(neighborCell.getAttributes())) {
                        if(outgoingPort == null)
                            outgoingPort = createLinkPort(cell, nested,
                                    cellWidth, portY);
                        y += setLinkNode(nested, conn, edge, outgoingPort,
                                false, x, y, neighborCell);
                    }
                }
                maxY = Math.max(maxY, y - cellY);
            }
        }
        return maxY;
    }

    /**
     * Creates a port for a link node.
     * @param cell a vertex
     * @param nested a map with changes
     * @param cellWidth the width of the vertex
     * @param portY the y-coordinate of the port
     * @return the created port
     */
    private DefaultPort createLinkPort(DefaultGraphCell cell, Map<GraphCell, Map> nested,
            int cellWidth, int portY) {
        DefaultPort port = new DefaultPort();
        cell.add(port);
        Map<?, ?> attributeMap = new Hashtable<Object, Object>();
        GraphConstants.setAbsolute(attributeMap, true);
        GraphConstants.setOffset(attributeMap, new Point(cellWidth, portY));
        nested.put(port, attributeMap);
        return port;
    }

    /**
     * Places a link node and connects it with a edge.
     * @param nested a map with changes
     * @param conn a list of connections
     * @param edge an edge
     * @param parentPort the port of the node in this PDG
     * @param edgeincoming true if the edge points to the node in this PDG
     * @param x the x-coordinate of the link node
     * @param y the y-coordinate of the link node
     * @param neighborCell the link node
     * @return height + 2 of the link node
     */
    private int setLinkNode(Map<GraphCell, Map> nested, ConnectionSet conn, Object edge,
            Port parentPort, boolean edgeincoming, int x, int y,
            DefaultGraphCell neighborCell) {
        Rectangle2D linkBounds =
                GraphConstants.getBounds(neighborCell.getAttributes());
        int height = (int) linkBounds.getHeight();
        DefaultPort port = new DefaultPort();
        neighborCell.add(port);
        Map<?, ?> attributeMap = new Hashtable<Object, Object>();
        GraphConstants.setAbsolute(attributeMap, true);
        GraphConstants.setOffset(attributeMap, new Point(0, height / 2));
        nested.put(port, attributeMap);
        if(edgeincoming)
            conn.connect(edge, port, parentPort);
        else
            conn.connect(edge, parentPort, port);
        attributeMap = new Hashtable<Object, Object>();
        GraphConstants.setBounds(attributeMap, new Rectangle(x, y,
                (int) linkBounds.getWidth(),
                (int) linkBounds.getHeight()));
        nested.put(neighborCell, attributeMap);
        return height + 2;
    }

    /**
     * Removes unnecessary bend points from edges.
     * @param graph a PDG
     */
    private void removeUnnecessaryPoints(PDG graph) {
        JGraph jGraph = graph.getJGraph();
        GraphModel model = jGraph.getModel();
        GraphLayoutCache view = jGraph.getGraphLayoutCache();
        CellView[] cellViews = view.getCellViews();
        Map<Object, Map<?, ?>> nested = new Hashtable<Object, Map<?, ?>>();
        for(CellView cellView : cellViews) {
            Object cell = cellView.getCell();
            if(model.isEdge(cell)) {
                EdgeView edge = (EdgeView) cellView;
                List<?> points = edge.getPoints();
                LinkedList<Object> newPoints = new LinkedList<Object>();
                newPoints.add(points.get(0));
                for(int i = 1; i < points.size() - 1; i++) {
                    Object p = points.get(i);
                    Point2D p0 = getPoint(newPoints.getLast());
                    Point2D p1 = getPoint(p);
                    Point2D p2 = getPoint(points.get(i + 1));
                    if(p0.getX() != p1.getX() || p1.getX() != p2.getX())
                        newPoints.add(p);
                }
                newPoints.add(points.get(points.size() - 1));
                Map<?, ?> map = new Hashtable<Object, Object>();
                GraphConstants.setPoints(map, newPoints);
                nested.put(cell, map);
            }
        }
        view.edit(nested);
    }

    /**
     * Returns the coordinates of a bend point.
     * @param point a bend point
     * @return the coordinates of the bend point
     */
    private Point2D getPoint(Object point) {
        if(point instanceof PortView) {
            PortView port = (PortView) point;
            Point2D offset = GraphConstants.getOffset(port.getAllAttributes());
            if (offset == null) {
            	offset = new Point(0, 0);
            }
            CellView vertex = port.getParentView();
            Rectangle2D bounds = vertex.getBounds();
            double x = bounds.getX() + offset.getX();
            double y = bounds.getY() + offset.getY();
            return new Point2D.Double(x, y);
        } else {
            return (Point2D) point;
        }
    }
}
