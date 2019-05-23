/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.LeastCommonAncestor;

/**
 * TODO: @author Add your name here.
 */
public abstract class AbstractPseudoTreeNode<V extends IntegerIdentifiable, S> implements LeastCommonAncestor.PseudoTreeNode<S>, IntegerIdentifiable {
	final V v;

	boolean changed;
	boolean inWorkset;
	boolean isRelevant;

	
	S next;
	Object inPathOf;
	
	S[] successors;

	static final int UNDEFINED = -1;
	int dfsNumber;

	
	protected AbstractPseudoTreeNode(V v) {
		this.v = v;

		this.changed = false;
		this.inWorkset = false;
		this.isRelevant = false;
		
		this.dfsNumber = UNDEFINED;

	}
	
	public final V getV() {
		return v;
	}

	@Override
	public final S getNext() {
		return next;
	}
	
	public S[] getSuccessors() {
		return successors;
	}
	
	public void setSuccessors(S[] successors) {
		this.successors = successors;
	}
	@Override
	public String toString() {
		return v.toString();
	}
	
	@Override
	public final int getId() {
		return v.getId();
	}
	
	@Override
	public final void addToPath(Object o) {
		inPathOf = o;
	}
	
	@Override
	public final boolean onPath(Object o) {
		return inPathOf == o;
	}

	public boolean isRelevant() {
		return isRelevant;
	}
	
	public void setRelevant(boolean isRelevant) {
		this.isRelevant = isRelevant;
	}


}
