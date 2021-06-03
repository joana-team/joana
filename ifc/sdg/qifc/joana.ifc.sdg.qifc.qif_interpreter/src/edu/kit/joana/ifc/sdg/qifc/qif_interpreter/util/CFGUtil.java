package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;

import java.util.*;

public class CFGUtil {

	public static List<BasicBlock> topological(Collection<BasicBlock> blocks, BasicBlock start) {
		return topologicalRec(new ArrayList<>(blocks), start, new ArrayList<>());
	}

	private static List<BasicBlock> topologicalRec(Collection<BasicBlock> blocks, BasicBlock current,
			List<BasicBlock> ordered) {
		if (blocks.isEmpty()) {
			return ordered;
		}
		blocks.remove(current);
		ordered.add(current);
		for (BasicBlock b : current.succs()) {

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
	public static List<BasicBlock> blocksBetween(BasicBlock start, BasicBlock end) {
		return topological(blocksBetweenRec(start, end, new HashSet<>()), start);
	}

	private static Set<BasicBlock> blocksBetweenRec(BasicBlock current, BasicBlock end, Set<BasicBlock> bridge) {
		bridge.add(current);
		if (current.equals(end)) {
			return bridge;
		} else {
			for (BasicBlock succ : current.succs()) {
				bridge = blocksBetweenRec(succ, end, bridge);
			}
		}
		return bridge;
	}

	/**
	 * returns a list of blocks that belong to a branch in an if-statement
	 * <p>
	 * invariant: all nodes that belong to the branch are dominated by the first block in the branch. We recursively (DFS) add successors, until we find one that is not dominated by {@code firstBlock}
	 *
	 * @param head       block containing the if-condition
	 * @param firstBlock successor of head that belongs to the branch we want to compute
	 * @return list of all basic blocks that belong to the branch
	 */
	public static List<BasicBlock> computeConditionalBranch(BasicBlock head, BasicBlock firstBlock) {
		return computeConditionalBranch(head, firstBlock, firstBlock, new ArrayList<>(Arrays.asList(firstBlock)));
	}

	private static List<BasicBlock> computeConditionalBranch(BasicBlock head, BasicBlock firstBlock, BasicBlock current,
			List<BasicBlock> branch) {
		for (BasicBlock succ : current.succs()) {
			if ((succ.isDummy() || head.getCFG().isDominatedBy(succ, firstBlock)) && !branch.contains(succ)) {
				branch.add(succ);
				branch = computeConditionalBranch(head, firstBlock, succ, branch);
			}
		}
		return branch;
	}
}