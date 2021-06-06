package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.App;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.AnalysisPipeline;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.pipeline.IStage;
import nildumu.Parser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class NildumuProgramTest {

	private static Map<String, String> exprected = new HashMap<>();

	static {
		exprected.put("Call",
				"use_sec basic;\n" + "bit_width 16;\n" + "int Call_add_II_I(int v__2, int v__3){\n" + "  int v__5;\n"
						+ "  v__5 = (v__2 + v__3);\n" + "  return v__5;\n" + "}\n"
						+ "h input int v__2 = 0buuuuuuuuuuuuuuuu;\n" + "h input int v__3 = 0buuuuuuuuuuuuuuuu;\n"
						+ "int x0;\n" + "int v__5;\n" + "v__5 = (v__2 + v__3);\n" + "x0 = v__5;\n"
						+ "l output int x1 = x0;");
		exprected.put("ArrayParam",
				"use_sec basic;\n" + "bit_width 16;\n" + "int ArrayParam_add__II_I(int[16] v__2, int v__3){\n"
						+ "  int v__6; int v__7;\n" + "  v__6 = (v__2[0]);\n" + "  v__7 = (v__6 + v__3);\n"
						+ "  return v__7;\n" + "}\n" + "int[16] v__2;\n" + "h input int x0 = 0buuuuuuuuuuuuuuuu;\n"
						+ "v__2[0] = x0;\n" + "int x1 = 0;\n" + "v__2[1] = x1;\n" + "int x2 = 0;\n" + "v__2[2] = x2;\n"
						+ "int x3 = 0;\n" + "v__2[3] = x3;\n" + "int x4 = 0;\n" + "v__2[4] = x4;\n" + "int x5 = 0;\n"
						+ "v__2[5] = x5;\n" + "int x6 = 0;\n" + "v__2[6] = x6;\n" + "int x7 = 0;\n" + "v__2[7] = x7;\n"
						+ "int x8 = 0;\n" + "v__2[8] = x8;\n" + "int x9 = 0;\n" + "v__2[9] = x9;\n" + "int x10 = 0;\n"
						+ "v__2[10] = x10;\n" + "int x11 = 0;\n" + "v__2[11] = x11;\n" + "int x12 = 0;\n"
						+ "v__2[12] = x12;\n" + "int x13 = 0;\n" + "v__2[13] = x13;\n" + "int x14 = 0;\n"
						+ "v__2[14] = x14;\n" + "int x15 = 0;\n" + "v__2[15] = x15;\n"
						+ "h input int v__3 = 0buuuuuuuuuuuuuuuu;\n" + "int x16;\n" + "int v__6;\n" + "int v__7;\n"
						+ "v__6 = (v__2[0]);\n" + "v__7 = (v__6 + v__3);\n" + "x16 = v__7;\n"
						+ "l output int x17 = x16;");
		exprected.put("ArrayReturn",
				"use_sec basic;\n" + "bit_width 16;\n" + "int[16] ArrayReturn_g_I__I(int v__2){\n" + "  int[16] v__5;\n"
						+ "  v__5 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};\n" + "  if ((v__2 >= 0))\n"
						+ "    {\n" + "      v__5[0] = 2;\n" + "      return v__5;\n" + "    } \n" + "  else\n"
						+ "    {\n" + "      v__5[0] = 1;\n" + "      return v__5;\n" + "    }\n" + "}\n"
						+ "h input int v__2 = 0buuuuuuuuuuuuuuuu;\n" + "int[16] x0;\n" + "int[16] v__5;\n"
						+ "v__5 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};\n" + "if ((v__2 >= 0))\n" + "  {\n"
						+ "    v__5[0] = 2;\n" + "    x0 = v__5;\n" + "  } \n" + "else\n" + "  {\n"
						+ "    v__5[0] = 1;\n" + "    x0 = v__5;\n" + "  }\n" + "l output int x1 = (x0[0]);\n"
						+ "l output int x2 = (x0[1]);\n" + "l output int x3 = (x0[2]);\n"
						+ "l output int x4 = (x0[3]);\n" + "l output int x5 = (x0[4]);\n"
						+ "l output int x6 = (x0[5]);\n" + "l output int x7 = (x0[6]);\n"
						+ "l output int x8 = (x0[7]);\n" + "l output int x9 = (x0[8]);\n"
						+ "l output int x10 = (x0[9]);\n" + "l output int x11 = (x0[10]);\n"
						+ "l output int x12 = (x0[11]);\n" + "l output int x13 = (x0[12]);\n"
						+ "l output int x14 = (x0[13]);\n" + "l output int x15 = (x0[14]);\n"
						+ "l output int x16 = (x0[15]);");
		exprected.put("MultipleReturns",
				"use_sec basic;\n" + "bit_width 16;\n" + "int MultipleReturns_g_I_I(int v__2){\n"
						+ "  if ((v__2 >= 0))\n" + "    {\n" + "      if ((v__2 >= 2))\n" + "        {\n"
						+ "          return 1;\n" + "        } \n" + "      else\n" + "        {\n"
						+ "          return 0;\n" + "        }\n" + "    } \n" + "  else\n" + "    {\n"
						+ "      return -1;\n" + "    }\n" + "}\n" + "h input int v__2 = 0buuuuuuuuuuuuuuuu;\n"
						+ "int x0;\n" + "if ((v__2 >= 0))\n" + "  {\n" + "    if ((v__2 >= 2))\n" + "      {\n"
						+ "        x0 = 1;\n" + "      } \n" + "    else\n" + "      {\n" + "        x0 = 0;\n"
						+ "      }\n" + "  } \n" + "else\n" + "  {\n" + "    x0 = -1;\n" + "  }\n"
						+ "l output int x1 = x0;");
		exprected.put("Recursion", "use_sec basic;\n" + "bit_width 16;\n" + "int Recursion_rec_I_I(int v__2){\n"
				+ "  int v__8; int v__5; int v__10;\n" + "  v__5 = (v__2 & 1);\n" + "  if ((v__5 != 0))\n" + "    {\n"
				+ "      v__8 = (v__2 & -2);\n" + "      v__10 = Recursion_rec_I_I(v__8);\n" + "      return v__10;\n"
				+ "    } \n" + "  else\n" + "    {\n" + "      return 0;\n" + "    }\n" + "}\n"
				+ "h input int v__2 = 0buuuuuuuuuuuuuuuu;\n" + "int x0;\n" + "int v__8;\n" + "int v__5;\n"
				+ "int v__10;\n" + "v__5 = (v__2 & 1);\n" + "if ((v__5 != 0))\n" + "  {\n" + "    v__8 = (v__2 & -2);\n"
				+ "    v__10 = Recursion_rec_I_I(v__8);\n" + "    x0 = v__10;\n" + "  } \n" + "else\n" + "  {\n"
				+ "    x0 = 0;\n" + "  }\n" + "l output int x1 = x0;");
	}

	@Test void methodProgramTest_Call() {
		methodProgramTest("Call", new int[] { 2, 2 });
	}

	@Test void methodProgramTest_ArrayParam() {
		methodProgramTest("ArrayParam", new int[] { 5, 2 });
	}

	@Test void methodProgramTest_ArrayReturn() {
		methodProgramTest("ArrayReturn", new int[] { 2 });
	}

	@Test void methodProgramTest_MultipleReturn() {
		methodProgramTest("MultipleReturns", new int[] { 2 });
	}

	@Test void methodProgramTest_Recursion() {
		methodProgramTest("Recursion", new int[] { 2 });
	}

	@Test void cc_Call() {
		partialCCTest("Call", new int[] { 2, 2 }, 16.0);
	}

	@Test void cc_ArrayParam() {
		partialCCTest("ArrayParam", new int[] { 5, 2 }, 16.0);
	}

	@Test void cc_ArrayReturn() {
		partialCCTest("ArrayReturn", new int[] { 2 }, 1.0);
	}

	@Test void cc_MultipleReturn() {
		partialCCTest("MultipleReturns", new int[] { 2 }, 2);
	}

	@Test void cc_Recursion() {
		partialCCTest("Recursion", new int[] { 2 }, 0.0);
	}

	void methodProgramTest(String testcase, int[] callArgs) {
		App.Args args = TestUtils.getDummyArgs(testcase);
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.SAT_ANALYSIS);

		Parser.ProgramNode res = pipeline.env.nProgram
				.fromMethod(pipeline.env.iProgram.getMethods().get(0), pipeline.env.iProgram.getMethods().get(1),
						callArgs);
		System.out.println(res.toPrettyString());
		Assertions.assertEquals(exprected.get(testcase), res.toPrettyString());
	}

	void partialCCTest(String testcase, int[] callArgs, double expected) {
		App.Args args = TestUtils.getDummyArgs(testcase);
		AnalysisPipeline pipeline = new AnalysisPipeline();
		pipeline.runPipelineUntil(args, IStage.Stage.SAT_ANALYSIS);

		double res = pipeline.env.nProgram.computeMethodCallCC(pipeline.env.iProgram.getMethods().get(0),
				pipeline.env.iProgram.getMethods().get(1), callArgs);
		Assertions.assertEquals(expected, res);
	}

}