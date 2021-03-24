package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.FormulaFactory;

import java.io.IOException;

class SimplePhiVisitorTest {

	@Test void implicitFlowTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("Implicit");

		StaticAnalysis sa = new StaticAnalysis(p);
		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());
		sa.computeSATDeps();
		SimplePhiVisitor spv = new SimplePhiVisitor(p.getEntryMethod());
		spv.computePhiDeps();

		/*
		TODO repair test
		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}

		 */
	}

	@Test void implicitFlowTest2() throws IOException, InterruptedException {
		Program p = TestUtils.build("IfinIf");
		FormulaFactory ff = new FormulaFactory();

		StaticAnalysis sa = new StaticAnalysis(p);
		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());
		sa.computeSATDeps();
		SimplePhiVisitor spv = new SimplePhiVisitor(p.getEntryMethod());
		spv.computePhiDeps();

		/*
		TODO: repair test
		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}

		 */
	}

	@Test void implicitFlowTest3() throws IOException, InterruptedException {
		Program p = TestUtils.build("IfinLoop");
		FormulaFactory ff = new FormulaFactory();

		StaticAnalysis sa = new StaticAnalysis(p);
		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());
		sa.computeSATDeps();
		SimplePhiVisitor spv = new SimplePhiVisitor(p.getEntryMethod());
		spv.computePhiDeps();

		/*
		TODO repair test
		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}

		 */
	}

	@Test void implicitFlowTest4() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		FormulaFactory ff = new FormulaFactory();

		StaticAnalysis sa = new StaticAnalysis(p);
		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());
		sa.computeSATDeps();
		SimplePhiVisitor spv = new SimplePhiVisitor(p.getEntryMethod());
		spv.computePhiDeps();

		/*
		TODO repair test
		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}

		 */
	}

	@Test void implicitFlowTest5() throws IOException, InterruptedException {
		Program p = TestUtils.build("Loop");
		FormulaFactory ff = new FormulaFactory();

		StaticAnalysis sa = new StaticAnalysis(p);
		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());
		sa.computeSATDeps();
		SimplePhiVisitor spv = new SimplePhiVisitor(p.getEntryMethod());
		spv.computePhiDeps();
		/*
		TODO repair test
		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}

		 */
	}

	@Test void valueCombinationTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("If3");
		Method m = p.getEntryMethod();

		StaticAnalysis sa = new StaticAnalysis(p);
		sa.computeSATDeps(m);

		/*
		TODO repair test
		m.getCFG().print();
		m.getProgramValues().keySet().forEach(i -> System.out.println(i + " " + Arrays.toString(m.getDepsForValue(i))));
		m.getPhiDeps().keySet().forEach(i -> System.out.println(i + " " + Arrays.toString(m.getPhiDeps().get(i))));

		 */
	}

}