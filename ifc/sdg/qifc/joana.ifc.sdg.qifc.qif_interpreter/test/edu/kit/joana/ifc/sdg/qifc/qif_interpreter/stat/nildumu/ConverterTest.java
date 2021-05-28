package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.Environment;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.IStage;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.StaticPreprocessingStage;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConverterTest {

	private static final Map<Pair<String, Integer>, Pair<Set<Integer>, Set<Integer>>> branches;

	static {
		branches = new HashMap<>();
		branches.put(Pair.make("If", 1), Pair.make(Util.newHashSet(-2), Util.newHashSet(-3, 2)));
		branches.put(Pair.make("ArrayInIf", 2), Pair.make(Util.newHashSet(-2, 5), Util.newHashSet(-3, 3, 4)));
		branches.put(Pair.make("IfinIf", 2), Pair.make(Util.newHashSet(-4, 4), Util.newHashSet(-5, 3)));
		branches.put(Pair.make("IfinIf", 1), Pair.make(Util.newHashSet(-2), Util.newHashSet(-3, 2, -4, 4, 3, -5, 7)));
	}

	private static final Map<String, String> prettyPrint;

	static {
		prettyPrint = new HashMap<>();
		prettyPrint.put("Empty",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "h input int v3 = 0buuu;");
		prettyPrint.put("OnlyArgs",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "l output int o_v2 = v2;");
		prettyPrint.put("And",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "h input int v3 = 0buuu;\n"
						+ "int v5;\n" + "v5 = (v2 & v3);\n" + "l output int o_v5 = v5;");
		prettyPrint.put("SimpleArithmetic",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "h input int v3 = 0buuu;\n"
						+ "int v5 = 2;\n" + "int v7 = 1;\n" + "int v13 = 0;\n" + "int v8;\n" + "int v9;\n" + "int v6;\n"
						+ "v6 = (2 + v3);\n" + "v8 = (v3 + 1);\n" + "v9 = (v6 - v8);\n" + "l output int o_v6 = v6;\n"
						+ "l output int o_v8 = v8;\n" + "l output int o_v9 = v9;");
		prettyPrint.put("If",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "int v4 = 0;\n" + "int v5 = 1;\n"
						+ "int v6;\n" + "int v7;\n" + "if ((v2 <= 0))\n" + "  {\n" + "\n" + "  } \n" + "else\n"
						+ "  {\n" + "    v6 = (1 + 0);\n" + "  }\n" + "v7 = phi(v4, v6);\n"
						+ "l output int o_v7 = v7;");
		prettyPrint.put("IfinIf",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "int v4 = 0;\n" + "int v5 = 1;\n"
						+ "int v6 = 2;\n" + "int v8;\n" + "int v9;\n" + "int v10;\n" + "int v7;\n" + "if ((v2 <= 0))\n"
						+ "  {\n" + "\n" + "  } \n" + "else\n" + "  {\n" + "    if ((v2 > 1))\n" + "      {\n"
						+ "        v7 = (2 + 0);\n" + "      } \n" + "    else\n" + "      {\n"
						+ "        v8 = (1 + 0);\n" + "      }\n" + "    v9 = phi(v7, v8);\n" + "  }\n"
						+ "v10 = phi(v4, v9);\n" + "l output int o_v10 = v10;");
		prettyPrint.put("Loop",
				"use_sec basic;\n" + "bit_width 3;\n" + "(int) mLoopfII_2(int v2, int v5, int v8){\n" + "  int x0;\n"
						+ "  if (!(v2 <= v8))\n" + "    {\n" + "    int v7;\n" + "      v7 = (v8 + v5);\n"
						+ "      x0 = *mLoopfII_2(v2, v5, v7);\n" + "    }\n" + "  int x1 = phi(x0, v8);\n"
						+ "  return (x1,);\n" + "}\n" + "h input int v2 = 0buuu;\n" + "int v4 = 0;\n" + "int v5 = 1;\n"
						+ "int v8;\n" + "int v6;\n" + "v6 = (0 + 1);\n" + "v8 = *mLoopfII_2(v2, v5, v6);\n"
						+ "l output int o_v8 = v8;");
		prettyPrint.put("WhileAfterIf", "use_sec basic;\n" + "bit_width 3;\n"
				+ "(int, int) mWhileAfterIffII_3(int v10, int v4, int v5, int v11, int v8){\n" + "  int x0; int x1;\n"
				+ "  if (!(v10 <= v4))\n" + "    {\n" + "      int v7 = (v11 + v5);\n" + "      int v9 = (v10 + v8);\n"
				+ "      x0, x1 = *mWhileAfterIffII_3(v9, v4, v5, v7, v8);\n" + "    }\n" + "  int x2 = phi(x0, v10);\n"
				+ "  int x3 = phi(x1, v11);\n" + "  return (x2, x3);\n" + "}\n" + "h input int v2 = 0buuu;\n"
				+ "int v4 = 0;\n" + "int v5 = 1;\n" + "int v8 = -1;\n" + "if ((v2 <= 1))\n" + "  {\n" + "\n" + "  } \n"
				+ "else\n" + "  {\n" + "\n" + "  }\n" + "int v6 = phi(v4, v5);\n" + "int v10;\n" + "int v11;\n"
				+ "v10, v11 = *mWhileAfterIffII_3(v2, v4, v5, v6, v8);\n" + "l output int o_v11 = v11;");
		prettyPrint.put("LoopInIf", "use_sec basic;\n" + "bit_width 3;\n"
				+ "(int, int) mLoopInIffIII_2(int v10, int v11, int v6, int v8){\n" + "  int x0; int x1;\n"
				+ "  if (!(v10 <= 0))\n" + "    {\n" + "    int v9; int v7;\n" + "      v7 = (v11 + v6);\n"
				+ "      v9 = (v10 + v8);\n" + "      x0, x1 = *mLoopInIffIII_2(v9, v7, v6, v8);\n" + "    }\n"
				+ "  int x2 = phi(x0, v10);\n" + "  int x3 = phi(x1, v11);\n" + "  return (x2, x3);\n" + "}\n"
				+ "h input int v2 = 0buuu;\n" + "h input int v3 = 0buuu;\n" + "int v5 = 0;\n" + "int v6 = 1;\n"
				+ "int v8 = -1;\n" + "int v10;\n" + "int v11;\n" + "int v14;\n" + "if ((v2 <= 0))\n" + "  {\n" + "\n"
				+ "  } \n" + "else\n" + "  {\n" + "    v10, v11 = *mLoopInIffIII_2(v3, v5, v6, v8);\n" + "  }\n"
				+ "v14 = phi(v5, v11);\n" + "l output int o_v14 = v14;");
		prettyPrint.put("LoopinLoop", "use_sec basic;\n" + "bit_width 3;\n"
				+ "(int, int, int) mLoopinLoopfIII_2(int v14, int v15, int v16, int v6, int v8){\n"
				+ "  int x0; int x1; int x2;\n" + "  if (!(v14 <= 0))\n" + "    {\n" + "      int v10;\n"
				+ "      int v11;\n" + "      v10, v11 = *mLoopinLoopfIII_3(v16, v5, v6, v8, v15);\n"
				+ "      int v12 = (v14 + v8);\n" + "      int v13 = (v11 + v6);\n"
				+ "      x0, x1, x2 = *mLoopinLoopfIII_2(v12, v10, v13, v6, v8);\n" + "    }\n"
				+ "  int x3 = phi(x0, v16);\n" + "  int x4 = phi(x1, v14);\n" + "  int x5 = phi(x2, v15);\n"
				+ "  return (x3, x4, x5);\n" + "}\n" + "\n"
				+ "(int, int) mLoopinLoopfIII_3(int v11, int v5, int v6, int v8, int v10){\n" + "  int x6; int x7;\n"
				+ "  if (!(v10 <= v5))\n" + "    {\n" + "      int v7 = (v11 + v6);\n" + "      int v9 = (v10 + v8);\n"
				+ "      x6, x7 = *mLoopinLoopfIII_3(v7, v5, v6, v8, v9);\n" + "    }\n" + "  int x8 = phi(x6, v10);\n"
				+ "  int x9 = phi(x7, v11);\n" + "  return (x8, x9);\n" + "}\n" + "h input int v2 = 0buuu;\n"
				+ "h input int v3 = 0buuu;\n" + "int v5 = 0;\n" + "int v6 = 1;\n" + "int v8 = -1;\n" + "int v16;\n"
				+ "int v14;\n" + "int v15;\n" + "v16, v14, v15 = *mLoopinLoopfIII_2(v2, v3, v5, v6, v8);\n"
				+ "l output int o_v16 = v16;");
		prettyPrint.put("Call",
				"use_sec basic;\n" + "bit_width 3;\n" + "int CalladdIII(int v2, int v3){\n" + "  int v5 = (v2 + v3);\n"
						+ "  return v5;\n" + "}\n" + "h input int v2 = 0buuu;\n" + "int v4 = -1;\n" + "int v8 = 0;\n"
						+ "v6 = CalladdIII(v4, v2);\n" + "l output int o_v6 = v6;");
		prettyPrint.put("IrrelevantLoop", "use_sec basic;\n" + "bit_width 3;\n"
				+ "(int) mIrrelevantLoopfII_2(int v_IrrelevantLoop_f_I_I_2, int v_IrrelevantLoop_f_I_I_7){\n"
				+ "  int x0;\n" + "  if (!(v_IrrelevantLoop_f_I_I_2 <= v_IrrelevantLoop_f_I_I_7))\n" + "    {\n"
				+ "      int v_IrrelevantLoop_f_I_I_4 = 0;\n" + "      int v_IrrelevantLoop_f_I_I_5 = 1;\n"
				+ "      int v_IrrelevantLoop_f_I_I_6;\n"
				+ "      v_IrrelevantLoop_f_I_I_6 = (v_IrrelevantLoop_f_I_I_7 + 1);\n"
				+ "      x0 = *mIrrelevantLoopfII_2(v_IrrelevantLoop_f_I_I_2, v_IrrelevantLoop_f_I_I_6);\n" + "    }\n"
				+ "  int x1 = phi(x0, v_IrrelevantLoop_f_I_I_7);\n" + "  return (x1,);\n" + "}\n"
				+ "h input int v_IrrelevantLoop_f_I_I_2 = 0buuu;\n" + "int v_IrrelevantLoop_f_I_I_4 = 0;\n"
				+ "int v_IrrelevantLoop_f_I_I_5 = 1;\n" + "int v_IrrelevantLoop_f_I_I_7;\n"
				+ "int v_IrrelevantLoop_f_I_I_8;\n"
				+ "v_IrrelevantLoop_f_I_I_7 = *mIrrelevantLoopfII_2(v_IrrelevantLoop_f_I_I_2, v_IrrelevantLoop_f_I_I_4);\n"
				+ "v_IrrelevantLoop_f_I_I_8 = (v_IrrelevantLoop_f_I_I_2 + 1);\n"
				+ "l output int o_v_IrrelevantLoop_f_I_I_8 = v_IrrelevantLoop_f_I_I_8;");
	}

	/*
	Test fails if all testcases are executed in a single test run.
	Make sure to only run a single testcase!
	 */
	@Test void computeConditionalBranch() throws IOException, InterruptedException {
		for (Pair<String, Integer> condStmt : branches.keySet()) {
			computeConditionalBranchTestcase(condStmt.fst, condStmt.snd);
		}
	}

	void computeConditionalBranchTestcase(String testcase, int condHeaderIdx) throws IOException, InterruptedException {
		Program p = TestUtils.build(testcase);
		Method m = p.getEntryMethod();
		DotGrapher.exportDotGraph(m.getCFG());

		BBlock condHeader = m.getCFG().getBlock(condHeaderIdx);
		int trueTargetIdx = condHeader.getTrueTarget();
		System.out.println("True target idx: " + trueTargetIdx);
		BBlock trueTarget = condHeader.succs().stream().filter(b -> b.idx() == trueTargetIdx).findAny().get();
		BBlock falseTarget = condHeader.succs().stream().filter(b -> b.idx() != trueTargetIdx).findAny().get();

		Converter c = new Converter();
		List<BBlock> left = c.computeConditionalBranch(condHeader, trueTarget, trueTarget,
				new ArrayList<>(Arrays.asList(trueTarget)));
		List<BBlock> right = c.computeConditionalBranch(condHeader, falseTarget, falseTarget,
				new ArrayList<>(Arrays.asList(falseTarget)));

		Pair<Set<Integer>, Set<Integer>> expected = branches.get(Pair.make(testcase, condHeader.idx()));
		HashSet<Integer> trueSet = left.stream().map(BBlock::idx).collect(Collectors.toCollection(HashSet::new));
		HashSet<Integer> falseSet = right.stream().map(BBlock::idx).collect(Collectors.toCollection(HashSet::new));

		assertEquals(expected.fst.size(), left.size());
		assertEquals(expected.snd.size(), right.size());
		assertEquals(expected.fst, trueSet);
		assertEquals(expected.snd, falseSet);
	}

	@Test void convertEmptyTest() throws IOException, InterruptedException {
		assertEquals(prettyPrint.get("Empty"), convertProgram("Empty"));
	}

	@Test void convertOnlyArgsTest() throws IOException, InterruptedException {
		assertEquals(prettyPrint.get("OnlyArgs"), convertProgram("OnlyArgs"));
	}

	@Test void convertAndTest() throws IOException, InterruptedException {
		testConversion("And", true);
	}

	@Test void convertSimpleArithmeticTest() throws IOException, InterruptedException {
		testConversion("SimpleArithmetic", true);
	}

	@Test void convertIfTest() throws IOException, InterruptedException {
		testConversion("If", true);
	}

	@Test void convertIfinIfTest() throws IOException, InterruptedException {
		testConversion("IfinIf", true);
	}

	@Test void convertWhileTest() throws IOException, InterruptedException {
		testConversion("Loop", true);
	}

	@Test void convertDoubleWhileTest() throws IOException, InterruptedException {
		testConversion("LoopinLoop", true);
	}

	@Test void convertDoubleWhile2Test() throws IOException, InterruptedException {
		testConversion("LoopinLoop2", true);
	}

	@Test void convertDoubleWhile3Test() throws IOException, InterruptedException {
		testConversion("LoopinLoop3", true);
	}

	@Test void convertWhileAfterIfTest() throws IOException, InterruptedException {
		testConversion("WhileAfterIf", true);
	}

	@Test void convertLoopInIfTest() throws IOException, InterruptedException {
		testConversion("LoopInIf", true);
	}

	@Test void convertCallTest() throws IOException, InterruptedException {
		testConversion("Call", true);
	}

	@Test void convertIrrelevantLoop() throws IOException, InterruptedException {
		testConversion("IrrelevantLoop", true);
	}

	void testConversion(String testCase, boolean print) throws IOException, InterruptedException {
		String res = convertProgram(testCase);
		if (print) {
			System.out.println(res);
		}
		assertEquals(prettyPrint.get(testCase), res);
	}

	String convertProgram(String testCase) throws IOException, InterruptedException {
		Program p = TestUtils.build(testCase);
		DotGrapher.exportDotGraph(p.getEntryMethod().getCFG());
		Environment env = new Environment(null);
		env.iProgram = p;
		env.completedSuccessfully.put(IStage.Stage.BUILD, true);
		StaticPreprocessingStage prep = new StaticPreprocessingStage();
		env = prep.execute(env);
		return env.nProgram.ast.toPrettyString();
	}

	@Test void varName() throws IOException, InterruptedException {
		Program p = TestUtils.build("And");
		String res = Converter.varName(2, p.getEntryMethod());
		assertEquals("v_And.f(II)I_2", res);
	}

	@Test void valNum() throws IOException, InterruptedException {
		int res = Converter.valNum("v_And.f(II)I_2");
		assertEquals(2, res);
	}
}