/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * BrandesKoepf.java
 *
 * Created on 7. September 2005, 19:37
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class implements the Brandes/Koepf algorithm.
 * @author Siegfried Weber
 */
public class BrandesKoepf extends CoordinateAssignmentStrategy {

    /**
     * vertical alignments
     */
    enum Vertical {UPPER, LOWER};
    /**
     * horizontal alignments
     */
    enum Horizontal {LEFT, RIGHT};

    /**
     * If true the nodes will be centered in the blocks.
     * If false the nodes will be left and right aligned in the blocks.
     */
    private static final boolean CENTER_NODES = true;
    /**
     * Skip marking of type 1 conflicts.
     */
    private static final boolean OMIT_TYPE1_MARK = false;
    /**
     * Prefer control dependant edges in the vertical alignment.
     */
    private static final boolean PREFER_CD_EDGES = true;
    /**
     * Align data dependant edges.
     */
    private static final boolean ALIGN_DD_EDGES = true;

    /**
     * estimated time
     */
    private static final int TIME = 348;

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
     * Starts the Brandes/Koepf algorithm (see alg. 3.10).
     * @param graph a PDG
     * @param delta the minimum space between blocks
     */
    @Override
	public void assignCoordinates(PDG graph, int delta) {
        start();
        resolveType2(graph);
        Set<Segment> markedSegments;
        if(OMIT_TYPE1_MARK) {
            markedSegments = new HashSet<Segment>();
        }
        else {
            markedSegments = markType1(graph);
        }
        int[][][] ranges = new int[Vertical.values().length]
                [Horizontal.values().length][2];
        Vertical vMin = null;
        Horizontal hMin = null;
        int minWidth = Integer.MAX_VALUE;
        for(Vertical v : Vertical.values())
            for(Horizontal h : Horizontal.values()) {
                alignVertical(graph, markedSegments, v, h);
                int vi = v.ordinal();
                int hi = h.ordinal();
                ranges[vi][hi] = compactHorizontal(graph, delta, v, h);
                int width = ranges[vi][hi][1] - ranges[vi][hi][0];
                if(width < minWidth) {
                    minWidth = width;
                    vMin = v;
                    hMin = h;
                }
            }
        int[][] shifts = alignAssignments(ranges,
                ranges[vMin.ordinal()][hMin.ordinal()]);
        setCoordinates(graph, shifts);
        check("assignCoordinates");
        go(TIME);
        complete(graph);
    }

    /**
     * Resolves type 2 conflicts (see alg. 3.5).
     * @param graph a PDG
     */
    private void resolveType2(PDG graph) {
        for(int level = 1; level < graph.getLevelCount() - 2; level++) {
            Level l = graph.getLevel(level);
            int i = 0;
            for(Node node : l) {
                if(!node.isVertex()) {
                    for(Node lowerNode : node.getLowerNeighbors()) {
                        int lowerIndex = lowerNode.getIndex();
                        if(!lowerNode.isVertex()) {
                            if(lowerIndex < i)
                                l.changeIndex(i, lowerIndex);
                            i = lowerIndex;
                        }
                    }
                }
            }
        }
    }

    /**
     * Marks type 1 conflicts (see alg. 3.6).
     * @param graph a PDG
     * @return marked type 1 conflicts
     */
    private Set<Segment> markType1(PDG graph) {
        HashSet<Segment> markedSegments = new HashSet<Segment>();
        for(int i = 1; i < graph.getLevelCount() - 2; i++) {
            Level level0 = graph.getLevel(i);
            Level level1 = graph.getLevel(i + 1);
            int nLevel1 = level1.size();
            int k0 = 0;
            int l = 0;
            for(int l1 = 0; l1 < nLevel1; l1++) {
                Node v1 = level1.get(l1);
                Node v0 = null;
                boolean isInnerSegment = false;
                if(!v1.isVertex()) {
                    Node[] upper = v1.getUpperNeighbors();
                    v0 = upper[0];
                    isInnerSegment = !v0.isVertex();
                }
                if(l1 == nLevel1 - 1 || isInnerSegment) {
                    int k1 = level0.size() - 1;
                    if(isInnerSegment)
                        k1 = v0.getIndex();
                    for(; l <= l1; l++) {
                        Node vl = level1.get(l);
                        for(Node vk : vl.getUpperNeighbors()) {
                            int k = vk.getIndex();
                            if(k < k0 || k > k1)
                                markedSegments.add(new Segment(vk, vl));
                        }
                    }
                    k0 = k1;
                }
            }
        }
        return markedSegments;
    }

