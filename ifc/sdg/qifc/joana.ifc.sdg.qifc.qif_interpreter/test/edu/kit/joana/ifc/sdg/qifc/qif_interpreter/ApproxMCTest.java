package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ApproxMCTest {

	@Test void patternTest() {
		ApproxMC approxMC = new ApproxMC();
		assertTrue(approxMC.isResult("s mc 3\n"));
		assertTrue(approxMC.isResult("s mc 123455\n"));
		assertFalse(approxMC.isResult("sdasfh"));
		assertFalse(approxMC.isResult("s mc "));
	}

	@Test void invokeTest() throws IOException, InterruptedException {
		ApproxMC approxMC = new ApproxMC();
		long res = approxMC.invokeApproxMC("testResources/mc/1.cnf", null);
		assertEquals(3, res);
		res = approxMC.invokeApproxMC("testResources/mc/2.cnf", null);
		assertEquals(5, res);
	}

	@Test void projectedMCTest() throws IOException, InterruptedException {
		Variable x = LogicUtil.ff.variable("x");
		Variable y1 = LogicUtil.ff.variable("y1");
		Variable y2 = LogicUtil.ff.variable("y2");
		Variable z = LogicUtil.ff.variable("z");

		Formula f = LogicUtil.ff.and(LogicUtil.ff.equivalence(y1, x), LogicUtil.ff.equivalence(y2, x),
				LogicUtil.ff.or(z, LogicUtil.ff.not(z)));
		ApproxMC approxMC = new ApproxMC();
		long res = approxMC.estimateModelCount(f, Arrays.asList(y1, y2));
		assertEquals(2, res);
	}

}