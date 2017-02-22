/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * PDG.java
 *
 * Created on 30. August 2005, 10:29
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.jgraph.JGraph;
import org.jgraph.graph.AttributeMap;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.Edge;
import org.jgraph.graph.GraphCell;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.VertexView;



/**
 * This class represents a PDG.
 * It wraps some of the functionality of JGraph.
 * @author Siegfried Weber
 */
public class PDG {

    /**
     * the width of a link node with its edge
     */
    private static final int LINK_NODE_SPACE = 28;

    /**
     * a JGraph component
     */
    private final JGraph jGraph;
    /**
     * maps JGraph cells to <CODE>Node</CODE>s
     */
    private final Map<GraphCell, Node> vertexMap;
    /**
     * the levels in this PDG
     */
    private final Vector<Level> levels;
    /**
     * maps a node to its neighbors in the same level
     */
    private final Map<Node, SortedSet<Node>> intraLevelNeighbors;
    /**
     * number of vertices in the PDG
     */
    private int nNodes;
    /**
     * the entry node of this PDG
     */
    private Node entry;

    /**
     * Creates a new instance of PDG
     * @param jGraph a JGraph component containing the PDG
     */
    public PDG(JGraph jGraph) {
        this.jGraph = jGraph;
        vertexMap = new Hashtable<GraphCell, Node>();
        levels = new Vector<Level>();
        intraLevelNeighbors = new Hashtable<Node, SortedSet<Node>>();
        nNodes = 0;

        GraphModel model = jGraph.getModel();
        Object[] cells = DefaultGraphModel.getAll(model);
        for(Object obj : cells) {
            if(!model.isEdge(obj) && !model.isPort(obj)) {
                GraphCell cell = (GraphCell) obj;
                AttributeMap map = cell.getAttributes();
                int id = PDGConstants.getID(map);
                Node node = new Node(cell);
                nNodes++;
                NodeConstants.setID(node, id);
                vertexMap.put(cell, node);
                if("ENTR".equals(PDGConstants.getKind(map)) &&
                        !PDGConstants.isLinkNode(map))
                    entry = node;
            }
        }
    }

    /**
     * Returns the JGraph component.
     * @return the JGraph component
     */
    public JGraph getJGraph() {
        return jGraph;
    }

    /**
     * Returns the number of vertices in this PDG.
     * @return the number of vertices
     */
    public int getNodeCount() {
        return nNodes;
    }

    /**
     * Returns the entry node.
     * @return the entry node
     */
    public Node getEntryNode() {
        return entry;
    }

    /**
     * Returns the roots of this PDG.
     * Link nodes are roots, too!
     * @return the roots of this PDG
     */
    public Set<Node> getRoots() {
        Set<Node> roots = new TreeSet<Node>(new IDComparator());
        for(Node node : vertexMap.values()) {
            if(node.isVertex()) {
                Set<Node> parents = getParents(node);
                if(parents.isEmpty())
                    roots.add(node);
            }
        }
        return roots;
    }

    /**
     * Returns the parents of the specified node.
     * @param node a node
     * @return the parents of the specified node
     */
    public Set<Node> getParents(Node node) {
        return getNeighbors(node, true);
    }

    /**
     * Returns the children of the specified node.
     * @param node a node
     * @return the children of the specified node
     */
    public Set<Node> getChildren(Node node) {
        return getNeighbors(node, false);
    }

    /**
     * Returns all neighbors of the specified node.
     * @param node a node
     * @return the neighbors of the specified node
     */
    public Set<Node> getNeighbors(Node node) {
        Set<Node> neighbors = getParents(node);
        neighbors.addAll(getChildren(node));
        return neighbors;
    }

    /**
     * Returns the parents or children of the specified node.
     * @param node a node
     * @param incoming true if the parents shall be returned
     * @return the parents or children of the specified node
     */
    private Set<Node> getNeighbors(Node node, boolean incoming) {
        Set<Node> neighbors = new TreeSet<Node>(new IDComparator());
        if(node.isVertex()) {
            GraphCell cell = node.getCell();
            GraphModel model = jGraph.getModel();
            Object[] edges;
            if(incoming)
                edges = DefaultGraphModel.getIncomingEdges(model, cell);
            else
                edges = DefaultGraphModel.getOutgoingEdges(model, cell);
            List<Edge> dagEdges = filterDAGEdges(edges);
            for(Object edge : dagEdges) {
                GraphCell neighborCell;
                if(incoming)
                    neighborCell = (GraphCell) DefaultGraphModel.
                            getSourceVertex(model, edge);
                else
                    neighborCell = (GraphCell) DefaultGraphModel.
                            getTargetVertex(model, edge);
                Node neighbor = vertexMap.get(neighborCell);
                if(neighbor != node)
                    neighbors.add(neighbor);
            }
        }
        return neighbors;
    }

