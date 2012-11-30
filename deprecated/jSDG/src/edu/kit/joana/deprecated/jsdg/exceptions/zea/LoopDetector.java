/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.exceptions.zea;

import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

/**
 * Representation of a loop with all its nested loops. This list only contains
 * the indices of the blocks inside the loop, the entry and the exit; the proper
 * subgraph is contained elsewhere (see FlowGraph.loopBlockSubgraph)
 */

class LoopList {
    FlowGraph mainGraph;

    private int entry;

    private int exit;

    private TreeSet<Integer> nodes;

    Vector<LoopList> subLoops;

    LoopList(FlowGraph mainGraph, TreeSet<Integer> nodes, int entry, int exit) {
        this.mainGraph = mainGraph;
        this.nodes = nodes;
        this.entry = entry;
        this.exit = exit;
        subLoops = new Vector<LoopList>();
    }

    boolean containsEdge(Node start, Node end) {
        return nodes.contains(start) && nodes.contains(end);
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("[ ");
        for (Integer node : nodes) {
            buf.append(node + " ");
        }
        for (LoopList graph : subLoops) {
            buf.append(graph.toString());
        }
        buf.append("(" + entry + ";" + exit + ")");
        buf.append("] ");
        return buf.toString();
    }

    LoopList getCycle(Node node) {
        for (LoopList graph : subLoops) {
            if (graph.entry == node.getIndex()) {
                return graph;
            }
        }
        return null;
    }

    TreeSet<Integer> getNodeIndices() {
        return nodes;
    }

    int getEntry() {
        return entry;
    }

    int getExit() {
        return exit;
    }
}

class Vertex {
    int index;

    int lowlink;

    Vertex() {
        index = -1;
        lowlink = -1;
    }

    boolean isUndefined() {
        return index == -1 && lowlink == -1;
    }
}

/** Creates a list of loops and loop subgraphs */
public class LoopDetector {
    private FlowGraph graph;

    private LoopList subGraph;

    private TreeMap<Integer, Vertex> indices;

    private Stack<Integer> stack;

    private Vector<LoopList> cyclicSubgraphs;

    private int depth;

    void detectLoops(LoopList sub) {
        graph = sub.mainGraph;
        subGraph = sub;

        indices = new TreeMap<Integer, Vertex>();
        for (Integer node : subGraph.getNodeIndices()) {
            indices.put(node, new Vertex());
        }

        cyclicSubgraphs = new Vector<LoopList>();
        depth = 0;
        stack = new Stack<Integer>();

        // System.out.println("Graph: " + subGraph);
        applyTarjan(sub.getEntry());

        sub.subLoops = cyclicSubgraphs;
        for (LoopList subgraph : cyclicSubgraphs) {
            new LoopDetector().detectLoops(subgraph);
        }
    }

    /**
     * Tarjan algorithm (with small variations) (Robert Tarjan: Depth-first
     * search and linear graph algorithms. In: SIAM Journal on Computing. Vol. 1
     * (1972), No. 2, P. 146-160)
     */
    private void applyTarjan(int index) {
        // System.out.println("Calling tarjan on " + index);
        Vertex v = indices.get(index);
        v.index = depth;
        v.lowlink = depth;
        depth++;

        stack.push(index);
        Node node = graph.getNode(index);
        TreeSet<Node> children = new TreeSet<Node>();
        children.addAll(node.getChildren());
        children.addAll(node.getCatchers());
        for (Node child : children) {
            if (!subGraph.getNodeIndices().contains(child.getIndex())) {
                continue;
            }
            if (node.getIndex() == subGraph.getExit()
                    && child.getIndex() == subGraph.getEntry()) {
                continue;
            }
            Vertex vp = indices.get(child.getIndex());
            if (vp.isUndefined()) {
                applyTarjan(child.getIndex());
                v.lowlink = Math.min(v.lowlink, vp.lowlink);
            } else if (stack.contains(child.getIndex())) {
                v.lowlink = Math.min(v.lowlink, vp.index);
            }
        }
        if (v.index == v.lowlink) {
            if (stack.peek() == index) {
                stack.pop(); // cycle of 1 is no cycle!
            } else {
                TreeSet<Integer> cycle = new TreeSet<Integer>();
                int child;
                int entry = index;
                int exit = -1;
                do {
                    child = stack.pop();
                    // the exit is the last visited node that is a parent of
                    // node
                    if (exit == -1
                            && (node.getParents()
                                    .contains(graph.getNode(child)) || node
                                    .getCaught().contains(graph.getNode(child)))) {
                        exit = child;
                    }
                    cycle.add(child);
                } while (child != index);
                if (exit == -1)
                    throw new IllegalArgumentException();
                LoopList subgraph = new LoopList(graph, cycle, entry, exit);
                // System.out.println("Adding " + subgraph);
                cyclicSubgraphs.add(subgraph);
            }
        }
    }
}
