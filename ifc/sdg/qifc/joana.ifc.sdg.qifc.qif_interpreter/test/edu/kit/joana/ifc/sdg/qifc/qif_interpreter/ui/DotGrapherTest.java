package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class DotGrapherTest {

	@Test
	public void dotGraphTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		DotGrapher.exportDotGraph(p.getEntryMethod().getCFG());
	}

}