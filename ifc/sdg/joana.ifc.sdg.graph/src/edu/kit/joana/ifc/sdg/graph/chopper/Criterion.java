/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper;

import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * A chopping criterion purpose-built for efficient same-level chopping{@link SummaryMergedChopper}.
 *
 * Contains an entry node, a set of formal-in node used as source criterion and
 * a set of formal-out nodes used as target criterion.
 *
 * -- Created on July 17, 2007
 *
 * @author  Dennis Giffhorn
 */
public class Criterion {
	SDGNode entry;
    TreeSet<SDGNode> source;
	TreeSet<SDGNode> target;

	/**
     * Creates a new instance of Criterion.
     * @param entry  The entry node. Must not be null.
     */
    public Criterion(SDGNode entry) {
    	this.entry = entry;
        source = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        target = new TreeSet<SDGNode>(SDGNode.getIDComparator());
    }

    /**
     * @return the entry node.
     */
    public SDGNode getEntry() {
		return entry;
	}

    /**
     * @return the source criterion.
     */
	public TreeSet<SDGNode> getSource() {
		return source;
	}

    /**
     * @return the target criterion.
     */
	public TreeSet<SDGNode> getTarget() {
		return target;
	}

    /**
     * @return a textual representation.
     */
    public String toString() {
        return entry + ":  ({"+source+"}, {"+target+"})";
    }
}
