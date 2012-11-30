/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 *
 *
 * -- Created on March 14, 2006
 *
 * @author  Dennis Giffhorn
 */
public class IntraproceduralSlicerForward extends IntraproceduralSlicer implements Slicer {
    /**
     * Creates a new instance of IntraproceduralSlicer
     */
    public IntraproceduralSlicerForward(SDG g) {
        super(g);
    }

    public Collection<SDGNode> slice(Collection<SDGNode> nodes) {
        LinkedList<SDGNode> worklist = new LinkedList<SDGNode>();
        HashSet<SDGNode> slice = new HashSet<SDGNode>();

        worklist.addAll(nodes);
        slice.addAll(nodes);

        while (!worklist.isEmpty()) {
            SDGNode next = worklist.poll();

            for (SDGEdge e : graph.outgoingEdgesOf(next)) {
                if (e.getKind().isSDGEdge()
                		&& e.getKind().isIntraproceduralEdge()
                		&& !slice.contains(e.getTarget())){

                    worklist.add(e.getTarget());
                    slice.add(e.getTarget());
                }
            }
        }

        return slice;
    }

    public static void main(String[] args) throws Exception {
        SDG graph = SDG.readFrom(args[0]);
        SDGNode c = graph.getNode(Integer.parseInt(args[1]));

        IntraproceduralSlicerForward slicer = new IntraproceduralSlicerForward(graph);
        Collection<SDGNode> slice = slicer.slice(Collections.singleton(c));
        System.out.println(slice);

        for (SDGNode n : slice) {
            if (n.getKind() == SDGNode.Kind.ENTRY
                    || n.getKind() == SDGNode.Kind.ACTUAL_OUT
                    || n.getKind() == SDGNode.Kind.FORMAL_IN) {

                System.out.print(n+", ");
            }
        }
        System.out.println("\n ");
        for (SDGNode n : slice) {

            for (SDGEdge e : graph.outgoingEdgesOf(n)) {
                if (e.getKind() == SDGEdge.Kind.INTERFERENCE) {
                    System.out.println(n+", ");
                }
            }
        }
    }
}
