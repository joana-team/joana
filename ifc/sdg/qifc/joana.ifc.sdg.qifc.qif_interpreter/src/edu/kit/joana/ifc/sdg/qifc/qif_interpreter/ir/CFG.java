package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.SSACFG;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Dominators;

import java.util.Set;

/**
 * wrapper class for Wala CFG w/ some utility functions
 */
public class CFG {

	private final Method m;
	private final SSACFG walaCFG;
	private Set<BBlock> blocks;
	private final BBlock entry;
	private Dominators<BBlock> doms;

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
		cfg.doms = new Dominators<>(cfg.entry, BBlock::succs);
		for(BBlock bb: cfg.blocks) {
			if (cfg.doms.isPartOfLoop(bb)) {
				bb.setPartOfLoop(true);
				if(bb.equals(cfg.doms.loopHeader(bb))) {
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
}
