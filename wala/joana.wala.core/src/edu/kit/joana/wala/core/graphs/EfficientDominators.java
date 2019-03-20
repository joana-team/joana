/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;

import edu.kit.joana.util.collections.SimpleVector;
import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

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
public class EfficientDominators<V extends IntegerIdentifiable, E> {

    public static <Y extends IntegerIdentifiable, Z> EfficientDominators<Y, Z> compute(DirectedGraph<Y, Z> graph, Y entry) {
    	EfficientDominators<Y, Z> dom = new EfficientDominators<Y, Z>(graph, entry);
        dom.compute();

        assert dom.assertConsistence();

        return dom;
    }

    final Map<V, Node<V>> v2node;

    private final DirectedGraph<V, E> graph;
    private final V start;
    private Node<V> startNode;
    
    @SuppressWarnings("unchecked")
    private EfficientDominators(DirectedGraph<V, E> graph, V start) {
        this.graph = graph;
        this.start = start;
        this.dfsnum2node = (Node<V>[]) new Node[graph.vertexSet().size()];
        this.v2node = new HashMap<>(graph.vertexSet().size());
    }

    /**
     * Contains the mapping from dfs number to node
     */
    private final Node<V>[] dfsnum2node;

    public V getIDom(V node) {
    	final Node<V> idom = v2node.get(node).getIdom();
    	assert (idom == null) == (node == start);
        return idom == null ? null : idom.getV();
    }

    private Map<Node<V>, BitSet> idom2domiated;

    private final Iterable<V> emptyIterable = new DFSNumNodeIterable(new BitSet());

    public Iterable<V> getNodesWithIDom(V v) {
    	final Node<V> node = v2node.get(v);
        if (idom2domiated == null) {
            // build datastructure on demand
            idom2domiated = new SimpleVector<Node<V>, BitSet>(0, graph.vertexSet().size());

            // start from 1 - omit the root node at 0
            for (int i = 1; i < dfsnum2node.length; i++) {
                final Node<V> curr = dfsnum2node[i];
                final Node<V> idominator = curr.getIdom();
                BitSet dominates = idom2domiated.get(idominator);
                if (dominates == null) {
                    dominates = new BitSet();
                    idom2domiated.put(idominator, dominates);
                }
                assert i == curr.getDfsnum();
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
                domTree.addEdgeUnsafe(idom, node, domTree.getEdgeFactory().createEdge(idom, node));
            }
        }

        return domTree;
    }

    private static final class DomEdgeFactory<V> implements EdgeFactory<V, DomEdge<V>> {

        public DomEdge<V> createEdge(V sourceVertex, V targetVertex) {
            return new DomEdge<V>(sourceVertex, targetVertex);
        }

    }

    public static class DomTree<V extends IntegerIdentifiable> extends AbstractJoanaGraph<V, DomEdge<V>> {

        private static final long serialVersionUID = 1445142467229185713L;

        @SuppressWarnings("unchecked")
		private DomTree() {
        	super(new DomEdgeFactory<V>(), () -> new HashMap<>(), (Class<DomEdge<V>>) new DomEdge<V>(null, null).getClass());
        }

        public String toString() {
            return "IDOM Tree";
        }
    }

    public static class DomEdge<V> implements KnowsVertices<V> {
    	private final V source;
    	private final V target;
		public DomEdge(V source, V target) {
			this.source = source;
			this.target = target;
		}
		
		@Override
		public V getSource() {
			return source;
		}
		
		@Override
		public V getTarget() {
			return target;
		}

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

                    return dfsnum2node[index - 1].getV();
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

        final Forest<V> forest = new Forest<V>(graph.vertexSet().size());
        
