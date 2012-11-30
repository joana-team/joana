/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;

/**
 * in a conditional, contains which node will be branched into in case of a
 * positive evaluation, and a negative evaluation.
 */
class ConditionalClause {
    Node yesNode;

    Node noNode;
}

class Node implements Comparable<Node> {
    private int index = 0;

    private IExplodedBasicBlock block = null;

    /** list of normal parents */
    private TreeSet<Node> parents;

    /** list of normal children */
    private TreeSet<Node> children;

    /** nodes from whom this node catches exceptions */
    private TreeSet<Node> caught;

    /** exceptions that this node throws, and the corresponding catcher */
    private HashMap<TypeReference, Node> thrown;

    /**
     * if this node represents a conditional, this contains which child
     * represents the yes, and which the no.
     */
    private ConditionalClause condClause = null;

    Node(int i) {
        index = i;
    }

    void initNode(IExplodedBasicBlock b) {
        block = b;
        parents = new TreeSet<Node>();
        children = new TreeSet<Node>();
        thrown = new HashMap<TypeReference, Node>();
        caught = new TreeSet<Node>();
    }

    public int compareTo(Node o) {
        return index - o.index;
    }

    public boolean equals(Object o) {
        return o instanceof Node && ((Node) o).index == index;
    }

    public String toString() {
        return "[" + index + " "
                + (block == null ? "null" : DotWriter.getLabel(block)) + "]";
    }

    boolean hasInstruction() {
        return block == null ? false : (block.getInstruction() != null);
    }

    IExplodedBasicBlock getBlock() {
        return block;
    }

    TreeSet<Node> getChildren() {
        return children;
    }

    TreeSet<Node> getParents() {
        return parents;
    }

    /** get a list of blocks for whom we act as a catcher */
    TreeSet<Node> getCaught() {
        return caught;
    }

    int getIndex() {
        return index;
    }

    /** get a list of exceptions and the blocks that catch it */
    HashMap<TypeReference, Node> getThrown() {
        return thrown;
    }

    /** which exceptions does this node throw? */
    Set<TypeReference> getThrownExceptions() {
        return thrown.keySet();
    }

    /** which blocks catch an exception from this node? */
    Collection<Node> getCatchers() {
        return thrown.values();
    }

    ConditionalClause getCondClause() {
        return condClause;
    }

    boolean hasCondClause() {
        return condClause != null;
    }

    void createCondClause(Node yes, Node no) {
        condClause = new ConditionalClause();
        condClause.yesNode = yes;
        condClause.noNode = no;
    }

}
