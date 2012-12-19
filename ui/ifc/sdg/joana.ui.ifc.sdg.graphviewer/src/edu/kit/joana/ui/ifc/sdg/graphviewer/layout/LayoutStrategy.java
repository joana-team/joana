/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * LayoutStrategy.java
 *
 * Created on 29. September 2005, 12:40
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

/**
 * This class is the super class for post processing strategies.
 * @author Siegfried Weber
 */
public abstract class LayoutStrategy extends LayoutProgress {

    /**
     * Starts the post processing.
     * @param graph a PDG
     * @param levelSpacing the minimum space between the nodes and the
     *                     horizontal edge segments
     */
    public abstract void layout(PDG graph, int levelSpacing);
}
