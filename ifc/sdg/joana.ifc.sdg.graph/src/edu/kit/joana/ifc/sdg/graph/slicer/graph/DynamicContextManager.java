/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.JoanaGraph;
import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;


/**
 *
 * @author  Dennis Giffhorn
 */
public class DynamicContextManager implements ContextManager {
	public static class DynamicContext extends Context {
	    /** A list, simulating a stack containing the context. */
	    private LinkedList<SDGNode> callStack;

	    /** Creates a new instance of Context.
	     * Initialises attribut 'context' with an empty list.
	     */
	    public DynamicContext() {
	    	super(null, 0);
	        this.callStack = new LinkedList<SDGNode>();
	    }

	    /** Creates a new instance of Context with the given vertex as sole element.
	     *
	     * @param node  The given vertex.
	     */
	    public DynamicContext(SDGNode node) {
	    	super(node, node.getThreadNumbers()[0]);
	        this.callStack = new LinkedList<SDGNode>();
	    }

	    /** Creates a new instance of Context.
	     * Uses the given context and vertex.
	     *
	     * @param context  The context.
	     * @param node  The unmapped vertex on top of the context.
	     */
	    public DynamicContext(LinkedList<SDGNode> callStack, SDGNode node) {
	    	super(node, node.getThreadNumbers()[0]);
	        this.callStack = callStack;
	    }

	    /** Creates a new instance of Context with the given vertex as sole element.
	     *
	     * @param node  The given vertex.
	     */
	    public DynamicContext(SDGNode node, int thread) {
	    	super(node, thread);
	        this.callStack = new LinkedList<SDGNode>();
	    }

	    /** Creates a new instance of Context.
	     * Uses the given context and vertex.
	     *
	     * @param context  The context.
	     * @param node  The unmapped vertex on top of the context.
	     */
	    public DynamicContext(LinkedList<SDGNode> callStack, SDGNode node, int thread) {
	    	super(node, thread);
	        this.callStack = callStack;
	    }

	    /** Creates a clone of a Context object.
	     * It clones the list of the attribute 'context', but not the vertices in it or
	     * attribute 'node'. Thus their references keep the same.
	     *
	     * @return  A clone of the calling Context instance.
	     */
	    @SuppressWarnings("unchecked")
	    public DynamicContext copy() {
	        DynamicContext clone = new DynamicContext((LinkedList<SDGNode>) callStack.clone(), node, thread);

	        return clone;
	    }

	    /* getter */


	    /** Returns the context.
	     * The first element in the returned list is the topmost element, the second
	     * element is the top of the call stack.
	     */
	    @SuppressWarnings("unchecked")
	    LinkedList<SDGNode> asList() {
	        LinkedList<SDGNode> clone = (LinkedList<SDGNode>) callStack.clone();

	        clone.addFirst(node);

	        return clone;
	    }

	    /** Returns the topmost call site of the calling Context object.
	     * That is the second node in the Context.
	     */
	    public SDGNode top() {
	        if (this.callStack.size() > 0) {
	            return this.callStack.get(0);

	        } else {
	            return null;
	        }
	    }

	    /** Returns the size of the Context.
	     */
	    public int size() {
	        return this.callStack.size() +1;
	    }

	    /** Returns the node of the context at the given position.
	     *
	     * @param index  The position of the desired node.
	     */
	    public SDGNode get(int index) {
	        if (index == 0) {
	            return node;

	        } else {
	            return this.callStack.get(index -1);
	        }
	    }

	    /** Returns the call stack of the calling Context object.
	     */
	    @SuppressWarnings("unchecked")
	    public LinkedList<SDGNode> getCallStack() {
	        LinkedList<SDGNode> cs = (LinkedList<SDGNode>) this.callStack.clone();

	        return cs;
	    }

	    /** Checks whether the Context is empty.
	     */
	    public boolean isEmpty() {
	        return callStack.isEmpty() && node == null;
	    }

