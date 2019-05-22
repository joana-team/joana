/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.LinkedHashMap;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;

/**
 * This is a control dependence graph. Control dependencies are computed for nodes
 * of a control flow graph. n -> m reads as "m is control dependent on n".
 *
 * Control dependencies tell us which nodes may control iff a certain node can be reached.
 * E.g. the following control flow graph
 *
 * <pre>
 * CFG:
 * start -> n1 -> n2 -> n -\
 *            \-> n3 -> n4 -> n5 -> exit
 * </pre>
 * results in this control dependence graph:
 * <pre>
 * CDG:
 * start -> n1 -> n2
 *      \     \-> n
 *       |    |-> n3
 *       |    \-> n4
 *       |-> n5
 *       \-> exit
 * </pre>
 *
 * The outcome of node n1 can control if node n is executed or not, because the control
 * flow offers two alternatives at node n1: When we choose
 * the path through node n3, n is never executed. When we choose the path through
 * node n2, n is executed.
 *
 * Node n5 is not control dependent on n1, because it does not matter which path we choose,
 * we will always reach and thus execute n5. So n1 has no control over the execution of n5.
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("serial")
public class CDG extends AbstractJoanaGraph<PDGNode, PDGEdge> {

    public static CDG build(DirectedGraph<PDGNode, PDGEdge> cfg, PDGNode entry, PDGNode exit) {
        final CDG cdg = new CDG(cfg, entry, exit);

        cdg.build();

        return cdg;
    }

    private final DirectedGraph<PDGNode, PDGEdge> cfg;
    private final PDGNode entry;
    private final PDGNode exit;

    private CDG(final DirectedGraph<PDGNode, PDGEdge> cfg, PDGNode entry, PDGNode exit) {
        super(PDG.DEFAULT_EDGE_FACTORY, () -> new LinkedHashMap<>(), PDGEdge.class);
        this.cfg = cfg;
        this.entry = entry;
        this.exit = exit;
    }

    private void build() {
        final DirectedGraph<PDGNode, PDGEdge> reversedCfg = new EdgeReversedGraph<PDGNode, PDGEdge>(cfg);
        final DominanceFrontiers<PDGNode, PDGEdge> frontiers = DominanceFrontiers.compute(reversedCfg, exit);

        for (final PDGNode node : cfg.vertexSet()) {
            addVertex(node);
        }

        addEdge(entry, exit);

        for (final PDGNode node : cfg.vertexSet()) {
            for (final PDGNode domFrontier : frontiers.getDominanceFrontier(node)) {
                if (node != domFrontier) {
                    // no self dependencies
                    addEdge(domFrontier, node);
                }
            }
        }
    }

    public PDGNode getEntry() {
        return entry;
    }

    public PDGNode getExit() {
        return exit;
    }

    public PDGEdge addEdge(PDGNode from, PDGNode to) {
        return addEdge(from, to, PDGEdge.Kind.CONTROL_DEP);
    }

    public PDGEdge addEdge(PDGNode from, PDGNode to, PDGEdge.Kind kind) {
        final PDGEdge edge = new PDGEdge(from, to, kind);

        return (addEdge(from, to, edge) ? edge : null);
    }

    public String toString() {
        return "CDG of " + entry.getLabel() + "(" + vertexSet().size() + ", " + edgeSet().size() + ")" ;
    }

}
