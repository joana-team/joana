/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * LeftAssignment.java
 *
 * Created on 7. September 2005, 19:38
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

/**
 * This class implements the coordinate assignment where all nodes are left aligned.
 * It is meant only for test purposes.
 * @author Siegfried Weber
 */
public class LeftAssignment extends CoordinateAssignmentStrategy {

    /**
     * Returns the maximum time for this graph.
     * @param graph a PDG
     * @return the maximum time
     */
    @Override
	int getMaxTime(PDG graph) {
        return 0;
    }

    /**
     * Starts the left aligned coordinate assignment.
     * @param graph a PDG
     * @param delta the minimum space between nodes
     */
    @Override
	public void assignCoordinates(PDG graph, int delta) {
        start();
        for(Level level : graph.getLevels()) {
            int x = 0;
            for(Node node : level) {
                node.setXCoord(x);
                x += delta;
            }
        }
        check("assignCoordinates");
        complete(graph);
    }
}
