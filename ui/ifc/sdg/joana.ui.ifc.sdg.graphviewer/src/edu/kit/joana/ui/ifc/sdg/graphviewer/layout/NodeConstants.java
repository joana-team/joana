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
 * PDGConstants.java
 *
 * Created on 7. September 2005, 18:07
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

/**
 * This class sets and gets the attributes of nodes.
 * @author Siegfried Weber
 */
public class NodeConstants {

    /**
     * key of the node ID
     */
    private static final String ID = "id";
    /**
     * key of the barycenter value
     */
    private static final String BARYCENTER_VALUE = "barycenterValue";
    /**
     * key of the root (Brandes/Koepf)
     */
    private static final String BK_ROOT = "bkRoot";
    /**
     * key of the alignment (Brandes/Koepf)
     */
    private static final String BK_ALIGN = "bkAlign";
    /**
     * key of the sink (Brandes/Koepf)
     */
    private static final String BK_SINK = "bkSink";
    /**
     * key of the shift (Brandes/Koepf)
     */
    private static final String BK_SHIFT = "bkShift";
    /**
     * key of the x-coordinate (Brandes/Koepf)
     */
    private static final String BK_XCOORD = "bkXCoord";
    /**
     * key of the block width (Brandes/Koepf)
     */
    private static final String BK_BLOCK_WIDTH = "bkBlockWidth";

    /**
     * Returns the node ID.
     * @param node a node
     * @return the node ID
     */
    public static final int getID(Node node) {
        return getInt(node, ID);
    }

    /**
     * Sets a node ID.
     * @param node a node
     * @param id an ID
     */
    public static final void setID(Node node, int id) {
        node.setAttribute(ID, id);
    }

    /**
     * Returns the barycenter value.
     * @param node a node
     * @return the barycenter value
     */
    public static final float getBarycenter(Node node) {
        return getFloat(node, BARYCENTER_VALUE);
    }

    /**
     * Sets the barycenter value.
     * @param node a node
     * @param value the barycenter value
     */
    public static final void setBarycenter(Node node, float value) {
        node.setAttribute(BARYCENTER_VALUE, value);
    }

    /**
     * Returns the root (Brandes/Koepf).
     * @param node a node
     * @return the root
     */
    public static final Node getBKRoot(Node node) {
        Node root = (Node) node.getAttribute(BK_ROOT);

        return root == null ? node : root;
    }

    /**
     * Sets the root (Brandes/Koepf).
     * @param node a node
     * @param root the root
     */
    public static final void setBKRoot(Node node, Node root) {
        node.setAttribute(BK_ROOT, root);
    }

    /**
     * Returns the aligned node (Brandes/Koepf).
     * @param node a node
     * @return the aligned node
     */
    public static final Node getBKAlign(Node node) {
        return (Node) node.getAttribute(BK_ALIGN);
    }

    /**
     * Sets the aligned node (Brandes/Koepf).
     * @param node a node
     * @param lowerNeighbor the aligned node
     */
    public static final void setBKAlign(Node node, Node lowerNeighbor) {
        node.setAttribute(BK_ALIGN, lowerNeighbor);
    }

    /**
     * Returns the sink (Brandes/Koepf).
     * @param node a node
     * @return the sink
     */
    public static final Node getBKSink(Node node) {
        Node sink = (Node) node.getAttribute(BK_SINK);

        return sink == null ? node : sink;
    }

    /**
     * Sets the sink (Brandes/Koepf).
     * @param node a node
     * @param sink the sink
     */
    public static final void setBKSink(Node node, Node sink) {
        node.setAttribute(BK_SINK, sink);
    }

    /**
     * Returns the shift (Brandes/Koepf).
     * @param node a node
     * @return the shift
     */
    public static final int getBKShift(Node node) {
        return getInt(node, BK_SHIFT);
    }

    /**
     * Sets the shift (Brandes/Koepf).
     * @param node a node
     * @param shift the shift
     */
    public static final void setBKShift(Node node, int shift) {
        node.setAttribute(BK_SHIFT, shift);
    }

    /**
     * Returns the x-coordinate (Brandes/Koepf).
     * @param node a node
     * @param vAlign the vertical alignment
     * @param hAlign the horizontal alignment
     * @return the x-coordinate
     */
    public static final int getBKXCoord(Node node,
            BrandesKoepf.Vertical vAlign, BrandesKoepf.Horizontal hAlign) {
        return getInt(node, BK_XCOORD + vAlign + hAlign);
    }

    /**
     * Sets the x-coordinate (Brandes/Koepf).
     * @param node a node
     * @param vAlign the vertical alignment
     * @param hAlign the horizontal alignment
     * @param x the x-coordinate
     */
    public static final void setBKXCoord(Node node,
            BrandesKoepf.Vertical vAlign, BrandesKoepf.Horizontal hAlign,
            int x) {
        node.setAttribute(BK_XCOORD + vAlign + hAlign, x);
    }

    /**
     * Returns the block width (Brandes/Koepf).
     * @param node a node
     * @return the block width
     */
    public static final int getBKBlockWidth(Node node) {
        return getInt(node, BK_BLOCK_WIDTH);
    }

    /**
     * Sets the block width (Brandes/Koepf).
     * @param node a node
     * @param width the block width
     */
    public static final void setBKBlockWidth(Node node, int width) {
        node.setAttribute(BK_BLOCK_WIDTH, width);
    }

    /**
     * Returns an integer value of an attribute.
     * @param node a node
     * @param key the attribute key
     * @return the attribute value
     */
    private static final int getInt(Node node, String key) {
        Integer value = (Integer) node.getAttribute(key);
        if(value != null)
            return value;
        return -1;
    }

    /**
     * Returns a float value of an attribute.
     * @param node a node
     * @param key the attribute key
     * @return the attribute value
     */
    private static final float getFloat(Node node, String key) {
        Float value = (Float) node.getAttribute(key);
        if(value != null)
            return value;
        return -1;
    }
}
