/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.DirectedPseudograph;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

public final class CallGraph extends DirectedPseudograph<CallGraph.Node, CallGraph.Edge> {

	private static final long serialVersionUID = 6040188211027171404L;
	private static EdgeFactory<Node, Edge> DEFAULT_EDGE_FACTORY = new CGEdgeFactory();

	public static CallGraph build(CallGraphFilter filter, com.ibm.wala.ipa.callgraph.CallGraph callgraph, PointerAnalysis pts, IMethod entry,
			IProgressMonitor progress) throws IllegalArgumentException, CallGraphBuilderCancelException {
		CallGraph cg = new CallGraph(filter, entry, callgraph, pts);

		cg.run(progress);

		return cg;
	}

	public static int NO_INDEX = -1;

	public static final class Edge {
		public final Node from;
		public final Node to;
		public final int instrIndex;
		public boolean dynamic;

		private Edge(Node from, Node to, int instrIndex, boolean dynamic) {
			this.from = from;
			this.to = to;
			this.instrIndex = instrIndex;
		}

		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (obj instanceof Edge) {
				Edge other = (Edge) obj;
				return from.equals(other.from) && to.equals(other.to) && instrIndex == other.instrIndex;
			}

			return false;
		}

		public int hashCode() {
			return (from.hashCode() ^ (to.hashCode() >> 6)) + instrIndex;
		}

		public String toString() {
//			return from.toString() + (dynamic ? "-" + instrIndex + "->" : "=" + instrIndex + "=>") + to.toString();
			return "CL";
		}
	}

	public static final class Node {
		public final CGNode node;

		private Node(CGNode node) {
			this.node = node;
		}

		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof Node) {
				Node other = (Node) obj;
				return node == other.node;
			}

			return false;
		}

		public int hashCode() {
			return node.hashCode();
		}

		public String toString() {
			return "CG[" + node.getMethod().getSignature() + "]";
		}
	}

	private static final class CGEdgeFactory implements EdgeFactory<Node, Edge> {

		@Override
		public Edge createEdge(Node arg0, Node arg1) {
			throw new UnsupportedOperationException("You need to specify an instruction index.");
		}

	}

	private final com.ibm.wala.ipa.callgraph.CallGraph cg;
	private final PointerAnalysis pts;
	private final CallGraphFilter filter;
	private final Node root;
	private final Map<CGNode, Node> method2node = new HashMap<CGNode, Node>();

	private CallGraph(CallGraphFilter filter, IMethod entry, com.ibm.wala.ipa.callgraph.CallGraph orig, PointerAnalysis pts) {
		super(DEFAULT_EDGE_FACTORY);
		this.filter = filter;
		this.cg = orig;
		this.pts = pts;
		this.root = findOrCreate(orig.getEntrypointNodes().iterator().next());
	}

	public com.ibm.wala.ipa.callgraph.CallGraph getOrig() {
		return cg;
	}

	public PointerAnalysis getPTS() {
		return pts;
	}

	private void run(final IProgressMonitor progress) throws IllegalArgumentException, CallGraphBuilderCancelException {
		final CGNode first;

		if (filter.ignoreWalaFakeWorldClinit()) {
			Collection<CGNode> entries = cg.getEntrypointNodes();
			assert entries.size() == 1;
			first = entries.iterator().next();
		} else {
			first = cg.getFakeRootNode();
		}

		Set<CGNode> visited = new HashSet<CGNode>();
		LinkedList<CGNode> todo = new LinkedList<CGNode>();
		visited.add(first);
		todo.add(first);

		while (!todo.isEmpty()) {
			final CGNode current = todo.removeFirst();

			final IR ir = current.getIR();
			if (ir == null) {
				continue;
			}

			final Node curNode = findOrCreate(current);

			if (filter.ignoreCallsFrom(current.getMethod())) {
				continue;
			}

			final SSAInstruction[] instructions = ir.getInstructions();

			for (int index = 0; index < instructions.length; index++) {
				final SSAInstruction instr = instructions[index];

				if (instr instanceof SSAAbstractInvokeInstruction) {
					final SSAAbstractInvokeInstruction invk = (SSAAbstractInvokeInstruction) instr;
					final Set<CGNode> targets = cg.getPossibleTargets(current, invk.getCallSite());
					if (targets == null) {
						continue;
					}

					assert (invk.isStatic() && targets.size() <= 1) || !invk.isStatic() : "Method: " + invk.getDeclaredTarget();

					for (CGNode tgtcg : targets) {
						final IMethod tgt = tgtcg.getMethod();

						if (filter.ignoreCallsTo(tgt)) {
							continue;
						}

						final Node tgtNode = findOrCreate(tgtcg);

						addEdge(curNode, tgtNode, invk.iindex, invk.isStatic());

						if (!visited.contains(tgtcg)) {
							visited.add(tgtcg);
							todo.add(tgtcg);
						}
					}
				}
			}
		}
	}

	public Node getRoot() {
		return root;
	}

	public Node findNode(CGNode m) {
		return method2node.get(m);
	}

	public List<Edge> findTarges(Node caller, int instrIndex) {
		List<Edge> targets = new LinkedList<Edge>();

		for (Edge edge : outgoingEdgesOf(caller)) {
			if (edge.instrIndex == instrIndex || edge.instrIndex == NO_INDEX) {
				targets.add(edge);
			}
		}

		return targets;
	}

	public Set<Edge> findCallers(Node caller) {
		return incomingEdgesOf(caller);
	}

	public String toString() {
		return "Callgraph of " + root.node.getMethod().getSignature();
	}

	private Node findOrCreate(CGNode m) {
		Node n = method2node.get(m);
		if (n == null) {
			n = new Node(m);
			addVertex(n);
			method2node.put(m, n);
		}

		return n;
	}

	private boolean addEdge(Node from, Node to, int index, boolean dynamic) {
		Edge edge = new Edge(from, to, index, dynamic);
		if (!containsEdge(edge)) {
			addEdge(from, to, edge);
			return true;
		}

		return false;
	}

	public static interface CallGraphFilter {

		/**
		 * Returns true iff a call to this method can be ignored.
		 * @param method The method in quesion.
		 * @return true if the method can be ignored.
		 */
		boolean ignoreCallsTo(IMethod method);

		/**
		 * Returns true iff a call from this method can be ignored.
		 * This happens for immutable classes we create stubs for (String)
		 * @param method The method in quesion.
		 * @return true if the method can be ignored.
		 */
		boolean ignoreCallsFrom(IMethod method);

		/**
		 * Returns true if the artificial calls to static initializers
		 * from the WALA callgraph should be ignored.
		 * @return true if the artificial calls to static initializers
		 * from the WALA callgraph should be ignored.
		 */
		boolean ignoreWalaFakeWorldClinit();

	}

}
