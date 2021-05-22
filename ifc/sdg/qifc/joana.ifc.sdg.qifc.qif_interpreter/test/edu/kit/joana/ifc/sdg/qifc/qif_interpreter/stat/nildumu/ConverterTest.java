package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import com.ibm.wala.util.collections.Pair;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.oopsies.ConversionException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ui.DotGrapher;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util.Util;
import nildumu.Parser;
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
						+ "int v5 = (v2 & v3);\n" + "l output int o_v5 = v5;");
		prettyPrint.put("SimpleArithmetic",
				"use_sec basic;\n" + "bit_width 3;\n" + "h input int v2 = 0buuu;\n" + "h input int v3 = 0buuu;\n"
						+ "int v6 = (2 + v3);\n" + "int v8 = (v3 + 1);\n" + "int v9 = (v6 - v8);\n"
						+ "l output int o_v6 = v6;\n" + "l output int o_v8 = v8;\n" + "l output int o_v9 = v9;");
	}

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

	@Test void convertEmptyTest() throws IOException, InterruptedException, ConversionException {
		assertEquals(prettyPrint.get("Empty"), convertProgram("Empty"));
	}

	@Test void convertOnlyArgsTest() throws ConversionException, IOException, InterruptedException {
		assertEquals(prettyPrint.get("OnlyArgs"), convertProgram("OnlyArgs"));
	}

	@Test void convertAndTest() throws ConversionException, IOException, InterruptedException {
		testConversion("And", true);
	}

	@Test void convertSimpleArithmeticTest() throws ConversionException, IOException, InterruptedException {
		testConversion("SimpleArithmetic", true);
	}

	void testConversion(String testCase, boolean print) throws ConversionException, IOException, InterruptedException {
		String res = convertProgram(testCase);
		if (print) {
			System.out.println(res);
		}
		assertEquals(prettyPrint.get(testCase), res);
	}

	String convertProgram(String testCase) throws IOException, InterruptedException, ConversionException {
		Program p = TestUtils.build(testCase);
		Converter c = new Converter();
		Parser.ProgramNode res = c.convertProgram(p);
		return res.toPrettyString();
	}
}