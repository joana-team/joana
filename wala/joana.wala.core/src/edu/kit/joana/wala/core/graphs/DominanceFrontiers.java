/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;

import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.wala.core.graphs.EfficientDominators.DomEdge;

/**
 * This class computes the dominance frontiers for each node in a flow graph.
 * A flow graph is a directed graph with a unique start node, where all nodes
 * are reachable from this start node. A standard control flow graph is such a graph
 * and its also true for the inverted cfg, when the exit node is used as start node.
 *
 * The dominance frontier (DF) of a node n is a set nodes that are not strictly dominated
 * by n, but their predecessors are. So this are the nodes where the influence/domination
 * of n ends: <tt>DF(n) = { w | pred(w) is in DOM(n) and w not in SDOM(n) }</tt>
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class DominanceFrontiers<V extends IntegerIdentifiable, E> {

    public static <V extends IntegerIdentifiable, E> DominanceFrontiers<V, E> compute(final DirectedGraph<V, E> graph, final V entry) {
        final EfficientDominators<V, E> dom = EfficientDominators.compute(graph, entry);
        final DominanceFrontiers<V, E> df = new DominanceFrontiers<V, E>(graph, entry, dom);

        df.analyze();

        return df;
    }

    private final DirectedGraph<V, E> flowGraph;
    private final V entry;
    private final EfficientDominators<V, E> dom;
    private final Map<V, Set<V>> frontiers = new HashMap<V, Set<V>>();

    private DominanceFrontiers(final DirectedGraph<V, E> graph, final V entry, final EfficientDominators<V, E> dom) {
        this.flowGraph = graph;
        this.entry = entry;
        this.dom = dom;
    }

    public Set<V> getDominanceFrontier(V node) {
        final Set<V> domFront = frontiers.get(node);
        return (domFront == null ? null : Collections.unmodifiableSet(domFront));
    }

    private void analyze() {
        DomFrontWalker<DomEdge<V>> walker = new DomFrontWalker<DomEdge<V>>(dom.getDominationTree());
        walker.traverseDFS(entry);
    }

    private final class DomFrontWalker<X> extends GraphWalker<V, X> {

        public DomFrontWalker(DirectedGraph<V, X> domTree) {
            super(domTree);
        }

        @Override
        public void discover(V node) {
            // nothing to do at discover time
        }

        @Override
        public void finish(V current) {
            final Set<V> dfCur = new HashSet<V>();
            frontiers.put(current, dfCur);

            for (final E out : flowGraph.outgoingEdgesOf(current)) {
                final V succ = flowGraph.getEdgeTarget(out);

                if (dom.getIDom(succ) != current) {
                    dfCur.add(succ);
                }
            }

            for (V succ : dom.getNodesWithIDom(current)) {
                for (V succFrontier : frontiers.get(succ)) {
                    if (dom.getIDom(succFrontier) != current) {
                        dfCur.add(succFrontier);
                    }
                }
            }
        }

    }

}
