package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn.SATAnalysis;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class LoopBodyTest {

	@Test void implicitFlowTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		Method m = p.getEntryMethod();

		SATAnalysis sa = new SATAnalysis(p);
		sa.computeSATDeps(m);

		LoopBody l = m.getLoops().get(0);

		List<Integer> loopPhis = m.getPhiValPossibilities().keySet().stream().filter(i -> l.getIn().containsKey(i))
				.collect(Collectors.toList());
		for (int phi : loopPhis) {
			System.out.println(phi);
			m.getPhiValPossibilities().get(phi)
					.forEach(poss -> System.out.println(Arrays.toString(poss.fst) + " " + poss.snd));
		}
	}

	@Test void breaksTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("Break");

		SATAnalysis sa = new SATAnalysis(p);
		sa.computeSATDeps();

		LoopBody l = p.getEntryMethod().getLoops().get(0);
		Assertions.assertEquals(1, l.getBreaks().size());
	}

	@Test void breaksTest2() throws IOException, InterruptedException {
		Program p = TestUtils.build("MultipleBreaks");

		SATAnalysis sa = new SATAnalysis(p);
		sa.computeSATDeps();

		LoopBody l = p.getEntryMethod().getLoops().get(0);
		Assertions.assertEquals(2, l.getBreaks().size());
	}

	@Test void breaksTest3() throws IOException, InterruptedException {
		Program p = TestUtils.build("Breaks2");

		SATAnalysis sa = new SATAnalysis(p);
		sa.computeSATDeps();

		LoopBody l = p.getEntryMethod().getLoops().get(1);
		Assertions.assertEquals(2, l.getBreaks().size());
	}

	@Test void breakToPostLoop() throws IOException, InterruptedException {
		Program p = TestUtils.build("Break");
		DotGrapher.exportGraph(p.getEntryMethod().getCFG());

		LoopBody l = p.getEntryMethod().getLoops().get(0);
		BasicBlock breakBlock = l.getBreaks().get(0);
		List<Integer> result = l.breakToPostLoop(breakBlock).stream().map(b -> b.idx()).collect(Collectors.toList());
		Assertions.assertEquals(Arrays.asList(-5, 4), result);
	}

	@Test void breakToPostLoop2() throws IOException, InterruptedException {
		Program p = TestUtils.build("Break3");
		DotGrapher.exportGraph(p.getEntryMethod().getCFG());

		LoopBody l = p.getEntryMethod().getLoops().get(0);
		BasicBlock breakBlock = l.getBreaks().get(0);
		List<Integer> result = l.breakToPostLoop(breakBlock).stream().map(b -> b.idx()).collect(Collectors.toList());
		Assertions.assertEquals(Arrays.asList(-5, 4, -6, -7, 5, 9), result);
	}

	@Test void breakToPostLoop3() throws IOException, InterruptedException {
		Program p = TestUtils.build("Break4");
		DotGrapher.exportGraph(p.getEntryMethod().getCFG());

		LoopBody l = p.getEntryMethod().getLoops().get(0);
		BasicBlock breakBlock = l.getBreaks().get(0);
		List<Integer> result = l.breakToPostLoop(breakBlock).stream().map(b -> b.idx()).collect(Collectors.toList());
		Assertions.assertEquals(Arrays.asList(-5, 4, -6, 6, -7, 5, 10), result);
	}
}