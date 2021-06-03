package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.ssa.ISSABasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CFGTest {

	@Test public void predOrderTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("IfinIf");
		CFG g = p.getEntryMethod().getCFG();
		List<BasicBlock> blocks = p.getEntryMethod().getCFG().getBlocks();

		for (BasicBlock b : blocks) {
			if (b.isDummy())
				continue;
			assertEquals(b.preds().size(),
					p.getEntryMethod().getCFG().getWalaCFG().getPredNodeCount(b.getWalaBasicBlock()));
			b.succs().forEach(succ -> assertTrue(succ.preds().contains(b)));
			List<ISSABasicBlock> correctOrder = Util
					.asList(p.getEntryMethod().getCFG().getWalaCFG().getPredNodes(b.getWalaBasicBlock()));

			for (int i = 0; i < b.preds().size(); i++) {
				int correctIdx = correctOrder.get(i).getNumber();
				int idx = (b.preds().get(i).isDummy()) ? b.preds().get(i).getReplacedPredIdx() : b.preds().get(i).idx();
				assertEquals(correctIdx, idx);
			}

		}
	}

	@Test public void predOrderTest2() throws IOException, InterruptedException {
		Program p = TestUtils.build("IfinLoop");
		CFG g = p.getEntryMethod().getCFG();
		List<BasicBlock> blocks = p.getEntryMethod().getCFG().getBlocks();

		for (BasicBlock b : blocks) {
			if (b.isDummy())
				continue;
			assertEquals(b.preds().size(),
					p.getEntryMethod().getCFG().getWalaCFG().getPredNodeCount(b.getWalaBasicBlock()));
			b.succs().forEach(succ -> assertTrue(succ.preds().contains(b)));
			List<ISSABasicBlock> correctOrder = Util
					.asList(p.getEntryMethod().getCFG().getWalaCFG().getPredNodes(b.getWalaBasicBlock()));

			for (int i = 0; i < b.preds().size(); i++) {
				int correctIdx = correctOrder.get(i).getNumber();
				int idx = (b.preds().get(i).isDummy()) ? b.preds().get(i).getReplacedPredIdx() : b.preds().get(i).idx();
				assertEquals(correctIdx, idx);
			}

		}
	}

	@Test public void domTestSimpleIf() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		CFG g = p.getEntryMethod().getCFG();
		Optional<BasicBlock> phiBlock = p.getEntryMethod().getCFG().getBlocks().stream()
				.filter(bb -> bb.getWalaBasicBlock().hasPhi()).findFirst();
		Optional<BasicBlock> condBlock = p.getEntryMethod().getCFG().getBlocks().stream()
				.filter(BasicBlock::isCondHeader).findFirst();
		assert (phiBlock.isPresent());
		assert (condBlock.isPresent());

		System.out.println(phiBlock.get().idx());
		System.out.println(condBlock.get().idx());
		BasicBlock dom = p.getEntryMethod().getCFG().getImmDom(phiBlock.get());
		assertEquals(condBlock.get(), dom);
	}

	@Test public void startImmDom() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		BasicBlock start = p.getEntryMethod().getCFG().entry();
		BasicBlock dom = p.getEntryMethod().getCFG().getImmDom(start);
		assertNull(dom);
	}

	@Test public void bbTestIf() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		int walaBBNum = p.getEntryMethod().getCFG().getWalaCFG().getNumberOfNodes();
		int ownBBNum = p.getEntryMethod().getCFG().getNumberOfNodes();

		p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::isDummy)
				.forEach(b -> assertEquals(1, b.preds().size()));

		assertEquals(walaBBNum + 2, ownBBNum);
	}

	@Test public void bbTestNoCF() throws IOException, InterruptedException {
		Program p = TestUtils.build("SimpleArithmetic");
		int walaBBNum = p.getEntryMethod().getCFG().getWalaCFG().getNumberOfNodes();
		int ownBBNum = p.getEntryMethod().getCFG().getNumberOfNodes();

		p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::isDummy)
				.forEach(b -> assertEquals(1, b.preds().size()));

		assertEquals(walaBBNum, ownBBNum);
	}

	@Test public void bbTestLoop() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		int walaBBNum = p.getEntryMethod().getCFG().getWalaCFG().getNumberOfNodes();
		int ownBBNum = p.getEntryMethod().getCFG().getNumberOfNodes();

		p.getEntryMethod().getCFG().print();

		assertEquals(walaBBNum + 2, ownBBNum);
		p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::isDummy)
				.forEach(b -> assertEquals(1, b.preds().size()));
	}

	@Test public void loopBlocksTest1() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		BasicBlock header = p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::isLoopHeader)
				.findFirst().get();

		Set<BasicBlock> inLoop = p.getEntryMethod().getCFG().getBasicBlocksInLoop(header);
		Set<Integer> inLoopIdx = new HashSet<>();
		inLoop.stream().forEach(b -> inLoopIdx.add(b.idx()));

		assertEquals(3, inLoop.size());

		Set<Integer> expected = new HashSet<>(Arrays.asList(2, 3, -3));
		assertEquals(expected, inLoopIdx);
	}

	@Test public void loopBlocksTest2() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop2");
		BasicBlock header = p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::isLoopHeader)
				.findFirst().get();

		p.getEntryMethod().getCFG().print();

		Set<BasicBlock> inLoop = p.getEntryMethod().getCFG().getBasicBlocksInLoop(header);
		Set<Integer> inLoopIdx = new HashSet<>();
		inLoop.stream().forEach(b -> inLoopIdx.add(b.idx()));

		assertEquals(3, inLoop.size());

		Set<Integer> expected = new HashSet<>(Arrays.asList(2, 3, -3));
		assertEquals(expected, inLoopIdx);
	}

	@Test public void loopBlocksTest3() throws IOException, InterruptedException {
		Program p = TestUtils.build("IfinLoop");
		BasicBlock header = p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::isLoopHeader)
				.findFirst().get();

		p.getEntryMethod().getCFG().print();

		Set<BasicBlock> inLoop = p.getEntryMethod().getCFG().getBasicBlocksInLoop(header);
		Set<Integer> inLoopIdx = new HashSet<>();
		inLoop.stream().forEach(b -> inLoopIdx.add(b.idx()));

		assertEquals(8, inLoop.size());

		Set<Integer> expected = new HashSet<>(Arrays.asList(2, -3, 3, -5, -4, 4, 5, 6));
		assertEquals(expected, inLoopIdx);
	}

	@Test public void loopBlocksTest4() throws IOException, InterruptedException {
		Program p = TestUtils.build("LoopinLoop");
		BasicBlock header1 = p.getEntryMethod().getCFG().getBlocks().stream().filter(b -> b.idx() == 2).findFirst()
				.get();

		p.getEntryMethod().getCFG().print();

		Set<BasicBlock> inLoop = p.getEntryMethod().getCFG().getBasicBlocksInLoop(header1);
		Set<Integer> inLoopIdx = new HashSet<>();
		inLoop.stream().forEach(b -> inLoopIdx.add(b.idx()));

		assertEquals(7, inLoop.size());

		Set<Integer> expected = new HashSet<>(Arrays.asList(2, -3, 3, -5, -4, 4, 5));
		assertEquals(expected, inLoopIdx);

		BasicBlock header2 = p.getEntryMethod().getCFG().getBlocks().stream().filter(b -> b.idx() == 3).findFirst()
				.get();

		Set<BasicBlock> inLoop2 = p.getEntryMethod().getCFG().getBasicBlocksInLoop(header2);
		Set<Integer> inLoopIdx2 = new HashSet<>();
		inLoop2.stream().forEach(b -> inLoopIdx2.add(b.idx()));

		assertEquals(3, inLoop2.size());

		Set<Integer> expected2 = new HashSet<>(Arrays.asList(3, -5, 4));
		assertEquals(expected2, inLoopIdx2);
	}
}