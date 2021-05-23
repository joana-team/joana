package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BBlockOrdering {

	public static List<BBlock> topological(Collection<BBlock> blocks, BBlock start) {
		return topologicalRec(blocks, start, new ArrayList<>(Collections.singletonList(start)));
	}

	private static List<BBlock> topologicalRec(Collection<BBlock> blocks, BBlock current, List<BBlock> ordered) {
		if (blocks.isEmpty()) {
			return ordered;
		}

		blocks.remove(current);
		ordered.add(current);
		for (BBlock b : current.succs()) {
			List<BBlock> finalOrdered = ordered;
			if (b.preds().stream().allMatch(pred -> finalOrdered.contains(pred)) || b.isLoopHeader()) {
				ordered = topologicalRec(blocks, b, ordered);
			}
		}
		return ordered;
	}
}