package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.dominators.Dominators;

import java.util.Iterator;
import java.util.Set;

/**
 * wrapper class for Wala CFG w/ some utility functions
 */
public class CFG implements Graph<BBlock> {

	private final Method m;
	private final SSACFG walaCFG;
	private Set<BBlock> blocks;
	private final BBlock entry;
	private Dominators<BBlock> walaDoms;
	private edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators<BBlock> nildumuDoms;

	private CFG(Method m) {
		this.m = m;
		this.entry = new BBlock(m.getIr().getControlFlowGraph().entry(), this);
		this.walaCFG = m.getIr().getControlFlowGraph();
	}

	public static CFG buildCFG(Method m) {
		CFG cfg = new CFG(m);
		cfg.entry.findSuccessorsRec(cfg.walaCFG);
		cfg.blocks = BBlock.allBlocks();

		// loop and conditionals info
		cfg.walaDoms = Dominators.make(cfg, cfg.entry);
		cfg.nildumuDoms = new edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators<>(cfg.entry, (BBlock::succs));
		for(BBlock bb: cfg.blocks) {
			if (cfg.nildumuDoms.isPartOfLoop(bb)) {
				bb.setPartOfLoop(true);
				if(bb.equals(cfg.nildumuDoms.loopHeader(bb))) {
					bb.setLoopHeader(true);
				}
			} else {
				if (bb.succs().stream().filter(s -> !s.getWalaBasicBLock().isExitBlock()).count() > 1) {
					bb.setCondHeader(true);
				}
			}
		}
		return cfg;
	}

	public void print() {
		for(BBlock b: blocks) {
			b.print();
			StringBuilder succs = new StringBuilder("Successors: ");
			for(BBlock s: b.succs()) {
				succs.append(s.getWalaBasicBLock().toString()).append(" ");
			}
			System.out.println(succs.toString());
		}
	}

	public Set<BBlock> getBlocks() {
		return blocks;
	}

	public Method getMethod() {
		return m;
	}

	public BBlock entry() {
		return BBlock.bBlock(m.getCFG().walaCFG.entry());
	}

	public SSACFG getWalaCFG() {
		return walaCFG;
	}

	public boolean isDominatedBy(BBlock node, BBlock potentialDom) {
		return walaDoms.isDominatedBy(node, potentialDom);
	}

	public BBlock getImmDom(BBlock node) {
		return walaDoms.getIdom(node);
	}

	@Override public void removeNodeAndEdges(BBlock n) throws UnsupportedOperationException {
		this.blocks.remove(n);
		for(BBlock b: blocks) {
			b.preds().remove(n);
			b.succs().remove(n);
		}
	}

	@Override public Iterator<BBlock> getPredNodes(BBlock n) {
		return n.preds().iterator();
	}

	@Override public int getPredNodeCount(BBlock n) {
		return n.preds().size();
	}

	@Override public Iterator<BBlock> getSuccNodes(BBlock n) {
		return n.succs().iterator();
	}

	@Override public int getSuccNodeCount(BBlock N) {
		return N.succs().size();
	}

	@Override public void addEdge(BBlock src, BBlock dst) {
		src.succs().add(dst);
		dst.preds().add(src);
	}

	@Override public void removeEdge(BBlock src, BBlock dst) throws UnsupportedOperationException {
		src.succs().remove(dst);
		dst.preds().remove(src);
	}

	@Override public void removeAllIncidentEdges(BBlock node) throws UnsupportedOperationException {
		removeIncomingEdges(node);
		removeOutgoingEdges(node);
	}

	@Override public void removeIncomingEdges(BBlock node) throws UnsupportedOperationException {
		node.emptyPreds();
		for (BBlock b: blocks) {
			b.succs().remove(node);
		}
	}

	@Override public void removeOutgoingEdges(BBlock node) throws UnsupportedOperationException {
		node.emptySuccs();
		for (BBlock b: blocks) {
			b.preds().remove(node);
		}
	}

	@Override public boolean hasEdge(BBlock src, BBlock dst) {
		assert(src.succs().contains(dst) == dst.preds().contains(src));
		return src.succs().contains(dst);
	}

	@Override public Iterator<BBlock> iterator() {
		return blocks.iterator();
	}

	@Override public int getNumberOfNodes() {
		return blocks.size();
	}

	@Override public void addNode(BBlock n) {
		blocks.add(n);
	}

	@Override public void removeNode(BBlock n) throws UnsupportedOperationException {
		removeAllIncidentEdges(n);
		blocks.remove(n);
	}

	@Override public boolean containsNode(BBlock n) {
		return blocks.contains(n);
	}
}
