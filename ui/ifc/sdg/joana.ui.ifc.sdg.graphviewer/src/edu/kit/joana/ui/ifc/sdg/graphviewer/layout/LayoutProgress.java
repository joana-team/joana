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
 * LayoutProgress.java
 *
 * Created on 29. September 2005, 12:50
 */

package edu.kit.joana.ui.ifc.sdg.graphviewer.layout;

/**
 * This class provides methods for determine the progress of the layout.
 * @author Siegfried Weber
 */
public abstract class LayoutProgress {

    /**
     * verbose output
     */
    private static final boolean VERBOSE = false;

    /**
     * the current time (between 0 and getMaxTime())
     */
    private int time;
    /**
     * stores the system time (used for verbose output)
     */
    private long timer;

    /**
     * Returns the maximum time for a phase with the given PDG.
     * @param graph a PDG
     * @return the maximum time
     */
    abstract int getMaxTime(PDG graph);

    /**
     * Returns the current time of the phase.
     * @return the current time of the phase
     */
    public int getTime() {
        return time;
    }

    /**
     * Progresses the time.
     * @param time the progressed time
     */
    public void go(int time) {
        this.time += time;
    }

    /**
     * Completes a phase.
     * @param graph a PDG
     */
    public void complete(PDG graph) {
        if(VERBOSE && time != getMaxTime(graph))
            System.out.println(this.getClass() + ": not complete (" + time +
                    "/" + getMaxTime(graph) + ")");
    }

    /**
     * Starts a phase.
     */
    protected void start() {
        if(VERBOSE)
            timer = System.currentTimeMillis();
    }

    /**
     * Prints the current status of the phase if verbose output is active.
     * @param proc the current procedure of the phase
     */
    protected void check(String proc) {
        if(VERBOSE) {
            System.out.println(this.getClass() + "." + proc + ": " +
                    (System.currentTimeMillis() - timer) + " ms");
            timer = System.currentTimeMillis();
        }
    }
}
