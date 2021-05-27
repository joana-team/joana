package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.Slicer;
import nildumu.Lattices;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

class StaticPreprocessingStageTest {

	@Test void constantBitsTest() throws IOException, InterruptedException {
		Environment env = untilPreProcessStage("IrrelevantLoop");

		Map<Integer, Lattices.Value> bits = ((StaticPreprocessingStage.PreprocessingResult) env.lastStage).bits;
		for (int i : bits.keySet()) {
			System.out.println(i + " " + bits.get(i).toString());
		}

		// ------------ try out slicing stuff ----------------
		Slicer slicer = new Slicer(env.iProgram);
		int leakedVal = 8;
		Map<Integer, Boolean> neededDefs = slicer.findSlice(leakedVal, env.iProgram.getEntryMethod());
		System.out.println(neededDefs.entrySet().stream().filter(Map.Entry::getValue).collect(Collectors.toList()));
	}

	Environment untilPreProcessStage(String testCase) throws IOException, InterruptedException {
		Program p = TestUtils.build(testCase);
		Environment env = new Environment(null);
		env.iProgram = p;
		env.completedSuccessfully.put(IStage.Stage.BUILD, true);
		StaticPreprocessingStage prep = new StaticPreprocessingStage();
		env = prep.execute(env);
		return env;
	}

}