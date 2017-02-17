/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.building;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.jgrapht.alg.KosarajuStrongConnectivityInspector;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;

/** Uses Krinke's context-sensitive SCC folding method for CFG's.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 */
public class KrinkeSCCInspector {
    /** An ICFG whose SCC's shall be determined. */
    private final CFG icfg;
    /** A buffer for temporarily removed edges. */
    private final LinkedList<SDGEdge> buffer;

    /** Creates a new instance of KrinkesSCCInspector.
     *
     * @param icfg  An ICFG whose SCC's shall be determined.
     */
    public KrinkeSCCInspector(CFG icfg) {
        this.icfg = icfg;
        this.buffer = new LinkedList<SDGEdge>();
    }

    /** Determines all SCC's that doesn't contain return edges.
     *
     * @return  A list of SCC's without return edges.
     */
    public List<Set<SDGNode>> firstPass() {
        Object[] edges = icfg.edgeSet().toArray();

        // first, temporarily remove all edges but CF and CALL
        for (int i = 0; i < edges.length; i++) {
            SDGEdge e = (SDGEdge) edges[i];
            if (e.getKind() != SDGEdge.Kind.CONTROL_FLOW
                    && e.getKind() != SDGEdge.Kind.CALL) {

                buffer.addFirst(e);
                icfg.removeEdge(e);
            }
        }

        // then compute SCCs
        KosarajuStrongConnectivityInspector<SDGNode, SDGEdge> ksci = new KosarajuStrongConnectivityInspector<SDGNode, SDGEdge>(icfg);
        List<Set<SDGNode>> erg = ksci.stronglyConnectedSets();

        // put return edges back
        for (SDGEdge e : buffer) {
            icfg.addEdge(e);
        }
        buffer.clear();

        return erg;
    }

    /** Determines all SCC's that doesn't contain call edges.
     *
     * @return  A list of SCC's without call edges.
     */
    public List<Set<SDGNode>> secondPass() {
        Object[] edges = icfg.edgeSet().toArray();

        // first, temporarily remove all edges but CF and RT
        for (int i = 0; i < edges.length; i++) {
            SDGEdge e = (SDGEdge) edges[i];
            if (e.getKind() != SDGEdge.Kind.CONTROL_FLOW && e.getKind() != SDGEdge.Kind.RETURN) {
                buffer.addFirst(e);
                icfg.removeEdge(e);
            }
        }

        // then compute SCCs
        KosarajuStrongConnectivityInspector<SDGNode, SDGEdge> ksci = new KosarajuStrongConnectivityInspector<SDGNode, SDGEdge>(icfg);
        List<Set<SDGNode>> erg = ksci.stronglyConnectedSets();

        // put call edges back
        for (SDGEdge e : buffer) {
            icfg.addEdge(e);
        }
        buffer.clear();

        return erg;
    }
}
