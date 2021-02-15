package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec.Interpreter;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StaticAnalysisTest {

	private static StaticAnalysis sa;
	private static StaticAnalysis.SATVisitor sv;
	private static FormulaFactory ff;

	private static final char[] zero = "0000".toCharArray();
	private static final char[] one = "0001".toCharArray();
	private static final char[] two = "0010".toCharArray();
	private static final char[] three = "0011".toCharArray();
	private static final char[] four = "0100".toCharArray();
	private static final char[] five = "0101".toCharArray();
	private static final char[] six = "0110".toCharArray();
	private static final char[] seven = "0111".toCharArray();
	private static final char[] minusEight = "1000".toCharArray();
	private static final char[] minusSeven = "1001".toCharArray();
	private static final char[] minusSix = "1010".toCharArray();
	private static final char[] minusFive = "1011".toCharArray();
	private static final char[] minusFour = "1100".toCharArray();
	private static final char[] minusThree = "1101".toCharArray();
	private static final char[] minusTwo = "1110".toCharArray();
	private static final char[] minusOne = "1111".toCharArray();



	@BeforeEach void setUp() {
		ff = new FormulaFactory();
		sa = new StaticAnalysis(ff);
		sv = sa.new SATVisitor();
	}

	@AfterEach void tearDown() {
		sa = null;
		sv = null;
	}

	@Test void addTest() {
		Formula[] oneF = sa.asFormulaArray(one);
		Formula[] threeF = sa.asFormulaArray(three);
		Formula[] fourF = sa.asFormulaArray(four);
		Formula[] sevenF = sa.asFormulaArray(seven);

		Formula[] minusFourF = sa.asFormulaArray(minusFour);

		assertArrayEquals(sv.add(oneF, threeF), fourF);
		assertArrayEquals(sv.add(threeF, fourF), sevenF);
		assertArrayEquals(sv.add(minusFourF, sevenF), threeF);
	}

	@Test void multTest() {
		Formula[] oneF = sa.asFormulaArray(one);
		Formula[] threeF = sa.asFormulaArray(three);
		Formula[] twoF = sa.asFormulaArray(two);
		Formula[] minusTwoF = sa.asFormulaArray(minusTwo);
		Formula[] minusThreeF = sa.asFormulaArray(minusThree);
		Formula[] minusOneF = sa.asFormulaArray(minusOne);
		Formula[] sixF = sa.asFormulaArray(six);
		Formula[] minusSixF = sa.asFormulaArray(minusSix);

		assertArrayEquals(threeF, sv.mult(oneF, threeF));
		assertArrayEquals(oneF, sv.mult(minusOneF, minusOneF));
		assertArrayEquals(sixF, sv.mult(threeF, twoF));
		assertArrayEquals(sixF, sv.mult(minusTwoF, minusThreeF));
		assertArrayEquals(minusSixF, sv.mult(twoF, minusThreeF));
		assertArrayEquals(minusSixF, sv.mult(minusTwoF, threeF));
	}

	@Test void subTest() {
		Formula[] oneF = sa.asFormulaArray(one);
		Formula[] threeF = sa.asFormulaArray(three);
		Formula[] twoF = sa.asFormulaArray(two);
		Formula[] minusTwoF = sa.asFormulaArray(minusTwo);

		assertArrayEquals(twoF, sv.sub(threeF, oneF));
		assertArrayEquals(minusTwoF, sv.sub(oneF, threeF));

		Formula[] minusFiveF = sa.asFormulaArray(minusFive);
		Formula[] minusThreeF = sa.asFormulaArray(minusThree);
		Formula[] minusOneF = sa.asFormulaArray(minusOne);

		assertArrayEquals(minusOneF, sv.sub(minusThreeF, minusTwoF));
		assertArrayEquals(minusFiveF, sv.sub(minusThreeF, twoF));
	}

	@Test void twosComplementTest() {
		char[] res = sa.twosComplement(1, 4);
		assertArrayEquals(one, res);
		res = sa.twosComplement(2, 4);
		assertArrayEquals(two, res);
		res = sa.twosComplement(3, 4);
		assertArrayEquals(three, res);
		res = sa.twosComplement(4, 4);
		assertArrayEquals(four, res);
		res = sa.twosComplement(5, 4);
		assertArrayEquals(five, res);
		res = sa.twosComplement(6, 4);
		assertArrayEquals(six, res);
		res = sa.twosComplement(7, 4);
		assertArrayEquals(seven, res);
		res = sa.twosComplement(0, 4);
		assertArrayEquals(zero, res);
	}

	@Test void twosComplementTestNegNumbers() {
		char[] one = sa.twosComplement(-1, 4);
		assertArrayEquals(new char[]{'1', '1', '1', '1'}, one);
		char[] two = sa.twosComplement(-2, 4);
		assertArrayEquals(new char[]{'1', '1', '1', '0'}, two);
		char[] three = sa.twosComplement(-3, 4);
		assertArrayEquals(new char[]{'1', '1', '0', '1'}, three);
		char[] four = sa.twosComplement(-4, 4);
		assertArrayEquals(new char[]{'1', '1', '0', '0'}, four);
		char[] five = sa.twosComplement(-5, 4);
		assertArrayEquals(new char[]{'1', '0', '1', '1'}, five);
		char[] six = sa.twosComplement(-6, 4);
		assertArrayEquals(new char[]{'1', '0', '1', '0'}, six);
		char[] seven = sa.twosComplement(-7, 4);
		assertArrayEquals(new char[]{'1', '0', '0', '1'}, seven);
		char[] eight = sa.twosComplement(-8, 4);
		assertArrayEquals(new char[]{'1', '0', '0', '0'}, eight);
	}

	@Test void negTest() {
		Formula[] twoF = sa.asFormulaArray(two);
		Formula[] minusTwoF = sa.asFormulaArray(minusTwo);
		Formula[] zeroF = sa.asFormulaArray(zero);

		assertArrayEquals(twoF, sv.neg(minusTwoF));
		assertArrayEquals(minusTwoF, sv.neg(twoF));
		assertArrayEquals(zeroF, sv.neg(zeroF));
	}

	@Test void piTest() throws IOException, InterruptedException, InvalidClassFileException {
		Program p = TestUtils.build("If2");
		StaticAnalysis sa = new StaticAnalysis(p);
		
		sa.computeSATDeps();

	}

}