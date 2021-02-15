package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.Interpreter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.CFG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.MissingValueException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.StaticAnalysis;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

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

	@Test public void simpleArithmeticTest()
			throws IOException, InterruptedException, ParameterException, OutOfScopeException, MissingValueException,
			InvalidClassFileException {

		Program p = TestUtils.build("SimpleArithmetic");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		Interpreter i = new Interpreter(p, ps);
		StaticAnalysis sa = new StaticAnalysis(p);
		List<String> args = Arrays.asList("1", "2");

		i.execute(args);
		sa.computeSATDeps();
		assertEquals("4\n3\n1\n", baos.toString());

	}


}