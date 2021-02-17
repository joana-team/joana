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

	@Test public void startImmDom() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		BBlock start = p.getEntryMethod().getCFG().entry();
		BBlock dom = p.getEntryMethod().getCFG().getImmDom(start);
		assertNull(dom);
	}

	@Test public void bbTestIf() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		int walaBBNum = p.getEntryMethod().getCFG().getWalaCFG().getNumberOfNodes();
		int ownBBNum = p.getEntryMethod().getCFG().getNumberOfNodes();

		p.getEntryMethod().getCFG().getBlocks().stream().filter(BBlock::isDummy).forEach(b -> assertEquals(1, b.preds().size()));

		assertEquals(walaBBNum + 2, ownBBNum);
	}

	@Test public void bbTestNoCF() throws IOException, InterruptedException {
		Program p = TestUtils.build("SimpleArithmetic");
		int walaBBNum = p.getEntryMethod().getCFG().getWalaCFG().getNumberOfNodes();
		int ownBBNum = p.getEntryMethod().getCFG().getNumberOfNodes();

		p.getEntryMethod().getCFG().getBlocks().stream().filter(BBlock::isDummy).forEach(b -> assertEquals(1, b.preds().size()));

		assertEquals(walaBBNum, ownBBNum);
	}

	@Test public void bbTestLoop() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		int walaBBNum = p.getEntryMethod().getCFG().getWalaCFG().getNumberOfNodes();
		int ownBBNum = p.getEntryMethod().getCFG().getNumberOfNodes();

		p.getEntryMethod().getCFG().print();

		assertEquals(walaBBNum + 2, ownBBNum);
		p.getEntryMethod().getCFG().getBlocks().stream().filter(BBlock::isDummy).forEach(b -> assertEquals(1, b.preds().size()));
	}
}