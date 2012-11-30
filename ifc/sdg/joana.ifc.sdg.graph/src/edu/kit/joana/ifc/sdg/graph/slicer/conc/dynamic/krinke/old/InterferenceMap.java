/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.Context;


/**
 *
 *
 * -- Created on December 5, 2005
 *
 * @author  Dennis Giffhorn
 */
public class InterferenceMap {
    private HashMap<Key, Collection<WorklistElement>> imap;
    private States initStates;

    /**
     * Creates a new instance of InterferenceMap
     */
    public InterferenceMap(States initStates) {
        this.imap = new HashMap<Key, Collection<WorklistElement>>();
        this.initStates = initStates;
    }

    public LinkedList<WorklistElement> get(WorklistElement elem) {
        Collection<WorklistElement> rawValues =
                imap.get(new Key(elem.getContext(), elem.getNode()));

        if (rawValues == null) {
            return null;

        } else {
            LinkedList<WorklistElement> result = new LinkedList<WorklistElement>();
            States elemStates = elem.getStates();

            for (WorklistElement raw : rawValues) {
                WorklistElement clone = raw.clone();

                for (int i = 0; i < elemStates.size(); i++) {
                    // substitute empty state tuples
                    if (clone.getStates().state(i).equals(initStates.state(i))) {
                        Context con = elemStates.state(i);
                        clone.setState(i, con);
                    }
                }

                result.add(clone);
            }

            return result;
        }
    }

    public void put(WorklistElement dummyCriterium, Collection<WorklistElement> dummyInterference) {
        Key key = new Key(dummyCriterium.getContext(), dummyCriterium.getNode());
        imap.put(key, dummyInterference);
    }

    /** Returns a string representation of the map.
     */
    public String toString() {
        return imap.toString();
    }

    static class Key {
        private Context context;
        private SDGNode node;

        Key(Context c,SDGNode n) {
            context = c;
            node = n;
        }

        Context getContext() {
            return context;
        }

        SDGNode getNode() {
            return node;
        }

        public String toString() {
            return context.toString();
        }

        public int hashCode() {
            return context.hashCode() | (node.getId() << 16);
        }

        public boolean equals(Object o) {
            Key k = (Key) o;
            return node == k.node && context.equals(k.context);
        }
    }
}