    /**
     * Returns the control dependence edges.
     * @param edges the array of edges to filter
     * @return the control dependence edges
     */
    private List<Edge> filterDAGEdges(Object[] edges) {
        List<Edge> dagEdges = new LinkedList<Edge>();
        for(Object obj : edges) {
            Edge edge = (Edge) obj;
            if(isDAGEdge(edge))
                dagEdges.add(edge);
        }
        return dagEdges;
    }

    /**
     * Returns true if the specified edge is a control dependence edge.
     * @param edge an edge
     * @return true if the specified edge is a control dependence edge
     */
    public static boolean isDAGEdge(Edge edge) {
        AttributeMap map = edge.getAttributes();
        String value = PDGConstants.getKind(map);
        return "HE".equals(value) || "UN".equals(value) || "CE".equals(value);
    }

    /**
     * Returns all levels in this PDG.
     * @return the levels in this PDG
     */
    public List<Level> getLevels() {
        return levels;
    }

    /**
     * Returns the specified level.
     * @param level a level
     * @return the specified level
     */
    public Level getLevel(int level) {
        return levels.get(level);
    }

    /**
     * Sets the level of a node.
     * Call this method only one time for a node!
     * @param vertex a node
     * @param level a level
     */
    public void setLevel(Node vertex, int level) {
        if(!PDGConstants.isLinkNode(vertex.getCell().getAttributes())) {
            if(levels.size() < level + 1)
                levels.setSize(level + 1);
            Level l = levels.get(level);
            if(l == null) {
                l = new Level(level);
                levels.set(level, l);
            }
            l.add(vertex);
        }
    }

    /**
     * Returns the number of levels.
     * @return the number of levels
     */
    public int getLevelCount() {
        return levels.size();
    }

    /**
     * Returns the neighbors in the same level of the specified node.
     * @param source a node
     * @return the neighbors in the same level
     */
    public SortedSet<Node> getIntraLevelNeighbors(Node source) {
        SortedSet<Node> neighbors = intraLevelNeighbors.get(source);
        return neighbors == null ? new TreeSet<Node>(new IDComparator()) :
            neighbors;
    }

    /**
     * Returns the target node of the edge.
     * @param edge an edge
     * @return the target node
     */
    public Node getTarget(GraphCell edge) {
        GraphModel model = jGraph.getModel();
        GraphCell targetCell = (GraphCell) DefaultGraphModel.
                getTargetVertex(model, edge);
        return vertexMap.get(targetCell);
    }

    /**
     * Returns the block width of a node with its link nodes
     * (but without loop width).
     * @param node a node
     * @return the block width
     */
    public int getBlockWidth(Node node) {
        if(!node.isVertex())
            return 1;
        int width = getNodeWidth(node);
        if(!getLinkNodes(node).isEmpty())
            width += LINK_NODE_SPACE;
        return width;
    }

    /**
     * Returns the width of a node.
     * @param node a node
     * @return the width of a node
     */
    private int getNodeWidth(Node node) {
        if(!node.isVertex())
            return 1;
        GraphLayoutCache view = jGraph.getGraphLayoutCache();
        CellView cell = view.getMapping(node.getCell(), false);
        return (int) cell.getBounds().getWidth();
    }

    /**
     * Returns the width of the largest vertex.
     * @return the width of the largest vertex
     */
    public int getMaxVertexWidth() {
        GraphLayoutCache view = jGraph.getGraphLayoutCache();
        CellView[] cells = view.getCellViews();
        int width = 0;
        for(CellView cell : cells) {
            if(cell instanceof VertexView) {
                Rectangle2D bounds = cell.getBounds();
                width = Math.max(width, (int) bounds.getWidth());
            }
        }
        return width;
    }

    /**
     * Returns the height of the highest node in the specified level.
     * @param level a level
     * @return the height of the highest node
     */
    public int getMaxVertexHeight(Level level) {
        int height = 0;
        for(Node node : level) {
            if(node.isVertex()) {
                Rectangle2D bounds = jGraph.getCellBounds(node.getCell());
                height = Math.max(height, (int) bounds.getHeight());
            }
        }
        return height;
    }

