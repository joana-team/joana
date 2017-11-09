/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ContextGraph.ContextEdge;
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
	public boolean restrictiveTest(TopologicalNumber actual, TopologicalNumber previous, int thread) {
		if (previous == TopologicalNumber.NONRESTRICTIVE) {
			return true;

		} else if (actual == TopologicalNumber.NONRESTRICTIVE) {
			return false;

		} else if (actual == TopologicalNumber.NONE) {
			return true;

		} else if (previous == TopologicalNumber.NONE) {
			return false;

		} else {
			return contextGraphs.reach(previous, actual, thread);
		}
	}

	public Iterator<TopologicalNumber> reachingContexts(SDGNode reached, int reachedThread, TopologicalNumber state) {
    	return contextGraphs.realisablePathForward(reached, reachedThread, state).iterator();
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

		if (edge.getKind() == SDGEdge.Kind.FORK_IN
                || edge.getKind() == SDGEdge.Kind.FORK_OUT
                || edge.getKind() == SDGEdge.Kind.FORK
                || edge.getKind() == SDGEdge.Kind.JOIN
                || edge.getKind() == SDGEdge.Kind.JOIN_OUT) {

            ret = Nanda.Treatment.FORKJOIN;

        } else if (edge.getKind() == SDGEdge.Kind.INTERFERENCE) {
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

		if (edge.getKind() == SDGEdge.Kind.FORK_IN
                || edge.getKind() == SDGEdge.Kind.FORK_OUT
                || edge.getKind() == SDGEdge.Kind.FORK
                || edge.getKind() == SDGEdge.Kind.JOIN
                || edge.getKind() == SDGEdge.Kind.JOIN_OUT) {

            ret = Nanda.Treatment.FORKJOIN;

        } else if (edge.getKind() == SDGEdge.Kind.INTERFERENCE) {
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
	public Iterator<TopologicalNumber> intraproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from, int thread) {
		LinkedList<TopologicalNumber> nrs = new LinkedList<TopologicalNumber>();
        List<TopologicalNumber> reached = contextGraphs.getTopologicalNumbersNew(neighbourNode, thread);

        if (reached == null) {
        	return nrs.iterator();
        } else if (reached.size() == 1) {
        	// a simple optimization
        	return reached.iterator();

        } else {
	        for (TopologicalNumber t : reached) {
	            if (t.getProcID() == from.getProcID()) {
	                if (contextGraphs.reach(from, t, thread)) {
	                    nrs.add(t);
	                }
	            }
	        }
        }

        // FIXME: this hack is necessary because the control flow of phi nodes is incorrect
        // the hack will work most of the times, but the slices are not guaranteed to be
        // time- and context-sensitive
        // please complain to Juergen Graf (grafj@ipd.uni-karlsruhe.de)
        if (nrs.isEmpty()) {
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
    public Iterator<TopologicalNumber> interproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from, int thread) {
    	LinkedList<TopologicalNumber> nrs = contextGraphs.getSuccessors(from, thread);
    	List<TopologicalNumber> reached = contextGraphs.getTopologicalNumbersNew(neighbourNode, thread);

        nrs.add(from); // to account for folded cycles
        nrs.retainAll(reached);

        return nrs.iterator();
    }

	@Override
	public Iterator<TopologicalNumber> getTopologicalNumbers(SDGNode node, int thread) {
		return contextGraphs.getTopologicalNumbersNew(node, thread).iterator();
	}

	@Override
	public Collection<TopologicalNumber> getForkJoin(SDGEdge edge, TopologicalNumber from, int fromThread) {
		if (edge.getKind() == SDGEdge.Kind.FORK || edge.getKind() == SDGEdge.Kind.FORK_IN) {
			HashSet<TopologicalNumber> entries = new HashSet<TopologicalNumber>();
			for (ContextEdge e : contextGraphs.getWholeGraph().outgoingEdgesOf(from)) {
				if (e.getKind() == SDGEdge.Kind.FORK || e.getKind() == SDGEdge.Kind.FORK_IN) {
					entries.add(e.getTarget());
				}
			}
			return entries;

		} else if (edge.getKind() == SDGEdge.Kind.JOIN || edge.getKind() == SDGEdge.Kind.JOIN_OUT) {
			HashSet<TopologicalNumber> joins = new HashSet<TopologicalNumber>();
			for (ContextEdge e : contextGraphs.getWholeGraph().outgoingEdgesOf(from)) {
				if (e.getKind() == SDGEdge.Kind.JOIN || e.getKind() == SDGEdge.Kind.JOIN_OUT) {
					joins.add(e.getTarget());
				}
			}
			return joins;

		} else {
			throw new IllegalArgumentException();
		}
	}
}
