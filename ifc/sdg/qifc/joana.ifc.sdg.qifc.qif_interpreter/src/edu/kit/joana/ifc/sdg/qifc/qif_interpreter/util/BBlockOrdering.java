package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;

import java.util.*;

public class BBlockOrdering {

	public static List<BBlock> topological(Collection<BBlock> blocks, BBlock start) {
		return topologicalRec(new ArrayList<>(blocks), start, new ArrayList<>());
	}

	private static List<BBlock> topologicalRec(Collection<BBlock> blocks, BBlock current, List<BBlock> ordered) {
		if (blocks.isEmpty()) {
			return ordered;
		}
		blocks.remove(current);
		ordered.add(current);
		for (BBlock b : current.succs()) {

			if ((ordered.containsAll(b.preds()) || b.isLoopHeader()) && !ordered.contains(b) && blocks.contains(b)) {
				ordered = topologicalRec(blocks, b, ordered);
			}
		}
		return ordered;
	}

	/**
	 * returns a list of all blocks that lay on a path between {@code start} and {@code end} in topological order
	 * <p>
	 * Pre-condition: {@code end} post-dominates {@code start}
	 * list includes {@code start} and {@code end}
	 */
	public static List<BBlock> blocksBetween(BBlock start, BBlock end) {
		return topological(blocksBetweenRec(start, end, new HashSet<>()), start);
	}

	private static Set<BBlock> blocksBetweenRec(BBlock current, BBlock end, Set<BBlock> bridge) {
		bridge.add(current);
		if (current.equals(end)) {
			return bridge;
		} else {
			for (BBlock succ : current.succs()) {
				bridge = blocksBetweenRec(succ, end, bridge);
			}
		}
		return bridge;
	}
}