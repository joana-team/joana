package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

class StaticPreprocessingStageTest {

	@Test void constantBits_IrrelevantLoop() {
		constantBitsTest("Irrelevantloop");
	}

	@Test void constantBits_If() {
		constantBitsTest("If");
	}

	@Test void constantBits_Loop() {
		constantBitsTest("Loop");
	}

	@Test void constantBits_And() {
		constantBitsTest("And");
	}

	@Test void constantBits_SimpleArithmetic() {
		constantBitsTest("SimpleArithmetic");
	}

	@Test void constantBits_Arithmetic() {
		constantBitsTest("Arithmetic");
	}

	void constantBitsTest(String testcase) {
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(TestUtils.getDummyArgs(testcase), IStage.Stage.SAT_ANALYSIS);

		Method m = pipeline.env.iProgram.getEntryMethod();
		for (Map.Entry<Integer, Value> entry : m.getProgramValues().entrySet()) {
			if (entry.getValue().getType() == Type.INTEGER) {
				Value.BitLatticeValue[] mask = entry.getValue().getConstantBitMask();
				System.out.println(entry.getKey() + " " + Arrays.toString(mask));
				Assertions.assertEquals(entry.getValue().getWidth(), mask.length);
				for (int i = 0; i < entry.getValue().getConstantBitMask().length; i++) {
					switch (mask[i]) {
					case ONE:
						Assertions.assertEquals(LogicUtil.ff.constant(true), entry.getValue().getDepForBit(i));
						break;
					case ZERO:
						Assertions.assertEquals(LogicUtil.ff.constant(false), entry.getValue().getDepForBit(i));
						break;
					default:
						// do nothing
					}
				}
			}
		}
	}
}