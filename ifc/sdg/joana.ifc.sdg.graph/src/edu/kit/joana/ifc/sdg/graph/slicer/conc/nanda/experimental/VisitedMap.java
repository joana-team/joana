/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * A map for saving already appeared state tuples.
 * Every state tuple entry is bound to the corresponding node and its context.
 *
 * -- Created on October 4, 2005
 *
 * @author  Dennis Giffhorn
 * @version 1.0
 * @see States
 * @see SDGNode
 * @see ISCR
 */
public class VisitedMap {
    static class Key {
        SDGNode v;
        int thread;
        int topNr;

        Key(SDGNode v, int thread, int topNr) {
            this.v = v;
            this.thread = thread;
            this.topNr = topNr;
        }

        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key k = (Key) o;
                return k.topNr == topNr && k.thread == thread && k.v == v;

            } else {
                return false;
            }

        }

        public int hashCode() {
            return (31*thread + v.hashCode()) * 31 + topNr;
        }
    }


    Map<Key, List<States>> map;

    /**
     * Creates a new instance of VisitedStatesMap
     */
    public VisitedMap() {
        this.map = new HashMap<Key, List<States>>();
    }

    /**
     * Inserts a state tuple into the map.
     * The given SDGNode and the ISCR form the key.
     *
     * @param node          A part of the key.
     * @param nodesISCR     A part of the key.
     * @param statesToMark  The state tuple to insert.
     */
    public void put(SDGNode v, int thread, int tnr, States statesToMark) {
        // make a new Key
        List<States> marks = map.get(new Key(v, thread, tnr));

        if (marks == null) {
        	marks = new LinkedList<States>();
            map.put(new Key(v, thread, tnr), marks);
        }

        // update the list and then the map
        marks.add(statesToMark);
    }

    /**
     * Returns the saved States for the Key consisting of the given
     * SDGNode and ISCR.
     *
     * @param node          A part of the key.
     * @param nodesISCR     A part of the key.
     * @return              The saved state tuples as a List of States.
     */
    public List<States> get(SDGNode v, int thread, int tnr) {
        return map.get(new Key(v, thread, tnr));
    }

    /**
     * Clears the map.
     * Wraps the clear-method of the used Map.
     */
    public void clear() {
        map.clear();
    }
}
