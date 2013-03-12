/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;


public class Criterion {
	private Collection<SDGNode> source;
	private Collection<SDGNode> target;
	

	Criterion(SDGNode target) {
		this.target = Collections.singleton(target);
	}

	Criterion(SDGNode source, SDGNode target) {
		this.source = Collections.singleton(source);
		this.target = Collections.singleton(target);
	}

	Collection<SDGNode> getSource() {
		return source;
	}

	Collection<SDGNode> getTarget() {
		return target;
	}

	static List<Criterion> createNCriteriaRandomly(int n, SDG g)
	throws IllegalArgumentException {
		LinkedList<Criterion> result = new LinkedList<Criterion>();
		List<SDGNode> nodes = g.getNRandomNodes(n);

		for (SDGNode m : nodes) {
			result.add(new Criterion(m));
		}

		return result;
	}

	static List<Criterion> createNCriteria(int n, int offset, SDG g)
	throws IllegalArgumentException {
		LinkedList<Criterion> result = new LinkedList<Criterion>();
		List<SDGNode> nodes = g.getNNodes(n, offset);

		for (SDGNode m : nodes) {
			result.add(new Criterion(m));
		}

		return result;
	}

	static List<Criterion> createNRandomChoppingCriteria(int n, SDG g)
	throws IllegalArgumentException {
		LinkedList<Criterion> result = new LinkedList<Criterion>();
		List<SDGNode> sources = g.getNRandomNodes(n);
		List<SDGNode> targets = g.getNRandomNodes(n);

		for (int i = 0; i < n; i++) {
			result.add(new Criterion(sources.remove(0), targets.remove(0)));
		}

		return result;
	}
}
