package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BBlockOrdering {

	public static List<BBlock> topological(Collection<BBlock> blocks, BBlock start) {
		return topologicalRec(new ArrayList<>(blocks), start, new ArrayList<>(Collections.singletonList(start)));
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
}