/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * CrossReducing.java
 *
 * Created on 1. September 2005, 09:52
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

/**
 * This class is the super class for cross reduction strategies.
 * @author Siegfried Weber
 */
public abstract class CrossReductionStrategy extends LayoutProgress {

    /**
     * Starts the cross reduction.
     * @param graph a PDG
     */
    public abstract void reduce(PDG graph);
}
