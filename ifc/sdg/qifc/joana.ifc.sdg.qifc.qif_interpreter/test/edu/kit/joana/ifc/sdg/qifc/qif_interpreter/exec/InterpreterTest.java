package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.OutOfScopeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ParameterException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InterpreterTest {

	@Test public void arithmeticTest() throws IOException, InterruptedException, ParameterException,
			OutOfScopeException {

		Program p = TestUtils.build("SimpleArithmetic");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		Interpreter i = new Interpreter(p, ps);

		List<String> args = Arrays.asList("1", "0");

		i.execute(args);
		assertEquals("2\n1\n1\n", baos.toString());

	}

	@Test public void applyArgsTest() throws IOException, InterruptedException, ParameterException {

		Program p = TestUtils.build("OnlyArgs");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		Interpreter i = new Interpreter(p, ps);

		List<String> args = Arrays.asList("1", "0");

		i.applyArgs(args, p, p.getEntryMethod());
		Map<Integer, Value> argValues = p.getProgramValues();

		assertEquals(2, argValues.size());
		assertEquals(1, argValues.get(2).getVal());
		assertEquals(0, argValues.get(3).getVal());
	}

	@Test public void simplePhiTestFalse() throws IOException, InterruptedException, ParameterException,
			OutOfScopeException {

		Program p = TestUtils.build("If2");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		Interpreter i = new Interpreter(p, ps);

		List<String> args = Collections.singletonList("0");

		p.getEntryMethod().getCFG().print();

		i.execute(args);
		assertEquals("0\n", baos.toString());

	}

	@Test public void simplePhiTestTrue() throws IOException, InterruptedException, ParameterException,
			OutOfScopeException {

		Program p = TestUtils.build("If2");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos, true, StandardCharsets.UTF_8.name());
		Interpreter i = new Interpreter(p, ps);

		List<String> args = Collections.singletonList("1");

		i.execute(args);
		assertEquals("1\n", baos.toString());

	}

}