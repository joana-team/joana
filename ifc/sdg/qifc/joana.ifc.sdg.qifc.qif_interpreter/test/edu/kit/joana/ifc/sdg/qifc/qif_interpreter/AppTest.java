package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.Interpreter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.*;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.StaticAnalysis;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Variable;
import org.logicng.io.parsers.ParserException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

	@Test
	public void recognizeCondHead() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		p.getEntryMethod().getCFG().print();

		CFG g = p.getEntryMethod().getCFG();
		assertEquals(1, g.getBlocks().stream().filter(BBlock::isCondHeader).count());
		assertTrue(g.getBlocks().stream().filter(BBlock::isCondHeader).findFirst().get().getWalaBasicBlock()
				.getLastInstruction().toString().contains("conditional branch"));
	}

	@Test
	public void recognizeLoopHead() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		p.getEntryMethod().getCFG().print();

		CFG g = p.getEntryMethod().getCFG();

		assertEquals(1, g.getBlocks().stream().filter(BBlock::isLoopHeader).count());
		assertTrue(g.getBlocks().stream().filter(BBlock::isLoopHeader).findFirst().get().getWalaBasicBlock()
				.getLastInstruction().toString().contains("conditional branch"));
	}

	@Test public void simpleArithmeticTest()
			throws IOException, InterruptedException, ParameterException, OutOfScopeException,
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

	@Test void FullRun()
			throws IOException, InterruptedException, ParameterException, OutOfScopeException, UnexpectedTypeException {
		Program p = TestUtils.build("If");
		// execute
		Interpreter i = new Interpreter(p);
		StaticAnalysis sa = new StaticAnalysis(p);

		sa.computeSATDeps();
		i.execute(Arrays.asList("0"));

		Method entry = p.getEntryMethod();
		Value leaked = entry.getProgramValues().values().stream().filter(Value::isLeaked).findFirst().get();
		int[] params = entry.getIr().getParameterValueNumbers();
		List<Value> hVals = Arrays.stream(params).mapToObj(entry::getValue).filter(Objects::nonNull)
				.collect(Collectors.toList());
		LeakageComputation lc = new LeakageComputation(hVals, leaked, entry);
		lc.compute(null);
	}

	@Test void fullRunLoop()
			throws IOException, UnexpectedTypeException, ParameterException, OutOfScopeException, InterruptedException {
		Program p = TestUtils.build("Loop7");
		// execute
		Interpreter i = new Interpreter(p);
		StaticAnalysis sa = new StaticAnalysis(p);

		Method entry = p.getEntryMethod();
		entry.getCFG().print();
		p.getMethods().forEach(m -> DotGrapher.exportDotGraph(m.getCFG()));

		sa.computeSATDeps();

		i.execute(Arrays.asList("0"));

		Value leaked = entry.getProgramValues().values().stream().filter(Value::isLeaked).findFirst().get();
		int[] params = entry.getIr().getParameterValueNumbers();
		List<Value> hVals = Arrays.stream(params).mapToObj(entry::getValue).filter(Objects::nonNull)
				.collect(Collectors.toList());
		System.out.println("Leaked: " + Arrays.toString(leaked.getDeps()));
		LeakageComputation lc = new LeakageComputation(hVals, leaked, entry);
		lc.compute(null);
	}

	@Test void debug() throws ParserException {
		FormulaFactory ff = new FormulaFactory();
		Variable x = ff.variable("x");
		Formula a = ff.equivalence(x, ff.constant(true));
		Formula b = ff.equivalence(x, ff.constant(false));
		System.out.println(a);
		System.out.println(b);

		String f = "~(~z & ~((y | z) & (~y | ~z)) & ~((x | y | z) & (~x | ~(y | z)))) & ~((x | y | z) & (~x | ~(y | z)))";
		TestUtils.printModels(ff.parse(f), ff);
	}
}