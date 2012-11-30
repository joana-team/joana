/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.old;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

/**
 * A map needed for computing the thread regions.
 * It contains to a key of type VirtualNode all VirtualNodes, that the key
 * can reach (i.e. the thread region).
 * The class is a wrapper class that uses a HashMap and offers some method
 * to update the saved data.
 *
 * @author giffhorn
 * @version 1.0
 * @see VirtualNode
 * @see ThreadRegions
 */
public class VirtualMap {
    /** The used HashMap. */
    private Map<VirtualNode, List<VirtualNode>> map;

    /** Creates a new instance of VirtualMap. */
    public VirtualMap() {
        this.map = new HashMap<VirtualNode, List<VirtualNode>>();
    }

    /**
     * Shows if the map is empty.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Checks whether the map contains a certain key.
     *
     * @param virtual  The key.
     */
    public boolean containsKey(VirtualNode virtual) {
        return map.containsKey(virtual);
    }

    /**
     * Returns the saved VirtalNodes to a given key.
     *
     * @param virtual  The key.
     * @return         The assigned VirtualNodes in a List.
     */
    public List<VirtualNode> get(VirtualNode virtual) {
        return map.get(virtual);
    }

    /**
     * Returns the key set of this map.
     */
    public Set<VirtualNode> keySet() {
        return map.keySet();
    }

    /**
     * Inserts a VirtualNode to the List of VirtualNodes assigned to a given key.
     * If the key is not in the map yet, it is inserted with a new List,
     * containing the given VirtualNode that shall be assigned to the key.
     *
     * @param key   The key.
     * @param mark  To be assigned to the key.
     */
    public void update(VirtualNode key, VirtualNode mark) {
        if (map.containsKey(key)) {
            addMarkTo(key, mark);

        } else {
            put(key);
            addMarkTo(key, mark);
        }
    }

    /**
     * Inserts a VirtualNode to the List of VirtualNodes assigned to a given key.
     *
     * @param toMark  The key.
     * @param marker  To be assigned to the key.
     */
    private void addMarkTo(VirtualNode toMark, VirtualNode marker) {
        List<VirtualNode> marks = map.get(toMark);
        marks.add(marker);
    }

    /**
     * Inserts a new key to the map, with an empty List for assigned VirtualNodes.
     *
     * @param virtual  The new key.
     */
    private void put(VirtualNode virtual) {
        List<VirtualNode> maarks = new LinkedList<VirtualNode>();

        map.put(virtual, maarks);
    }
}
