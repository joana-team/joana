package edu.kit.joana.graph.dominators;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

public class JoanaCFGAdapter implements AbstractCFG<VirtualNode,VirtualEdge>{

	private CFG icfg;
	private DirectedGraph<VirtualNode, VirtualEdge> underlyingGraph = null;

	public JoanaCFGAdapter(CFG icfg) {
		this.icfg = icfg;
	}
	@Override
	public VirtualNode getRoot() {
		return new VirtualNode(icfg.getRoot(), 0);
	}

	@Override
	public Collection<? extends VirtualNode> vertexSet() {
		return getUnderlyingGraph().vertexSet();
	}

	@Override
	public Collection<? extends VirtualEdge> inc(VirtualNode v) {
		Set<VirtualEdge> ret = new HashSet<VirtualEdge>();
		for (SDGEdge e : icfg.incomingEdgesOf(v.getNode())) {
			if (BytecodeLocation.isCallRetNode(v.getNode()) && e.getSource().getKind() == SDGNode.Kind.CALL) continue;
			if (!e.getKind().isThreadEdge() && hasThreadNumber(e.getSource(), v.getNumber())) {
				ret.add(new VirtualEdge(e, v.getNumber(), v.getNumber()));
			} else {
				for (int threadSrc : e.getSource().getThreadNumbers()) {
					if (threadSrc != v.getNumber()) {
						ret.add(new VirtualEdge(e, threadSrc, v.getNumber()));
					}
				}
			}
		}
		return ret;
	}

	@Override
	public Collection<? extends VirtualEdge> out(VirtualNode v) {
		Set<VirtualEdge> ret = new HashSet<VirtualEdge>();
		for (SDGEdge e : icfg.outgoingEdgesOf(v.getNode())) {
			if (BytecodeLocation.isCallRetNode(e.getTarget()) && v.getNode().getKind() == SDGNode.Kind.CALL) continue;
			if (!e.getKind().isThreadEdge() && hasThreadNumber(e.getTarget(), v.getNumber())) {
				ret.add(new VirtualEdge(e, v.getNumber(), v.getNumber()));
			} else {
				for (int threadTgt : e.getTarget().getThreadNumbers()) {
					if (threadTgt != v.getNumber()) {
						ret.add(new VirtualEdge(e, v.getNumber(), threadTgt));
					}
				}
			}
		}
		return ret;
	}

	@Override
	public VirtualNode getSource(VirtualEdge e) {
		return e.getSource();
	}

	@Override
	public VirtualNode getTarget(VirtualEdge e) {
		return e.getTarget();
	}

	@Override
	public boolean isCallEdge(VirtualEdge e) {
		return e.getKind() == SDGEdge.Kind.CALL;
	}

	@Override
	public boolean isReturnEdge(VirtualEdge e) {
		return e.getKind() == SDGEdge.Kind.RETURN;
	}

	@Override
	public VirtualEdge mapCallToReturn(VirtualEdge call) {
		VirtualNode callSite = call.getSource();
		for (VirtualEdge eOut : getUnderlyingGraph().outgoingEdgesOf(callSite)) {
			VirtualNode tgt = eOut.getTarget();
			if (BytecodeLocation.isCallRetNode(tgt.getNode())) {
				VirtualNode callRetSite = tgt;
				for (VirtualEdge eIn : getUnderlyingGraph().incomingEdgesOf(callRetSite)) {
					if (eIn.getKind() == SDGEdge.Kind.RETURN && eIn.getSource().getNode().getProc() == call.getTarget().getNode().getProc()) {
						return eIn;
					}
				}
			}
		}
		throw new IllegalStateException(String.format("Could not find corresponding return edge for call edge %s", call));
	}

	@Override
	public VirtualEdge mapReturnToCall(VirtualEdge ret) {
		/**
		 * 1.) The target of ret is a call ret node. Look for an incident call node (there should be only one). Call this node callSite.
		 * 2.) Look for an outgoing call edge of callSite where the proc of the target is the same as the proc of the source of the ret edge (the callee).
		 * 3.) This edge is the edge we are looking for!
		 */
		VirtualNode callRetSite = ret.getTarget();
		for (SDGEdge eIn : icfg.incomingEdgesOf(callRetSite.getNode())) {
			if (eIn.getSource().getKind() == SDGNode.Kind.CALL) {
				SDGNode callSite = eIn.getSource();
				for (SDGEdge eOut : icfg.outgoingEdgesOf(callSite)) {
					if (eOut.getKind() == SDGEdge.Kind.CALL && eOut.getTarget().getProc() == ret.getSource().getNode().getProc()) {
						return new VirtualEdge(eOut, ret.getSourceThread(), ret.getTargetThread());
					}
				}
			}
		}
		throw new IllegalStateException(String.format("Could not find corresponding call edge for ret edge %s", ret));
	}
	@Override
	public DirectedGraph<VirtualNode, VirtualEdge> getUnderlyingGraph() {
		if (this.underlyingGraph == null) {
			DirectedGraph<VirtualNode, VirtualEdge> ret = new DefaultDirectedGraph<VirtualNode, VirtualEdge>(VirtualEdge.class);
			for (SDGEdge e : icfg.edgeSet()) {
				SDGNode s = e.getSource();
				SDGNode t = e.getTarget();
				if (!e.getKind().isThreadEdge()) {
					for (int thread : s.getThreadNumbers()) {
						VirtualNode vs = new VirtualNode(s, thread);
						if (hasThreadNumber(t, thread)) {
							VirtualNode vt = new VirtualNode(t, thread);
							ret.addVertex(vs);
							ret.addVertex(vt);
							ret.addEdge(vs, vt, new VirtualEdge(e, thread, thread));
						}
					}
				} else {
					for (int threadS : s.getThreadNumbers()) {
						for (int threadT : t.getThreadNumbers()) {
							if (threadS != threadT) {
								VirtualNode vs = new VirtualNode(s, threadS);
								VirtualNode vt = new VirtualNode(t, threadT);
								ret.addVertex(vs);
								ret.addVertex(vt);
								ret.addEdge(vs, vt, new VirtualEdge(e, threadS, threadT));
							}
						}
					}
				}
			}
			this.underlyingGraph = ret;
		}
		return this.underlyingGraph;
	}
	private boolean hasThreadNumber(SDGNode t, int thread) {
		for (int thr : t.getThreadNumbers()) {
			if (thr == thread) {
				return true;
			}
		}
		return false;
	}
	@Override
	public boolean isJoinEdge(VirtualEdge e) {
		return e.getKind() == SDGEdge.Kind.JOIN;
	}
}
