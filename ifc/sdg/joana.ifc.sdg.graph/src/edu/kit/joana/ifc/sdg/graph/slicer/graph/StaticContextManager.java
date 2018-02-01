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
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNodeTuple;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.DynamicContextManager.DynamicContext;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.CallGraphBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.GraphFolder;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.THashSet;


public class StaticContextManager implements ContextManager<StaticContextManager.StaticContext> {

	/* the context class */
	public static class StaticContext extends Context {

		protected CallString stack;

		StaticContext(SDGNode n, CallString s, int t) {
			super(n,t);
			if (s == null) throw new RuntimeException("null call string for node "+n+" in proc "+n.getProc()+" in thread "+t);
			stack = s;
		}

		private StaticContext(SDGNode n, CallString s) {
			super(n, n.getThreadNumbers()[0]);
			if (s == null) throw new RuntimeException("null call string for node "+n+" in proc "+n.getProc());
			stack = s;
		}

		public StaticContext copy() {
			return new StaticContext(node, stack, thread);
		}
		
		public StaticContext copyWithNewNode(SDGNode newNode) {
			return new StaticContext(newNode, stack, thread);
		}

		

		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((stack == null) ? 0 : stack.hashCode());
			return result;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof StaticContext)) {
				return false;
			}
			StaticContext other = (StaticContext) obj;
			return node == other.node && stack == other.stack;
		}

		public boolean isInCallingProcedure(SDGNode n) {
			if (stack.isEmpty()) {
				return false;

			} else if (stack.peek().getProc() < 0) {
				// fold node - could be true
				return true;

			} else {
				return stack.peek().getProc() == n.getProc();
			}
		}

	    /** Returns the size of the Context.
	     */
	    public int size() {
	        return stack.calls.size() +1;
	    }

	    /** Returns the node of the context at the given position.
	     *
	     * @param index  The position of the desired node.
	     */
	    public SDGNode get(int index) {
	        if (index == 0) {
	            return node;

	        } else {
	            return stack.calls.get(index -1);
	        }
	    }

		public LinkedList<SDGNode> getCallStack() {
			return stack.calls;
		}

	    public String toString() {
	    	return "(" + node.getId()+", "+thread+", "+stack.toString()+")";
	    }

		public SDGNode top() {
			return stack.peek();
		}

		public boolean isEmpty() {
			return stack.isEmpty();
		}

		public void pop() {
			throw new UnsupportedOperationException();
		}

		public void push(SDGNode call) {
			throw new UnsupportedOperationException();
		}

		public StaticContext level(SDGNode reachedNode) {
			StaticContext newContext = this.copyWithNewNode(reachedNode);
	    	return newContext;
	    }

		public StaticContext descend(SDGNode reachedNode, SDGNodeTuple callSite) {
	        CallString called = this.stack.desc.get(callSite);
	        StaticContext down = new StaticContext(reachedNode, called, thread);
	        return down;
        }

		public StaticContext ascend(SDGNode reachedNode, SDGNodeTuple callSite) {
        	CallString caller = stack.asc.get(callSite);
        	if (caller != null) {
        		// if caller == null, the reached procedure has no other calling context
        		StaticContext up = new StaticContext(reachedNode, caller, thread);
//		            verify(up);
	            return up;
        	}

	        return null;
	    }


	    /** Sets attribute 'node' to the given value.
	     *
	     * @param node  The new value.
	     */
	    public void setNode(SDGNode node) {
	        throw new UnsupportedOperationException();
	    }

		public void setThread(int t) {
	        throw new UnsupportedOperationException();
		}
	}




	/* the call-string class */
	static class CallString {
		private final LinkedList<SDGNode> calls;
		private final THashMap<SDGNodeTuple, CallString> asc;  // call edge -> call string
		private final THashMap<SDGNodeTuple, CallString> desc; // call edge -> call string

		CallString (LinkedList<SDGNode> stack) {
			calls = stack;
			asc = new THashMap<SDGNodeTuple, CallString>();
			desc = new THashMap<SDGNodeTuple, CallString>();
		}

		private SDGNode peek() {
			return calls.peek();
		}

		private boolean isEmpty() {
			return calls.isEmpty();
		}

	    public String toString() {
	    	return "" + calls;
	    }
	}


	private static class ContextCreator {
		private final ContextComputer co;
//		private CallGraph c;
		private final SDG g;
		private final FoldedCallGraph fc;
		private final THashSet<DynamicContext> allContexts;
		private final TIntObjectHashMap<TIntObjectHashMap<LinkedList<CallString>>> map;
		private final THashMap<LinkedList<SDGNode>, CallString> unique;
		private final THashMap<DynamicContext, CallString> cons;

		private ContextCreator(SDG g, CallGraph c, FoldedCallGraph fc) {
			this.g = g;
//			this.c = c;
			this.fc = fc;
			allContexts = new THashSet<DynamicContext>();
			map = new TIntObjectHashMap<TIntObjectHashMap<LinkedList<CallString>>>();
			unique = new THashMap<LinkedList<SDGNode>, CallString>();
			cons = new THashMap<DynamicContext, CallString>();
			co = new ContextComputer(g, c, fc);
		}

		private void execute() {
			final Logger debug = Log.getLogger(Log.L_SDG_CALLGRAPH_DEBUG);
			
	        debug.outln("create all contexts...");
			create();
			debug.outln("created "+allContexts.size()+" contexts.");
			debug.outln("convert them to unique call strings...");
			convert();
			debug.outln("connect call strings to a call graph...");
			connect();
			debug.outln("done");
		}

		private void create() {
			for (SDGNode n : g.vertexSet()) {
				if (n.getKind() == SDGNode.Kind.ENTRY) {
					allContexts.addAll(co.allPossibleContextsForNode(n));
				}
			}
		}

		private void convert() {
			for (DynamicContext c : allContexts) {
				// retrieve the call string
				CallString s = unique.get(c.getCallStack());

				// update the unique call strings map
				if (s == null) {
					s = new CallString(c.getCallStack());
					unique.put(c.getCallStack(), s);
				}

				// update the main map
				int proc = c.getNode().getProc();
				TIntObjectHashMap<LinkedList<CallString>> procMap = map.get(proc);

				if (procMap == null) {
					procMap = new TIntObjectHashMap<LinkedList<CallString>>();
					map.put(proc, procMap);
				}

				for (int thread : c.getNode().getThreadNumbers()) {
					LinkedList<CallString> callStrings = procMap.get(thread);

					if (callStrings == null) {
						callStrings = new LinkedList<CallString>();
						procMap.put(thread, callStrings);
					}

					callStrings.add(s);
				}

				// update the context map
				cons.put(c, s);
			}
		}

		private void connect() {
			for (DynamicContext con : cons.keySet()) {
				// process incoming call edges that match the call stack
				for (SDGEdge e : g.incomingEdgesOf(con.getNode())) {
					if (e.getKind() == SDGEdge.Kind.CALL && fc.map(e.getSource()) == con.top()) {
						// compute new call stack by leaving actual procedure
						LinkedList<SDGNode> caller = con.getCallStack(); // returns a clone
				        caller.pop();

				        /* now we have the actual call stack, the calling call stack and the call edge -
				         * map them to CallStrings and connect those accordingly */
				        CallString from = unique.get(caller);

				        // check if this is a valid call string of the calling procedure
				        LinkedList<CallString> coll = map.get(e.getSource().getProc()).get(con.getThread());
				        if (coll == null || !coll.contains(from)) {
				        	continue;
				        }

				        CallString to = cons.get(con);
				        SDGNodeTuple callSite = new SDGNodeTuple(e.getSource(), e.getTarget());
						from.desc.put(callSite, to);
						to.asc.put(callSite, from);
					}
				}
			}
		}
	}

	/* ********* */
	/* Factories */

	public static StaticContextManager create(SDG g, CallGraph c, FoldedCallGraph fc) {
		ContextCreator cc = new ContextCreator(g, c, fc);
		cc.execute();
		return new StaticContextManager(fc, cc.map);
	}

	public static StaticContextManager create(SDG sdg) {
		CallGraph c = CallGraphBuilder.buildCallGraph(sdg);
        FoldedCallGraph fc = GraphFolder.foldCallGraph(c);
		return create(sdg, c, fc);
	}


	/* ********************************************* */
	/* ************ the context manager ************ */

	private final FoldedCallGraph foldedCall;
	private final TIntObjectHashMap<TIntObjectHashMap<LinkedList<CallString>>> procsThreadsCallStrings;

	private StaticContextManager(FoldedCallGraph fc, TIntObjectHashMap<TIntObjectHashMap<LinkedList<CallString>>> map) {
		foldedCall = fc;
		procsThreadsCallStrings = map;
	}


	/* public methods */

	public Collection<StaticContext> getAllContextsOf(SDGNode node) {
		THashSet<StaticContext> result = new THashSet<>();

		for (int thread : node.getThreadNumbers()) {
			result.addAll(getContextsOf(node, thread));
		}

		return result;
	}

	public Collection<StaticContext> getContextsOf(SDGNode node, int thread) {
		THashSet<StaticContext> result = new THashSet<>();
		LinkedList<CallString> l = procsThreadsCallStrings.get(node.getProc()).get(thread);

		for (CallString s : l) {
			StaticContext newContext = new StaticContext(node, s, thread);
			result.add(newContext);
		}

		return result;
	}

    public StaticContext level(SDGNode reachedNode, StaticContext oldContext) {
    	return oldContext.level(reachedNode);
    }

    public StaticContext descend(SDGNode reachedNode, SDGNodeTuple callSite, StaticContext oldContext) {
        // if the corresponding call site is recursive,
        // clone context and set `oldContext' as new node
        // else compute new context by going into procedure
        if (foldedCall.isFolded(callSite.getFirstNode())
        		&& oldContext.top() == this.foldedCall.map(callSite.getFirstNode())) {

        	return oldContext.level(reachedNode);

        } else {
	        return oldContext.descend(reachedNode, callSite);
        }
    }

    public StaticContext[] ascend(SDGNode reachedNode, SDGNodeTuple callSite, StaticContext oldContext) {
    	StaticContext[] res = {null, null};
        //System.out.println("Ascending to: "+callSite);
        //System.out.println(": "+pre);

        // top of context and call site are matching : traverse edge
        if (match(callSite.getFirstNode(), oldContext)){
            // if the procedure call is recursive,
            // build a context with the same call stack
            if (foldedCall.isFolded(callSite.getFirstNode())){
                res[0] = oldContext.level(reachedNode);
//                verify(recursive);
            }

            // compute new context by leaving actual procedure
//	            verify(up);
	        res[1] = oldContext.ascend(reachedNode, callSite);
        }

        return res;
    }

    private boolean match(SDGNode callSite, Context con){
        return con.isEmpty() || foldedCall.map(callSite) == con.top();
    }

	@Override
	public SDGNode unmap(SDGNode node) {
		return foldedCall.unmap(node);
	}


	/* DEBUG */
	public static void main(String[] args) throws IOException {
		for (String file : PDGs.pdgs2) {
			SDG sdg = SDG.readFrom(file);
			StaticContextManager man = StaticContextManager.create(sdg);

			for (SDGNode n : sdg.vertexSet()) {
				System.out.print(".");
				man.getAllContextsOf(n);
			}
			System.out.println("\ndone");
		}
	}
}