    /**
     * Aligns the nodes (see alg. 3.7).
     * The PDG must have only one root.
     * @param graph a PDG
     * @param markedSegments marked type 1 conflicts
     * @param vertical the vertical alignment
     * @param horizontal the horizontal alignment
     */
    private void alignVertical(PDG graph, Set<Segment> markedSegments,
            Vertical vertical, Horizontal horizontal) {

        for(Level level : graph.getLevels()) {
            for(Node node : level) {
                NodeConstants.setBKRoot(node, node);
                NodeConstants.setBKAlign(node, node);
                int width = graph.getBlockWidth(node);
                NodeConstants.setBKBlockWidth(node, width);
            }
        }

        boolean leftmost = horizontal == Horizontal.LEFT;
        boolean upper = vertical == Vertical.UPPER;
        List<Level> levels = graph.getLevels();
        if(!upper) {
            levels = reverseList(levels);
        }

        for(Level level : levels) {
            int r = leftmost ? -1 : Integer.MAX_VALUE;
            List<Node> nodes = level.getNodes();

            if(!leftmost) {
                nodes = reverseList(nodes);
            }

            for(Node node : nodes) {
                Node[] neighbors = new Node[0];
                if(upper) {
                    if(PREFER_CD_EDGES && node.hasUpperCDNeighbors()) {
                        neighbors = node.getUpperCDNeighbors();
                    }
                    else if(ALIGN_DD_EDGES) {
                        neighbors = node.getUpperNeighbors();
                    }

                } else {
                    if(PREFER_CD_EDGES && node.hasLowerCDNeighbors()) {
                        neighbors = node.getLowerCDNeighbors();
                    }
                    else if(ALIGN_DD_EDGES) {
                        neighbors = node.getLowerNeighbors();
                    }
                }

                int d = neighbors.length;
                if(d > 0) {
                    Node[] medians;
                    if((d & 1) == 1)
                        medians = new Node[] {neighbors[(d - 1) / 2]};
                    else {
                        Node left = neighbors[(int) Math.floor((d - 1) / 2f)];
                        Node right = neighbors[(int) Math.ceil((d - 1) / 2f)];
                        if(leftmost)
                            medians = new Node[] {left, right};
                        else
                            medians = new Node[] {right, left};
                    }

                    for(Node median : medians) {
                        int medianIndex = median.getIndex();
                        if(NodeConstants.getBKAlign(node) == node &&
                            !markedSegments.contains(new Segment(median, node))
                            && ((leftmost && r < medianIndex) ||
                                (!leftmost && r > medianIndex))) {
                            NodeConstants.setBKAlign(median, node);
                            Node root = NodeConstants.getBKRoot(median);
	                            NodeConstants.setBKRoot(node, root);
	                            NodeConstants.setBKAlign(node, root);
	                            int nodeWidth = graph.getBlockWidth(node);
	                            int blockWidth = NodeConstants.getBKBlockWidth(root);
	                            blockWidth = Math.max(nodeWidth, blockWidth);
	                            NodeConstants.setBKBlockWidth(root, blockWidth);
	                            r = medianIndex;
                        }
                    }
                }
            }
        }
    }

