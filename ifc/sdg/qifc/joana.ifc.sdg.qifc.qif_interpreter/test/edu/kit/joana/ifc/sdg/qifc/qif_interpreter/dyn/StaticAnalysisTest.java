package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.dyn;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StaticAnalysisTest {

	@Test void condExprTest() throws IOException, InterruptedException, InvalidClassFileException {
		Program p = TestUtils.build("IfinLoop");

		SATAnalysis sa = new SATAnalysis(p);
		sa.computeSATDeps();
		p.getEntryMethod().getCFG().getBlocks().stream().filter(BasicBlock::splitsControlFlow)
				.forEach(b -> assertNotNull(b.getCondExpr()));
	}

	@Test void loopHandlingTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("ConstantLoop");

		SATAnalysis sa = new SATAnalysis(p);

		p.getEntryMethod().getCFG().print();

		sa.computeSATDeps();

		for (int i : p.getEntryMethod().getProgramValues().keySet()) {
			System.out.println(i + " " + Arrays.toString(p.getEntryMethod().getDepsForValue(i)));
		}
	}

}