/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.graph;

import java.util.LinkedList;
import java.util.List;

public class DFSNode<V> {

	private final V node;
	private final int discoveryTime;
	private final int finishingTime;
	private final List<DFSNode<V>> successors;

	public DFSNode(V node, int start, int finish, List<DFSNode<V>> successors) {
		this.node = node;
		this.discoveryTime = start;
		this.finishingTime = finish;
		this.successors = successors;
	}

	public V getNode() {
		return node;
	}

	public int getDiscoveryTime() {
		return discoveryTime;
	}

	public int getFinishingTime() {
		return finishingTime;
	}

	public List<DFSNode<V>> getSuccessors() {
		return new LinkedList<DFSNode<V>>(successors);
	}


}