    /**
     * Places the blocks (see alg. 3.9).
     * @param v the node of a block which will be placed
     * @param graph a PDG
     * @param delta the minimum space between the blocks
     * @param vAlign the vertical alignment
     * @param hAlign the horizontal alignment
     */
    private void placeBlock(Node v, PDG graph, int delta, Vertical vAlign,
            Horizontal hAlign) {
        if(NodeConstants.getBKXCoord(v, vAlign, hAlign) == -1) {
            NodeConstants.setBKXCoord(v, vAlign, hAlign, 0);
            Node w = v;
            do {
            	int wLevel = w.getLevel();
            	if (wLevel == Node.UNDEFINED) {
            		wLevel = graph.getLevelCount() - 1;
            	}
                Level level = graph.getLevel(wLevel);
                int wIndex = w.getIndex();
                if(wIndex > 0) {
                    Node pred = level.get(wIndex - 1);
                    Node u = NodeConstants.getBKRoot(pred);
                    placeBlock(u, graph, delta, vAlign, hAlign);
                    int xu = NodeConstants.getBKXCoord(u, vAlign, hAlign);
                    int xv = NodeConstants.getBKXCoord(v, vAlign, hAlign);
                    Node sinkU = NodeConstants.getBKSink(u);
                    Node sinkV = NodeConstants.getBKSink(v);
                    if(sinkV == v) {
                        sinkV = sinkU;
                        NodeConstants.setBKSink(v, sinkV);
                    }
                    int blockWidth = NodeConstants.getBKBlockWidth(u);
                    if(sinkV == sinkU) {
                        xv = Math.max(xv, xu + blockWidth + delta);
                        NodeConstants.setBKXCoord(v, vAlign, hAlign, xv);
                    } else {
                        int shiftU = NodeConstants.getBKShift(sinkU);
                        shiftU = Math.min(shiftU, xv - xu - blockWidth - delta);
                        NodeConstants.setBKShift(sinkU, shiftU);
                    }
                }
                w = NodeConstants.getBKAlign(w);
            } while(w != v);
        }
    }

    /**
     * Compacts the blocks (see alg. 3.8).
     * @param graph a PDG
     * @param delta the minimum space between the blocks
     * @param vAlign the vertical alignment
     * @param hAlign the horizontal alignment
     * @return the bounding coordinates
     */
    private int[] compactHorizontal(PDG graph, int delta, Vertical vAlign,
            Horizontal hAlign) {
        int[] range = new int[2];
        range[0] = Integer.MAX_VALUE;
        range[1] = Integer.MIN_VALUE;
        boolean leftmost = hAlign == Horizontal.LEFT;
        boolean upper = vAlign == Vertical.UPPER;
        for(Level level : graph.getLevels()) {
            for(Node node : level) {
                NodeConstants.setBKSink(node, node);
                NodeConstants.setBKShift(node, Integer.MAX_VALUE);
            }
        }
        for(Level level : graph.getLevels()) {
            for(Node node : level) {
                if(NodeConstants.getBKRoot(node) == node)
                    placeBlock(node, graph, delta, vAlign, hAlign);
            }
        }
        List<Level> levels = graph.getLevels();
        if(upper)
            levels = reverseList(levels);
        for(Level level : levels)
            for(Node node : level) {
                Node root = NodeConstants.getBKRoot(node);
                Node sink = NodeConstants.getBKSink(root);
                int shift = NodeConstants.getBKShift(sink);
                if(shift == Integer.MAX_VALUE)
                    shift = 0;
                int rootX = NodeConstants.getBKXCoord(root, vAlign, hAlign);
                int x = rootX + shift;
                NodeConstants.setBKXCoord(node, vAlign, hAlign, x);
                int blockWidth = NodeConstants.getBKBlockWidth(root);
                range[0] = Math.min(range[0], x);
                range[1] = Math.max(range[1], x + blockWidth);
            }
        if(!leftmost || CENTER_NODES) {
            for(Level level : graph.getLevels())
                for(Node node : level) {
                    Node root = NodeConstants.getBKRoot(node);
                    int x = NodeConstants.getBKXCoord(node, vAlign, hAlign);
                    int nodeWidth = graph.getBlockWidth(node);
                    int blockWidth = NodeConstants.getBKBlockWidth(root);
                    if(CENTER_NODES) {
                        x += (blockWidth - nodeWidth) / 2;
                    }
                    else {
                        x += blockWidth - nodeWidth;
                    }
                    NodeConstants.setBKXCoord(node, vAlign, hAlign, x);
                }
        }
        return range;
    }

