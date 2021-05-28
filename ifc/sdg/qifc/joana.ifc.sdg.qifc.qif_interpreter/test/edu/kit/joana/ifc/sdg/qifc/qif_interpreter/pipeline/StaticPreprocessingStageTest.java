package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline;

import com.ibm.wala.ssa.SSAInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Value;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.LogicUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

class StaticPreprocessingStageTest {

	@Test void constantBits_IrrelevantLoop() {
		constantBitsTest("IrrelevantLoop");
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

	@Test public void slice_arithmeticTest() {
		slicingTest("Arithmetic");
	}

	@Test public void slice_IrrelevantloopTest() {
		slicingTest("IrrelevantLoop");
	}

	@Test public void slice_IfTest() {
		slicingTest("If");
	}

	@Test public void slice_LoopTest() {
		slicingTest("Loop");
	}

	@Test public void slice_AndTest() {
		slicingTest("And");
	}

	@Test void slice_SimpleArithmetic() {
		slicingTest("SimpleArithmetic");
	}

	void slicingTest(String testcase) {
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(TestUtils.getDummyArgs(testcase), IStage.Stage.STATIC_PREPROCESSING);

		Set<Integer> computed = new HashSet<>();
		Set<Integer> needsToBeComputed = new HashSet<>();

		Method m = pipeline.env.iProgram.getEntryMethod();
		for (Map.Entry<Integer, Value> e : m.getProgramValues().entrySet()) {
			SSAInstruction i = m.getDef(e.getKey());
			if (i == null)
				continue;
			if (e.getValue().influencesLeak()) {
				computed.add(e.getKey());
				IntStream.range(0, i.getNumberOfUses()).forEach(j -> needsToBeComputed.add(i.getUse(j)));
			}
		}

		for (Integer i : needsToBeComputed) {
			Assertions.assertTrue(computed.contains(i) || constantOrParameter(m.getValue(i)));
		}

		for (Integer i : m.getLeakedValues()) {
			Assertions.assertTrue(computed.contains(i) || constantOrParameter(m.getValue(i)));
		}
	}

	private boolean constantOrParameter(Value v) {
		return v.isConstant() || v.isEffectivelyConstant() || v.isParameter();
	}
}