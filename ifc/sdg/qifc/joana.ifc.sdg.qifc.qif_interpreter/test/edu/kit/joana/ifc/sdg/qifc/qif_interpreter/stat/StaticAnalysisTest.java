package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.ISATAnalysisFragment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.LoopBody;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class StaticAnalysisTest {

	@Test void condExprTest() throws IOException, InterruptedException, InvalidClassFileException {
		Program p = TestUtils.build("IfinLoop");

		StaticAnalysis sa = new StaticAnalysis(p);
		sa.computeSATDeps();
		p.getEntryMethod().getCFG().getBlocks().stream().filter(BBlock::splitsControlFlow).forEach(b -> assertNotNull(b.getCondExpr()));
	}

	@Test void loopHandlingTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("ConstantLoop");

		StaticAnalysis sa = new StaticAnalysis(p);


		p.getEntryMethod().getCFG().print();

		sa.computeSATDeps();

		for (int i: p.getEntryMethod().getProgramValues().keySet()) {
			System.out.println(i + " " + Arrays.toString(p.getEntryMethod().getDepsForValue(i)));
		}
		System.out.println("Loops:");
		for (LoopBody l: p.getEntryMethod().getLoops()) {
			l.print();
			System.out.println("Adding Runs");
			l.getRun(3);
			l.printRun(0);
			l.printRun(1);
			l.printRun(2);
			l.printRun(3);
		}


	}

}