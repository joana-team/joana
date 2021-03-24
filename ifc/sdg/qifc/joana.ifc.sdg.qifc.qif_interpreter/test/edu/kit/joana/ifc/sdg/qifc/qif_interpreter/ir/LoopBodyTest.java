package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.StaticAnalysis;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class LoopBodyTest {

	@Test void implicitFlowTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("ConstantLoop");
		Method m = p.getEntryMethod();

		StaticAnalysis sa = new StaticAnalysis(p);
		sa.computeSATDeps(m);

		LoopBody l = m.getLoops().get(0);

		for (int i = 0; i <= 3; i++) {
			System.out.println(i + " " + l.getImplicitFlowForIter(i));
		}
	}

}