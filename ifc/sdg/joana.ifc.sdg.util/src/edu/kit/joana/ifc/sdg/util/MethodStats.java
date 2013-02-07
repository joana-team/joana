/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util;

import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;

/**
 * Provides information about the number of nodes and edges in a single method
 * (represented by a corresponding part of an SDG).
 * 
 * @author Martin Mohr
 */
class MethodStats {

	/** the number of nodes in the represented method */
	private final int numNodes;

	/**
	 * the number of edges for which both source and target are contained in the
	 * represented method
	 */
	private final int numIntraEdges;

	/** the number of edges entering the represented method */
	private final int numInterInEdges;

	/** the number of edges leaving of the represented method */
	private final int numInterOutEdges;

	/**
	 * Constructs a new MethodStats object from the given data.
	 * 
	 * @param numNodes
	 *            number of nodes in the represented method
	 * @param numIntraEdges
	 *            number of edges for which both source and target are contained
	 *            in the represented method
	 * @param numInterInEdges
	 *            number of edges entering the represented method
	 * @param numInterOutEdges
	 *            number of edges leaving of the represented method
	 */
	private MethodStats(int numNodes, int numIntraEdges, int numInterInEdges, int numInterOutEdges) {
		this.numNodes = numNodes;
		this.numIntraEdges = numIntraEdges;
		this.numInterOutEdges = numInterOutEdges;
		this.numInterInEdges = numInterInEdges;
	}

	/**
	 * Returns the number of nodes of the represented method.
	 * 
	 * @return the number of nodes of the represented method
	 */
	public int getNumberOfNodes() {
		return numNodes;
	}

	/**
	 * Returns the number of edges of the represented method.
	 * 
	 * @return the number of edges of the represented method
	 */
	public int getNumberOfEdges() {
		return numIntraEdges + numInterOutEdges + numInterInEdges;
	}

	/**
	 * Returns whether both the number of nodes and edges are zero.
	 * 
	 * @return {@code true}, if both the number of nodes and egdes are zero.
	 */
	public boolean isZero() {
		return getNumberOfNodes() == 0 && getNumberOfEdges() == 0;
	}
	
	public double computeSimilarity(MethodStats ms) {
		if (isZero() || ms.isZero()) {
			throw new IllegalArgumentException("can only compute similarity between non-zero method stats!");
		}
		int[] v1 = getStatsVector();
		int[] v2 = ms.getStatsVector();
		return scalarProduct(v1, v2) / (length(v1) * length(v2));
	}
	
	private int[] getStatsVector() {
		int[] ret = new int[4];
		ret[0] = numNodes;
		ret[1] = numIntraEdges;
		ret[2] = numInterInEdges;
		ret[3] = numInterOutEdges;
		return ret;
	}
	
	private static double length(int[] v) {
		return Math.sqrt(scalarProduct(v,v));
	}
	
	private static double scalarProduct(int[] v, int[] w) {
		if (v.length != w.length) {
			throw new IllegalArgumentException("can only calculate scalar product between vectors of same length!");
		}
		
		double ret = 0.0;
		for (int i = 0; i < v.length; i++) {
			ret += v[i]*w[i];
		}
		
		return ret;
	}
	
	

	/**
	 * Builds a MethodStats object from the given entry node. All the nodes and
	 * edges of the method (or its PDG, respectively) entered by the given entry
	 * nodes are traversed and counted. The additional SDG parameter provides
	 * information about the egdes, especially the edges leaving or entering the
	 * method under consideration.
	 * 
	 * @param entry
	 *            entry node of the method to build the MethodStats object from
	 * @param sdg
	 *            system dependence graph which contains the method under
	 *            consideration and provides in particular information about the
	 *            edges leaving or entering that method
	 * @return a MethodStats object containing information about the number of
	 *         nodes and edges in the method entered by the given entry node
	 */
	public static MethodStats computeFrom(SDGNode entry, SDG sdg) {
		int numNodes = 0, numIntraEdges = 0, numInterOutEdges = 0, numInterInEdges = 0;
		for (SDGNode n : sdg.getNodesOfProcedure(entry)) {

			for (SDGEdge eIn : sdg.incomingEdgesOf(n)) {
				if (eIn.getSource().getProc() == n.getProc()) {
					numIntraEdges++;
				} else {
					numInterInEdges++;
				}
			}

			for (SDGEdge eOut : sdg.outgoingEdgesOf(n)) {
				if (eOut.getTarget().getProc() == n.getProc()) {
					numIntraEdges++;
				} else {
					numInterOutEdges++;
				}
			}

			numNodes++;
		}

		// we have to halve the number of intraprocedural edge because we
		// counted it twice (as outgoing and incoming edge)
		return new MethodStats(numNodes, numIntraEdges / 2, numInterInEdges, numInterOutEdges);
	}

	public static MethodStats subtract(MethodStats ms1, MethodStats ms2) {
		return new MethodStats(ms1.numNodes - ms1.numNodes, ms2.numIntraEdges - ms2.numIntraEdges, ms1.numInterInEdges
				- ms2.numInterInEdges, ms1.numInterOutEdges - ms2.numInterOutEdges);
	}

	public static List<MethodStats> subtract(MethodStats ms1, List<MethodStats> ms2) {
		List<MethodStats> ret = new LinkedList<MethodStats>();
		for (MethodStats m : ms2) {
			ret.add(subtract(ms1, m));
		}
		return ret;
	}

}