    /**
     * Aligns the assignments with the smallest assignment.
     * @param ranges the bounding coordinates of the alignments
     * @param minRange the bounding coordinates of the smallest assignment
     * @return the shifts of the assignments
     */
    private int[][] alignAssignments(int[][][] ranges, int[] minRange) {
        int[][] shifts = new int[Vertical.values().length]
                [Horizontal.values().length];
        int hli = Horizontal.LEFT.ordinal();
        int hri = Horizontal.RIGHT.ordinal();
        for(Vertical v : Vertical.values()) {
            int vi = v.ordinal();
            shifts[vi][hli] = minRange[0] - ranges[vi][hli][0];
            shifts[vi][hri] = minRange[1] - ranges[vi][hri][1];
        }
        return shifts;
    }

    /**
     * Sets the x-coordinates of the nodes.
     * @param graph a PDG
     * @param shifts the shifts of the assignments
     */
    private void setCoordinates(PDG graph, int[][] shifts) {
        int minX = Integer.MAX_VALUE;
        for(Level level : graph.getLevels())
            for(Node node : level) {
                int[] coords = new int[Vertical.values().length *
                        Horizontal.values().length];
                int i = 0;
                for(Vertical v : Vertical.values())
                    for(Horizontal h : Horizontal.values()) {
                        coords[i++] = NodeConstants.getBKXCoord(node, v, h) +
                                shifts[v.ordinal()][h.ordinal()];
                    }
                Arrays.sort(coords);
                int size = coords.length;
                int x = (coords[(int) Math.floor((size - 1) / 2f)] +
                        coords[(int) Math.ceil((size - 1) / 2f)]) / 2;

                // Uncomment if only one assignment should be applied.
                //x = NodeConstants.getBKXCoord(node, Vertical.LOWER, Horizontal.LEFT);

                minX = Math.min(minX, x);
                node.setXCoord(x);
            }
        if(minX != 0) {
            for(Level level : graph.getLevels())
                for(Node node : level)
                    node.setXCoord(node.getXCoord() - minX);
        }
    }

    /**
     * Reverses a list.
     * @param list a list
     * @return the reversed list
     */
    private <E> List<E> reverseList(List<E> list) {
        LinkedList<E> newList = new LinkedList<E>();
        for(E element : list)
            newList.addFirst(element);
        return newList;
    }

    /**
     * Represents an edge segment.
     */
    class Segment {

        /**
         * the start point of the edge segment
         */
        private final Node v0;
        /**
         * the end point of the edge segment
         */
        private final Node v1;

        /**
         * Creates a new edge segment.
         * @param v0 the start point
         * @param v1 the end point
         */
        Segment(Node v0, Node v1) {
            this.v0 = v0;
            this.v1 = v1;
        }

        /**
         * Returns a hash code.
         * @return a hash code
         */
        @Override
		public int hashCode() {
            return v0.hashCode() + v1.hashCode();
        }

        /**
         * Compares two edge segments.
         * @param obj an edge segment
         * @return true if the two nodes represent the same edge segment.
         */
        @Override
		public boolean equals(Object obj) {
            Segment segment = (Segment) obj;
            return (v0 == segment.v0 && v1 == segment.v1) ||
                    (v0 == segment.v1 && v1 == segment.v0);
        }
    }
}
