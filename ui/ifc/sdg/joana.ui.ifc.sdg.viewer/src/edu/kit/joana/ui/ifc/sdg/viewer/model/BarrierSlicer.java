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
import java.util.Map;
import java.util.TreeSet;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


/** This class wraps barrier slicing algorithms.
 * It is suitable for all slicing algorithms that expect a slicing criterion
 * and a barrier and return a set of nodes as result.
 *
 * @author giffhorn
 *
 */
public class BarrierSlicer extends Run {
	/** Kind models the different kinds of criteria.
	 * In this case, it one kind for the slicing criterion
	 * and one for the barrier.
	 *
	 * @author giffhorn
	 *
	 */
	public static enum Kind {
		// the slicing criterion
        SLICING_CRITERION("Slicing Criterion"),
        // the barrier criterion
        BARRIER("Barrier");

        private final String value;

        Kind(String s) { value = s; }

        public String toString() {
            return value;
        }
    }

	// the slicer
	private edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierSlicer slicer;
	private TreeSet<SDGNode> criteria;
    private TreeSet<SDGNode> barrier;
	private Collection<SDGNode> result;

	/** Creates a new Slicer.
	 *
	 * @param alg    The algorithm to use.
	 * @param graph  The graph to slice.
	 */
	public BarrierSlicer(String className, Graph g) {
		// load the slicing algorithm
		Object o = AlgorithmFactory.loadClass(className, g);
		slicer = (edu.kit.joana.ifc.sdg.graph.slicer.barrier.BarrierSlicer)o;

		// initialize the result set
		criteria = new TreeSet<SDGNode>(SDGNode.getIDComparator());
        barrier = new TreeSet<SDGNode>(SDGNode.getIDComparator());
	}

	/** Returns the result or null, if computation is not finished.
	 *
	 */
	public Map<SDGNode, Integer> getResult() {
	    HashMap<SDGNode, Integer> map = new HashMap<SDGNode, Integer>();

        for (SDGNode n : result) {
            map.put(n, DEPENDENCY);
        }

        for (SDGNode n : criteria) {
            map.put(n, TARGET);
        }

        for (SDGNode n : barrier) {
            map.put(n, BARRIER);
        }

        return map;
	}

	/** Executes the slice and saves the result.
	 * It notifies the observer when the slice is finished.
	 */
	public void execute(){
		slicer.setBarrier(barrier);
	    result = slicer.slice(criteria);
	    //observer.update();
	}

	/** Returns the different kinds of criteria.
	 *
	 */
    public Kind[] getKindsOfCriteria() {
		return BarrierSlicer.Kind.values();
	}

	/** Adds new criteria to the slicing criterion.
	 *
	 * @param crit  The criteria to add.
	 * @param kind  Must be the kind SLICING_CRITERION.
	 */
	public void convertCriteria() {
		for (CriteriaCategory cc : this.getChildren()) {
            Collection<SDGNode> crit = null;

            if (cc.getName().equals(Kind.SLICING_CRITERION.toString())) {
                crit = criteria;

            } else if (cc.getName().equals(Kind.BARRIER.toString())) {
                crit = barrier;
            }

            if (crit != null) {
	            for (Criteria c : cc.getChildren()) {
	                crit.addAll(c.retrieveCriteria());
	            }
            }
        }
	}
}
