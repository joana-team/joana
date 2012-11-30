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
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class ContextGraphs {
	private ContextGraph[] graphs;
	private ContextGraph whole;

	ContextGraphs(ContextGraph[] graphs, ContextGraph whole) {
		this.graphs = graphs;
		this.whole = whole;
	}

	public ContextGraph getWholeGraph() {
		return whole;
	}

	public int getNumberOfThreads() {
		return graphs.length;
	}

	public Collection<TopologicalNumber> getForkSites(int thread) {
		return whole.getForkSites(thread);
	}

	public HashSet<TopologicalNumber> getAllForkSites() {
		return whole.getAllForkSites();
	}

	public LinkedList<TopologicalNumber> getTopologicalNumbersNew(SDGNode node, int thread) {
		return graphs[thread].getTopologicalNumbers(node);
	}

	public Collection<TopologicalNumber> getTopologicalNumbers(SDGNode node) {
		LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
		for (int thread : node.getThreadNumbers()) {
			l.addAll(graphs[thread].getTopologicalNumbers(node));
		}
		return l;
	}

	public LinkedList<Integer> getThreadsOf(TopologicalNumber nr) {
		LinkedList<Integer> result = new LinkedList<Integer>();
		for (int i = 0; i < graphs.length; i++) {
			ContextGraph g = graphs[i];
			if (g.contains(nr)) {
				result.add(i);
			}
		}
		return result;
	}

	public boolean reach(TopologicalNumber from, TopologicalNumber to, int thread) {
		if (from.getNumber() == to.getNumber()) return true; // special case

		return graphs[thread].reach(from, to);
	}

	public LinkedList<TopologicalNumber> realisablePathBackward(SDGNode reached, int reachedThread, TopologicalNumber oldState) {
		// determine all contexts of reached which can be reached by state
		LinkedList<TopologicalNumber> tuples = graphs[reachedThread].getTopologicalNumbers(reached);

		if (oldState == null) {
			// initial state - all contexts of reached are valid
			return tuples;

		} else {
			LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
			for (TopologicalNumber t : tuples) {
				if (graphs[reachedThread].reach(t, oldState)) {
					l.add(t);
				}
			}
			return l;
		}

	}

	public LinkedList<TopologicalNumber> realisablePathForward(SDGNode reached, int reachedThread, TopologicalNumber oldState) {
		// determine all contexts of reached which can be reached by state
		LinkedList<TopologicalNumber> tuples = graphs[reachedThread].getTopologicalNumbers(reached);

		if (oldState == null) {
			// initial state - all contexts of reached are valid
			return tuples;

		} else {
			LinkedList<TopologicalNumber> l = new LinkedList<TopologicalNumber>();
			for (TopologicalNumber t : tuples) {
				if (graphs[reachedThread].reach(oldState, t)) {
					l.add(t);
				}
			}
			return l;
		}
	}

	public LinkedList<TopologicalNumber> getPredecessors(TopologicalNumber nr, int thread) {
		return graphs[thread].getPredecessors(nr);
	}

	public LinkedList<TopologicalNumber> getSuccessors(TopologicalNumber nr, int thread) {
		return graphs[thread].getSuccessors(nr);
	}

	public Collection<TopologicalNumber> getPredecessorsPlusNoFlow(TopologicalNumber nr, int thread) {
		return graphs[thread].getPredecessorsPlusNoFlow(nr);
	}

	public Collection<TopologicalNumber> getSuccessorsPlusNoFlow(TopologicalNumber nr, int thread) {
		return graphs[thread].getSuccessorsPlusNoFlow(nr);
	}

	public String toString() {
		StringBuilder b = new StringBuilder();
		int x = 0;
		for (ContextGraph g : this.graphs) {
			b.append("Thread "+x+":\n");
			b.append(g.toString());
			b.append("\n");
			x++;
		}
		return b.toString();
	}
}
