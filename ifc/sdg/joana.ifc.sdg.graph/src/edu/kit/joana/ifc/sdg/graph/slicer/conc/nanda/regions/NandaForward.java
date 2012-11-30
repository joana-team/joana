/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;


public class NandaForward implements NandaMode {
	private ContextGraphs contextGraphs;
	private MHPAnalysis mhp;
	private SDG graph;

	public void init(ContextGraphs contextGraphs, MHPAnalysis mhp, SDG graph) {
		this.contextGraphs = contextGraphs;
		this.mhp = mhp;
		this.graph = graph;
	}

	public SummarySlicer initSummarySlicer(SDG g) {
        return new SummarySlicer.Forward(g);
    }

	@Override
	public boolean restrictiveTest(State actual, State previous) {
		if (previous == State.NONRESTRICTIVE) {
			return true;

		} else if (actual == State.NONRESTRICTIVE) {
			return false;

		} else if (actual == State.NONE) {
			return true;

		} else if (previous == State.NONE) {
			return false;

		} else {
			return contextGraphs.reach(previous.getTopolNr(), actual.getTopolNr());
		}
	}

	public Iterator<TopologicalNumber> reachingContexts(SDGNode reached, int reachedThread, State state) {
    	return contextGraphs.realisablePathForward(reached, reachedThread, state.getTopolNr()).iterator();
	}

    /** Initialize the outer worklist .
     * @param criteria  The slicing criteria.
     * @return  The initialized worklist.
     */
	public VirtualNode initialState(int thread) {
        return new VirtualNode(mhp.getThreadEntry(thread), thread);
    }

	public SDGNode adjacentNode(SDGEdge edge) {
		return edge.getTarget();
	}

	public Collection<SDGEdge> getEdges(SDGNode node) {
		return graph.outgoingEdgesOf(node);
	}

	public Nanda.Treatment phase1Treatment(SDGEdge edge) {
		Nanda.Treatment ret = Nanda.Treatment.OMIT;

		if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
                || edge.getKind() == SDGEdge.Kind.FORK_IN
                || edge.getKind() == SDGEdge.Kind.FORK_OUT
                || edge.getKind() == SDGEdge.Kind.FORK) {

            ret = Nanda.Treatment.THREAD;

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_IN || edge.getKind() == SDGEdge.Kind.CALL) {
            if (edge.getSource().getKind() == SDGNode.Kind.FORMAL_OUT) {
            	ret = Nanda.Treatment.CLASS_INITIALIZER;

            } else {
                ret = Nanda.Treatment.DESCEND;
            }

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
            ret = Nanda.Treatment.ASCEND;

        } else if (edge.getKind().isSDGEdge()) {
        	ret = Nanda.Treatment.INTRA;
        }

        return ret;
	}

	public Nanda.Treatment phase2Treatment(SDGEdge edge) {
		Nanda.Treatment ret = Nanda.Treatment.OMIT;

		if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
                || edge.getKind() == SDGEdge.Kind.FORK_IN
                || edge.getKind() == SDGEdge.Kind.FORK_OUT
                || edge.getKind() == SDGEdge.Kind.FORK) {

            ret = Nanda.Treatment.THREAD;

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_IN || edge.getKind() == SDGEdge.Kind.CALL) {
            if (edge.getSource().getKind() == SDGNode.Kind.FORMAL_OUT) {
            	ret = Nanda.Treatment.CLASS_INITIALIZER;

            } else {
                ret = Nanda.Treatment.DESCEND;
            }

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
            // OMIT

        } else if (edge.getKind().isSDGEdge()) {
        	ret = Nanda.Treatment.INTRA;
        }

        return ret;
	}


    /** Compute the intra-procedural successor of a context
     */
	public Iterator<TopologicalNumber> intraproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from) {
		LinkedList<TopologicalNumber> nrs = new LinkedList<TopologicalNumber>();
        LinkedList<TopologicalNumber> reached =
        	contextGraphs.getTopologicalNumbersNew(neighbourNode, from.getThread());

        if (reached.size() == 1) {
        	// a simple optimization
        	return reached.iterator();

        } else {
	        for (TopologicalNumber t : reached) {
	            if (t.getProcID() == from.getProcID()) {
	                if (contextGraphs.reach(from, t)) {
	                    nrs.add(t);
	                }
	            }
	        }
        }

        // FIXME: this hack is necessary because the control flow of phi nodes is incorrect
        // the hack will work most of the times, but the slices are not guaranteed to be
        // time- and context-sensitive
        // please complain to Juergen Graf (grafj@ipd.uni-karlsruhe.de)
        if (nrs.size() == 0) {
        	for (TopologicalNumber t : reached) {
	        	if (t.getProcID() == from.getProcID()) {
                    nrs.add(t);
	            }
	        }
        }

        return nrs.iterator();
    }

	/** Compute the intra-procedural predecessors of a context.
     */
    public Iterator<TopologicalNumber> interproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from) {
    	LinkedList<TopologicalNumber> nrs = contextGraphs.getSuccessors(from);
    	LinkedList<TopologicalNumber> reached = contextGraphs.getTopologicalNumbersNew(neighbourNode, from.getThread());

        nrs.add(from); // to account for folded cycles
        nrs.retainAll(reached);

        return nrs.iterator();
    }

	@Override
	public Iterator<TopologicalNumber> getTopologicalNumbers(SDGNode node, int thread) {
		return contextGraphs.getTopologicalNumbersNew(node, thread).iterator();
	}
}
