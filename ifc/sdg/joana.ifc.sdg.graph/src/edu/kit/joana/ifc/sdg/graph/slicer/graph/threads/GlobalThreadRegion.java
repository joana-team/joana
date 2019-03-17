/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

public class GlobalThreadRegion {
    /** A list of the nodes this ThreadRegion covers. */
    protected final Collection<SDGNode> nodes;

    protected final SDGNode start;

    public GlobalThreadRegion(SDGNode start, Collection<SDGNode> nodes) {
    	this.start = start;
    	this.nodes = nodes;
    }
    
    
    public boolean knowsNodes() {
    	return nodes != null;
    }

    /**
     * Returns the list of the covered nodes.
     */
    public Collection<SDGNode> getNodes() {
    	if (!knowsNodes()) throw new UnsupportedOperationException();
        return nodes;
    }
    
    public SDGNode getStart() {
    	return start;
    }
    

    /**
     * Checks whether this ThreadRegion covers a given node.
     *
     * @param node  The node to check.
     */
    public boolean contains(SDGNode node) {
        if (!knowsNodes()) throw new UnsupportedOperationException();
        return nodes.contains(node);
    }
    
    public boolean consistsOf(Collection<SDGNode> nodes) {
    	if (!knowsNodes()) throw new UnsupportedOperationException();
        if (nodes.size() != this.nodes.size()) return false;

        for (SDGNode n : nodes) {
            if (!this.nodes.contains(n)) {
                return false;
            }
        }

        return true;
    }





}
