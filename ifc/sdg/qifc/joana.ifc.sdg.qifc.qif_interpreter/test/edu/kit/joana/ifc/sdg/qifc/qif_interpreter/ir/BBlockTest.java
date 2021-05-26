package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BBlockTest {

	@Test void getOwningLoopTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("LoopinLoop3");
		Method m = p.getEntryMethod();
		DotGrapher.exportDotGraph(m.getCFG());

		testOwningLoop(m, 3, 2);
		testOwningLoop(m, 2, 2);
		testOwningLoop(m, -3, 2);
		testOwningLoop(m, -4, 2);
		testOwningLoop(m, 6, 2);
		testOwningLoop(m, 4, 4);
		testOwningLoop(m, -5, 4);
		testOwningLoop(m, 5, 4);
	}

	void testOwningLoop(Method m, int blockIdx, int loopHeadIdx) {
		BBlock inOuterLoop = BBlock.getBlockForIdx(m, blockIdx);
		LoopBody l = inOuterLoop.getOwningLoop();
		assertEquals(loopHeadIdx, l.getHead().idx());
	}

}