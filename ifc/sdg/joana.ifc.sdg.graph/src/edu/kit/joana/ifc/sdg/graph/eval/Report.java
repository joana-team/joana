/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public interface Report {
	public enum Kind {
        PERFORMANCE("P") {
			@Override
			public Report instantiate(Collection<Algorithm> algs) {
				return new PerformanceReport(algs);
			}
		},
        HOT_SPOTS("H") {
			@Override
			public Report instantiate(Collection<Algorithm> algs) {
				return new HotSpotReport(algs);
			}
		},
        EMPTY_CHOPS("E") {
			@Override
			public Report instantiate(Collection<Algorithm> algs) {
				return new EmptyChopsReport(algs);
			}
		};

        private final String value;

        private Kind(String val) {
        	value = val;
        }

        public String getValue() {
            return value;
        }

	    public abstract Report instantiate(Collection<Algorithm> algs);
	}

	void nextIteration();

	void iterationSucceeded();

	void iterationAborted(RuntimeException ex, Criterion crit);

	void update(Algorithm alg, Criterion crit, long time, Collection<SDGNode> nodes);

	void evaluationFinished();

	void clear();
}
