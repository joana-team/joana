/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;


/**
 * An Instace of ThreadRegion represents a thread region based on Nandas definition.
 * It contains the start node and a list of the covered nodes.
 *
 * @author  Dennis Giffhorn
 * @version 1.0
 * @see VirtualNode
 */
public class ThreadRegion {
    static class Color implements Iterable<SDGNode> {
        Set<SDGNode> color;

        Color() {
        	color = new HashSet<SDGNode>();
        }

        public int hashCode() {
            int hc = 0;

            for (SDGNode n : color) {
                hc += n.getId() * 31;
            }

            return hc;
        }

        public boolean equals (Object o) {
            boolean equal = false;

            if (o instanceof Color) {
                Color c = (Color) o;

                if (c.color.equals(color)) {
                    equal = true;
                }
            }

            return equal;
        }

        /* (non-Javadoc)
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<SDGNode> iterator() {
            return color.iterator();
        }

        public String toString() {
            return color.toString();
        }
    }

    /** A list of the nodes this ThreadRegion covers. */
    private Collection<SDGNode> nodes;
    private Color key;

    /** An ID. */
    private final int id;
    private final int thread;
    private final boolean dynamic;
    private final SDGNode start;

    public ThreadRegion(int id, SDGNode start, int thread, boolean dynamic, Collection<SDGNode> nodes) {
        this.id = id;
        this.thread = thread;
        this.nodes = nodes;
        this.start = start;
        this.dynamic = dynamic;
    }

    public Color getKey() {
        return key;
    }

    public SDGNode getStart() {
    	return start;
    }

    /**
     * Returns the list of the covered nodes.
     */
    public Collection<SDGNode> getNodes() {
    	if (!knowsNodes()) throw new UnsupportedOperationException();
        return nodes;
    }

    /**
     * Returns the region's ID.
     */
    public int getID() {
        return id;
    }

    /**
     * Returns the thread this thread region belongs to.
     */
    public int getThread() {
        return thread;
    }
    
    public boolean knowsNodes() {
    	return nodes != null;
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

    /**
     * Prints the region's attributes.
     */
    public String toString() {
        String str = "REGION "+id+"\n";

        str += "Start: ";
        str += start+"\n";
        str += "Thread: ";
        str += thread+"\n";
        str += "Dynamic: ";
        str += dynamic+"\n";
        str += "Size: ";
        str += nodes == null ? "UNKNOWN\n" : nodes.size()+"\n";
        str += nodes == null ? "\n"        : nodes+"\n";

        return str;
    }

    public int hashCode() {
        return id;
    }

    public boolean equals(Object o) {
        boolean b = false;

        if (o instanceof ThreadRegion) {
            ThreadRegion t = (ThreadRegion) o;
            b = t.id == id;
        }

        return b;
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

    public boolean isDynamic() {
        return dynamic;
    }

    public boolean verify() {
    	if (!knowsNodes()) return true;
        for (SDGNode n : nodes) {
            if (!n.isInThread(thread)) {
                System.out.println(n+" does not belong to this thread");
                return false;
            }
        }
        return true;
    }
}