	    /** Checks if this Context is an initial thread state.
	     */
	    boolean isInit() {
	        return node == null;
	    }

	    /** Checks if the call stack of the Context is empty.
	     */
	    boolean callStackIsEmpty() {
	        return callStack.isEmpty();
	    }

	    boolean isFolded() {
	        if (callStack.isEmpty()) return false;
	        else return callStack.peek().getId() < 0;
	    }

	    /** Compares a given Context with the calling Context.
	     * They are considered equal if they have the same attribute 'node', the same
	     * stack size and their stacks contain the same vertices in the same order.
	     *
	     * @param c  The Context to comparre with.
	     */
	    public boolean equals(Object o){
	    	DynamicContext c = (DynamicContext) o;

	        if (isEmpty() && c.isEmpty()) {
	            return true;
	        }

	        if (isEmpty() || c.isEmpty()) {
	            return false;
	        }

	        if (c.size() != size()) {
	            return false;
	        }

	        if (node.getId() != c.getNode().getId()) {
	            return false;
	        }

	        if (c.getThread() != thread) {
	            return false;
	        }

	        for (int i = 0; i < size(); i++) {
	            if (c.get(i).getId() != get(i).getId()) {
	                return false;
	            }
	        }

	        return true;
	    }

	    /** Checks if the Context is a suffix of given Context 'con'
	     * @param con  The given Context.
	     * @return  'true' if this is a suffix of con.
	     */
	    public boolean isSuffixOf(DynamicContext con) {
	        List<SDGNode> callString = con.getCallStack();

	        if (callStack.size() > callString.size()) return false;

	        int i = callStack.size() -1;
	        int j = callString.size() -1;

	        while (i >= 0 && j >= 0) {
	            if (callStack.get(i) != callString.get(j)) {
	                return false;
	            }
	            i--;
	            j--;
	        }

	        return true;
	    }

	    boolean extensionOf(DynamicContext con) {
	        if (con.size() > callStack.size()) return false;

	        int i = callStack.size() -1;
	        int j = con.size() -1;

	        while (i >= 0 && j >= 0) {
	            if (callStack.get(i) != con.get(j)) {
	                return false;
	            }
	            i--;
	            j--;
	        }

	        return true;
	    }

	    /** Checks whether the stack of the calling Context contains a given vertex.
	     *
	     * @param callSite  The vertex.
	     */
	    boolean contains(SDGNode vertex) {
	        if (node == vertex) {
	            return true;
	        }

	        for (SDGNode n : callStack) {
	            if (n == vertex) {
	                return true;
	            }
	        }

	        return false;
	    }

	    /** Returns 'true' if this Context subsumes the given Context c, else 'false.
	     *
	     * @param c  The Context to compare with.
	     */
	    public boolean subsumes(DynamicContext c) {
	        if (c.size() < this.size() || c.getNode().getId() != this.getNode().getId()) {
	            return false;
	        }

	        boolean subsuming = true;

	        for (int i = 0; i < this.size(); i++) {
	            SDGNode thisN = this.get(i);
	            SDGNode cN = c.get(i);
	            if (thisN.getId() != cN.getId()) {
	                subsuming = false;
	            }
	        }

	        return subsuming;
	    }


	    /* setter */

	    /** Removes the topmost node of the context.
	     * That is the first element in attribute 'context'.
	     */
	    public void pop() {
	        this.callStack.poll();
	    }

	    /** Pushes  a new node on the call stack of this Context.
	     *
	     * @param call  The new top of stack.
	     */
	    public void push(SDGNode call) {
	        this.callStack.addFirst(call);
	    }

	    /** Returns a string representation of this Context.
	     */
	    public String toString() {

	        String str = "<Node: " + node + ", ";

	        str += "thread: "+thread;

	        if (this.size() > 1) {
	        	str += ", ";
	            str += "Call Stack: [";
	            for (int i = 0; i < this.callStack.size(); i++) {

	                str += this.callStack.get(i).getId();
	                if (i < this.callStack.size() - 1) {
	                	str += ", ";
	                }
	            }
	            str += "]";
	        }
	        str += ">";
	        return str;
	    }