    /**
     * Calls methods to set necessary node attributes after the leveling phase.
     */
    public void completeLevels() {
        GraphModel model = jGraph.getModel();
        Object[] cells = DefaultGraphModel.getAll(model);
        for(Object cell : cells) {
            if(model.isEdge(cell)) {
                GraphCell sourceCell = (GraphCell) DefaultGraphModel.
                        getSourceVertex(model, cell);
                Node source = vertexMap.get(sourceCell);
                GraphCell targetCell = (GraphCell) DefaultGraphModel.
                        getTargetVertex(model, cell);
                Node target = vertexMap.get(targetCell);
                if(!PDGConstants.isLinkNode(sourceCell.getAttributes()) &&
                        !PDGConstants.isLinkNode(
                        targetCell.getAttributes()))
                    insertEdge((Edge) cell, source, target);
            }
        }
    }

    /**
     * Returns the link nodes of the specified node.
     * @param node a node
     * @return the link nodes of the specified node
     */
    public List<Node> getLinkNodes(Node node) {
        List<Node> linkNodes = new LinkedList<Node>();
        GraphModel model = jGraph.getModel();
        GraphCell cell = node.getCell();
        Object[] incoming = DefaultGraphModel.getEdges(model, cell, true);
        for(Object edge : incoming) {
            GraphCell neighborCell =
                    (GraphCell) DefaultGraphModel.getSourceVertex(model, edge);
            if(PDGConstants.isLinkNode(neighborCell.getAttributes()))
                linkNodes.add(vertexMap.get(neighborCell));
        }
        Object[] outgoing = DefaultGraphModel.getEdges(model, cell, false);
        for(Object edge : outgoing) {
            GraphCell neighborCell =
                    (GraphCell) DefaultGraphModel.getTargetVertex(model, edge);
            if(PDGConstants.isLinkNode(neighborCell.getAttributes()))
                linkNodes.add(vertexMap.get(neighborCell));
        }
        return linkNodes;
    }

    /**
     * Calls the various methods depending on the number of levels the edge
     * spans.
     * @param edge an edge
     * @param source the start node
     * @param target the target node
     */
    private void insertEdge(Edge edge, Node source, Node target) {
        int sourceLevel = source.getLevel();
        int targetLevel = target.getLevel();
        if(sourceLevel != targetLevel) {
            boolean pointsDown = sourceLevel < targetLevel;
            Node startNode;
            Node endNode;
            if(pointsDown) {
                startNode = source;
                endNode = target;
            } else {
                startNode = target;
                endNode = source;
            }
            int startLevel = startNode.getLevel();
            int endLevel = endNode.getLevel();
            if(startLevel == endLevel - 1)
                insertStraightEdge(startNode, endNode, edge);
            else
                insertEdgeWithPoints(startNode, endNode, edge, pointsDown);
        } else if(source != target) {
            SortedSet<Node> neighbors = intraLevelNeighbors.get(source);
            if(neighbors == null) {
                neighbors = new TreeSet<Node>(new IDComparator());
                intraLevelNeighbors.put(source, neighbors);
            }
            neighbors.add(target);
            neighbors = intraLevelNeighbors.get(target);
            if(neighbors == null) {
                neighbors = new TreeSet<Node>(new IDComparator());
                intraLevelNeighbors.put(target, neighbors);
            }
            neighbors.add(source);
        }
    }

    /**
     * Adds attributes for a short edge to the nodes.
     * @param startNode the start node
     * @param endNode the end node
     * @param edge the edge
     */
    private void insertStraightEdge(Node startNode, Node endNode, Edge edge) {
        boolean controlDependent = isDAGEdge(edge);
        startNode.addLowerNeighbor(endNode, controlDependent);
        endNode.addUpperNeighbor(startNode, controlDependent);
    }

    /**
     * Breaks a long edge into short segments and adds the attributes to the nodes.
     * @param startNode the start node
     * @param endNode the end node
     * @param edge the edge
     * @param pointsDown true if the edge points down
     */
    private void insertEdgeWithPoints(Node startNode, Node endNode,
            Edge edge, boolean pointsDown) {
        int startLevel = startNode.getLevel();
        int endLevel = endNode.getLevel();
        Vector<Node> edgeNodes = new Vector<Node>();
        edgeNodes.add(startNode);
        for(int level = startLevel + 1; level < endLevel; level++) {
            Node node = new Node(edge);
            nNodes++;
            node.setPointsDown(pointsDown);
            setLevel(node, level);
            edgeNodes.add(node);
        }
        edgeNodes.add(endNode);
        boolean controlDependent = isDAGEdge(edge);
        for(int i = 1; i < edgeNodes.size(); i++) {
            Node upperNode = edgeNodes.get(i - 1);
            Node lowerNode = edgeNodes.get(i);
            upperNode.addLowerNeighbor(lowerNode, controlDependent);
            lowerNode.addUpperNeighbor(upperNode, controlDependent);
        }
    }
}
