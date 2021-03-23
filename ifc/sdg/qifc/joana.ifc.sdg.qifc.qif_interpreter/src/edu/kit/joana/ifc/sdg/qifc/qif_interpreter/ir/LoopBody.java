package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import org.logicng.formulas.Formula;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class LoopBody {

	private final Map<Integer, Formula[]> in;
	private final Map<Integer, Formula[]> out;
	private final Method owner;
	private final BBlock head;
	private final Set<BBlock> blocks;

	public LoopBody(Method owner, BBlock head) {
		this.owner = owner;
		this.in = new HashMap<>();
		this.out = new HashMap<>();
		this.head = head;
		this.blocks = this.owner.getCFG().getBasicBlocksInLoop(head);
	}

	public Set<BBlock> getBlocks() {
		return blocks;
	}

	public BBlock getHead() {
		return head;
	}

	public void addInDeps(int i, Formula[] deps) {
		this.in.put(i, deps);
	}

	public void addOutDeps(int i, Formula[] deps) {
		this.out.put(i, deps);
	}
}
