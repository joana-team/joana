package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.dominators.Dominators;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGraph;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * wrapper class for Wala CFG w/ some utility functions
 */
public class CFG implements Graph<BasicBlock>, DotGraph {

	private final Method m;
	private final SSACFG walaCFG;
	private final BiMap<SSACFG.BasicBlock, BasicBlock> repMap = HashBiMap.create();
	private final List<BasicBlock> blocks;
	private final BasicBlock entry;
	private Dominators<BasicBlock> walaDoms;
	private edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators<BasicBlock> nildumuDoms;

	private CFG(Method m) {
		this.m = m;
		this.entry = new BasicBlock(m.getIr().getControlFlowGraph().entry(), this);
		this.blocks = new ArrayList<>();
		this.blocks.add(entry);
		this.walaCFG = m.getIr().getControlFlowGraph();
	}

	public static CFG buildCFG(Method m) {
		CFG cfg = new CFG(m);
		cfg.entry.findSuccessorsRec(cfg);
		cfg.blocks.forEach(b -> b.findPredecessors(cfg));

		// sorting these makes testing and debugging easier. However it should not be assumed that this list is sorted in the interpreter itself
		cfg.blocks.sort(Comparator.comparingInt(BasicBlock::idx));

		// loop and conditionals info
		cfg.nildumuDoms = new edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators<>(cfg.entry, (BasicBlock::succs));
		for (BasicBlock bb : cfg.blocks) {
			if (cfg.nildumuDoms.isPartOfLoop(bb)) {
				bb.setPartOfLoop(true);
				if (bb.equals(cfg.nildumuDoms.loopHeader(bb))) {
					bb.setLoopHeader(true);
				}
			}
			if (!bb.isLoopHeader()
					&& bb.succs().stream().filter(s -> !s.getWalaBasicBlock().isExitBlock()).count() > 1) {
				bb.setCondHeader(true);
			}
		}

		cfg.addDummyBlocks();
		cfg.walaDoms = Dominators.make(cfg, cfg.entry);

		DotGrapher.exportGraph(cfg);

		return cfg;
	}

	public int getLevel(BasicBlock b) {
		if (b.isDummy()) {
			return getLevel(b.succs().get(0));
		}
		return nildumuDoms.loopDepth(b);
	}

	private void addDummyBlocks() {
		List<BasicBlock> decisionNodes = this.blocks.stream().filter(BasicBlock::splitsControlFlow)
				.collect(Collectors.toList());
		for (BasicBlock b : decisionNodes) {
			List<BasicBlock> newSuccs = new ArrayList<>();
			for (BasicBlock succ : b.succs()) {
				BasicBlock newDummy = BasicBlock.createDummy(this, b.idx());
				if (succ.isPartOfLoop()) {
					newDummy.setPartOfLoop(true);
				}
				this.addNode(newDummy);
				this.replaceEdge(newDummy, succ, b);
				newSuccs.add(newDummy);
			}
			this.removeOutgoingEdges(b);
			newSuccs.forEach(d -> this.addEdge(b, d));
		}
	}

	public Set<BasicBlock> getBasicBlocksInLoop(BasicBlock header) {
		assert (header.isLoopHeader());
		Set<BasicBlock> inLoop = new HashSet<>();
		inLoop.add(header);

		// find predecessor w/ back-edge
		Optional<BasicBlock> loopJmpBack = header.preds().stream().filter(pred -> isDominatedBy(pred, header))
				.findFirst();
		assert (loopJmpBack.isPresent());

		// add all predecessors until header is reached
		inLoop.add(loopJmpBack.get());
		addLoopNodeRec(inLoop, loopJmpBack.get());

		return inLoop;
	}

	private void addLoopNodeRec(Set<BasicBlock> alreadyFound, BasicBlock current) {
		for (BasicBlock b : current.preds()) {
			if (!alreadyFound.contains(b)) {
				alreadyFound.add(b);
				addLoopNodeRec(alreadyFound, b);
			}
		}
	}