	    public int hashCode() {
	    	int hc = (node == null ? 1 : node.hashCode());

	    	for (SDGNode n : callStack) {
	    		hc = 31*hc + (n==null ? 0 : n.hashCode());
	    	}

	    	return hc;
	    }

		@Override
		public boolean isInCallingProcedure(SDGNode n) {
			if (callStack.isEmpty()) {
				return false;

			} else if (callStack.peek().getProc() < 0) {
				// fold node - could be true
				return true;

			} else {
				return callStack.peek().getProc() == n.getProc();
			}
		}

		/**
	     * Traverses an intraprocedural edge.
	     *
	     * @param pre The vertex is directly reachable from the current context.
	     * @param next The current context.
	     * @return The context of pre.
	     */
	    public Context level(SDGNode reachedNode) {
	        Context newContext = copy();

	        // set 'pre' as new node of context
	        newContext.setNode(reachedNode);

	        return newContext;
	    }

	    /**
	     * Returns a new context when leaving a procedure.
	     *
	     * @param old The old context.
	     * @param newNode The reached node.
	     * @return The new context.
	     */
	    public Context ascend(SDGNode reachedNode, SDGNodeTuple callSite){
	        Context up = copy();

	        // pop the topmost call site from the context
	        up.pop();

	        // set the given node as the new context node
	        up.setNode(reachedNode);

	        return up;
	    }


	    /**
	     * Returns a context when going into a procedure.
	     *
	     * @param con  The old context.
	     * @param call  The new call site.
	     * @return  The context for 'node'.
	     */
	    public Context descend(SDGNode reachedNode, SDGNodeTuple callSite){
	        Context down = copy();
	        down.push(callSite.getFirstNode());
	        down.setNode(reachedNode);
	        return down;
	    }
	}

    /** A comparator for DynamicContexts.
     */
    public static class ContextComparator implements Comparator<DynamicContext> {

        /** Compares two given Contexts by comparing their SDGNode's IDs and
         * their thread numbers.
         *
         * @return  -1: If 'one's SDGNode ID is smaller than 'two's or
         *              if 'one's thread numnber is smaller than 'two's.
         *           1: If 'one's SDGNode ID is bigger than 'two's or
         *              if 'one's thread numnber is smaller than 'two's.
         *           0: If both Contexts are equal.
         */
        public int compare(DynamicContext one, DynamicContext two) {
            // compare contexts
            if (one.isEmpty() && two.isEmpty()) {
                return 0;
            }

            if (one.isEmpty()) {
                return -1;
            }

            if (two.isEmpty()) {
                return 1;
            }

            if (one.getThread() > two.getThread()) {
                return 1;

            } else if (one.getThread() < two.getThread()) {
                return -1;
            }

            if (one.size() == two.size()) {
                for (int i = 0; i < one.size(); i++) {

                    if(one.get(i).getId() < two.get(i).getId()) {
                        return -1;

                    } else if (one.get(i).getId() > two.get(i).getId()) {
                        return 1;
                    }
                }

            } else if (one.size() > two.size()) {
                return 1;

            } else if (one.size() < two.size()) {
                return -1;
            }

            return 0;
        }
    }

    private static final ContextComparator COMP = new ContextComparator();

	/** Returns a Comparator for Contexts.
     */
    public static ContextComparator contextComparator() {
        return COMP;
    }

    /* ******************************** */
    /* The actual DynamicContextManager */

    /** The call graph of the program. */
    private final FoldedCallGraph foldedCall;
    private final ContextComputer conCom;

