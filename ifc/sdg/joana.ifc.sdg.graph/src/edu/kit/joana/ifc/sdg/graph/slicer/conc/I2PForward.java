/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;


import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 *
 * @author hammer, giffhorn
 */
public class I2PForward extends Iterative2PhaseSlicer {

    public I2PForward(SDG g) {
        super(g);

    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
        return this.g.outgoingEdgesOf(node);
    }

    protected SDGNode reachedNode(SDGEdge edge) {
        return edge.getTarget();
    }

    protected Phase phase1() {
        return new Phase() {
            public boolean follow(SDGEdge e) {
                return true;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.PARAMETER_IN
                        || e.getKind() == SDGEdge.Kind.CALL;
            }
        };
    }

    protected Phase phase2() {
        return new Phase() {
            public boolean follow(SDGEdge e) {
                return e.getKind() != SDGEdge.Kind.PARAMETER_OUT;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.INTERFERENCE ||
                       e.getKind() == SDGEdge.Kind.FORK_IN ||
                       e.getKind() == SDGEdge.Kind.FORK ||
                       e.getKind() == SDGEdge.Kind.FORK_OUT;
            }
        };
    }


    public static void main(String[] args) throws IOException {
            edu.kit.joana.ifc.sdg.graph.SDG g = edu.kit.joana.ifc.sdg.graph.SDG.readFrom(args.length > 0 ? args[0] :
                            "/home/st/hammer/scratch/pdg/javacard.framework.JCMainPurse.pdg");
            SDGNode c = g.getNode(args.length > 1 ? Integer.parseInt(args[1]) : 591);
            Set<SDGNode> s = new java.util.TreeSet<SDGNode>(SDGNode.getIDComparator());
            s.addAll(new I2PForward(g).slice(Collections.singleton(c)));
            for (Iterator<SDGNode> i = s.iterator(); i.hasNext(); ) {
                String l = i.next().getLabel();
                if (l != null && !l.equals("[]"))
                    i.remove();
            }
            System.out.println(s);
    }

}
