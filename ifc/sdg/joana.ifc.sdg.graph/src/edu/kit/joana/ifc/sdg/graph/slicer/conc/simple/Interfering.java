/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.graph.slicer.conc.simple;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * A data structure representing an interference edge traversion that has to be validated.
 * It consists of the edge's sink node and it's thread states, and of the edge's source and
 * it's thread.
 *
 * -- Created on March 21, 2006
 *
 * @author  Dennis Giffhorn
 */
public class Interfering {
    /** The sink of the interference edge. */
    private final SDGNode sink;
    private final int sinkThread;

    /** The interference edge's source. */
    private final SDGNode source;

    /** The thread of 'source'. */
    private final int sourceThread;

    /**
     * Creates a new instance of Interfering
     *
     * @param so  The interference edge's source.
     * @param si  The sink of the interference edge.
     * @param sot  The thread of so.
     * @param sit  The thread of si.
     */
    public Interfering(SDGNode so, int sot, SDGNode si, int sit) {
        source = so;
        sourceThread = sot;
        sink = si;
        sinkThread = sit;
    }

    /**
     * Retrieves the sink of the interference edge.
     */
    public SDGNode getSink() {
        return sink;
    }

    /**
     * Returns the interference edge's source.
     */
    public SDGNode getSource() {
        return source;
    }

    /**
     * Returns the state of the interference edge's source thread.
     *
     * @return  A node, representing the state.
     */
//    public SDGNode getTarget() {
//        return sink.getStates().get(sourceThread);
//    }

    /**
     * Returns the thread of the interference edge's source.
     */
    public int getSourceThread() {
        return sourceThread;
    }

    public int getSinkThread() {
        return sinkThread;
    }

    /**
     * A textual representation of this Interfering object.
     */
    public String toString() {
        String str = "";

        str += "from: ("+sink+", "+sinkThread+")\n";
        str += "to: ("+source+", "+sourceThread+")";

        return str;
    }

    public boolean equals(Object o) {
        if (o instanceof Interfering) {
            Interfering i = (Interfering) o;
            return i.source == source && i.sourceThread == sourceThread;

        } else {
            return false;
        }
    }

    public int hashCode() {
        return sourceThread * 101 + source.hashCode();
    }
}
