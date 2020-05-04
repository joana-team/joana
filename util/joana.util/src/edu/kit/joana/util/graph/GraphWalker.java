/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util.graph;

import org.jgrapht.DirectedGraph;

import java.util.*;

/**
 * Provides ways to traverse a directed graph. Currently, only depth first search (DFS) is implemented.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <V>
 * @param <E>
 */
public abstract class GraphWalker<V, E> {

    private final DirectedGraph<V, E> graph;

    public GraphWalker(DirectedGraph<V, E> graph) {
        this.graph = graph;
    }

    public final DirectedGraph<V, E> getGraph() {
        return graph;
    }

    public enum Mode {
        /**
         * More advanced, non recursive for larger graphs
         */
        NO_RECURSION,
        /**
         * Simple, recursive for small graphs
         */
        RECURSION
    }

    public static final Mode mode = Mode.NO_RECURSION;

    public final void traverseDFS(final V start) {
        dfs(start);
    }

    public final Set<V> traverseDFS(final Iterable<V> starts) {
        Set<V> visited = createInitialSet();
        for (V start : starts) {
            dfs(mode, start, visited);
        }
        return visited;
    }

    private void dfs(final V node){
        dfs(mode, node, createInitialSet());
    }

    protected void dfs(Mode mode, final V node, final Set<V> visited){
        switch (mode){
        case NO_RECURSION:
            dfsNoRecursion(node, visited);
            break;
        case RECURSION:
            dfsRecursion(node, visited);
        }
    }

    protected Set<V> createInitialSet(){
        return new HashSet<V>(graph.vertexSet().size());
    }

    private void dfsRecursion(final V node, final Set<V> visited) {
        if (visited.add(node)) {

            discover(node);

            final Iterable<E> outEdges = newOutEdges(graph.outgoingEdgesOf(node));
            for (final E out : outEdges) {
                if (traverse(node, out)) {
                    final V succ = graph.getEdgeTarget(out);
                    dfsRecursion(succ, visited);
                }
            }

            finish(node);
        }
    }
    
    /**
     * Change this to specify which edges should be used by the walker.
     * @param edge The edge that may be traversed.
     * @return true if the provided edge should be traversed.
     */
    public boolean traverse(V node, E edge) {
    	return true;
    }

    /**
     * Change this to specify how the edges to be traverse by a given walker is to be remembers.
     * This can be used to, e.g. specify a fixed iteration order of egdes, or,
     * apparently, to create a new Iterable in case the walker modifies the underlying graph, and hence 
     * prevent concurrent modification exceptions (though i'm not quite sure this ever makes sense).
     * @param outEdges
     * @return
     */
    protected Iterable<E> newOutEdges(Set<E> outEdges) {
    	return outEdges;
    }
    
    /**
     * Is called when the node is discovered by the walker.
     * @param node The node that has been discovered.
     */
    public abstract void discover(V node);

    /**
     * Is called when the node is left (finished) by the walker.
     * @param node The node that is left.
     */
    public abstract void finish(V node);



    private class StackFrame {
        private final V node;
        private final Iterator<E> iter;

        private StackFrame(V node) {
            this.node = node;
            this.iter = newOutEdges(graph.outgoingEdgesOf(node)).iterator();
        }
    }

    /**
     * Faster non recursive version that produces exactly the same results as the recursive version
     *
     * @param node
     * @param visited
     */
    private void dfsNoRecursion(final V node, final Set<V> visited) {

        Stack<StackFrame> stack = new Stack<>();
        if (visited.add(node)) {

            discover(node);
            stack.push(new StackFrame(node));

            while (!stack.isEmpty()){
                StackFrame topFrame = stack.peek();
                if (topFrame.iter.hasNext()){
                    E edge = topFrame.iter.next();
                    if (traverse(topFrame.node, edge)){
                        V succ = graph.getEdgeTarget(edge);
                        if (visited.add(succ)){
                            discover(succ);
                            stack.push(new StackFrame(succ));
                        }
                    }
                } else {
                    stack.pop();
                    finish(topFrame.node);
                }
            }
        }
    }

    static class DFSCollector<V, E> extends GraphWalker<V, E>  {

        private List<V> nodes;
        private final Mode mode;

        DFSCollector(DirectedGraph<V, E> graph, Mode mode) {
            super(graph);
            this.mode = mode;
        }

        List<V> collect(V start){
            nodes = new ArrayList<>(getGraph().vertexSet().size());
            dfs(mode, start, createInitialSet());
            return nodes;
        }

        @Override public void discover(V node) {
            nodes.add(node);
        }

        @Override public void finish(V node) {
            nodes.add(node);
        }
    }

    public boolean checkDifferentModes(V node){
        List<V> rec = new DFSCollector<>(graph, Mode.RECURSION).collect(node);
        List<V> nonRec = new DFSCollector<>(graph, Mode.NO_RECURSION).collect(node);
        return rec.equals(nonRec);
    }
}