    /**
     * Creates a new instance.
     */
    public DynamicContextManager(JoanaGraph g) {
    	CallGraph call = CallGraphBuilder.buildCallGraph(g);
        foldedCall = GraphFolder.foldCallGraph(call);
        conCom = new ContextComputer(g, call, foldedCall);
    }


    /* function definitions for slicing algorithm */

	/**
     * Traverses an intra-procedural edge.
     *
     * @param pre The vertex is directly reachable from the current context.
     * @param next The current context.
     * @return The context of pre.
     */
    public Context level(SDGNode reachedNode, Context oldContext) {
        return oldContext.level(reachedNode);
    }

    /**
     * Ascends into a calling procedure, if the call site is matching the top
     * of the current context.
     *
     * @param pre The vertex is directly reachable from the current context.
     * @param e The edge connecting 'pre' and the current context.
     * @param next The current context.
     * @return The contexts of the reached node - can be null, 1 or 2 (recursive calls)
     */
    public Context[] ascend(SDGNode reachedNode, SDGNodeTuple callSite, Context oldContext){
        // the call site of the calling procedure
        SDGNode call = callSite.getFirstNode();
        Context[] res = {null, null};

        // top of context and call site are matching : traverse edge
        if (match(call, oldContext)){
            // if the procedure call is recursive,
            // build a context with the same call stack
            if (foldedCall.isFolded(call)){
                res[0] = oldContext.level(reachedNode);
            }

            // compute new context by leaving actual procedure
            res[1] = oldContext.ascend(reachedNode, null);
        }

        return res;
    }

    /**
     * Descends into a called procedure.
     *
     * @param pre 'pre' is directly reachable from the current context.
     * @param e The edge connecting 'pre' and the current context.
     * @param next The current context.
     * @return The resulting context.
     */
    public Context descend(SDGNode reachedNode, SDGNodeTuple callSite, Context oldContext) {
        // the call site calling the procedure to descend into
        SDGNode call = callSite.getFirstNode();
        SDGNode mapped = foldedCall.map(call);
        Context newContext = null;

        // if the corresponding call site is recursive,
        // clone context and set 'pre' as new node
        // else compute new context by going into procedure
        if (foldedCall.isFolded(call) && oldContext.top() == mapped){
            newContext = oldContext.level(reachedNode);

        } else {
            SDGNodeTuple t = new SDGNodeTuple(foldedCall.map(call), null);
            newContext = oldContext.descend(reachedNode, t);
        }

        return newContext;
    }



    /**
     * Checks whether a call site matches with the topmost call site of a context.
     * It considers fold nodes.
     * Returns also true if the context is empty.
     *
     * @param callSite  The call site.
     * @param con The context.
     */
    protected boolean match(SDGNode callSite, Context con){
        return con.isEmpty()
        			// check folded nodes
        			|| foldedCall.map(callSite) == con.top();
    }

	public Collection<DynamicContext> getExtendedContextsOf(SDGNode node) {
		return conCom.getExtendedContextsOf(node);
	}

	@Override
	public Collection<Context> getAllContextsOf(SDGNode node) {
		return conCom.getAllContextsOf(node);
	}

	@Override
	public Collection<Context> getContextsOf(SDGNode node, int thread) {
		return conCom.getContextsOf(node, thread);
	}

	@Override
	public SDGNode unmap(SDGNode node) {
		return foldedCall.unmap(node);
	}

	public SDGNode map(SDGNode node) {
		return foldedCall.map(node);
	}

	public boolean isFolded(SDGNode node) {
		return foldedCall.isFolded(node);
	}

	/* DEBUG */
	public static void main(String[] args) throws IOException {
		for (String file : PDGs.pdgs2) {
			SDG sdg = SDG.readFrom(file);
			DynamicContextManager man = new DynamicContextManager(sdg);

			for (SDGNode n : sdg.vertexSet()) {
				System.out.print(".");
				man.getAllContextsOf(n);
			}
			System.out.println("\ndone");
		}
	}
}

