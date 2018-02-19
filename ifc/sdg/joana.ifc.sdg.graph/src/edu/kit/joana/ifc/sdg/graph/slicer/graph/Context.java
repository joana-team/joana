/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;


/** Represents a vertex and its context.
 * The context consists of a call site stack with the vertex on top.
 * All vertices in the stack are mapped to a folded graph, therefore a Context
 * object contains also the unmapped topmost vertex as a separate field.
 *
 * @author Dennis Giffhorn
 * @version 1.0
 * @see DynamicContext,StaticContext
 */
public abstract class Context<C> {
	protected final SDGNode node;
	protected final int thread;

	Context(SDGNode n, int t) {
		node = n;
		thread = t;
	}

	/* **************** */
	/* public interface */

	public abstract boolean isInCallingProcedure(SDGNode n);

	public abstract boolean isEmpty();

	public abstract LinkedList<SDGNode> getCallStack();

	public abstract C copy();

	public abstract int size();

	public abstract SDGNode get(int pos);

	public SDGNode getNode() {
		return node;
	}

	public int getThread() {
		return thread;
	}

	public boolean isCallStringPrefixOf(Context<C> c) {
		LinkedList<SDGNode> s = getCallStack();
		LinkedList<SDGNode> cs = c.getCallStack();
		if (s.size() <= cs.size()) {
			for (int i = 0; i < s.size(); i++) {
				if (s.get(i) != cs.get(i)) {
					// different nodes -> no prefix
					return false;
				}
			}

			// this is a prefix of c
			return true;

		} else {
			// this is longer that c, so no prefix
			return false;
		}
	}


	/* ***************** */
	/* package interface */

	public abstract void push(SDGNode call);

	public abstract void pop();

	public abstract SDGNode top();

    public abstract C level(SDGNode reachedNode);

    public abstract C descend(SDGNode reachedNode, SDGNodeTuple callSite);

    public abstract C ascend(SDGNode reachedNode, SDGNodeTuple callSite);
}
