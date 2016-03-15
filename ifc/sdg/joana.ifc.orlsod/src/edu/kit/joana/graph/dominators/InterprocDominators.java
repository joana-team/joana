package edu.kit.joana.graph.dominators;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.util.CancelException;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

public class InterprocDominators implements IInterprocDominators {
	protected CFG icfg;
	private Collection<VirtualNode> allNodes = null;
	private Map<VirtualNode, SDGNodeSetVariable> node2var = new HashMap<VirtualNode, SDGNodeSetVariable>();

	public InterprocDominators(CFG icfg) {
		this.icfg = icfg;
	}

	public Collection<VirtualNode> getAllNodes() {
		if (allNodes == null) {
			allNodes = new ConcurrentLinkedDeque<VirtualNode>();
			for (SDGNode n : icfg.vertexSet()) {
				for (int thread : n.getThreadNumbers()) {
					allNodes.add(new VirtualNode(n, thread));
				}
			}
		}
		return allNodes;
	}
	/* (non-Javadoc)
	 * @see edu.kit.joana.graph.dominators.IInterprocDominators#run()
	 */
	@Override
	public void run() {
		Solver s = new Solver();
		try {
			s.solve(null);
			Set<VirtualNode> allNodes = new HashSet<VirtualNode>();
			for (SDGNode n : icfg.vertexSet()) {
				for (int thread : n.getThreadNumbers()) {
					VirtualNode v = new VirtualNode(n, thread);
					allNodes.add(v);
				}
			}
			for (VirtualNode v : allNodes) {
				SDGNodeSetVariable vv = node2var.get(v);
				if (vv.getValue().equals(allNodes)) {
					System.out.println(v + " is dominated by all nodes?!?!?!");
				}
			}
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}

	public void refineWithMHPInfo(MHPAnalysis mhp) {
		for (SDGNodeSetVariable v : node2var.values()) {
			v.refineWithMHPInfo(mhp);
		}
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.graph.dominators.IInterprocDominators#getDominators(edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode)
	 */
	@Override
	public Set<VirtualNode> getDominators(VirtualNode n) {
		SDGNodeSetVariable v = node2var.get(n);
		Set<VirtualNode> ret = new HashSet<VirtualNode>();
		if (v.getValue().isUnset()) {
			ret.addAll(getAllNodes());
		} else {
			return v.getValue().getElements();
		}
		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.graph.dominators.IInterprocDominators#getStrictDominators(edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode)
	 */
	@Override
	public Set<VirtualNode> getStrictDominators(VirtualNode n) {
		Set<VirtualNode> ret = new HashSet<VirtualNode>();
		ret.addAll(getDominators(n));
		ret.remove(n);
		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.graph.dominators.IInterprocDominators#getImmediateDominators(edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode)
	 */
	@Override
	public Set<VirtualNode> getImmediateDominators(VirtualNode n) {
		Set<VirtualNode> ret = new HashSet<VirtualNode>();
		// strict dominators of strict dominators of n
		Set<VirtualNode> strictDom = getStrictDominators(n);
		Set<VirtualNode> domDom = new HashSet<VirtualNode>();
		for (VirtualNode d : strictDom) {
			domDom.addAll(getStrictDominators(d));
		}
		ret.addAll(strictDom);
		ret.removeAll(domDom);
		return ret;
	}

	/* (non-Javadoc)
	 * @see edu.kit.joana.graph.dominators.IInterprocDominators#getDominated(edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode)
	 */
	@Override
	public Set<VirtualNode> getDominated(VirtualNode n) {
		Set<VirtualNode> ret = new HashSet<VirtualNode>();
		for (Map.Entry<VirtualNode, SDGNodeSetVariable> nodeAndVar : node2var.entrySet()) {
			VirtualNode m = nodeAndVar.getKey();
			NodeSetValue<VirtualNode> v = nodeAndVar.getValue().getValue();
			if (!m.equals(n) && v.contains(m)) {
				ret.add(m); // n is contained in the dominator set of m
			}
		}
		return ret;
	}

	private class Solver extends
			DefaultFixedPointSolver<NodeSetVariable<VirtualNode>> {

		@Override
		protected NodeSetVariable<VirtualNode>[] makeStmtRHS(int size) {
			return new SDGNodeSetVariable[size];
		}

		@Override
		protected void initializeVariables() {
			for (SDGNode n : icfg.vertexSet()) {
				for (int thread : n.getThreadNumbers()) {
					VirtualNode v = new VirtualNode(n, thread);
					node2var.put(v, new SDGNodeSetVariable(v));
				}
			}
		}

		@Override
		protected void initializeWorkList() {
			for (SDGEdge e : icfg.edgeSet()) {
				if (e.getKind() != SDGEdge.Kind.FORK && e.getSource().getKind() == SDGNode.Kind.CALL && BytecodeLocation.isCallRetNode(e.getTarget())) {
					// ignore edges from call sites to call_ret nodes
					continue;
				}
				for (int thread1 : e.getSource().getThreadNumbers()) {
					for (int thread2 : e.getTarget().getThreadNumbers()) {
						SDGNodeSetVariable v = node2var.get(new VirtualNode(e.getSource(), thread1));
						SDGNodeSetVariable w = node2var.get(new VirtualNode(e.getTarget(), thread2));
						switch (e.getKind()) {
						case RETURN:
							if (thread1 != thread2) throw new RuntimeException(e.toString());
							VirtualNode c = new VirtualNode(findCorrespondingCallNode(e), thread2);
							SDGNodeSetVariable vc = node2var.get(c);
							newStatement(w, new BinaryPropagator<VirtualNode>(), v, vc, true, false);
							break;
						default:
							newStatement(w, new SubsetPropagator<VirtualNode>(), v, true, false);
						}
					}
				}
			}
			System.out.println(getFixedPointSystem());
		}

		private SDGNode findCorrespondingCallNode(SDGEdge retEdge) {
			assert retEdge.getKind() == SDGEdge.Kind.RETURN : retEdge
					+ " is not a return edge!";
			for (SDGEdge inc : icfg.incomingEdgesOf(retEdge.getTarget())) {
				if (inc.getSource().getKind() == SDGNode.Kind.CALL) {
					return inc.getSource();
				}
			}
			throw new IllegalArgumentException("return edge " + retEdge
					+ " without corresponding call node?!?!?!");
		}
	}

	private class SDGNodeSetVariable extends NodeSetVariable<VirtualNode> {
		public SDGNodeSetVariable(VirtualNode id) {
			super(id, new NodeSetValue.NotSet<VirtualNode>());

			// test whether node has incoming intraproc cf edges or call edges
			// from _other_ methods
			// node is considered an entry node if the only incoming edges it
			// has are call edges
			// from the same method
			System.out.println(this + " " + id.getNode().getKind());
			boolean recursiveEntryOrNoInc = true;
			if (id.getNode().equals(icfg.getRoot())) {
				setValue(new NodeSetValue.SetBased<VirtualNode>(Collections.singleton(id)));
			}
			System.out.println(this + " " + recursiveEntryOrNoInc +" end");
		}

		public void refineWithMHPInfo(MHPAnalysis mhp) {
			if (value.isUnset()) {
				Set<VirtualNode> newElements = new HashSet<VirtualNode>(getAllNodes());
				for (VirtualNode n : allNodes) {
					if (!n.equals(id) && mhp.isParallel(n, id)) {
						newElements.remove(n);
					}
				}
				setValue(new NodeSetValue.SetBased<VirtualNode>(newElements));
			} else {
				Set<VirtualNode> elements = value.getElements();
				List<VirtualNode> toRemove = new LinkedList<VirtualNode>();
				for (VirtualNode n : elements) {
					if (!n.equals(id) && mhp.isParallel(n, id)) {
						toRemove.add(n);
					}
				}
				elements.removeAll(toRemove);
				setValue(new NodeSetValue.SetBased<VirtualNode>(elements));
			}
		}

		private boolean contains(int[] arr, int x) {
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] == x) {
					return true;
				}
			}
			return false;
		}
	}
}
