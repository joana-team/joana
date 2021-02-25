package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.FormulaFactory;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimplePhiVisitorTest {

	@Test void implicitFlowTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("Implicit");

		StaticAnalysis sa = new StaticAnalysis(p);
		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());
		sa.computeSATDeps();
		SimplePhiVisitor spv = new SimplePhiVisitor(p.getEntryMethod());
		spv.computePhiDeps();

		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}
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

		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}
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

		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}
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

		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}
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

		List<? extends SSAInstruction> phis = Util.asList(p.getEntryMethod().getIr().iteratePhis());
		for (SSAInstruction phi : phis) {
			assertNotNull(spv.getPhiDeps(phi.getDef()));
		}
	}

}