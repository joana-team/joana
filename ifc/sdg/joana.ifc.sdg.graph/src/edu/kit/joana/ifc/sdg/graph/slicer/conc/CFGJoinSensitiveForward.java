/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.CFGSlicer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class CFGJoinSensitiveForward extends CFGForward {
	private SDGNode join;

    /**
	 * @return the join
	 */
	public SDGNode getJoin() {
		return join;
	}

	/**
	 * @param join the join to set
	 */
	public void setJoin(SDGNode join) {
		this.join = join;
	}

	public CFGJoinSensitiveForward(CFG g) {
        super(g);
    }

    public CFGJoinSensitiveForward(SDG g) {
        super(g);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
    	Collection<SDGEdge> ret = new LinkedList<SDGEdge>();
    	for (SDGEdge e : this.g.outgoingEdgesOf(node)) {
    		if (!(e.getTarget() == join)) {
    			ret.add(e);
    		}
    	}
    	return ret;
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
                return false;
            }
        };
    }
}