        // for  all nodes except the root node (at index 0) we compute the dominator
        for (int dfsW = dfsnum2node.length - 1; dfsW > 0; dfsW--) {
            final Node<V> w = dfsnum2node[dfsW];

            if (w == null) {
                throw new IllegalStateException("Null node at dfsW=" + dfsW + " of a total of " + dfsnum2node.length
                        + ". This may happen when unreachable code is in the cfg.");
            }

            // step 2
            for (final E predEdge : graph.incomingEdgesOf(w.getV())) {
                final Node<V> v = v2node.get(graph.getEdgeSource(predEdge));
                assert v != null;
                // u = EVAL(v)
                final Node<V> u = forest.eval(v);

                // if semi(u) < semi(w) then semi(w) := semi(u)
                final Node<V> semiW = w.getSemi();
                final Node<V> semiU = u.getSemi();
                if (semiU.getDfsnum() < semiW.getDfsnum()) {
                    w.setSemi(semiU);
                }
            }

            // add w to bucket(vertex(semi(w)))
            final Node<V> semiW = w.getSemi();
            BitSet bs = semiW.getBucket();
            if (bs == null) {
                bs = new BitSet(dfsnum2node.length);
                semiW.setBucket(bs);
            }
            bs.set(dfsW);

            // LINK(parent(w), w)
            final Node<V> parentW = w.getParent();
            forest.link(parentW, w);

            // step 3
            // for each v in bucket(parent(w)) do
            final BitSet dominated = parentW.getBucket();
            if (dominated == null) {
                continue;
            }
            for (int i = dominated.nextSetBit(0); i >= 0; i = dominated.nextSetBit(i + 1)) {
                final Node<V> v = dfsnum2node[i];
                // delete v from bucket(parent(w))
                dominated.clear(i);
                // u := EVAL(v)
                final Node<V> u = forest.eval(v);
                // dom(v) := if semi(u) < semi(v) then u else parent(w)
                final int semiU = u.getSemi().getDfsnum();
                final int semiV = v.getSemi().getDfsnum(); 
                if (semiU < semiV) {
                	v.setIdom(u);
                } else {
                	v.setIdom(parentW);
                }
            }

        }

        // step 4
        // for i := 2 to n do  -> note that our dfs numbers start at 0 not 1
        for (int i = 1; i < dfsnum2node.length; i++) {
            // w := vertex(i)
            final Node<V> w = dfsnum2node[i];
            // if dom(w) != vertex(semi(w)) then dom(w) := dom(dom(w))
            final Node<V> domW = w.getIdom();
            assert domW != null;
            final Node<V> semiW = w.getSemi();
            assert (domW == semiW) == (domW.getV() == semiW.getV());
            if (domW != semiW) {
                final Node<V> domdomW = domW.getIdom();
                assert (domdomW != null);
                w.setIdom(domdomW);
            }
        }

        // dom(r) : = 0
        startNode.setIdom(null);
        
        for (Node<V> node : dfsnum2node) {
        	node.setBucket(null);
        }
    }

    private final class DomDFSNumWalker extends GraphWalker<V, E> {
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
            final Node<V> currentNode = new Node<V>(current, dfsnum);
            v2node.put(current, currentNode);
            
            if (current == start) {
            	startNode = currentNode;
            }
            // changes value of Dominators.dfnum2node - side effect...
            dfsnum2node[dfsnum] = currentNode;
            currentNode.setSemi(currentNode);
            
            if (pred.size() > 0) {
                final int parentDFSnum = pred.peek();
                assert parentDFSnum < dfsnum;
                currentNode.setParent(dfsnum2node[parentDFSnum]);
            } else {
                currentNode.setParent(null);
            }

            pred.push(dfsnum);
            dfsnum++;
        }

