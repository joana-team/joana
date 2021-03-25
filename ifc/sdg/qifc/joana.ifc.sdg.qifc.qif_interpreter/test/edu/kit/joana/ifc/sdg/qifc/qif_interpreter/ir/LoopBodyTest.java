package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.StaticAnalysis;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class LoopBodyTest {

	@Test void implicitFlowTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		Method m = p.getEntryMethod();

		StaticAnalysis sa = new StaticAnalysis(p);
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
}