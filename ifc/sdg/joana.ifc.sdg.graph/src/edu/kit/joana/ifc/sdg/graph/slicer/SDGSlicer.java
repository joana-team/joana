/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


/**
 * @author hammer
 *
 */
public class SDGSlicer {
    public static Set<SDGEdge.Kind> omittedEdges = SDGEdge.Kind.threadEdges();

    SDG g;
    Collection<SDGNode> criteria;

    /**
     *
     */
    public SDGSlicer(SDG g, Collection<SDGNode> c) {
        this.g = g;
        criteria = c;
    }

    public Collection<SDGNode> slice() {
        Set<SDGNode> slice = new HashSet<SDGNode>();
        Stack<SDGNode> worklist = new Stack<SDGNode>();
        Stack<SDGNode> worklistDown = new Stack<SDGNode>();
        worklist.addAll(criteria);
        slice.addAll(criteria);
        while (!worklist.isEmpty()) {
            SDGNode w = worklist.pop();
            for (SDGEdge e : g.incomingEdgesOf(w)) {
                if (!e.getKind().isSDGEdge())
                    continue;
                SDGNode v = e.getSource();
                if (!slice.contains(v)) {
                    if (e.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
                        worklistDown.add(v);
                        slice.add(v);
                    } else {
                        worklist.add(v);
                        slice.add(v);
                    }
                }
            }
        }
        ContextInsensitiveSlicer cis = new ContextInsensitiveBackward(g, omittedEdges);
        slice.addAll(cis.slice(worklistDown));
        return slice;
    }

    public static void main(String[] args) throws IOException {
        SDG g = SDG.readFrom(
                "/home/st/hammer/scratch/pdg/javacard.framework.JCMainPurse.pdg");
        SDGNode c = g.getNode(591);
        System.out.println(new SDGSlicer(g, Collections.singleton(c)).slice());
    }
}
