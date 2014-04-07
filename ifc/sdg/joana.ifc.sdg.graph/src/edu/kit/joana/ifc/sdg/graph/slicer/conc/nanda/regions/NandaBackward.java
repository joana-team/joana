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
import edu.kit.joana.util.Log;


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
			return contextGraphs.reach(actual.getTopolNr(), previous.getTopolNr());
		}
	}

    public Iterator<TopologicalNumber> reachingContexts(SDGNode reached, int reachedThread, State state) {
    	return contextGraphs.realisablePathBackward(reached, reachedThread, state.getTopolNr()).descendingIterator();
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

    	if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
                || edge.getKind() == SDGEdge.Kind.FORK_IN
                || edge.getKind() == SDGEdge.Kind.FORK_OUT
                || edge.getKind() == SDGEdge.Kind.FORK) {

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

    	if (edge.getKind() == SDGEdge.Kind.INTERFERENCE
                || edge.getKind() == SDGEdge.Kind.FORK_IN
                || edge.getKind() == SDGEdge.Kind.FORK_OUT
                || edge.getKind() == SDGEdge.Kind.FORK) {

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
    public Iterator<TopologicalNumber> intraproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from) {
        LinkedList<TopologicalNumber> nrs = new LinkedList<TopologicalNumber>();
        LinkedList<TopologicalNumber> reached =
        	contextGraphs.getTopologicalNumbersNew(neighbourNode, from.getThread());
        try {
        if (reached.size() == 1) {
        	// a simple optimization
        	return reached.descendingIterator();

        } else {
	        for (TopologicalNumber t : reached) {
	        	if (t.getProcID() == from.getProcID()) {
	                if (contextGraphs.reach(t, from)) {
	                    nrs.add(t);
	                }
	            }
	        }
        }
        } catch (NullPointerException ex) {
        	Log.ERROR.outln("failed intraprocedural neighbour check (reach): " + ex.getStackTrace()[0]);
//        	System.out.println("A");
//        	System.out.println(reached);
//        	System.out.println(neighbourNode+" reached in thread "+from.getThread());
//        	for (int t : neighbourNode.getThreadNumbers()) {
//            	System.out.println(t);
//        	}
        }
        // FIXME: this hack is necessary because the control flow phi nodes are not part of the control flow
        // the hack will work most of the times, but the slices are not guaranteed to be
        // time- and context-sensitive
        try{
        if (nrs.size() == 0) {
        	for (TopologicalNumber t : reached) {
	        	if (t.getProcID() == from.getProcID()) {
                    nrs.add(t);
	            }
	        }
        }
        } catch (NullPointerException ex) {
        	Log.ERROR.outln("failed intraprocedural neighbour check (phi control flow): " + ex.getStackTrace()[0]);
//        	System.out.println("B");
//        	System.out.println(nrs);
        }

        return nrs.descendingIterator();//.iterator();
    }

	/** Compute the intra-procedural predecessors of a context.
     */
    public Iterator<TopologicalNumber> interproceduralNeighbours(SDGNode neighbourNode, TopologicalNumber from) {
    	LinkedList<TopologicalNumber> nrs = contextGraphs.getPredecessors(from);
        LinkedList<TopologicalNumber> reached = contextGraphs.getTopologicalNumbersNew(neighbourNode, from.getThread());
        nrs.add(from); // to account for folded cycles
        nrs.retainAll(reached);

        return nrs.descendingIterator();//.iterator();
    }

	@Override
	public Iterator<TopologicalNumber> getTopologicalNumbers(SDGNode node, int thread) {
		return contextGraphs.getTopologicalNumbersNew(node, thread).descendingIterator();//.iterator();
	}
}
