/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * DecyclingStrategy.java
 *
 * Created on 13. Dezember 2005, 15:04
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.util.List;

import org.jgraph.graph.DefaultEdge;

/**
 * This class is the super class for decycling strategies.
 * @author Siegfried Weber
 */
public abstract class DecyclingStrategy extends LayoutProgress {

    /**
     * Starts the decycling.
     * @param graph a PDG
     * @return the turned around edges
     */
    public abstract List<DefaultEdge> decycle(PDG graph);

    /**
     * Recreates the cycles.
     * @param graph a PDG
     * @param turnedEdges the turned around edges
     */
    public abstract void undo(PDG graph, List<DefaultEdge> turnedEdges);
}
