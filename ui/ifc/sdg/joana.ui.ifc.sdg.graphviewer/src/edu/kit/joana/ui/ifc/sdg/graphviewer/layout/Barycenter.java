/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Barycenter.java
 *
 * Created on 1. September 2005, 09:53
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;



/**
 * This class implements the Barycenter algorithm.
 * @author Siegfried Weber
 */
public class Barycenter extends CrossReductionStrategy {

    /**
     * level sweep steps
     */
    private static final int REDUCTION_STEPS = 4;
    /**
     * estimated time
     */
    private static final int TIME = (194 + 57 + 23 + 23) / REDUCTION_STEPS;

    /**
     * Returns the maximum time for this graph.
     * @param graph a PDG
     * @return the maximum time
     */
    @Override
	int getMaxTime(PDG graph) {
        return TIME * 4;
    }

    /**
     * Implements the level sweep.
     * @param graph a PDG
     */
    @Override
	public void reduce(PDG graph) {
        start();
        sortVertices(graph.getLevel(0));
        for(int i = 0; i < REDUCTION_STEPS; i++) {
            for(int level = 1; level < graph.getLevelCount(); level++)
                reduceLevel(graph.getLevel(level), true);
            for(int level = graph.getLevelCount() - 2; level >= 0; level--)
                reduceLevel(graph.getLevel(level), false);
            check("reduceLevel");
            go(TIME);
        }
        // commented because the most crosses should be in the upper layers
        //for(int level = 1; level < graph.getLevelCount(); level++)
        //    reduceLevel(graph.getLevel(level), true);
        complete(graph);
    }

    /**
     * Calculates the barycenter values.
     * @param level the level for which the barycenter values are calculated
     * @param down true if the crosses with at lower level are regarded
     */
    private void reduceLevel(Level level, boolean down) {
        for(Node node : level) {
            Average value = new Average();
            Node[] neighbors;
            if(down)
                neighbors = node.getUpperNeighbors();
            else
                neighbors = node.getLowerNeighbors();
            for(Node neighborNode : neighbors)
                value.add(neighborNode.getIndex());
            if(value.isValid()) {
                // Trick: Wenn zwei Knoten einen Vater haben, dann haben sie
                // den gleich Barycenter-Wert. Im letzten Durchlauf hatten sie
                // aber verschiedene Werte. Diese Werte werden zu einem kleinen
                // Teil mituebernommen und beeinflussen so die Kantenkreuzungen
                // in den Ebenen, die in diesem Durchlauf eigentlich nicht
                // betrachtet werden.
                //float prevBary = NodeConstants.getBarycenter(node);
                NodeConstants.setBarycenter(node, value.getValue());
            }
        }
        // correct barycenter values
        sortVertices(level);
        level.sort(new BarycenterComparator());
    }

    /**
     * Changes the barycenter values so that the nodes are sorted to their IDs.
     * @param level the level for which the barycenter values are changed
     */
    private void sortVertices(Level level) {
        Set<Node> orderedSet = new TreeSet<Node>(new IDComparator());
        for(Node node : level)
            if(node.isVertex())
                orderedSet.add(node);
        Node[] orderedID = orderedSet.toArray(new Node[orderedSet.size()]);
        Node[] orderedBary = orderedSet.toArray(new Node[orderedSet.size()]);
        Arrays.sort(orderedBary, new BarycenterComparator());
        Set<Node> a = new TreeSet<Node>(new IDComparator());
        Set<Node> b = new TreeSet<Node>(new IDComparator());
        Average avg = new Average();
        for(int i = 0; i < orderedID.length; i++) {
            add(orderedID[i], a, b, avg);
            add(orderedBary[i], a, b, avg);
            if(a.size() == 0) {
                float f = 0f;
                for(Node n : b) {
                    NodeConstants.setBarycenter(n, avg.getValue() + f);
                    f += 0.1f;
                }
                avg = new Average();
                b.clear();
            }
        }
    }

    /**
     * Adds a node to set A and add its barycenter value to an average value.
     * If A already contains this node then delete it from A and add it to set
     * B.
     * @param node a node
     * @param a set A
     * @param b set B
     * @param avg an average value
     */
    private void add(Node node, Set<Node> a, Set<Node> b, Average avg) {
        if(a.contains(node)) {
            a.remove(node);
            b.add(node);
            avg.add(NodeConstants.getBarycenter(node));
        }
        else
            a.add(node);
    }

    /**
     * This comparator compares the barycenter values of two nodes.
     */
    class BarycenterComparator implements Comparator<Node> {

        /**
         * Compares the barycenter value of two nodes.
         * @param o1 a node
         * @param o2 a node
         * @return the same as the Float-comparator
         */
        public int compare(Node o1, Node o2) {
            float o1Val = NodeConstants.getBarycenter(o1);
            float o2Val = NodeConstants.getBarycenter(o2);
            return Float.compare(o1Val, o2Val);
        }
    }

    /**
     * Stores an average value.
     */
    class Average {

        /**
         * the sum of the values
         */
        private float sum;
        /**
         * the number of values
         */
        private int numbers;

        /**
         * Creates a new instance.
         */
        public Average() {
            sum = 0f;
            numbers = 0;
        }

        /**
         * Adds a value to this average value.
         * @param value the value to add
         */
        public void add(float value) {
            sum += value;
            numbers++;
        }

        /**
         * The average value is valid if it contains at least one entry.
         * @return true if this average value contains at least one entry
         */
        public boolean isValid() {
            return numbers > 0;
        }

        /**
         * Returns the average value.
         * @return the average value
         */
        public float getValue() {
            return sum / numbers;
        }
    }
}
