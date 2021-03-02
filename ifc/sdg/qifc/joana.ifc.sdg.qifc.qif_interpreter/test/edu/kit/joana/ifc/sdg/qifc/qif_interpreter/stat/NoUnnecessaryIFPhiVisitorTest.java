package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import static org.junit.jupiter.api.Assertions.*;

class NoUnnecessaryIFPhiVisitorTest {



	@Test void iFidenticalTest() {
		NoUnnecessaryIFPhiVisitor v = new NoUnnecessaryIFPhiVisitor(null);

		Variable x = LogicUtil.ff.variable("x");
		Variable y = LogicUtil.ff.variable("y");

		Formula op1 = LogicUtil.ff.and(x, LogicUtil.ff.not(y));
		Formula op2 = LogicUtil.ff.not(y);

		boolean res = v.iFIdentical(op1, op2);
		assertFalse(res);
	}

	@Test void iFidenticalTest1() {
		NoUnnecessaryIFPhiVisitor v = new NoUnnecessaryIFPhiVisitor(null);

		Variable x = LogicUtil.ff.variable("x");

		boolean res = v.iFIdentical(x, x);
		assertTrue(res);
	}

}