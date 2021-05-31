package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ssa.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;

public class BBlock implements DotNode {

	private static final Map<Integer, BBlock> dummies = new HashMap<>();
	private static int dummyCtr = -2;

	private final SSACFG.BasicBlock walaBBlock;
	private final CFG g;
	private final List<SSAInstruction> instructions;
	private IFTreeNode ifTree;
	private final int idx;
	private final boolean isDummy;
	private List<BBlock> succs;
	private List<BBlock> preds;
	private boolean isLoopHeader = false;
	private boolean isPartOfLoop = false;
	private boolean isCondHeader = false;
	private boolean isBreak;
	private boolean inScope = true;
	/**
	 * expression used for conditional jump to successor expressed as SAT formula.
	 */
	private Formula condExpr;
	private int replacedPredIdx;

	private boolean hasRelevantCF;

	public BBlock(SSACFG.BasicBlock walaBBlock, CFG g) {
		this.walaBBlock = walaBBlock;
		this.g = g;
		this.instructions = walaBBlock.getAllInstructions().stream().filter(i -> !(i == null))
				.collect(Collectors.toList());
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		this.ifTree = IFTreeNode.NoIFLeaf.SINGLETON;
		this.isDummy = false;
		this.idx = walaBBlock.getNumber();
		this.hasRelevantCF = true;
		g.addRep(walaBBlock, this);
	}

	private BBlock(CFG g, int idx, int replacedPredIdx) {
		this.isDummy = true;
		this.replacedPredIdx = replacedPredIdx;
		this.walaBBlock = null;
		this.instructions = new ArrayList<>();
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		this.ifTree = IFTreeNode.NoIFLeaf.SINGLETON;
		this.idx = idx;
		this.g = g;
		this.hasRelevantCF = true;
		dummies.put(this.idx, this);
	}

	public static BBlock createDummy(CFG g, int replacedPredIdx) {
		return new BBlock(g, dummyCtr--, replacedPredIdx);
	}

	private static void addEdge(BBlock from, BBlock to) {
		from.succs.add(to);
		to.preds.add(from);
	}

	public static BBlock bBlock(Method m, SSACFG.BasicBlock walaBBlock) {
		return m.getCFG().repMap().get(walaBBlock);
	}

	public static BBlock getBBlockForInstruction(SSAInstruction i, CFG g) {
		return g.repMap().values().stream().filter(b -> b.hasInstruction(i)).findFirst().get();
	}

	public static BBlock getBlockForIdx(Method m, int idx) {
		if (idx < -1) {
			return dummies.get(idx);
		}
		return m.getCFG().repMap()
				.get(m.getCFG().repMap().keySet().stream().filter(b -> b.getNumber() == idx).findFirst().get());
	}

	/**
	 * DFS over the CFG, creeates new Basic Blocks and adds successors accorsdingly
	 * In order to build a complete CFG, the predecessors need to be generated separately
	 * see {@code} findPredecessors
	 *
	 * @param g a cfg with an initialized start node and a walaCFG which is used to generate the rest of the CFG
	 */
	public void findSuccessorsRec(CFG g) {
		SSACFG walaCFG = g.getWalaCFG();
		List<ISSABasicBlock> succs = Util.asList(walaCFG.getSuccNodes(walaBBlock));
		for (ISSABasicBlock ibb : succs) {
			SSACFG.BasicBlock bb = (SSACFG.BasicBlock) ibb;

			if (g.repMap().containsKey(bb)) {
				addEdge(this, g.repMap().get(bb));
			} else {
				BBlock newSucc = new BBlock(bb, this.g);
				g.addNode(newSucc);
				this.succs.add(newSucc);
				newSucc.findSuccessorsRec(g);
			}
		}
	}

	/**
	 * computes the predecessors for a basic block and makes sure the order matches the order of walaCFG.getPredNdoes(b) which is used in the evaluation of SSA Phi instructions
	 *
	 * @param g a CFG, for which all nodes and their successors have already been initialized
	 */
	public void findPredecessors(CFG g) {
		this.preds = new ArrayList<>();
		for (Iterator<ISSABasicBlock> it = g.getWalaCFG().getPredNodes(this.getWalaBasicBlock()); it.hasNext(); ) {
			ISSABasicBlock b = it.next();
			this.preds.add(g.repMap().get(b));
		}
	}

	public Formula generateImplicitFlowFormula() {
		return this.ifTree.getImplicitFlowFormula();
	}

	public List<SSAInstruction> instructions() {
		return instructions;
	}

	public List<BBlock> succs() {
		return succs;
	}

	/**
	 * Order of the list corresponds to walaCFG.getPredNodes(walaBasicBlock), hence can be used to evaluate phi instructions
	 * If the list contains a dummy node, check {@code getReplacedIndex()} to find the block that contains the correct value for the phi operand
	 *
	 * @return ordered list of the blocks predecessors
	 */
	public List<BBlock> preds() {
		return preds;
	}

	public boolean isLoopHeader() {
		return isLoopHeader;
	}

	public void setLoopHeader(boolean loopHeader) {
		isLoopHeader = loopHeader;
	}

	public boolean isCondHeader() {
		return isCondHeader;
	}

	public void setCondHeader(boolean condHeader) {
		isCondHeader = condHeader;
	}

	public boolean splitsControlFlow() {
		return isCondHeader || isLoopHeader;
	}

	public boolean isExitBlock() {
		return !isDummy && walaBBlock.isExitBlock();
	}

	public SSACFG.BasicBlock getWalaBasicBlock() {
		return this.walaBBlock;
	}

	private boolean hasInstruction(SSAInstruction i) {
		return this.walaBBlock.getAllInstructions().contains(i);
	}

