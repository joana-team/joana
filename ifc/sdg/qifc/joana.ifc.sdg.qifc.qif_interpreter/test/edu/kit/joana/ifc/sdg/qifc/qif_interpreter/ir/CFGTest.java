package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CFGTest {

	@Test public void domTestSimpleIf() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		Optional<BBlock> phiBlock = p.getEntryMethod().getCFG().getBlocks().stream().filter(bb -> bb.getWalaBasicBLock().hasPhi()).findFirst();
		Optional<BBlock> condBlock = p.getEntryMethod().getCFG().getBlocks().stream().filter(BBlock::isCondHeader).findFirst();
		assert(phiBlock.isPresent());
		assert(condBlock.isPresent());

		System.out.println(phiBlock.get().idx());
		System.out.println(condBlock.get().idx());
		BBlock dom = p.getEntryMethod().getCFG().getImmDom(phiBlock.get());
		assertEquals(condBlock.get(), dom);
	}

}