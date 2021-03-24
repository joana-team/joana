package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.StaticAnalysis;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodTest {

	@Test void isComputedInLoopTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("WhileAfterIf");
		Method m = p.getEntryMethod();

		StaticAnalysis sa = new StaticAnalysis(p);
		sa.computeSATDeps(m);

		m.getCFG().print();
		m.getProgramValues().keySet().stream().filter(i -> i != 8).forEach(i -> assertFalse(m.isComputedInLoop(i)));
		assertTrue(m.isComputedInLoop(8));
	}
}