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

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.ContextGraph.ContextEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;


public class NandaBackward implements NandaMode {
	private ContextGraphs contextGraphs;
	private MHPAnalysis mhp;
	private SDG graph;

	public void init(ContextGraphs contextGraphs, MHPAnalysis mhp, SDG graph) {
		this.contextGraphs = contextGraphs;
		this.mhp = mhp;
		this.graph = graph;
	}

    public SummarySlicer initSummarySlicer(SDG g) {
        return new SummarySlicer.Backward(g);
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
			return contextGraphs.reach(actual, previous, thread);
		}
	}

    public Iterator<TopologicalNumber> reachingContexts(SDGNode reached, int reachedThread, TopologicalNumber state) {
    	return contextGraphs.realisablePathBackward(reached, reachedThread, state).descendingIterator();
	}

    /** Initialize the outer worklist .
     * @param criteria  The slicing criteria.
     * @return  The initialized worklist.
     */
    public VirtualNode initialState(int thread) {
        return new VirtualNode(mhp.getThreadExit(thread), thread);
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
                ret = Nanda.Treatment.ASCEND;
            }

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
            ret = Nanda.Treatment.DESCEND;

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
            // OMIT

        } else if (edge.getKind() == SDGEdge.Kind.PARAMETER_OUT) {
            ret = Nanda.Treatment.DESCEND;

        } else if (edge.getKind().isSDGEdge()) {
        	ret = Nanda.Treatment.INTRA;
        }

        return ret;
    }

    public Collection<SDGEdge> getEdges(SDGNode node) {
		return graph.incomingEdgesOf(node);
	}

    public SDGNode adjacentNode(SDGEdge edge) {
		return edge.getSource();
	}

	/** Compute the intra-procedural predecessors of a context.
     */
    public Iterator<TopologicalNumber> intraproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from, int thread) {
        LinkedList<TopologicalNumber> nrs = new LinkedList<TopologicalNumber>();
        LinkedList<TopologicalNumber> reached = contextGraphs.getTopologicalNumbersNew(neighbourNode, thread);

        try {
        if (reached.size() == 1) {
        	// a simple optimization
        	return reached.descendingIterator();//.iterator();

        } else {
	        for (TopologicalNumber t : reached) {
	        	if (t.getProcID() == from.getProcID()) {
	                if (contextGraphs.reach(t, from, thread)) {
	                    nrs.add(t);
	                }
	            }
	        }
        }
        }catch(NullPointerException ex) {
        	System.out.println("A");
        	System.out.println(reached);
        	System.out.println(neighbourNode+" reached in thread "+thread);
        	for (int t : neighbourNode.getThreadNumbers()) {
            	System.out.println(t);
        	}
        }
        // FIXME: this hack is necessary because the control flow of phi nodes is incorrect
        // the hack will work most of the times, but the slices are not guaranteed to be
        // time- and context-sensitive
        // please complain to Juergen Graf (grafj@ipd.uni-karlsruhe.de)
        try{
        if (nrs.size() == 0) {
        	for (TopologicalNumber t : reached) {
	        	if (t.getProcID() == from.getProcID()) {
                    nrs.add(t);
	            }
	        }
        }
        }catch(NullPointerException ex) {
        	System.out.println("B");
        	System.out.println(nrs);
        }

        return nrs.descendingIterator();
    }

	/** Compute the intra-procedural predecessors of a context.
     */
    public Iterator<TopologicalNumber> interproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from, int thread) {
    	LinkedList<TopologicalNumber> nrs = contextGraphs.getPredecessors(from, thread);
        LinkedList<TopologicalNumber> reached = contextGraphs.getTopologicalNumbersNew(neighbourNode, thread);
        nrs.add(from); // to account for folded cycles
        nrs.retainAll(reached);

        return nrs.descendingIterator();
    }

	@Override
	public Iterator<TopologicalNumber> getTopologicalNumbers(SDGNode node, int thread) {
		return contextGraphs.getTopologicalNumbersNew(node, thread).descendingIterator();
	}

	@Override
	public Collection<TopologicalNumber> getForkJoin(SDGEdge edge, TopologicalNumber from, int fromThread) {
		if (edge.getKind() == SDGEdge.Kind.FORK || edge.getKind() == SDGEdge.Kind.FORK_IN) {
			return contextGraphs.getForkSites(fromThread);

		} else if (edge.getKind() == SDGEdge.Kind.JOIN || edge.getKind() == SDGEdge.Kind.JOIN_OUT) {
			HashSet<TopologicalNumber> exits = new HashSet<TopologicalNumber>();
			for (ContextEdge e : contextGraphs.getWholeGraph().incomingEdgesOf(from)) {
				if (e.getKind() == SDGEdge.Kind.JOIN || e.getKind() == SDGEdge.Kind.JOIN_OUT) {
					exits.add(e.getSource());
				}
			}
			return exits;

		} else {
			throw new IllegalArgumentException();
		}
	}
}
