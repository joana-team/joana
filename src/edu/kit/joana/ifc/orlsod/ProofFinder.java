package edu.kit.joana.ifc.orlsod;

import java.util.List;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.Multigraph;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.building.ICFGBuilder;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.PreciseMHPAnalysis;

public class ProofFinder {

	private SDG sdg;
	private I2PBackward backw;
	private ICDomOracle cdomOracle;
	private SimpleTCFGChopper tcfgChopper;
	private PreciseMHPAnalysis mhpAnalysis;
	private Multigraph<VirtualNode, MIEdge> mayInfluenceGraph = new Multigraph<VirtualNode, MIEdge>(MIEdge.class);

	public ProofFinder(SDG sdg, PreciseMHPAnalysis mhp, ICDomOracle cdomOracle) {
		this.sdg = sdg;
		this.backw = new I2PBackward(sdg);
		this.cdomOracle = cdomOracle;
		this.tcfgChopper = new SimpleTCFGChopper(ICFGBuilder.extractICFG(sdg));
		this.mhpAnalysis = mhp;
	}


	public Multigraph<VirtualNode, MIEdge> buildMIGraph() {
		// 1.) x may-influence n if x \in BS(n)
		for (SDGNode n : sdg.vertexSet()) {
			for (SDGNode m : backw.slice(n)) {
				if (n.equals(m)) continue;
				for (int threadM : m.getThreadNumbers()) {
					VirtualNode vm = new VirtualNode(m, threadM);
					for (int threadN : n.getThreadNumbers()) {
						VirtualNode vn = new VirtualNode(n, threadN);
						mayInfluenceGraph.addVertex(vm);
						mayInfluenceGraph.addVertex(vn);
						mayInfluenceGraph.addEdge(vm, vn, new BWSliceEdge(vm, vn));
					}
				}

			}
		}


		// 2.) x may-influence n if MHP(m,n), c = cdom(m,n), x \in CFGChop(c,n)
		for (SDGNode n : sdg.vertexSet()) {
			for (int threadN : n.getThreadNumbers()) {
				VirtualNode vn = new VirtualNode(n, threadN);
				for (SDGNode m : sdg.vertexSet()) {
					for (int threadM : m.getThreadNumbers()) {
						VirtualNode vm = new VirtualNode(m, threadM);
						if (mhpAnalysis.isParallel(vn, vm)) {
							VirtualNode c = cdomOracle.cdom(vn.getNode(), vn.getNumber(), vm.getNode(), vm.getNumber());
							for (SDGNode x : tcfgChopper.chop(c.getNode(), n)) {
								if (n.equals(x)) continue;
								for (int threadX : x.getThreadNumbers()) {
									VirtualNode vx = new VirtualNode(x, threadX);
									mayInfluenceGraph.addVertex(vx);
									mayInfluenceGraph.addVertex(vn);
									mayInfluenceGraph.addEdge(vx, vn, new ProbEdge(vx, vn, vm, c));
								}
							}
						}
					}
				}
			}
		}

		return mayInfluenceGraph;
	}

	public List<MIEdge> findMIProof(VirtualNode m, VirtualNode n) {
		return DijkstraShortestPath.findPathBetween(this.mayInfluenceGraph, m, n);
	}

	public static abstract class MIEdge {
		protected VirtualNode source;
		protected VirtualNode target;
		MIEdge(VirtualNode source, VirtualNode target) {
			this.source = source;
			this.target = target;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((source == null) ? 0 : source.hashCode());
			result = prime * result
					+ ((target == null) ? 0 : target.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (!(obj instanceof MIEdge))
				return false;
			MIEdge other = (MIEdge) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			if (target == null) {
				if (other.target != null)
					return false;
			} else if (!target.equals(other.target))
				return false;
			return true;
		}
	}

	private static class BWSliceEdge extends MIEdge {
		BWSliceEdge(VirtualNode source, VirtualNode target) {
			super(source, target);
		}
	}

	private static class ProbEdge extends MIEdge {
		private VirtualNode mhpNode;
		private VirtualNode cdom;
		ProbEdge(VirtualNode x, VirtualNode n, VirtualNode mhpNode, VirtualNode cdom) {
			super(x, n);
			this.mhpNode = mhpNode;
			this.cdom = cdom;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = super.hashCode();
			result = prime * result + ((cdom == null) ? 0 : cdom.hashCode());
			result = prime * result
					+ ((mhpNode == null) ? 0 : mhpNode.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!super.equals(obj))
				return false;
			if (!(obj instanceof ProbEdge))
				return false;
			ProbEdge other = (ProbEdge) obj;
			if (cdom == null) {
				if (other.cdom != null)
					return false;
			} else if (!cdom.equals(other.cdom))
				return false;
			if (mhpNode == null) {
				if (other.mhpNode != null)
					return false;
			} else if (!mhpNode.equals(other.mhpNode))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return String.format("MHP(%s,%s), cdom(%s,%s) = %s, %s ∈ CFGChop(%s,%s)", target, mhpNode, target, mhpNode, cdom, source, cdom, target);
		}
	}
}
