/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.viewer.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** This class wraps barrier chopping algorithms.
 * It is suitable for all barrier chopping algorithms that only need a source criterion and
 * a sink criterion and return a set of nodes as result.
 *
 * @author giffhorn
 *
 */
public class BarrierChopper extends Run {
	/** Kind models the different kinds of criteria.
	 * In this case, it contains one kind for the source criterion
	 * and one for the sink criterion.
	 *
	 * @author giffhorn
	 *
	 */
	public static enum Kind {
		// the sink criterion
        SINK_CRITERION("Target Criterion"),
        // the source criterion
        SOURCE_CRITERION("Source Criterion"),
        // the barrier criterion
        BARRIER("Barrier");;

        private final String value;

        Kind(String s) { value = s; }

        public String toString() {
            return value;
        }
    }

	// the chopper
	private edu.kit.joana.ifc.sdg.graph.chopper.barrier.BarrierChopper chopper;
	// the source criteria
	private Collection<SDGNode> sourceCriteria;
	// the sink criteria
	private Collection<SDGNode> sinkCriteria;
	// the barrier
	private Collection<SDGNode> barrier;
    private Collection<SDGNode> result;

	/** Creates a new Chopper.
	 *
	 * @param alg    The algorithm to use.
	 * @param graph  The graph to slice.
	 */
	public BarrierChopper(String className, Graph g) {

		// load the demanded algorithm
		Object o = AlgorithmFactory.loadClass(className, g);
		chopper = (edu.kit.joana.ifc.sdg.graph.chopper.barrier.BarrierChopper) o;

		// initialize the sourceCriteria and sinkCriteria and barrier sets
		sourceCriteria = new TreeSet<SDGNode>(SDGNode.getIDComparator());
		sinkCriteria = new TreeSet<SDGNode>(SDGNode.getIDComparator());
		barrier = new TreeSet<SDGNode>(SDGNode.getIDComparator());
	}


	/** Returns the result or null, if computation is not finished.
	 *
	 */
	public HashMap<SDGNode, Integer> getResult() {
        HashMap<SDGNode, Integer> map = new HashMap<SDGNode, Integer>();

        for (SDGNode n : result) {
            map.put(n, DEPENDENCY);
        }

        for (SDGNode n : sourceCriteria) {
            map.put(n, SOURCE);
        }

        for (SDGNode n : sinkCriteria) {
            map.put(n, TARGET);
        }

        for (SDGNode n : barrier) {
            map.put(n, BARRIER);
        }

        return map;
    }

	/** Executes the chope and saves the result.
	 * It notifies the observer when the slice is finished.
	 */
	public void execute(){
		//System.out.println("Chop from: "+sourceCriteria);
		//System.out.println("to "+sinkCriteria);
		chopper.setBarrier(barrier);
        result = chopper.chop(sourceCriteria, sinkCriteria);
        //System.out.println("Result: "+result);
        //observer.update();
	}

	/** Returns the different kinds of criteria.
	 *
	 */
    public Kind[] getKindsOfCriteria() {
		return BarrierChopper.Kind.values();
	}

	/** Adds new criteria to the given criterion.
	 *
	 * @param crit  The criteria to add.
	 * @param kind  SOURCE_CRITERION or SINK_CRITERION).
	 */
	public void convertCriteria() {
		for (CriteriaCategory cc : this.getChildren()) {
			Collection<SDGNode> criteria = null;

			if (cc.getName().equals(Kind.SINK_CRITERION.toString())) {
				criteria = sinkCriteria;

			} else if (cc.getName().equals(Kind.SOURCE_CRITERION.toString())) {
				criteria = sourceCriteria;

			}  else if (cc.getName().equals(Kind.BARRIER.toString())) {
                criteria = barrier;
            }

			if (criteria != null) {
				for (Criteria crit : cc.getChildren()) {
					criteria.addAll(crit.retrieveCriteria());
				}
			}
		}
	}
}