	public void print() {
		System.out.println("-------------------------------------------");
		if (isDummy) {
			System.out.println("Dummy " + this.idx);
		} else {
			System.out.println(walaBBlock.toString());
		}
		if (isCondHeader) {
			System.out.println("Conditional header");
		}
		if (isLoopHeader) {
			System.out.println("Loop Header");
		}
		if (isPartOfLoop) {
			System.out.println("Part of Loop");
		}
		if (isExitBlock()) {
			System.out.println("Exit Block");
		}
		if (!isDummy && this.walaBBlock.isEntryBlock()) {
			System.out.println("Entry Block");
		}
		if (!isDummy && this.walaBBlock.isCatchBlock()) {
			System.out.println("Catch Block");
		}
		if (isReturnBlock()) {
			System.out.println("Return Block");
		}

		for (SSAInstruction i : instructions) {
			System.out.println(i.iindex + " -- " + i.toString());
		}
	}

	public CFG getCFG() {
		return this.g;
	}

	public int idx() {
		return this.idx;
	}

	public void emptyPreds() {
		this.preds = new ArrayList<>();
	}

	public void emptySuccs() {
		this.succs = new ArrayList<>();
	}

	public void acceptVisitor(IBBlockVisitor v) {
		if (isDummy) {
			v.visitDummyNode(this);
		} else if (this.walaBBlock.isEntryBlock()) {
			v.visitStartNode(this);
		} else if (isCondHeader || isLoopHeader) {
			v.visitDecisionNode(this);
		} else if (isExitBlock()) {
			v.visitExitNode(this);
		} else {
			v.visitStandardNode(this);
		}
	}

	public Formula getCondExpr() {
		return condExpr;
	}

	public void setCondExpr(Formula condExpr) {
		assert (splitsControlFlow());
		this.condExpr = condExpr;
	}

	public boolean isPartOfLoop() {
		return isPartOfLoop;
	}

	public void setPartOfLoop(boolean partOfLoop) {
		isPartOfLoop = partOfLoop;
	}

	public boolean isDummy() {
		return this.isDummy;
	}

	public boolean isReturnBlock() {
		return !isDummy && instructions.stream().anyMatch(i -> i.toString().contains("return"));
	}

	public int getReplacedPredIdx() {
		return replacedPredIdx;
	}

	public boolean isInScope() {
		return inScope;
	}

	public void setInScope(boolean inScope) {
		this.inScope = inScope;
	}

	public boolean isBreak() {
		return isBreak;
	}

	public void setBreak(boolean aBreak) {
		isBreak = aBreak;
	}

	public SSAReturnInstruction getReturn() {
		assert (this.isReturnBlock());
		// return is last instruction in block
		return (SSAReturnInstruction) this.walaBBlock.getLastInstruction();
	}

	public boolean ownsValue(int valNum) {
		return instructions.stream().filter(SSAInstruction::hasDef).anyMatch(i -> i.getDef() == valNum);
	}

	public int getTrueTarget() {
		assert (this.splitsControlFlow());
		int instructionIdx = ((SSAConditionalBranchInstruction) this.getWalaBasicBlock().getLastInstruction())
				.getTarget();
		int realTarget = this.getCFG().getBlocks().stream()
				.filter(b -> b.getWalaBasicBlock().getFirstInstructionIndex() == instructionIdx).findFirst().get().idx;

		int dummyTarget = BBlock.getBlockForIdx(this.g.getMethod(), realTarget).preds.stream()
				.filter(pred -> this.succs.contains(pred)).findFirst().get().idx;
		return dummyTarget;
	}

	public LoopBody getOwningLoop() {
		assert (this.isPartOfLoop);
		return this.getCFG().getMethod().getLoops().stream().filter(l -> l.hasBlock(this.idx)).max((o1, o2) -> Integer
				.compare(o1.getOwner().getCFG().getLevel(o1.getHead()), o1.getOwner().getCFG().getLevel(o2.getHead())))
				.get();
	}

	public boolean hasRelevantCF() {
		return hasRelevantCF;
	}

	public void setHasRelevantCF(boolean hasRelevantCF) {
		this.hasRelevantCF = hasRelevantCF;
	}

	/*
	public boolean hasDef(int valNum) {
		return this.instructions.stream().filter(i -> i.hasDef()).anyMatch(i -> i.getDef() == valNum);
	}

	 */

	public IFTreeNode getIfTree() {
		return ifTree;
	}

	public void setIfTree(IFTreeNode ifTree) {
		this.ifTree = ifTree;
	}

	@Override public String getLabel() {
		if (isDummy) {
			return "Dummy " + idx;
		}
		StringBuilder sb = new StringBuilder(String.valueOf(this.idx()));
		for (SSAInstruction i : this.instructions) {
			sb.append("\n").append(i.toString());
		}
		return sb.toString();
	}

	@Override public List<DotNode> getSuccs() {
		return this.succs.stream().map(b -> (DotNode) b).collect(Collectors.toList());
	}

	@Override public List<DotNode> getPreds() {
		return this.preds.stream().map(b -> (DotNode) b).collect(Collectors.toList());
	}

	@Override public boolean isExceptionEdge(DotNode succ) {
		assert (succs.contains(succ));
		if (succ.getId() < 0 || this.isDummy) {
			return false;
		}
		return this.g.getWalaCFG().getExceptionalSuccessors(this.walaBBlock).stream().mapToInt(IBasicBlock::getNumber)
				.boxed().collect(Collectors.toList()).contains(succ.getId());
	}

	@Override public int getId() {
		return idx;
	}
}