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
 * CoordinateAssignmentStrategy.java
 *
 * Created on 7. September 2005, 19:36
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

/**
 * This class is the super class for coordinate assignment strategies.
 * @author Siegfried Weber
 */
public abstract class CoordinateAssignmentStrategy extends LayoutProgress {

    /**
     * Starts the coordinate assignment.
     * @param graph a PDG
     * @param delta the minimum space between the nodes
     */
    public abstract void assignCoordinates(PDG graph, int delta);
}
