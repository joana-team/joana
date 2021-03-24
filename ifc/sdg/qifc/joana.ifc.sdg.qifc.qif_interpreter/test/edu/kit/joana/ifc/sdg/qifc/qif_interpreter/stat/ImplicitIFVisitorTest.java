package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImplicitIFVisitorTest {

	@Test void implicitFlowTestNoCF() throws IOException, InterruptedException {

		Program p = TestUtils.build("SimpleArithmetic");

		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());

		p.getEntryMethod().getCFG().getBlocks().forEach(b -> assertEquals(0, b.getImplicitFlows().size()));
	}

	@Test void implicitFlowTestIf() throws IOException, InterruptedException {

		Program p = TestUtils.build("If");

		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		iifv.compute(p.getEntryMethod().getCFG());

		assertEquals(0, p.getEntryMethod().getCFG().getBlock(0).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(1).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(2).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(3).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(4).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(5).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(6).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(-2).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(-3).getImplicitFlows().size());

		assertEquals(2, p.getEntryMethod().getCFG().getBlock(3).getImplicitFlows().get(0).fst);
		assertEquals(2, p.getEntryMethod().getCFG().getBlock(-3).getImplicitFlows().get(0).fst);
		assertEquals(2, p.getEntryMethod().getCFG().getBlock(-2).getImplicitFlows().get(0).fst);

		assertEquals(false, p.getEntryMethod().getCFG().getBlock(3).getImplicitFlows().get(0).snd);
		assertEquals(false, p.getEntryMethod().getCFG().getBlock(-3).getImplicitFlows().get(0).snd);
		assertEquals(true, p.getEntryMethod().getCFG().getBlock(-2).getImplicitFlows().get(0).snd);
	}

	@Test void implicitFlowTestLoop() throws IOException, InterruptedException {

		Program p = TestUtils.build("Loop");

		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		p.getEntryMethod().getCFG().print();
		iifv.compute(p.getEntryMethod().getCFG());

		p.getEntryMethod().getCFG().getBlocks().forEach(b -> System.out.println(b.idx() + " " + b.getImplicitFlows()));

		assertEquals(0, p.getEntryMethod().getCFG().getBlock(0).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(1).getImplicitFlows().size());
		assertEquals(0, p.getEntryMethod().getCFG().getBlock(2).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(3).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(4).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(5).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(6).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(-2).getImplicitFlows().size());
		assertEquals(1, p.getEntryMethod().getCFG().getBlock(-3).getImplicitFlows().size());

		p.getEntryMethod().getCFG().getBlocks().stream().filter(b -> b.getImplicitFlows().size() > 1).forEach(b -> assertEquals(2, b.getImplicitFlows().get(0).fst));

		assertEquals(false, p.getEntryMethod().getCFG().getBlock(3).getImplicitFlows().get(0).snd);
		assertEquals(false, p.getEntryMethod().getCFG().getBlock(-3).getImplicitFlows().get(0).snd);
		assertEquals(true, p.getEntryMethod().getCFG().getBlock(-2).getImplicitFlows().get(0).snd);
		assertEquals(true, p.getEntryMethod().getCFG().getBlock(4).getImplicitFlows().get(0).snd);
		assertEquals(true, p.getEntryMethod().getCFG().getBlock(6).getImplicitFlows().get(0).snd);
		assertEquals(true, p.getEntryMethod().getCFG().getBlock(5).getImplicitFlows().get(0).snd);
	}

	@Test void implicitFlowTestIfinIf() throws IOException, InterruptedException {

		Program p = TestUtils.build("IfinIf");

		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		p.getEntryMethod().getCFG().print();
		iifv.compute(p.getEntryMethod().getCFG());

		p.getEntryMethod().getCFG().getBlocks().forEach(b -> System.out.println(b.idx() + " " + b.getImplicitFlows()));

	}

	@Test void implicitFlowTestIfinLoop() throws IOException, InterruptedException {

		Program p = TestUtils.build("IfinLoop");

		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		p.getEntryMethod().getCFG().print();
		iifv.compute(p.getEntryMethod().getCFG());

		p.getEntryMethod().getCFG().getBlocks().forEach(b -> System.out.println(b.idx() + " " + b.getImplicitFlows()));

	}

	@Test void implicitFlowTestWhileAfterIf() throws IOException, InterruptedException {

		Program p = TestUtils.build("WhileAfterIf");

		ImplicitIFVisitor iifv = new ImplicitIFVisitor();
		p.getEntryMethod().getCFG().print();
		iifv.compute(p.getEntryMethod().getCFG());

		System.out.println("Values");
		p.getEntryMethod().getProgramValues().keySet()
				.forEach(i -> System.out.println(i + " " + p.getEntryMethod().getDepsForValue(i)));

		p.getEntryMethod().getCFG().getBlocks().forEach(b -> System.out.println(b.idx() + " " + b.getImplicitFlows()));

	}

}