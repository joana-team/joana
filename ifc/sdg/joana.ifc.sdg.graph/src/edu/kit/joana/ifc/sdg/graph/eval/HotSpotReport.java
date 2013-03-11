/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class HotSpotReport implements Report {
	private class ValueSet {
		double time;
		double size;
	}

	private Collection<Algorithm> algs;
	private HashMap<Algorithm, LinkedList<ValueSet>> data;
	private HashMap<Algorithm, ValueSet> temp;
	private long failCtr;

	HotSpotReport(Collection<Algorithm> algs) {
		this.algs = algs;
		clear();
	}

	public void clear() {
		data = new HashMap<Algorithm, LinkedList<ValueSet>>();
		temp = new HashMap<Algorithm, ValueSet>();
		for (Algorithm a : algs) {
			data.put(a, new LinkedList<ValueSet>());
		}

		failCtr = 0;
	}

	public void iterationAborted(RuntimeException ex, Criterion crit) {
		failCtr++;
	}

	public void iterationSucceeded() {
		for (Map.Entry<Algorithm, ValueSet> en : temp.entrySet()) {
			LinkedList<ValueSet> l = data.get(en.getKey());
			l.addLast(en.getValue());
		}
	}

	public void nextIteration() {
		// do nuffin'
	}

	public void update(Algorithm alg, Criterion crit, long time, Collection<SDGNode> nodes) {
		ValueSet tmp = new ValueSet();
		tmp.size = nodes.size();
		tmp.time = time;
		temp.put(alg, tmp);
	}

	public void evaluationFinished() {
		// compute ratio
		for (Map.Entry<Algorithm, LinkedList<ValueSet>> en : data.entrySet()) {
			double totalTime = 0.0;
			double totalSize = 0.0;

			for (ValueSet v : en.getValue()) {
				totalTime += v.time;
				totalSize += v.size;
			}

			for (ValueSet v : en.getValue()) {
				BigDecimal myDec = new BigDecimal(v.time = (v.time / totalTime) * 100.0);
				myDec = myDec.setScale( 2, BigDecimal.ROUND_HALF_UP );
				v.time = myDec.doubleValue();


				myDec = new BigDecimal(v.size = (v.size / totalSize) * 100.0);
				myDec = myDec.setScale( 2, BigDecimal.ROUND_HALF_UP );
				v.size = myDec.doubleValue();
			}
		}
	}

	public String toString() { // version for OpenOffice
		StringBuilder b = new StringBuilder();
		b.append("**** HOT SPOTS ****\n");
		b.append("failed iterations: "+failCtr+"\n");

		for (Algorithm a : algs) {
			b.append("---------------------\n");
			b.append(a.getName());
			b.append(": \n");
			for (ValueSet v : data.get(a)) {
				b.append(v.time+",  ");
				b.append(v.size);
				b.append("\n");
			}
		}

		return b.toString();
	}
}
