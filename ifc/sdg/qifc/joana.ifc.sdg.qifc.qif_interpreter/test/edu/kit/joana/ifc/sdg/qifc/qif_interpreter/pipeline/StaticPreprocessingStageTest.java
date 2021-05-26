package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class StaticPreprocessingStageTest {

	@Test void constantBitsTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("If");
		Environment env = new Environment(null);
		env.iProgram = p;
		env.completedSuccessfully.put(IStage.Stage.BUILD, true);
		StaticPreprocessingStage prep = new StaticPreprocessingStage();
		env = prep.execute(env);

	}

}