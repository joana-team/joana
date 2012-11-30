/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.SimpleDirectedGraph;

/**
 * Computation of dominators in a flow graph. Algorithm see "A fast algorithm
 * for finding dominators in a flowgraph" from Lengauer and Tarjan, TOPLAS 1979
 *
 * The dominators (DOM) of a node n in a directed graph with a unique start node (called flowgraph)
 * are all nodes that lie on every path from the start node to node n. Including the node n itself.
 * So when you cut the graph at any of node n's dominators, n cannot be reached from the
 * start anymore.
 *
 * Strict dominators (SDOM) of a node n are the same as dominators of node n, excluding the node n itself.
 *
 * As all nodes in the flowgraph have to be reachable from the start node (per definition), every node
 * has at least a single strict dominator - except the start node itself.
 *
 * So for each node n the set of strict dominantors SDOM(n) always contains a single dominator
 * called immediate dominator (IDOM). This is the only node in SDOM(n) that does not dominate any other
 * node in SDOM(n). There is always exactly one node that fulfills this property (theres a proof -> google)
 * This node the dominator that is "closest" to n.
 *
 * E.g.
 * <pre>
 * start -> n1 -> n2 -> n -\
 *            \-> n3 -> n4 -> n5
 *
 * DOM(n)  = {start, n1, n2, n}
 * SDOM(n) = {start, n1, n2}
 * IDOM(n) = n2
 *
 * SDOM(n5) = {start, n1}
 * IDOM(n5) = n1
 * </pre>
 *
 * As every node has a single immediate dominator, we can construct a graph in tree form where the
 * parent of each node is its immediate dominator. This tree is called dominator tree.
 *
 * E.g.
 * <pre>
 * start -> n1 -> n2 -> n
 *      |     \-> n3 -> n4
 *      \-> n5
 * </pre>
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <V> Type of the nodes in the flowgraph
 * @param <E> Type of the edges in the flowgraph
 */
public class Dominators<V, E> {

    public static <Y, Z> Dominators<Y, Z> compute(DirectedGraph<Y, Z> graph, Y entry) {
        Dominators<Y, Z> dom = new Dominators<Y, Z>(graph, entry);
        dom.compute();

        assert dom.assertConsistence();

        return dom;
    }

    private final DirectedGraph<V, E> graph;
    private final V start;

    @SuppressWarnings("unchecked")
    private Dominators(DirectedGraph<V, E> graph, V start) {
        this.graph = graph;
        this.start = start;
        this.dfsnum2node = (V[]) new Object[graph.vertexSet().size()];
    }

    /**
     *  Stores the parent node of each node during dfs computation. Nodes are identified
     *  by their dfs number.
     */
    private final Map<V, V> idom = new HashMap<V, V>();;

    /**
     * Contains the mapping from dfs number to node
     */
    private final V[] dfsnum2node;

    public V getIDom(V node) {
        return idom.get(node);
    }

    private Map<V, BitSet> idom2domiated;

    private final Iterable<V> emptyIterable = new DFSNumNodeIterable(new BitSet());

    public Iterable<V> getNodesWithIDom(V node) {
        if (idom2domiated == null) {
            // build datastructure on demand
            idom2domiated = new HashMap<V, BitSet>();

            // start from 1 - omit the root node at 0
            for (int i = 1; i < dfsnum2node.length; i++) {
                final V curr = dfsnum2node[i];
                final V idominator = idom.get(curr);
                BitSet dominates = idom2domiated.get(idominator);
                if (dominates == null) {
                    dominates = new BitSet();
                    idom2domiated.put(idominator, dominates);
                }
                dominates.set(i);
            }
        }

        final BitSet dominated = idom2domiated.get(node);

        return (dominated == null ? emptyIterable : new DFSNumNodeIterable(dominated));
    }

    public DomTree<V> getDominationTree() {
        DomTree<V> domTree = new DomTree<V>();

        for (V node : graph.vertexSet()) {
            domTree.addVertex(node);
        }

        for (V node : graph.vertexSet()) {
            V idom = getIDom(node);
            if (idom != null) {
                domTree.addEdge(idom, node);
            }
        }

        return domTree;
    }

    public static class DomTree<V> extends SimpleDirectedGraph<V, DomEdge> {

