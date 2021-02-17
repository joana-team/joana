package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Util;
import org.logicng.formulas.Formula;

import java.util.*;
import java.util.stream.Collectors;

public class BBlock {

	private static final BiMap<SSACFG.BasicBlock, BBlock> repMap = HashBiMap.create();
	private static final Map<Integer, BBlock> dummies = new HashMap<>();
	private static int dummyCtr = -2;

	private final SSACFG.BasicBlock walaBBlock;
	private final CFG g;
	private List<BBlock> succs;
	private List<BBlock> preds;
	private final List<SSAInstruction> instructions;
	private boolean isLoopHeader = false;
	private boolean isPartOfLoop = false;
	private boolean isCondHeader = false;
	private final List<Pair<Integer, Boolean>> implicitFlows;
	private Formula condExpr;
	private final boolean isDummy;
	private final int idx;

	public BBlock(SSACFG.BasicBlock walaBBlock, CFG g) {
		this.walaBBlock = walaBBlock;
		this.g = g;
		this.instructions = walaBBlock.getAllInstructions().stream().filter(i -> !(i == null)).collect(Collectors.toList());
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		this.implicitFlows = new ArrayList<>();
		this.isDummy = false;
		this.idx = walaBBlock.getNumber();
		repMap.put(walaBBlock, this);
	}


	private BBlock(CFG g, int idx) {
		this.isDummy = true;
		this.walaBBlock = null;
		this.instructions = new ArrayList<>();
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		this.implicitFlows = new ArrayList<>();
		this.idx = idx;
		this.g = g;
		dummies.put(this.idx, this);
	}

	public void findSuccessorsRec(CFG g) {
		SSACFG walaCFG = g.getWalaCFG();
		List<ISSABasicBlock> succs = Util.asList(walaCFG.getSuccNodes(walaBBlock));
		for(ISSABasicBlock ibb: succs) {
			SSACFG.BasicBlock bb = (SSACFG.BasicBlock) ibb;

			if (repMap.containsKey(bb)) {
				addEdge(this, repMap.get(bb));
			} else {
				BBlock newSucc = new BBlock(bb, this.g);
				g.addNode(newSucc);
				addEdge(this, newSucc);
				newSucc.findSuccessorsRec(g);
			}
		}
	}

	public static BBlock createDummy(CFG g) {
		return new BBlock(g, dummyCtr--);
	}

	private static void addEdge(BBlock from, BBlock to) {
		from.succs.add(to);
		to.preds.add(from);
	}

	public List<SSAInstruction> instructions() {
		return instructions;
	}

	public List<BBlock> succs() {
		return succs;
	}

	public List<BBlock> preds() {
		return preds;
	}

	public void setLoopHeader(boolean loopHeader) {
		isLoopHeader = loopHeader;
	}

	public boolean isLoopHeader() {
		return isLoopHeader;
	}

	public void setPartOfLoop(boolean partOfLoop) {
		isPartOfLoop = partOfLoop;
	}

	public boolean isCondHeader() {
		return isCondHeader;
	}

	public boolean splitsControlFlow() {
		return isCondHeader || isLoopHeader;
	}

	public void setCondHeader(boolean condHeader) {
		isCondHeader = condHeader;
	}

	public boolean isExitBlock() {
		return walaBBlock.isExitBlock();
	}

	public SSACFG.BasicBlock getWalaBasicBLock() {
		return repMap.inverse().get(this);
	}

	public static BBlock bBlock(SSACFG.BasicBlock walaBBlock) {
		return repMap.get(walaBBlock);
	}

	public static BBlock getBBlockForInstruction(SSAInstruction i, CFG g) {
		SSACFG.BasicBlock walaBlock = g.getWalaCFG().getBlockForInstruction(i.iindex);
		return repMap.values().stream().filter(b -> b.hasInstruction(i)).findFirst().get();
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
		for(SSAInstruction i: instructions) {
			System.out.println(i.iindex + " -- " + i.toString());
		}
	}

	public CFG getCFG() {
		return this.g;
	}

	public int idx() {
		return this.idx;
	}

	public static BBlock getBlockForIdx(int idx) {
		if (idx < -1) {
			return dummies.get(idx);
		}
		return repMap.get(repMap.keySet().stream().filter(b -> b.getNumber() == idx).findFirst().get());
	}

	public void emptyPreds() {
		this.preds = new ArrayList<>();
	}

	public void emptySuccs() {
		this.succs = new ArrayList<>();
	}

	public void acceptVisitor(IBBlockVisitor v) {
		if (this.walaBBlock.isEntryBlock()) {
			v.visitStartNode(this);
		} else if (isCondHeader || isLoopHeader) {
			v.visitDecisionNode(this);
		} else {
			v.visitStandardNode(this);
		}
	}

	public void addImplicitFlow(int blockIdx, boolean condition) {
		this.implicitFlows.add(Pair.make(blockIdx, condition));
	}

	public void copyImplicitFlowsFrom(int blockIdx) {
		this.implicitFlows.addAll(BBlock.getBlockForIdx(blockIdx).getImplicitFlows());
	}

	public List<Pair<Integer, Boolean>> getImplicitFlows() {
		return this.implicitFlows;
	}

	public Formula getCondExpr() {
		return condExpr;
	}

	public void setCondExpr(Formula condExpr) {
		assert(splitsControlFlow());
		this.condExpr = condExpr;
	}

	public boolean isPartOfLoop() {
		return isPartOfLoop;
	}
}
