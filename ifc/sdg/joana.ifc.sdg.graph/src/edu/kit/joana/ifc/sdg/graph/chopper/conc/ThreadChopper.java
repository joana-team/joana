/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.chopper.conc;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.Chopper;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaForward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.States;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.TopologicalNumber;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.WorklistElement;


/**
 * The AlmostTimeSensitiveThreadChopper is a context- and time-sensitive chopper for concurrent programs.
 *
 * For a description of its concrete functionality please consult the following Journal article {@link TODO}.
 * It employs Nanda's time-sensitive slicer {@link edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda [Nanda]} for the computation of
 * context- and time-sensitive chops.
 *
 * @author  Dennis Giffhorn
 */
public class ThreadChopper extends Chopper {
	/** A Nanda-style forward slicer. */
    private Forward forw;
	/** A Nanda-style backward slicer. */
    private Backward back;
	/** A canary for quick-checking if a chop is empty. */
    private ContextSensitiveThreadChopper canary;

    /**
     * Instantiates a ThreadChopper with a SDG.
     *
     * @param g   A SDG or a cSDG. Can be null.
     */
    public ThreadChopper(SDG g) {
    	super(g);
    }

    /**
     * Re-initializes the attributes.
     * Triggered by {@link Chopper#setGraph(SDG)}.
     */
    protected void onSetGraph() {
    	if (forw == null) {
    		forw = new Forward(sdg);
    	} else {
    		forw.setGraph(sdg);
    	}

		back = new Backward(forw);

    	if (canary == null) {
    		canary = new ContextSensitiveThreadChopper(sdg);
    	} else {
    		canary.setGraph(sdg);
    	}
    }

    /**
     * Computes a context-sensitive, time-insensitive chop from <code>sourceSet</code> to <code>sinkSet</code>.
     *
     * @param sourceSet  The source criterion set. Should not contain null, should not be empty.
     * @param sinkSet    The target criterion set. Should not contain null, should not be empty.
     * @return           The chop (a HashSet).
     */
    public Collection<SDGNode> chop(Collection<SDGNode> sourceSet, Collection<SDGNode> sinkSet) {
         Collection<SDGNode> canaryChop = canary.chop(sourceSet, sinkSet);

        if (canaryChop.isEmpty()) {
            return canaryChop;

        } else {
//        	System.out.println("("+sourceSet+", "+sinkSet+")");
        	long tmp = System.currentTimeMillis();
            Collection<SDGNode> backSlice = back.subgraphSlice(sinkSet, canaryChop);
            time1 += (System.currentTimeMillis() - tmp);
            forw.visited = back.visited;
            tmp = System.currentTimeMillis();
            Collection<SDGNode> chop = forw.subgraphSlice(sourceSet, backSlice);
            time2 += (System.currentTimeMillis() - tmp);
            return chop;
        }
    }
    public static long time1 = 0L;
    public static long time2 = 0L;
    /**
     * A variant of Nanda's forward slicer that checks if visited elements belong to the time-sensitive chop.
     *
     */
    static class Forward extends Nanda {
    	/** The slice computed by the backward slicer. */
    	private Collection<SDGNode> subGraph;
    	/** The elements visited by the backward slicer. */
        private HashMap<Key, LinkedList<States>> visited;

        /**
         * Creates a new instance of this algorithm.
         * @param graph  A SDG.
         */
        public Forward(SDG graph) {
            super(graph, new NandaForward());
        }

        /**
         * @return `true' if the edge belongs to the sub-graph spanned by the nodes in <code>subGraph</code>.
         */
        protected boolean omit(SDGEdge edge) {
        	return !subGraph.contains(edge.getTarget());
        }

        /**
         * Executes Nanda's slicing algorithm for a given set of slicing criterion.
         * Restricts the slice to the sub-graph spanned by the nodes in <code>sub</code>.
         *
         * @param crit  The slicing criterion.
         * @param sub   The nodes of a sub-graph.
         * @return      The slice.
         */
        public Collection<SDGNode> subgraphSlice(Collection<SDGNode> crit, Collection<SDGNode> sub) {
        	subGraph = sub;

        	HashSet<SDGNode> refined = new HashSet<SDGNode>();
        	for (SDGNode n : crit) {
                if (sub.contains(n)) {
                    refined.add(n);
                }
            }

        	return nandaSlice(refined);
        }

        protected void insert(boolean phase2, SDGNode reached, int thread, Iterator<TopologicalNumber> mu,
                States oldStates, LinkedList<WorklistElement> worklist, HashSet<SDGNode> slice) {

            // for all valid contexts of source ...
            boolean ok = false;

            while (mu.hasNext()) {
            	TopologicalNumber m = mu.next();
//                if (m == null) throw new RuntimeException("tried to insert null as tnr");
                // ... clone the current state tuple and update the clone ...
                States newStates = update(oldStates, m, thread);

                // ... run the restrictive state tuple optimization ...
                if (optimise(reached, thread, m, newStates, restrictive_1)){
                    continue;
                }

                if (phase2 && optimise(reached, thread, m, newStates, restrictive_2)) {
                	continue;
                }

                /* extended check: test if the new element is restrictive to the backward slice */
                if (valid(reached, thread, m, newStates)) {
                    ok = true;

	                // ... annotate the remaining contexts with the updated state tuple ...
	                WorklistElement newElement = new WorklistElement(reached, thread, m, newStates);

	                // ... add them to the worklist ...
	                worklist.addLast(newElement);

	                if (phase2) {
	                    restrictive_2.put(reached, thread, m.getNumber(), newStates);

	                } else {
	                	restrictive_1.put(reached, thread, m.getNumber(), newStates);
	                }
                }
            }

            if (ok) {
                slice.add(reached);
            }
        }


