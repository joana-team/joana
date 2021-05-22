package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {

	private static final Map<Pair<String, Integer>, Pair<Set<Integer>, Set<Integer>>> branches;

	static {
		branches = new HashMap<>();
		//branches.put(Pair.make("If", 1), Pair.make(Util.newHashSet(-2), Util.newHashSet(-3, 2)));
		//branches.put(Pair.make("ArrayInIf", 2), Pair.make(Util.newHashSet(-2, 5), Util.newHashSet(-3, 3, 4)));
		//branches.put(Pair.make("IfinIf", 2), Pair.make(Util.newHashSet(-4, 4), Util.newHashSet(-5, 3)));
		branches.put(Pair.make("IfinIf", 1), Pair.make(Util.newHashSet(-2), Util.newHashSet(-3, 2, -4, 4, 3, -5, 7)));

	}

	@Test void computeConditionalBranch() throws IOException, InterruptedException {
		for (Pair<String, Integer> condStmt : branches.keySet()) {
			computeConditionalBranchTestcase(condStmt.fst, condStmt.snd);
		}
	}

	void computeConditionalBranchTestcase(String testcase, int condHeaderIdx) throws IOException, InterruptedException {
		Program p = TestUtils.build(testcase);
		Method m = p.getEntryMethod();
		DotGrapher.exportDotGraph(m.getCFG());

		BBlock condHeader = m.getCFG().getBlock(condHeaderIdx);
		int trueTargetIdx = condHeader.getTrueTarget();
		System.out.println("True target idx: " + trueTargetIdx);
		BBlock trueTarget = condHeader.succs().stream().filter(b -> b.idx() == trueTargetIdx).findAny().get();
		BBlock falseTarget = condHeader.succs().stream().filter(b -> b.idx() != trueTargetIdx).findAny().get();

		Converter c = new Converter();
		List<BBlock> left = c.computeConditionalBranch(condHeader, trueTarget, trueTarget,
				new ArrayList<>(Arrays.asList(trueTarget)));
		List<BBlock> right = c.computeConditionalBranch(condHeader, falseTarget, falseTarget,
				new ArrayList<>(Arrays.asList(falseTarget)));

		Pair<Set<Integer>, Set<Integer>> expected = branches.get(Pair.make(testcase, condHeader.idx()));
		HashSet<Integer> trueSet = left.stream().map(BBlock::idx).collect(Collectors.toCollection(HashSet::new));
		HashSet<Integer> falseSet = right.stream().map(BBlock::idx).collect(Collectors.toCollection(HashSet::new));

		assertEquals(expected.fst.size(), left.size());
		assertEquals(expected.snd.size(), right.size());
		assertEquals(expected.fst, trueSet);
		assertEquals(expected.snd, falseSet);
	}
}