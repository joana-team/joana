package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.CFG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

	@Test
	public void recognizeCondHead() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		p.getEntryMethod().getCFG().print();

		CFG g = p.getEntryMethod().getCFG();
		assertEquals(1, g.getBlocks().stream().filter(BBlock::isCondHeader).count());
		assertTrue(g.getBlocks().stream().filter(BBlock::isCondHeader).findFirst().get().getWalaBasicBLock().getLastInstruction().toString().contains("conditional branch"));
	}

	@Test
	public void recognizeLoopHead() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		p.getEntryMethod().getCFG().print();

		CFG g = p.getEntryMethod().getCFG();
		assertEquals(1, g.getBlocks().stream().filter(BBlock::isLoopHeader).count());
		assertTrue(g.getBlocks().stream().filter(BBlock::isLoopHeader).findFirst().get().getWalaBasicBLock().getLastInstruction().toString().contains("conditional branch"));
	}


}