        /**
         * Tests the validity of a newly visited element.
         * It must be restrictive to at least one element visited by the backward slice.
         *
         * @param node     The node of the new element.
         * @param tnr      Its topological number.
         * @param states   Its state tuple.
         * @return         `true' in case the test succeeds.
         */
        private boolean valid(SDGNode node, int thread, TopologicalNumber tnr, States states) {
        	// get the corresponding elements visited by the backward slice
            Key k = new Key(node, thread, tnr);
            LinkedList<States> l = visited.get(k);
//            System.out.println(states);
//            System.out.println(l);
//            System.out.println("***");

            if (l == null) { // the context wasn't visited at all
                return false;
            }

            // check each state for quasi-restrictiveness
            for (States s : l) {
                boolean valid = true;

                // ... iterate over all single states ...
                for (int i = 0; i < states.size(); i++) {
                    // ... and if the state in toCheck can not reach the state of prev ...
                	if (!mode.restrictiveTest(states.get(i), s.get(i), i)) {
//                	if (!mhp.isParallel(states.get(i).getActualNode(), s.get(i).getActualNode())
//                			&& !mode.restrictiveTest(states.get(i), s.get(i))) {

                        // ... toCheck is not restrictive
                        valid = false;
                        break;
                    }
                }

                // ... else it is restrictive
                if (valid) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * A variant of Nanda's backward slicer stores all visited elements in an additional map.
     */
    static class Backward extends Nanda {
    	/** The sub-graph on which the slicer is restricted. */
    	private Collection<SDGNode> subGraph;
    	/** The elements visited by the backward slicer. */
        private HashMap<Key, LinkedList<States>> visited;

        /**
         * Creates a new instance of this algorithm.
         * @param graph  A SDG.
         */
        public Backward(Forward forw) {
        	super(forw, new NandaBackward());
        }

        /**
         * @return `true' if the edge belongs to the sub-graph spanned by the nodes in <code>subGraph</code>.
         */
        protected boolean omit(SDGEdge edge) {
        	return !subGraph.contains(edge.getSource());
        }

        /**
         * Stores every visited element in the <code>visited</code> map.
         * @see edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda#nandaSlice(Collection<SDGNode> crit, HashSet<SDGNode> slice)
         */
        protected void foo(WorklistElement next) {
        	 // new modification
            Key k = new Key(next.getNode(), next.getThread(), next.getTopolNr());
            LinkedList<States> l = visited.get(k);
            if (l == null) {
                l = new LinkedList<States>();
                visited.put(k,l);
            }
            l.add(next.getStates());
        }

        /**
         * Executes Nanda's slicing algorithm for a given set of slicing criteria.
         * Returns the computed slice as a sorted set of nodes.
         *
         * @param criteria  The slicing criteria.
         * @return          The slice.
         */
        public Collection<SDGNode> subgraphSlice(Collection<SDGNode> crit, Collection<SDGNode> sub) {
        	subGraph = sub;
            visited = new HashMap<Key, LinkedList<States>>();

        	HashSet<SDGNode> refined = new HashSet<SDGNode>();
        	for (SDGNode n : crit) {
                if (sub.contains(n)) {
                    refined.add(n);
                }
            }

        	return nandaSlice(refined);
        }
    }

    /**
    * A pair of VirtualNodes and TopologicalNumbers.
    * Used to store WorklistElements in a map.
    * Implements <code>equals</code> and <code>hashCode</code>.
    */
   static class Key {
       SDGNode node;
       int thread;
       TopologicalNumber tnr;

       /**
        * Initializes a new Key.
        *
        * @param node  A part of the key.
        * @param iscr  A part of the key.
        */
       Key(SDGNode node, int thread, TopologicalNumber tnr) {
           this.node = node;
           this.thread = thread;
           this.tnr = tnr;
       }

       /**
        * Returns the SDGNode of this Key.
        */
       SDGNode getNode() {
           return node;
       }

       int getThread() {
    	   return thread;
       }

       /**
        * Returns the ISCR of this Key.
        */
       TopologicalNumber getTNR() {
           return tnr;
       }

       public int hashCode() {
           return (31*thread + node.hashCode()) | (tnr.hashCode() << 16);
       }

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Key)) {
				return false;
			}

			Key k = (Key) o;
			return node == k.node && thread == k.thread && tnr.getNumber() == k.tnr.getNumber();
		}

       public String toString() {
           return node.toString()+" : "+ tnr.getNumber();
       }
   }
}
