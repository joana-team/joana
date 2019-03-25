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

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.util.collections.NotTightArraySet;


/**
 *
 * @author hammer, giffhorn
 */
public class CFGForward extends CFGSlicer {

    public CFGForward(CFG g) {
        super(g);
    }

    public CFGForward(SDG g) {
        super(g);
    }

    protected Iterable<SDGEdge> edgesToTraverse(SDGNode node) {
        return NotTightArraySet.own(this.g.outgoingEdgesOfUnsafe(node));
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
                return e.getKind() == SDGEdge.Kind.CALL;
            }
        };
    }

    protected Phase phase2() {
        return new Phase() {
            public boolean follow(SDGEdge e) {
            	return e.getKind() != SDGEdge.Kind.RETURN;
            }

            public boolean saveInOtherWorklist(SDGEdge e) {
                return e.getKind() == SDGEdge.Kind.FORK
                		|| e.getKind() == SDGEdge.Kind.JOIN;
            }
        };
    }

    public static void main(String[] args) throws IOException {
    	SDG g = SDG.readFrom("/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/eclipse/runtime-New_configuration/Tests/jSDG/conc.ac.AlarmClock.pdg");
    	CFG c = ICFGBuilder.extractICFG(g);
    	CFGForward slicer = new CFGForward(c);
    	Collection<SDGNode> slice = slicer.slice(c.getNode(4615));
    	System.out.println(slice);
    }
}
