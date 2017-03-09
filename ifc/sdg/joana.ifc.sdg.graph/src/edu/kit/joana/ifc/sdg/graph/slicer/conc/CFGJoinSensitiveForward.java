/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import gnu.trove.set.hash.TIntHashSet;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 * (code for blockCFGSummaryEdges adapted from GraphModifier.blockSummaryEdges by Dennis Giffhorn)
 */
public class CFGJoinSensitiveForward extends CFGForward {
	private Collection<SDGNode> joins;
	private final CFGForward secondSlicer;
	private Collection<SDGEdge> blockedEdges = new HashSet<SDGEdge>();

    /**
	 * @return the joins
	 */
	public Collection<SDGNode> getJoins() {
		return joins;
	}

	/**
	 * @param joins the joins to set
	 */
	public void setJoins(Collection<SDGNode> joins) {
		this.joins = joins;
		blockedEdges.clear();
		if (!joins.isEmpty()) {
			blockedEdges = blockCFGSummaryEdges();
		}
	}

	/**
	 * @param join the join to set
	 */
	public void setJoin(SDGNode join) {
		Collection<SDGNode> js = new LinkedList<SDGNode>();
		if (join != null) {
			js.add(join);
		}
		setJoins(js);
	}

	public CFGJoinSensitiveForward(CFG g) {
        super(g);
        secondSlicer = new CFGForward(g);
    }

    public CFGJoinSensitiveForward(SDG g) {
        super(g);
        secondSlicer = new CFGForward(g);
    }

    protected Collection<SDGEdge> edgesToTraverse(SDGNode node) {
    	Collection<SDGEdge> ret = new LinkedList<SDGEdge>();
    	for (SDGEdge e : this.g.outgoingEdgesOf(node)) {
    		if (blockedEdges.contains(e) || joins.contains(e.getTarget())) {
    			continue;
    		}
    		ret.add(e);
    	}
    	return ret;
    }
    
	public Collection<SDGNode> secondSlice(SDGNode c) {
		return secondSlice(Collections.singleton(c));
	}
    
	public Collection<SDGNode> secondSlice(Collection<SDGNode> c) {
		return secondSlicer.slice(c);
	}

	/* code copied from GraphModifier.blockSummaryEdges and adapted for CFGs */
    private Collection<SDGEdge> blockCFGSummaryEdges() {
        // initialisation
        HashSet<SDGEdge> deact = new HashSet<SDGEdge>();
        HashSet<SDGNodeTuple> exitList = new HashSet<SDGNodeTuple>();
        LinkedList<SDGNodeTuple> worklist = new LinkedList<SDGNodeTuple>();
        TIntHashSet markedProcs = new TIntHashSet();

        // block _all_ reachable summary edges
        for (SDGNode next : g.vertexSet()) {
            if (markedProcs.contains(next.getProc())) {
                continue;
            }
            
            markedProcs.add(next.getProc());

            SDGNode entry = g.getEntry(next);
            SDGNode exit = g.getExit(next);
            
            if (!joins.contains(exit)) {
                exitList.add(new SDGNodeTuple(exit, exit));
            }

            for (SDGEdge call : g.incomingEdgesOf(entry)) {
                SDGNode callSite = call.getSource();
                for (SDGEdge e : g.getOutgoingEdgesOfKind(callSite, SDGEdge.Kind.CONTROL_FLOW)) {
                	if ("CALL_RET".equals(e.getTarget().getLabel())) {
                		deact.add(e);
                	}
                }
            }
        }

        // unblock some summary edges
        HashSet<SDGNodeTuple> markedEdges = new HashSet<SDGNodeTuple>();
        worklist.addAll(exitList);
        markedEdges.addAll(worklist);

        while (!worklist.isEmpty()) {
        	SDGNodeTuple next = worklist.poll();

            if (next.getFirstNode().getKind() == SDGNode.Kind.ENTRY) {
                for (SDGEdge pi : g.getIncomingEdgesOfKind(next.getFirstNode(), SDGEdge.Kind.CALL)) {
                    for (SDGEdge po : g.getOutgoingEdgesOfKind(next.getSecondNode(), SDGEdge.Kind.RETURN)) {
                        SDGEdge unblock = null;

                        for (SDGEdge su : deact) {
                            if (su.getSource() == pi.getSource() && su.getTarget() == po.getTarget()) {
                                // unblock summary edge
                                unblock = su;
                                break;
                            }
                        }

                        if (unblock != null) {
                            deact.remove(unblock);
                            LinkedList<SDGNodeTuple> l = new LinkedList<SDGNodeTuple>();

                            for (SDGNodeTuple np : markedEdges) {
                                if (np.getFirstNode() == unblock.getTarget()
                            			&& !joins.contains(unblock.getSource())) {
                                	SDGNodeTuple p = new SDGNodeTuple(unblock.getSource(), np.getSecondNode());

                                    if (!markedEdges.contains(p)) {
                                        l.add(p);
                                    }
                                }
                            }

                            for (SDGNodeTuple np : l) {
                                markedEdges.add(np);
                                worklist.add(np);
                            }
                        }
                    }
                }

            } else{
                for (SDGEdge edge : g.incomingEdgesOf(next.getFirstNode())) {
                    if (edge.getKind() != SDGEdge.Kind.CONTROL_FLOW) {
                        continue;
                    }
                    
                    SDGNode s = edge.getSource();
                    SDGNode t = edge.getTarget();
                    
                    if (s.getKind() == SDGNode.Kind.CALL && "CALL_RET".equals(t.getLabel())) {
                    	continue;
                    }
                    
                    if (s.getKind() == SDGNode.Kind.ENTRY
                			&& edge.getKind() == SDGEdge.Kind.CONTROL_FLOW) {
                    	if (t.getKind() == SDGNode.Kind.EXIT) {
                    		continue;
                    	}
                    	if (t.getKind() == SDGNode.Kind.FORMAL_OUT
                    			&& "<exception>".equals(t.getBytecodeName())) {
                    		continue;
                    	}
                    }

                    SDGNodeTuple np = new SDGNodeTuple(s, next.getSecondNode());

                    if (!joins.contains(s) && !markedEdges.contains(np)) {
                        markedEdges.add(np);
                        worklist.add(np);
                    }
                }

                for (SDGEdge su : g.incomingEdgesOf(next.getFirstNode())) {
                	if (su.getSource().getKind() != SDGNode.Kind.CALL
                			|| su.getKind() != SDGEdge.Kind.CONTROL_FLOW) {
                		continue;
                	}
                	SDGNodeTuple np = new SDGNodeTuple(su.getSource(), next.getSecondNode());

                    if (!joins.contains(su.getSource())
                            && !deact.contains(su)
                            && !markedEdges.contains(np)) {

                        markedEdges.add(np);
                        worklist.add(np);
                    }
                }
            }
        }

        return deact;
    }
}