        private static final long serialVersionUID = 1445142467229185713L;

        private DomTree() {
            super(DomEdge.class);
        }

        public String toString() {
            return "IDOM Tree";
        }
    }

    public static class DomEdge {
        public DomEdge() {}

        public String toString() {
            return "IDOM";
        }
    }

    private class DFSNumNodeIterable implements Iterable<V> {
        private final BitSet nodes;

        public DFSNumNodeIterable(BitSet nodes) {
            this.nodes = nodes;
        }

        public Iterator<V> iterator() {
            return new Iterator<V>() {

                int index = 0;

                public boolean hasNext() {
                    return nodes.nextSetBit(index) >= 0;
                }

                public V next() {
                    if (index < 0) {
                        throw new NoSuchElementException();
                    }

                    index = nodes.nextSetBit(index);

                    if (index < 0) {
                        throw new NoSuchElementException();
                    }

                    index++;

                    return dfsnum2node[index - 1];
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

    }

    private void compute() {
        // first computation step: dfs discover time traversal of flow graph
        final DomDFSNumWalker walker = new DomDFSNumWalker();
        walker.traverseDFS(start);
        // walker.semi now contains mapping from node to dfs number (reverse mapping of dfsnum2node)

        final Forest<V> forest = new Forest<V>(walker.semi);
        // Maps a semidominator to a set of nodes it semidominates. Identified by dfsnum.
        final TIntObjectHashMap<BitSet> bucket = new TIntObjectHashMap<BitSet>();

        // for  all nodes except the root node (at index 0) we compute the dominator
        for (int dfsW = dfsnum2node.length - 1; dfsW > 0; dfsW--) {
            final V w = dfsnum2node[dfsW];

            if (w == null) {
                throw new IllegalStateException("Null node at dfsW=" + dfsW + " of a total of " + dfsnum2node.length
                        + ". This may happen when unreachable code is in the cfg.");
            }

            // step 2
            for (final E predEdge : graph.incomingEdgesOf(w)) {
                final V v = graph.getEdgeSource(predEdge);
                // u = EVAL(v)
                final V u = forest.eval(v);

                // if semi(u) < semi(w) then semi(w) := semi(u)
                final int semiW = walker.semi.get(w);
                final int semiU = walker.semi.get(u);
                if (semiU < semiW) {
                    walker.semi.put(w, semiU);
                }
            }

            // add w to bucket(vertex(semi(w)))
            final int semiW = walker.semi.get(w);
            BitSet bs = bucket.get(semiW);
            if (bs == null) {
                bs = new BitSet(dfsnum2node.length);
                bucket.put(semiW, bs);
            }
            bs.set(dfsW);

            // LINK(parent(w), w)
            final int parentNum = walker.parent.get(w);
            final V parentW = dfsnum2node[parentNum];
            forest.link(parentW, w);

            // step 3
            // for each v in bucket(parent(w)) do
            final BitSet dominated = bucket.get(parentNum);
            if (dominated == null) {
                continue;
            }
            for (int i = dominated.nextSetBit(0); i >= 0; i = dominated.nextSetBit(i + 1)) {
                final V v = dfsnum2node[i];
                // delete v from bucket(parent(w))
                dominated.clear(i);
                // u := EVAL(v)
                final V u = forest.eval(v);
                // dom(v) := if semi(u) < semi(v) then u else parent(w)
                final int semiU = walker.semi.get(u);
                final int semiV = walker.semi.get(v);
                if (semiU < semiV) {
                    idom.put(v, u);
                } else {
                    idom.put(v, parentW);
                }
            }

        }

        // step 4
        // for i := 2 to n do  -> note that our dfs numbers start at 0 not 1
        for (int i = 1; i < dfsnum2node.length; i++) {
            // w := vertex(i)
            final V w = dfsnum2node[i];
            // if dom(w) != vertex(semi(w)) then dom(w) := dom(dom(w))
            final V domW = idom.get(w);
            final V semiW = dfsnum2node[walker.semi.get(w)];
            if (domW != semiW) {
                final V domdomW = idom.get(domW);
                idom.put(w, domdomW);
            }
        }

        // dom(r) : = 0
        idom.put(start, null);
    }

    private final class DomDFSNumWalker extends GraphWalker<V, E> {

        /*
         * This variable has 2 usages depending on the state of the computation:
         * 1. It contains mapping from node to dfsnum after the first step
         * 2. It contains the semidominator of a node later on
         */
        final TObjectIntHashMap<V> semi = new TObjectIntHashMap<V>();
        /*
         * Stores the parent node of each node during dfs computation. Nodes are identified
         * by their dfs number.
         */
        final TObjectIntHashMap<V> parent = new TObjectIntHashMap<V>();
        /*
         * Remembers the dfsnumber of the predecessor in the dfs traversal.
         */
        private final TIntStack pred = new TIntArrayStack();
        private int dfsnum = 0;

        public DomDFSNumWalker() {
            super(graph);
        }

        @Override
        public void discover(V current) {
            // changes value of Dominators.dfnum2node - side effect...
            dfsnum2node[dfsnum] = current;
            semi.put(current, dfsnum);

            if (pred.size() > 0) {
                final int parentDFSnum = pred.peek();
                parent.put(current, parentDFSnum);
            } else {
                parent.put(current, -1);
            }

            pred.push(dfsnum);
            dfsnum++;
        }

        @Override
        public void finish(V node) {
            pred.pop();
        }
    }


    private static final class ForestEdge {}

    private static final class ForestEdgeFactory<V> implements EdgeFactory<V, ForestEdge> {

        public ForestEdge createEdge(V sourceVertex, V targetVertex) {
            return new ForestEdge();
        }

    }

    private static final class Forest<V> extends SimpleDirectedGraph<V, ForestEdge> {

        private final TObjectIntHashMap<V> semi;

        public Forest(TObjectIntHashMap<V> semi) {
            super(new ForestEdgeFactory<V>());
            this.semi = semi;
        }

        private static final long serialVersionUID = 514793894028572698L;

        public V eval(final V node) {
            if (!containsVertex(node)) {
                // a node can only be root, if it has no been added.
                return node;
            }

            final Set<ForestEdge> in = incomingEdgesOf(node);
            if (in.size() == 0) {
                // node is root
                return node;
            } else {
                return minSemiOnPathToRoot(node, node, semi.get(node));
            }
        }

        private V minSemiOnPathToRoot(final V node, final V currentMin, final int currentSemi) {
            final Set<ForestEdge> in = incomingEdgesOf(node);
            final int preds = in.size();
            if (preds == 0) {
                return currentMin;
            } else if (preds == 1) {
                final ForestEdge predEdge = in.iterator().next();
                final V pred = getEdgeSource(predEdge);
                final int nodeSemi = semi.get(node);

                if (nodeSemi < currentSemi) {
                    return minSemiOnPathToRoot(pred, node, nodeSemi);
                } else {
                    return minSemiOnPathToRoot(pred, currentMin, currentSemi);
                }
            } else {
                throw new IllegalStateException("Not a tree: " + preds + " preds of " + node);
            }
        }

        public void link(final V v, final V w) {
            addVertex(v);
            addVertex(w);
            addEdge(v, w);
        }

    }

    public final boolean assertConsistence() {
        DomTree<V> domTree = getDominationTree();

        boolean consistent = true;

        for (V node : domTree.vertexSet()) {
            Set<V> treeDom = new HashSet<V>();
            for (Dominators.DomEdge edge : domTree.outgoingEdgesOf(node)) {
                treeDom.add(domTree.getEdgeTarget(edge));
            }
            Set<V> quickDom = new HashSet<V>();
            for (V qdom : getNodesWithIDom(node)) {
                quickDom.add(qdom);
            }

            if (!quickDom.containsAll(treeDom)) {
                consistent = false;
                System.out.print("ERR: Tree has additional nodes: ");
                Set<V> clone = new HashSet<V>(treeDom);
                clone.removeAll(quickDom);
                for (V additional : clone) {
                    System.out.print(additional + "; ");
                }
                System.out.println();
            }
            if (!treeDom.containsAll(quickDom)) {
                consistent = false;
                System.out.print("ERR: Quick has additional nodes: ");
                Set<V> clone = new HashSet<V>(quickDom);
                clone.removeAll(treeDom);
                for (V additional : clone) {
                    System.out.print(additional + "; ");
                }
                System.out.println();
            }
        }

        return consistent;
    }
}
