package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.AnalysisPipeline;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.IStage;
import nildumu.Parser;
import org.junit.jupiter.api.Test;

class NildumuProgramTest {

	@Test void methodProgramTest() {
		App.Args args = TestUtils.getDummyArgs("Call");
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.SAT_ANALYSIS);

		Parser.ProgramNode res = pipeline.env.nProgram
				.fromMethod(pipeline.env.iProgram.getMethods().get(0), pipeline.env.iProgram.getMethods().get(1),
						new int[] { 2, 2 });
		System.out.println(res.toPrettyString());
	}

}