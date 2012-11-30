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
 * Context.java
 *
 * Created on 7. September 2005, 14:18
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

import java.util.List;
import org.jgraph.graph.DefaultEdge;

/**
 * The context of the strategy pattern.
 * @author Siegfried Weber
 */
public class Context {

    /**
     * the decycling phase
     */
    private DecyclingStrategy decycling;
    /**
     * the leveling phase of the Sugiyama algorithm
     */
    private LevelingStrategy leveling;
    /**
     * the cross reduction phase of the Sugiyama algorithm
     */
    private CrossReductionStrategy crossReduction;
    /**
     * the coordinate assingment phase of the Sugiyama algorithm
     */
    private CoordinateAssignmentStrategy coordinateAssignment;
    /**
     * the post processing phase
     */
    private LayoutStrategy layout;

    /**
     * Creates a new instance of Context and assigns concrete algorithms to the
     * phases.
     */
    public Context() {
        setDFSDecycling();
        setLongestPath();
        setBarycenter();
        setBrandesKoepf();
        setGraphLayout();
    }

    /**
     * Sets the depth first search algorithm for the decycling phase.
     */
    public void setDFSDecycling() {
        decycling = new DFSDecycling();
    }

    /**
     * Returns the decycling strategy.
     * @return the decycling strategy
     */
    public DecyclingStrategy getDecyclingStrategy() {
        return decycling;
    }

    /**
     * Starts the decycling phase.
     * @param graph a PDG
     * @return a list of turned around edges
     */
    public List<DefaultEdge> decycle(PDG graph) {
        return decycling.decycle(graph);
    }

    /**
     * Recreates the cycles.
     * @param graph a PDG
     * @param turnedEdges the turned around edges
     */
    public void undoDecycling(PDG graph, List<DefaultEdge> turnedEdges) {
        decycling.undo(graph, turnedEdges);
    }

    /**
     * Sets the longest path algorithm for the leveling phase.
     */
    public void setLongestPath() {
        leveling = new LongestPath();
    }

    /**
     * Returns the leveling strategy.
     * @return the leveling strategy
     */
    public LevelingStrategy getLevelingStrategy() {
        return leveling;
    }

    /**
     * Starts the leveling phase.
     * @param graph a PDG
     */
    public void leveling(PDG graph) {
        leveling.level(graph);
    }

    /**
     * Sets the barycenter algorithm for the cross reduction phase.
     */
    public void setBarycenter() {
        crossReduction = new Barycenter();
    }

    /**
     * Returns the cross reduction strategy.
     * @return the cross reduction strategy
     */
    public CrossReductionStrategy getCrossReductionStrategy() {
        return crossReduction;
    }

    /**
     * Starts the cross reduction phase.
     * @param graph a PDG
     */
    public void crossReduction(PDG graph) {
        crossReduction.reduce(graph);
    }

    /**
     * Sets the left assignment algorithm for the coordinate assignment phase.
     */
    public void setLeftAssignment() {
        coordinateAssignment = new LeftAssignment();
    }

    /**
     * Sets the Brandes/Koepf algorithm for the coordinate assignment phase.
     */
    public void setBrandesKoepf() {
        coordinateAssignment = new BrandesKoepf();
    }

    /**
     * Returns the coordinate assignment phase.
     * @return the coordinate assignment phase
     */
    public CoordinateAssignmentStrategy getCoordinateAssignmentStrategy() {
        return coordinateAssignment;
    }

    /**
     * Starts the coordinate assignment phase.
     * @param graph a PDG
     * @param delta the minimum space between the nodes
     */
    public void assignCoordinates(PDG graph, int delta) {
        coordinateAssignment.assignCoordinates(graph, delta);
    }

    /**
     * Sets the standard post processing algorithm for the post processing
     * phase.
     */
    public void setGraphLayout() {
        layout = new GraphLayout();
    }

    /**
     * Returns the post processing strategy.
     * @return the post processing strategy
     */
    public LayoutStrategy getLayoutStrategy() {
        return layout;
    }

    /**
     * Starts the post processing phase.
     * @param graph a PDG
     * @param levelSpacing the minimum space between the nodes and the
     *                     horizontal edge segments.
     */
    public void layout(PDG graph, int levelSpacing) {
        layout.layout(graph, levelSpacing);
    }
}
