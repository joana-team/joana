package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotNode;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
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
	/**
	 * describes which blocks' conditional jumps influence if this block will be executed or not.
	 * The first element of the pair refers to the block indices, the second element states whether
	 * the conditional jump condition needs to be true or false for this block to be executed
	 */
	private final List<Pair<Integer, Boolean>> implicitFlows;
	private final int idx;
	private final boolean isDummy;
	private List<BBlock> succs;
	private List<BBlock> preds;
	private boolean isLoopHeader = false;
	private boolean isPartOfLoop = false;
	private boolean isCondHeader = false;
	private boolean inScope = true;
	/**
	 * expression used for conditional jump to successor expressed as SAT formula.
	 */
	private Formula condExpr;
	private int replacedPredIdx;

	public BBlock(SSACFG.BasicBlock walaBBlock, CFG g) {
		this.walaBBlock = walaBBlock;
		this.g = g;
		this.instructions = walaBBlock.getAllInstructions().stream().filter(i -> !(i == null))
				.collect(Collectors.toList());
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		this.implicitFlows = new ArrayList<>();
		this.isDummy = false;
		this.idx = walaBBlock.getNumber();
		g.addRep(walaBBlock, this);
	}

	private BBlock(CFG g, int idx, int replacedPredIdx) {
		this.isDummy = true;
		this.replacedPredIdx = replacedPredIdx;
		this.walaBBlock = null;
		this.instructions = new ArrayList<>();
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		this.implicitFlows = new ArrayList<>();
		this.idx = idx;
		this.g = g;
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
		Formula iff = LogicUtil.ff.constant(true);
		for (Pair<Integer, Boolean> p : implicitFlows) {
			Formula x = this.g.getBlock(p.fst).condExpr;
			iff = LogicUtil.ff.and(iff, (p.snd) ? x : LogicUtil.ff.not(x));
		}
		return iff;
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

	public void addImplicitFlow(int blockIdx, boolean condition) {
		this.implicitFlows.add(Pair.make(blockIdx, condition));
	}

	public void copyImplicitFlowsFrom(Method m, int blockIdx) {
		this.implicitFlows.addAll(BBlock.getBlockForIdx(m, blockIdx).getImplicitFlows());
	}

	public List<Pair<Integer, Boolean>> getImplicitFlows() {
		return this.implicitFlows;
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

	public SSAReturnInstruction getReturn() {
		assert(this.isReturnBlock());
		// return is last instruction in block
		return (SSAReturnInstruction) this.walaBBlock.getLastInstruction();
	}

	@Override public String getLabel() {
		if (isDummy) {
			return "Dummy";
		}
		StringBuilder sb = new StringBuilder(String.valueOf(this.idx()));
		for (SSAInstruction i: this.instructions) {
			sb.append("\n").append(i.toString());
		}
		return sb.toString();
	}

	@Override public List<DotNode> getSuccs() {
		return this.succs.stream().map(b -> (DotNode)b).collect(Collectors.toList());
	}

	@Override public List<DotNode> getPreds() {
		return this.preds.stream().map(b -> (DotNode)b).collect(Collectors.toList());
	}

	@Override public int getId() {
		return idx;
	}
}