        @Override
        public void finish(V node) {
            pred.pop();
        }
    }


    private static final class ForestEdge<V> implements KnowsVertices<V>{
    	private final V source;
    	private final V target;
		public ForestEdge(V source, V target) {
			this.source = source;
			this.target = target;
		}
		
		@Override
		public V getSource() {
			return source;
		}
		
		@Override
		public V getTarget() {
			return target;
		}
    }

    private static final class ForestEdgeFactory<V> implements EdgeFactory<V, ForestEdge<V>> {

        public ForestEdge<V> createEdge(V sourceVertex, V targetVertex) {
            return new ForestEdge<V>(sourceVertex, targetVertex);
        }

    }
    
	static class Node<V> implements IntegerIdentifiable {
		private final V v;
		private final int dfsnum;
		
		/*
		 * This variable has 2 usages depending on the state of the computation: 1. It contains mapping from node to
		 * dfsnum after the first step 2. It contains the semidominator of a node later on
		 */
		private Node<V> semi;
		
		/*
		 * Stores the parent node of each node during dfs computation. Nodes are identified by their dfs number.
		 */
		private Node<V> parent;
		
		private Node<V> idom;
		
		// Maps a semidominator to a set of nodes it semidominates. Identified by dfsnum.
		private BitSet bucket;
		
		public Node(V v, int dfsnum) {
			this.v = v;
			this.dfsnum = dfsnum;
		}
		
		public V getV() {
			return v;
		}
		
		public Node<V> getSemi() {
			return semi;
		}
		
		public void setSemi(Node<V>  semi) {
			this.semi = semi;
		}
		
		public Node<V> getParent() {
			return parent;
		}
		
		public void setParent(Node<V> parent) {
			this.parent = parent;
		}
		
		public void setIdom(Node<V> idom) {
			this.idom = idom;
		}
		
		public Node<V> getIdom() {
			return idom;
		}
		
		@Override
		public String toString() {
			return v.toString();
		}
		
		@Override
		public int getId() {
			return dfsnum;
		}
		public int getDfsnum() {
			return dfsnum;
		}
		
		public void setBucket(BitSet bucket) {
			this.bucket = bucket;
		}
		
		public BitSet getBucket() {
			return bucket;
		}
	}


    private static final class Forest<V> extends AbstractJoanaGraph<Node<V>, ForestEdge<Node<V>>> {

        @SuppressWarnings("unchecked")
		public Forest(int size) {
            super(new ForestEdgeFactory<Node<V>>(), () -> new SimpleVector<>(0, size), (Class<ForestEdge<Node<V>>>) new ForestEdge<Node<V>>(null, null).getClass());
        }
        
        private static final long serialVersionUID = 514793894028572698L;

        public Node<V> eval(final Node<V> node) {
            final ForestEdge<Node<V>>[] in = incomingEdgesOfUnsafe(node);
            if (in == null || in.length == 0) {
                // node is root
                return node;
            } else {
                return minSemiOnPathToRoot(node, node, node.getSemi().getDfsnum());
            }
        }

        private Node<V> minSemiOnPathToRoot(Node<V> node, Node<V> currentMin, int currentSemi) {
            while(true) {
                final ForestEdge<Node<V>>[] in = incomingEdgesOfUnsafe(node);
                final int preds = in.length;
                if (preds == 0) {
                    return currentMin;
                } else if (preds == 1) {
                    final ForestEdge<Node<V>> predEdge = in[0];
                    final Node<V> pred = predEdge.getSource();
                    
                    final int nodeSemi = node.getSemi().getDfsnum();

                    if (nodeSemi < currentSemi) {
                        final Node<V> oldNode = node;
                        node = pred;
                        currentMin = oldNode;
                        currentSemi = nodeSemi;
                    } else {
                        node = pred;
                    }
                } else {
                    throw new IllegalStateException("Not a tree: " + preds + " preds of " + node);
                }
            }
        }

        public void link(final Node<V> v, final Node<V> w) {
            addVertex(v);
            addVertex(w);
            addEdgeUnsafe(v, w, this.getEdgeFactory().createEdge(v, w));
        }

    }

    public final boolean assertConsistence() {
        DomTree<V> domTree = getDominationTree();

        boolean consistent = true;

        for (V node : domTree.vertexSet()) {
            Set<V> treeDom = new HashSet<V>();
            for (DomEdge<V> edge : domTree.outgoingEdgesOf(node)) {
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
