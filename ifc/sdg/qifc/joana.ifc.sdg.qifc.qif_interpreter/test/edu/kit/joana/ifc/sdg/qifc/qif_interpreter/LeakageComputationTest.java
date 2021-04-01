package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Int;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.UnexpectedTypeException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.Variable;

import java.util.ArrayList;

class LeakageComputationTest {

	@Before public void setUp() {
		LogicUtil.ff.clear();
	}

	@Test void createCountingFormulaTest() throws UnexpectedTypeException {

		Variable x = LogicUtil.ff.variable("x");
		Variable y = LogicUtil.ff.variable("y");

		Value test = new Int(0);
		test.setVal(1, 0);
		test.setDeps(new Formula[] { x, x, LogicUtil.ff.not(y) });

		LeakageComputation lc = new LeakageComputation(new ArrayList<>(), test, new Method());
		Formula res = lc.createCountingFormula();
		Assertions.assertEquals(LogicUtil.ff.and(LogicUtil.ff.not(x), LogicUtil.ff.not(y)), res);
	}

}