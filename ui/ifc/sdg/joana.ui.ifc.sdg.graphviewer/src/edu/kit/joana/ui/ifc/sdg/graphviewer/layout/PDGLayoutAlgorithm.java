/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * PDGLayoutAlgorithm.java
 *
 * Created on 24. August 2005, 17:23
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;
import java.util.List;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.layout.JGraphLayoutAlgorithm;

/**
 * This is the main class of the layout algorithm.
 * @author Siegfried Weber
 */
public class PDGLayoutAlgorithm extends JGraphLayoutAlgorithm {


    /**
     * the context of the strategy pattern
     */
    private final Context context;
    /**
     * the layout progress
     */
    private LayoutProgress currentLayoutProgress;
    /**
     * the time to the last phase
     */
    private int time = 0;
    /**
     * the estimated time for the complete algorithm
     */
    private int maxTime = 1000;

    /**
     * Creates a new instance of the layout algorithm.
     */
    public PDGLayoutAlgorithm() {
        context = new Context();
    }

    /**
     * Layouts the PDG.
     * @param jGraph the JGraph component with the PDG
     * @param obj not used
     * @param param not used
     */
    @Override
	public void run(JGraph jGraph, Object[] obj) {
        PDG graph = new PDG(jGraph);

        // it's not really linear!
        maxTime = 50 * graph.getNodeCount();

        currentLayoutProgress = context.getDecyclingStrategy();
        List<DefaultEdge> turnedEdges = context.decycle(graph);

        currentLayoutProgress = context.getLevelingStrategy();
        context.leveling(graph);
        graph.completeLevels();

        maxTime = context.getCrossReductionStrategy().getMaxTime(graph) +
                context.getCoordinateAssignmentStrategy().getMaxTime(graph) +
                context.getLayoutStrategy().getMaxTime(graph);

        currentLayoutProgress = context.getCrossReductionStrategy();
        context.crossReduction(graph);
        time += currentLayoutProgress.getMaxTime(graph);

        currentLayoutProgress = context.getCoordinateAssignmentStrategy();
        context.assignCoordinates(graph, 3);
        time += currentLayoutProgress.getMaxTime(graph);

        currentLayoutProgress = context.getLayoutStrategy();
        context.layout(graph, 40);
        time += currentLayoutProgress.getMaxTime(graph);

        context.undoDecycling(graph, turnedEdges);

        currentLayoutProgress = null;
    }

    /**
     * Calculates and returns the progress.
     * @return the progress
     */
    @Override
	public int getProgress() {
        int progress = time;
        if(currentLayoutProgress != null)
            progress += currentLayoutProgress.getTime();
        return (int) (100 * (float) progress / maxTime);
    }

	@Override
	public void run(JGraph arg0, Object[] arg1, Object[] arg2) {
		run(arg0,arg1);

	}
}
