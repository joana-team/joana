package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.combo;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.AnalysisPipeline;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.IStage;
import org.junit.jupiter.api.Test;

class SegmentTest {

	@Test void segment() {
		App.Args args = TestUtils.getDummyArgs("Fib");
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.BUILD);
	}

	@Test void advanceTest() {
		App.Args args = TestUtils.getDummyArgs("Fib");
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.BUILD);
		ProgramSegment p = pipeline.env.segments;
	}
}