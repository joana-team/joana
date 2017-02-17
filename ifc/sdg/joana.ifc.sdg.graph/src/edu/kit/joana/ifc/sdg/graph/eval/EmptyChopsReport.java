/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class EmptyChopsReport implements Report {
	private final Collection<Algorithm> algs;
	private HashMap<Algorithm, Integer> data;
	private HashMap<Algorithm, Collection<SDGNode>> temp;
	private long failCtr;

	EmptyChopsReport(Collection<Algorithm> algs) {
		this.algs = algs;
		clear();
	}


	public void clear() {
		data = new HashMap<Algorithm, Integer>();
		temp = new HashMap<Algorithm, Collection<SDGNode>>();
		for (Algorithm a : algs) {
			data.put(a, 0);
			temp.put(a, null);
		}

		failCtr = 0;
	}

	public void iterationAborted(RuntimeException ex, Criterion crit) {
		failCtr++;
	}

	public void iterationSucceeded() {
		for (Map.Entry<Algorithm, Collection<SDGNode>> en : temp.entrySet()) {
			if (en.getValue().isEmpty()) {
				int counter = data.get(en.getKey()) +1;
				data.put(en.getKey(), counter);
			}
		}
	}

	public void nextIteration() {
		// do nuffin'
	}

	public void update(Algorithm alg, Criterion crit, long time, Collection<SDGNode> nodes) {
		temp.put(alg, nodes);
	}

	public void evaluationFinished() {
		// do nuffin'
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("**** EMPTY CHOPS ****\n");
		b.append("failed iterations: "+failCtr+"\n");


		for (Algorithm a : algs) {
			b.append(a.getName());
			b.append(": ");
			b.append("empty = " + data.get(a));
			b.append("\n");
		}

		return b.toString();
	}
}
