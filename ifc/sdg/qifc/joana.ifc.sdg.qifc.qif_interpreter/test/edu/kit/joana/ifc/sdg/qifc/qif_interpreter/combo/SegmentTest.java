package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BasicBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.CFG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.AnalysisPipeline;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.IStage;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class SegmentTest {

	@Test void segment() {
		App.Args args = TestUtils.getDummyArgs("Fib");
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.BUILD);
	}

	@Test void collapseTest() {
		App.Args args = TestUtils.getDummyArgs("Fib");
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.BUILD);
		ProgramSegment p = pipeline.env.segments;

		p.collapse();
		DotGrapher.exportGraph(p);
	}

	@Test void collapseTest1() {
		ProgramSegment p = ProgramSegment.skeleton(Mockito.mock(Program.class));

		for (int i = 0; i < 5; i++) {
			LinearSegment l = LinearSegment.newEmpty(p);
			BasicBlock b = BasicBlock.createDummy(Mockito.mock(CFG.class), i);
			l.addBlock(b);
			if (i == 3)
				l.dynAnaFeasible = false;
			p.children.add(l);
		}
		DotGrapher.exportGraph(p);
		p.collapse();
		DotGrapher.exportGraph(p);

		Assertions.assertEquals(3, p.children.size());
		Assertions.assertTrue(p.children.get(0) instanceof ContainerSegment);
		Assertions.assertEquals(3, p.children.get(0).children.size());
		Assertions.assertTrue(p.children.get(1) instanceof LinearSegment);
		Assertions.assertTrue(p.children.get(2) instanceof LinearSegment);
	}

	@Test void advanceTest() {
		App.Args args = TestUtils.getDummyArgs("Fib");
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.BUILD);
		ProgramSegment p = pipeline.env.segments;
	}
}