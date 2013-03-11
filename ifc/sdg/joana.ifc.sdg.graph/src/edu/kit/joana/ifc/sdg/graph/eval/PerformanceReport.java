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

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class PerformanceReport implements Report {
	private class ValueSet {
		long time;
		long size;
	}

	private Collection<Algorithm> algs;
	private HashMap<Algorithm, ValueSet> data;
	private HashMap<Algorithm, ValueSet> temp;
	private long failCtr;

	PerformanceReport(Collection<Algorithm> algs) {
		this.algs = algs;
		clear();
	}

	public void clear() {
		data = new HashMap<Algorithm, ValueSet>();
		temp = new HashMap<Algorithm, ValueSet>();
		for (Algorithm a : algs) {
			data.put(a, new ValueSet());
			temp.put(a, new ValueSet());
		}

		failCtr = 0;
	}

	public void iterationAborted(RuntimeException ex, Criterion crit) {
		failCtr++;
	}

	public void iterationSucceeded() {
		for (Algorithm a : temp.keySet()) {
			ValueSet tmp = temp.get(a);
			ValueSet dat = data.get(a);

			dat.time += tmp.time;
			dat.size += tmp.size;
		}
	}

	public void nextIteration() {
		// do nuffin'
	}

	public void update(Algorithm alg, Criterion crit, long time, Collection<SDGNode> nodes) {
		ValueSet tmp = temp.get(alg);
		tmp.size = nodes.size();
		tmp.time = time;
	}

	public void evaluationFinished() {
		// do nuffin'
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("**** TOTAL RUNTIME AND SIZE ****\n");
		b.append("failed iterations: "+failCtr+"\n");

		for (Algorithm a : algs) {
			ValueSet dat = data.get(a);

			b.append(a.getName());
			b.append(": ");
			b.append("time = " + dat.time+",  ");
			b.append("size = " + dat.size);
			b.append("\n");
		}

		return b.toString();
	}
}
