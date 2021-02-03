package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BBlock {

	private static final BiMap<SSACFG.BasicBlock, BBlock> repMap = HashBiMap.create();
	private final SSACFG.BasicBlock walaBBlock;
	private final CFG g;
	private final List<BBlock> succs;
	private final List<BBlock> preds;
	private final List<SSAInstruction> instructions;
	private boolean isLoopHeader = false;
	private boolean isPartOfLoop = false;
	private boolean isCondHeader = false;

	public BBlock(SSACFG.BasicBlock walaBBlock, CFG g) {
		this.walaBBlock = walaBBlock;
		this.g = g;
		this.instructions = walaBBlock.getAllInstructions().stream().filter(i -> !(i == null)).collect(Collectors.toList());
		this.preds = new ArrayList<>();
		this.succs = new ArrayList<>();
		repMap.put(walaBBlock, this);
	}

	public void findSuccessorsRec(SSACFG walaCFG) {
		List<ISSABasicBlock> succs = Util.asList(walaCFG.getSuccNodes(walaBBlock));
		for(ISSABasicBlock ibb: succs) {
			SSACFG.BasicBlock bb = (SSACFG.BasicBlock) ibb;

			if (repMap.containsKey(bb)) {
				addEdge(this, repMap.get(bb));
			} else {
				BBlock newSucc = new BBlock(bb, this.g);
				addEdge(this, newSucc);
				newSucc.findSuccessorsRec(walaCFG);
			}
		}
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

	public static Set<BBlock> allBlocks() {
		return repMap.values();
	}

	public void setLoopHeader(boolean loopHeader) {
		isLoopHeader = loopHeader;
	}

	public boolean isLoopHeader() {
		return isLoopHeader;
	}

	public boolean isPartOfLoop() {
		return isPartOfLoop;
	}

	public void setPartOfLoop(boolean partOfLoop) {
		isPartOfLoop = partOfLoop;
	}

	public boolean isCondHeader() {
		return isCondHeader;
	}

	public void setCondHeader(boolean condHeader) {
		isCondHeader = condHeader;
	}

	public SSACFG.BasicBlock getWalaBasicBLock() {
		return repMap.inverse().get(this);
	}

	public void print() {
		System.out.println("-------------------------------------------");
		System.out.println(walaBBlock.toString());
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
			System.out.println(i.toString());
		}
	}
}
