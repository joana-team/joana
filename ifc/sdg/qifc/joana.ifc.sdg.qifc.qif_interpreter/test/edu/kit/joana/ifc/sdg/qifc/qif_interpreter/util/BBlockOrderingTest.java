package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BBlockOrderingTest {

	@Test void topological() throws IOException, InterruptedException {
		Program p = TestUtils.build("Break");
		DotGrapher.exportDotGraph(p.getEntryMethod().getCFG());

		List<Integer> ordered = BBlockOrdering
				.topological(p.getEntryMethod().getCFG().getBlocks(), p.getEntryMethod().getCFG().entry()).stream()
				.map(BBlock::idx).collect(Collectors.toList());
		assertEquals(Arrays.asList(0, 1, 2, -2, -3, 3, -4, 5, -5, 4, 6, 7, 8), ordered);
	}
}