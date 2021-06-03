package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CFGUtilTest {

	@Test void topological() throws IOException, InterruptedException {
		Program p = TestUtils.build("Break");
		DotGrapher.exportGraph(p.getEntryMethod().getCFG());

		List<Integer> ordered = CFGUtil
				.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry()).stream()
				.map(BasicBlock::idx).collect(Collectors.toList());
		assertEquals(Arrays.asList(0, 1, 2, -2, -3, 3, -4, 5, -5, 4, 6, 7, 8), ordered);
	}
}