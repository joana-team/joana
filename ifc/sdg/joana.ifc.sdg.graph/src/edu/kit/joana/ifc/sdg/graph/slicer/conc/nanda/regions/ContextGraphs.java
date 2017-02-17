/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions;

import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;


public final class ContextGraphs {
	private final ContextGraph[] graphs;

	private ContextGraphs(ContextGraph[] graphs) {
		this.graphs = graphs;
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

	public boolean reach(TopologicalNumber from, TopologicalNumber to) {
		if (from.getThread() != to.getThread()) throw new RuntimeException("NO NO!");

		if (from.getNumber() == to.getNumber()) return true; // special case

		return graphs[from.getThread()].reach(from, to);
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

	public LinkedList<TopologicalNumber> getPredecessors(TopologicalNumber nr) {
		return graphs[nr.getThread()].getPredecessors(nr);
	}

	public LinkedList<TopologicalNumber> getSuccessors(TopologicalNumber nr) {
		return graphs[nr.getThread()].getSuccessors(nr);
	}

	public Collection<TopologicalNumber> getPredecessorsPlusNoFlow(TopologicalNumber nr) {
		return graphs[nr.getThread()].getPredecessorsPlusNoFlow(nr);
	}

	public Collection<TopologicalNumber> getSuccessorsPlusNoFlow(TopologicalNumber nr) {
		return graphs[nr.getThread()].getSuccessorsPlusNoFlow(nr);
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


	/* FACTORY */
	public static ContextGraphs build(CFG icfg) {
		/* 1. Build ISCR graphs */
		ISCRBuilder iscrBuilder = new ISCRBuilder();
		ISCRGraph[] iscrGraphs = iscrBuilder.buildISCRGraphs(icfg);

		/* 2. Build context graphs */
		ContextGraphBuilder builder = new ContextGraphBuilder(icfg);
		ContextGraph[] contextGraphs = builder.buildContextGraphs(iscrGraphs);

		/* 3. Done */
		ContextGraphs cg = new ContextGraphs(contextGraphs);

		return cg;
	}

	public static void main(String[] args) throws Exception {
    	for (String file : PDGs.pdgs) {
    		System.out.println(file);
            SDG g = SDG.readFrom(file);
            CFG cfg = ICFGBuilder.extractICFG(g);
            ContextGraphs cg = ContextGraphs.build(cfg);
            testNodes(g, cg);
//            System.out.println(cg);
        }
	}

	private static void testNodes(SDG g, ContextGraphs cg) {
		for (SDGNode n : g.vertexSet()) {
			for (int t : n.getThreadNumbers()) {
				if (cg.getTopologicalNumbersNew(n, t) == null) {
					System.out.println(n+" "+n.getKind()+" "+n.getProc()+" "+t);
//					throw new RuntimeException();
				}
			}
		}
	}
}
