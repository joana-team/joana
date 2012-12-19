/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * @(c)OrderingSugiyamaLayoutAlgorithm.java
 *
 * Project: GraphViewer
 *
 * Chair for Softwaresystems
 * Faculty of Informatics and Mathematics
 * University of Passau
 *
 * Created on 22.05.2005 at 12:49:43
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Comparator;


import org.jgraph.JGraph;
import org.jgraph.graph.CellView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphModel;
import org.jgraph.graph.VertexView;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.layout.SugiyamaLayoutAlgorithm;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * In contrary to <code>org.jgraph.layout.SugiyamaLayoutAlgorithm</code> this
 * class' algorithm aligns the nodes in a level ordered by their ID.
 *
 * For more detailed explanations see
 * <code>org.jgraph.layout.SugiyamaLayoutAlgorithm</code>
 *
 * @author <a href="mailto:westerhe@fmi.uni-passau.de>Marieke Westerheide </a>
 * @version 1.1
 */
public class OrderingSugiyamaLayoutAlgorithm extends SugiyamaLayoutAlgorithm {

    Vector movements = null;

    int movementsCurrentLoop = -1;

    int movementsMax = Integer.MIN_VALUE;

    int iteration = 0;

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * Method fills the levels and stores them in the member levels.
     *
     * Each level was represended by a Vector with Cell Wrapper objects. These
     * Vectors are the elements in the <code>levels</code> Vector.
     */
    protected Vector fillLevels(JGraph jgraph, CellView[] selectedCellViews,
            Vector rootVertexViews) {
        Vector levels = new Vector();

        // mark as not visited
        // O(allCells)
        for (int i = 0; i < selectedCellViews.length; i++) {
            CellView cellView = selectedCellViews[i];

            // more stabile
            if (cellView == null)
                continue;

            cellView.getAttributes().remove(SUGIYAMA_VISITED);
        }

        Enumeration enumRoots = rootVertexViews.elements();
        while (enumRoots.hasMoreElements()) {
            VertexView vertexView = (VertexView) enumRoots.nextElement();
            fillLevels(jgraph, levels, 0, vertexView);
        }

        return levels;

    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * Fills the Vector for the specified level with a wrapper for the
     * MyGraphCell. After that the method called for each neighbor graph cell.
     *
     * @param level
     *            The level for the graphCell
     * @param graphCell
     *            The Graph Cell
     */
    protected void fillLevels(JGraph jgraph, Vector levels, int level,
            VertexView vertexView) {
        //      precondition control
        if (vertexView == null)
            return;

        // be sure that a Vector container exists for the current level
        if (levels.size() == level)
            levels.insertElementAt(new Vector(), level);

        // if the cell already visited return
        if (vertexView.getAttributes().get(SUGIYAMA_VISITED) != null) {
            return;
        }

        // mark as visited for cycle tests
        vertexView.getAttributes().put(SUGIYAMA_VISITED, new Boolean(true));

        // put the current node into the current level
        // get the Level Vector
        Vector vecForTheCurrentLevel = (Vector) levels.get(level);

        // Create a wrapper for the node
        int numberForTheEntry = vecForTheCurrentLevel.size();

        CellWrapper wrapper = new CellWrapper(level, numberForTheEntry,
                vertexView);

        // put the Wrapper in the LevelVector
        vecForTheCurrentLevel.add(wrapper);

        // concat the wrapper to the cell for an easy access
        vertexView.getAttributes().put(SUGIYAMA_CELL_WRAPPER, wrapper);

        // if the Cell has no Ports we can return, there are no relations
        Object vertex = vertexView.getCell();
        GraphModel model = jgraph.getModel();
        int portCount = model.getChildCount(vertex);

        // iterate any NodePort
        for (int i = 0; i < portCount; i++) {

            Object port = model.getChild(vertex, i);

            // iterate any Edge in the port
            Iterator itrEdges = model.edges(port);

            while (itrEdges.hasNext()) {
                Object edge = itrEdges.next();

                // if the Edge is a forward edge we should follow this edge
                if (port == model.getSource(edge)) {
                    Object targetPort = model.getTarget(edge);
                    Object targetVertex = model.getParent(targetPort);
                    VertexView targetVertexView = (VertexView) jgraph
                            .getGraphLayoutCache().getMapping(targetVertex,
                                    false);
                    fillLevels(jgraph, levels, (level + 1), targetVertexView);
                }
            }
        }

        if (vecForTheCurrentLevel.size() > gridAreaSize) {
            gridAreaSize = vecForTheCurrentLevel.size();
        }

    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * Updates the progress based on the movements count
     */
    @Override
	protected void updateProgress4Movements() {
        // adds the current loop count
        movements.add(new Integer(movementsCurrentLoop));
        iteration++;

        // if the current loop count is higher than the max movements count
        // memorize the new max
        if (movementsCurrentLoop > movementsMax) {
            movementsMax = movementsCurrentLoop;
        }
    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * @see org.jgraph.layout.SugiyamaLayoutAlgorithm#solveEdgeCrosses(org.jgraph.JGraph,
     *      java.util.Vector)
     */
    protected void solveEdgeCrosses(JGraph jgraph, Vector levels) {
        movements = new Vector(100);
        movementsCurrentLoop = -1;
        movementsMax = Integer.MIN_VALUE;
        iteration = 0;

        while (movementsCurrentLoop != 0) {

            // reset the movements per loop count
            movementsCurrentLoop = 0;

            // top down
            for (int i = 0; i < levels.size() - 1; i++) {
                movementsCurrentLoop += solveEdgeCrosses(jgraph, true, levels,
                        i);
            }

            // bottom up
            for (int i = levels.size() - 1; i >= 1; i--) {
                movementsCurrentLoop += solveEdgeCrosses(jgraph, false, levels,
                        i);
            }

            updateProgress4Movements();
        }
    }

    /**
     * Only change to superclass: now the nodes inside a level are sorted by
     * their ID instead of according to their edge cross indicator.
     *
     * @return movements
     */
    protected int solveEdgeCrosses(JGraph jgraph, boolean down, Vector levels,
            int levelIndex) {
        // Get the current level
        Vector currentLevel = (Vector) levels.get(levelIndex);
        int movements = 0;

        // restore the old sort
        Object[] levelSortBefore = currentLevel.toArray();

        // now nodes inside levels are sorted by their ID instead of according
        // to their edge cross indicator as in superclass
        Comparator comp = new NodeComparator();
        Collections.sort(currentLevel, comp);

        // test for movements
        for (int j = 0; j < levelSortBefore.length; j++) {
            if (((CellWrapper) levelSortBefore[j]).getEdgeCrossesIndicator() != ((CellWrapper) currentLevel
                    .get(j)).getEdgeCrossesIndicator()) {
                movements++;

            }
        }

        GraphModel model = jgraph.getModel();

        // Collecations Sort sorts the highest value to the first value
        for (int j = currentLevel.size() - 1; j >= 0; j--) {
            CellWrapper sourceWrapper = (CellWrapper) currentLevel.get(j);

            VertexView sourceView = sourceWrapper.getVertexView();

            Object sourceVertex = sourceView.getCell();
            int sourcePortCount = model.getChildCount(sourceVertex);

            for (int k = 0; k < sourcePortCount; k++) {
                Object sourcePort = model.getChild(sourceVertex, k);

                Iterator sourceEdges = model.edges(sourcePort);
                while (sourceEdges.hasNext()) {
                    Object edge = sourceEdges.next();

                    // if it is a forward edge follow it
                    Object targetPort = null;
                    if (down && sourcePort == model.getSource(edge)) {
                        targetPort = model.getTarget(edge);
                    }
                    if (!down && sourcePort == model.getTarget(edge)) {
                        targetPort = model.getSource(edge);
                    }
                    if (targetPort == null)
                        continue;

                    Object targetCell = model.getParent(targetPort);
                    VertexView targetVertexView = (VertexView) jgraph
                            .getGraphLayoutCache()
                            .getMapping(targetCell, false);

                    if (targetVertexView == null)
                        continue;

                    CellWrapper targetWrapper = (CellWrapper) targetVertexView
                            .getAttributes().get(SUGIYAMA_CELL_WRAPPER);

                    // do it only if the edge is a forward edge to a deeper
                    // level
                    if (down && targetWrapper != null
                            && targetWrapper.getLevel() > levelIndex) {
                        targetWrapper.addToEdgeCrossesIndicator(sourceWrapper
                                .getEdgeCrossesIndicator());
                    }
                    if (!down && targetWrapper != null
                            && targetWrapper.getLevel() < levelIndex) {
                        targetWrapper.addToEdgeCrossesIndicator(sourceWrapper
                                .getEdgeCrossesIndicator());
                    }
                }
            }
        }

        return movements;
    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * @see org.jgraph.layout.SugiyamaLayoutAlgorithm#moveToBarycenter(org.jgraph.JGraph,
     *      org.jgraph.graph.CellView[], java.util.Vector)
     */
    protected void moveToBarycenter(JGraph jgraph, CellView[] allSelectedViews,
            Vector levels) {

        //================================================================
        // iterate any ReViewNodePort
        GraphModel model = jgraph.getModel();
        for (int i = 0; i < allSelectedViews.length; i++) {
            if (!(allSelectedViews[i] instanceof VertexView))
                continue;

            VertexView vertexView = (VertexView) allSelectedViews[i];

            CellWrapper currentwrapper = (CellWrapper) vertexView
                    .getAttributes().get(SUGIYAMA_CELL_WRAPPER);

            Object vertex = vertexView.getCell();
            int portCount = model.getChildCount(vertex);

            for (int k = 0; k < portCount; k++) {
                Object port = model.getChild(vertex, k);

                // iterate any Edge in the port

                Iterator edges = model.edges(port);
                while (edges.hasNext()) {
                    Object edge = edges.next();

                    Object neighborPort = null;
                    // if the Edge is a forward edge we should follow this edge
                    if (port == model.getSource(edge)) {
                        neighborPort = model.getTarget(edge);
                    } else {
                        if (port == model.getTarget(edge)) {
                            neighborPort = model.getSource(edge);
                        } else {
                            continue;
                        }
                    }

                    Object neighborVertex = model.getParent(neighborPort);

                    VertexView neighborVertexView = (VertexView) jgraph
                            .getGraphLayoutCache().getMapping(neighborVertex,
                                    false);

                    if (neighborVertexView == null
                            || neighborVertexView == vertexView)
                        continue;

                    CellWrapper neighborWrapper = (CellWrapper) neighborVertexView
                            .getAttributes().get(SUGIYAMA_CELL_WRAPPER);

                    if (currentwrapper == null || neighborWrapper == null
                            || currentwrapper.level == neighborWrapper.level)
                        continue;

                    currentwrapper.priority++;

                }
            }
        }

        //================================================================
        for (int j = 0; j < levels.size(); j++) {
            Vector level = (Vector) levels.get(j);
            for (int i = 0; i < level.size(); i++) {
                // calculate the initial Grid Positions 1, 2, 3, .... per Level
                CellWrapper wrapper = (CellWrapper) level.get(i);
                wrapper.setGridPosition(i);
            }
        }

        movements = new Vector(100);
        movementsCurrentLoop = -1;
        movementsMax = Integer.MIN_VALUE;
        iteration = 0;

        //int movements = 1;

        while (movementsCurrentLoop != 0) {

            // reset movements
            movementsCurrentLoop = 0;

            // top down
            for (int i = 1; i < levels.size(); i++) {
                movementsCurrentLoop += moveToBarycenter(jgraph, levels, i);
            }

            // bottom up
            for (int i = levels.size() - 1; i >= 0; i--) {
                movementsCurrentLoop += moveToBarycenter(jgraph, levels, i);
            }

            this.updateProgress4Movements();
        }

    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * @see org.jgraph.layout.SugiyamaLayoutAlgorithm#moveToBarycenter(org.jgraph.JGraph,
     *      java.util.Vector, int)
     */
    protected int moveToBarycenter(JGraph jgraph, Vector levels, int levelIndex) {

        // Counter for the movements
        int movements = 0;

        // Get the current level
        Vector currentLevel = (Vector) levels.get(levelIndex);
        GraphModel model = jgraph.getModel();

        for (int currentIndexInTheLevel = 0; currentIndexInTheLevel < currentLevel
                .size(); currentIndexInTheLevel++) {

            CellWrapper sourceWrapper = (CellWrapper) currentLevel
                    .get(currentIndexInTheLevel);

            float gridPositionsSum = 0;
            float countNodes = 0;

            VertexView vertexView = sourceWrapper.getVertexView();
            Object vertex = vertexView.getCell();
            int portCount = model.getChildCount(vertex);

            for (int i = 0; i < portCount; i++) {
                Object port = model.getChild(vertex, i);

                Iterator edges = model.edges(port);
                while (edges.hasNext()) {
                    Object edge = edges.next();

                    // if it is a forward edge follow it
                    Object neighborPort = null;
                    if (port == model.getSource(edge)) {
                        neighborPort = model.getTarget(edge);
                    } else {
                        if (port == model.getTarget(edge)) {
                            neighborPort = model.getSource(edge);
                        } else {
                            continue;
                        }
                    }

                    Object neighborVertex = model.getParent(neighborPort);

                    VertexView neighborVertexView = (VertexView) jgraph
                            .getGraphLayoutCache().getMapping(neighborVertex,
                                    false);

                    if (neighborVertexView == null)
                        continue;

                    CellWrapper targetWrapper = (CellWrapper) neighborVertexView
                            .getAttributes().get(SUGIYAMA_CELL_WRAPPER);

                    if (targetWrapper == sourceWrapper)
                        continue;
                    if (targetWrapper == null
                            || targetWrapper.getLevel() == levelIndex)
                        continue;

                    gridPositionsSum += targetWrapper.getGridPosition();
                    countNodes++;
                }
            }

            //----------------------------------------------------------
            // move node to new x coord
            //----------------------------------------------------------

            if (countNodes > 0) {
                float tmp = (gridPositionsSum / countNodes);
                int newGridPosition = Math.round(tmp);
                boolean toRight = (newGridPosition > sourceWrapper
                        .getGridPosition());

                boolean moved = true;

                while (newGridPosition != sourceWrapper.getGridPosition()
                        && moved) {
                    int tmpGridPos = sourceWrapper.getGridPosition();

                    moved = move(toRight, currentLevel, currentIndexInTheLevel,
                            sourceWrapper.getPriority());

                    if (moved)
                        movements++;

                }
            }
        }
        return movements;
    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * @param toRight
     *            <tt>true</tt>= try to move the currentWrapper to right;
     *            <tt>false</tt>= try to move the currentWrapper to left;
     * @param currentLevel
     *            Vector which contains the CellWrappers for the current level
     * @param currentIndexInTheLevel
     * @param currentPriority
     * @return The free GridPosition or -1 is position is not free.
     */
    protected boolean move(boolean toRight, Vector currentLevel,
            int currentIndexInTheLevel, int currentPriority) {

        CellWrapper currentWrapper = (CellWrapper) currentLevel
                .get(currentIndexInTheLevel);

        boolean moved = false;
        int neighborIndexInTheLevel = currentIndexInTheLevel
                + (toRight ? 1 : -1);
        int newGridPosition = currentWrapper.getGridPosition()
                + (toRight ? 1 : -1);

        // is the grid position possible?

        if (0 > newGridPosition || newGridPosition >= gridAreaSize) {
            return false;
        }

        // if the node is the first or the last we can move
        if (toRight && currentIndexInTheLevel == currentLevel.size() - 1
                || !toRight && currentIndexInTheLevel == 0) {

            moved = true;

        } else {
            // else get the neighbor and ask his gridposition
            // if he has the requested new grid position
            // check the priority

            CellWrapper neighborWrapper = (CellWrapper) currentLevel
                    .get(neighborIndexInTheLevel);

            int neighborPriority = neighborWrapper.getPriority();

            if (neighborWrapper.getGridPosition() == newGridPosition) {
                if (neighborPriority >= currentPriority) {
                    return false;
                } else {
                    moved = move(toRight, currentLevel,
                            neighborIndexInTheLevel, currentPriority);
                }
            } else {
                moved = true;
            }
        }

        if (moved) {
            currentWrapper.setGridPosition(newGridPosition);
        }
        return moved;
    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * This Method draws the graph. For the horizontal position we are using the
     * grid position from each graphcell. For the vertical position we are using
     * the level position.
     */
    protected void drawGraph(JGraph jgraph, Vector levels, Point min,
            Point spacing) {
        // paint the graph

        Map viewMap = new Hashtable();
        for (int rowCellCount = 0; rowCellCount < levels.size(); rowCellCount++) {
            Vector level = (Vector) levels.get(rowCellCount);

            for (int colCellCount = 0; colCellCount < level.size(); colCellCount++) {
                CellWrapper wrapper = (CellWrapper) level.get(colCellCount);
                VertexView view = wrapper.vertexView;
                /*
                 * While the Algorithm is running we are putting some
                 * attributeNames to the MyGraphCells. This method cleans this
                 * objects from the MyGraphCells.
                 */
                view.getAttributes().remove(SUGIYAMA_CELL_WRAPPER);
                view.getAttributes().remove(SUGIYAMA_VISITED);
                wrapper.vertexView = null;

                // get the bounds from the cellView
                if (view == null)
                    continue;
                Rectangle2D rect = (Rectangle2D) view.getBounds().clone();
                Rectangle bounds = new Rectangle((int) rect.getX(), (int) rect
                        .getY(), (int) rect.getWidth(), (int) rect.getHeight());
                //(Rectangle) view.getBounds().clone();

                // adjust
                bounds.x = min.x
                        + spacing.x
                        * ((vertical) ? wrapper.getGridPosition()
                                : rowCellCount);
                bounds.y = min.y
                        + spacing.y
                        * ((vertical) ? rowCellCount : wrapper
                                .getGridPosition());

                Object cell = view.getCell();
                Map map = new Hashtable();
                GraphConstants.setBounds(map, (Rectangle2D) bounds.clone());

                viewMap.put(cell, map);

            }

        }
        jgraph.getGraphLayoutCache().edit(viewMap, null, null, null);
    }

    /**
     * Code is equal to superclass code but had to be included here because
     * otherwise problems with the non-visible inner class CellWrapper would
     * occur.
     *
     * cell wrapper contains all values for one node
     */
    class CellWrapper implements Comparable {

        /**
         * sum value for edge Crosses
         */
        private double edgeCrossesIndicator = 0;

        /**
         * counter for additions to the edgeCrossesIndicator
         */
        private int additions = 0;

        /**
         * the vertical level where the cell wrapper is inserted
         */
        int level = 0;

        /**
         * current position in the grid
         */
        int gridPosition = 0;

        /**
         * priority for movements to the barycenter
         */
        int priority = 0;

        /**
         * reference to the wrapped cell
         */
        VertexView vertexView = null;

        /**
         * creates an instance and memorizes the parameters
         *
         */
        CellWrapper(int level, double edgeCrossesIndicator,
                VertexView vertexView) {
            this.level = level;
            this.edgeCrossesIndicator = edgeCrossesIndicator;
            this.vertexView = vertexView;
            additions++;
        }

        /**
         * returns the wrapped cell
         */
        VertexView getVertexView() {
            return vertexView;
        }

        /**
         * resets the indicator for edge crosses to 0
         */
        void resetEdgeCrossesIndicator() {
            edgeCrossesIndicator = 0;
            additions = 0;
        }

        /**
         * retruns the average value for the edge crosses indicator
         *
         * for the wrapped cell
         *
         */

        double getEdgeCrossesIndicator() {
            if (additions == 0)
                return 0;
            return edgeCrossesIndicator / additions;
        }

        /**
         * Addes a value to the edge crosses indicator for the wrapped cell
         *
         */
        void addToEdgeCrossesIndicator(double addValue) {
            edgeCrossesIndicator += addValue;
            additions++;
        }

        /**
         * gets the level of the wrapped cell
         */
        int getLevel() {
            return level;
        }

        /**
         * gets the grid position for the wrapped cell
         */
        int getGridPosition() {
            return gridPosition;
        }

        /**
         * Sets the grid position for the wrapped cell
         */
        void setGridPosition(int pos) {
            this.gridPosition = pos;
        }

        /**
         * increments the the priority of this cell wrapper.
         *
         * The priority was used by moving the cell to its barycenter.
         *
         */

        void incrementPriority() {
            priority++;
        }

        /**
         * returns the priority of this cell wrapper.
         *
         * The priority was used by moving the cell to its barycenter.
         */
        int getPriority() {
            return priority;
        }

        /**
         * @see java.lang.Comparable#compareTo(Object)
         */
        public int compareTo(Object compare) {
            if (((CellWrapper) compare).getEdgeCrossesIndicator() == this
                    .getEdgeCrossesIndicator())
                return 0;

            double compareValue = (((CellWrapper) compare)
                    .getEdgeCrossesIndicator() - this.getEdgeCrossesIndicator());

            return (int) (compareValue * 1000);

        }
    }

    /**
     * This class contains the criteria by which the nodes (contained in a
     * CellWrapper) shall be sorted in the levels.
     *
     * @author <a href="mailto:westerhe@fmi.uni-passau.de>Marieke Westerheide
     *         </a>
     * @version 1.1
     */
    class NodeComparator implements Comparator {

        /**
         * The nodes are sorted ascending by their ID.
         *
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         * @param o1
         *            the CellWrapper object containing a node to be compared to
         *            o2
         * @param o2
         *            the CellWrapper object containing a node to be compared to
         *            o1
         * @return a negative integer, zero, or a positive integer as the first
         *         argument is less than, equal to, or greater than the second.
         */
        public int compare(Object o1, Object o2) {
            if (!(o1 instanceof CellWrapper) || !(o2 instanceof CellWrapper)) {
                throw new ClassCastException("CellWrapper expected");
            }
            SDGNode n1 = (SDGNode) ((DefaultGraphCell) (((CellWrapper) o1)
                    .getVertexView()).getCell()).getUserObject();
            SDGNode n2 = (SDGNode) ((DefaultGraphCell) (((CellWrapper) o2)
                    .getVertexView()).getCell()).getUserObject();
            if (n1.getId() < n2.getId()) {
                return -1;
            } else if (n1.getId() > n2.getId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

}