	public void print() {
		for (BasicBlock b : blocks) {
			b.print();
			StringBuilder succs = new StringBuilder("Successors: ");
			for (BasicBlock s : b.succs()) {
				succs.append(s.idx()).append(" ");
			}
			succs.append("\nPredecessors: ");
			for (BasicBlock s : b.preds()) {
				succs.append(s.idx()).append(" ");
			}
			System.out.println(succs.toString());
		}
	}

	public List<BasicBlock> getBlocks() {
		return blocks;
	}

	public Method getMethod() {
		return m;
	}

	public BasicBlock entry() {
		return BasicBlock.bBlock(m, m.getCFG().walaCFG.entry());
	}

	public SSACFG getWalaCFG() {
		return walaCFG;
	}

	public boolean isDominatedBy(BasicBlock node, BasicBlock potentialDom) {
		return walaDoms.isDominatedBy(node, potentialDom);
	}

	public BasicBlock getImmDom(BasicBlock node) {
		return walaDoms.getIdom(node);
	}

	@Override public void removeNodeAndEdges(BasicBlock n) throws UnsupportedOperationException {
		this.blocks.remove(n);
		for (BasicBlock b : blocks) {
			b.preds().remove(n);
			b.succs().remove(n);
		}
	}

	@Override public Iterator<BasicBlock> getPredNodes(BasicBlock n) {
		return n.preds().iterator();
	}

	@Override public int getPredNodeCount(BasicBlock n) {
		return n.preds().size();
	}

	@Override public Iterator<BasicBlock> getSuccNodes(BasicBlock n) {
		return n.succs().iterator();
	}

	@Override public int getSuccNodeCount(BasicBlock N) {
		return N.succs().size();
	}

	@Override public void addEdge(BasicBlock src, BasicBlock dst) {
		src.succs().add(dst);
		dst.preds().add(src);
	}

	@Override public void removeEdge(BasicBlock src, BasicBlock dst) throws UnsupportedOperationException {
		src.succs().remove(dst);
		dst.preds().remove(src);
	}

	public void replaceEdge(BasicBlock src, BasicBlock dst, BasicBlock oldSrc) {
		int pos = dst.preds().indexOf(oldSrc);
		dst.preds().remove(pos);
		dst.preds().add(pos, src);
		src.succs().add(dst);
	}

	@Override public void removeAllIncidentEdges(BasicBlock node) throws UnsupportedOperationException {
		removeIncomingEdges(node);
		removeOutgoingEdges(node);
	}

	@Override public void removeIncomingEdges(BasicBlock node) throws UnsupportedOperationException {
		node.emptyPreds();
		for (BasicBlock b : blocks) {
			b.succs().remove(node);
		}
	}

	@Override public void removeOutgoingEdges(BasicBlock node) throws UnsupportedOperationException {
		node.emptySuccs();
		for (BasicBlock b : blocks) {
			b.preds().remove(node);
		}
	}

	@Override public boolean hasEdge(BasicBlock src, BasicBlock dst) {
		assert (src.succs().contains(dst) == dst.preds().contains(src));
		return src.succs().contains(dst);
	}

	@Override public Iterator<BasicBlock> iterator() {
		return blocks.iterator();
	}

	@Override public int getNumberOfNodes() {
		return blocks.size();
	}

	@Override public void addNode(BasicBlock n) {
		blocks.add(n);
	}

	@Override public void removeNode(BasicBlock n) throws UnsupportedOperationException {
		removeAllIncidentEdges(n);
		blocks.remove(n);
	}

	@Override public boolean containsNode(BasicBlock n) {
		return blocks.contains(n);
	}

	public BasicBlock getBlock(int i) {
		return blocks.stream().filter(b -> b.idx() == i).findFirst().get();
	}

	public void addRep(SSACFG.BasicBlock a, BasicBlock b) {
		this.repMap.put(a, b);
	}

	public BiMap<SSACFG.BasicBlock, BasicBlock> repMap() {
		return this.repMap;
	}

	@Override public BasicBlock getRoot() {
		return this.entry;
	}

	@Override public List<DotNode> getNodes() {
		return this.blocks.stream().map(b -> (DotNode) b).collect(Collectors.toList());
	}

	@Override public String getName() {
		return m.identifier().replace('.', '_')
				.replace('(', '_')
				.replace(')', '_');
	}
}