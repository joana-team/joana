/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;


/**
 * Base class for all pdg and sdg nodes.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class AbstractPDGNode implements Comparable<AbstractPDGNode> {

	/*
	 * count has to start at 1 because the sdg viewer expects the first node
	 * (labelled *Start*) to have this id
	 */
	public static volatile int unique_count = 1;

	private final int id;
	private final int unique;
	private String label;
	private AbstractPDGNode catchedBy = null;

	AbstractPDGNode(int id) {
		this.id = id;
		this.unique = unique_count++;
	}

	/**
	 * Gets the id of this node. All nodes belonging a graph will have the same
	 * id as the graph. During the analysis nodes from other graphs
	 * (e.g. form-in/out) may be added to a pdg/sdg. Using the node ids is easy
	 * to sort them out as needed.
	 * @return id of this node
	 */
	public final int getPdgId() {
		return id;
	}

	/**
	 * Gets an id that is unique among all nodes that have been created.
	 * @return an unique id
	 */
	public final int getUniqueId() {
		return unique;
	}

	public AbstractPDGNode getCatcher() {
		return catchedBy;
	}

	public void setCatchedBy(final AbstractPDGNode catcher) {
		this.catchedBy = catcher;
	}

	public abstract void accept(IPDGNodeVisitor visitor);

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String toString() {
		return getClass().getSimpleName() + "(" + getLabel() + ")";
	}

	public boolean isParameterNode() {
		return false;
	}

	public int compareTo(AbstractPDGNode o) {
		return getUniqueId() - o.getUniqueId();
	}

}
