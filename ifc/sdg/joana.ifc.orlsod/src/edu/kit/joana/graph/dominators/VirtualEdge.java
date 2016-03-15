package edu.kit.joana.graph.dominators;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;

public class VirtualEdge {
	private final SDGEdge e;
	private final int threadSrc;
	private final int threadTgt;
	public VirtualEdge(SDGEdge e, int threadSrc, int threadTgt) {
		this.e = e;
		this.threadSrc = threadSrc;
		this.threadTgt = threadTgt;
	}
	public VirtualNode getSource() {
		return new VirtualNode(e.getSource(), threadSrc);
	}
	public int getSourceThread() {
		return threadSrc;
	}
	public int getTargetThread() {
		return threadTgt;
	}
	public VirtualNode getTarget() {
		return new VirtualNode(e.getTarget(), threadTgt);
	}
	public SDGEdge.Kind getKind() {
		return e.getKind();
	}
	public String toString() {
		return String.format("(%d, %d) -%s-> (%d, %d)", e.getSource().getId(), threadSrc, e.getKind(), e.getTarget().getId(), threadTgt);
	}
}
