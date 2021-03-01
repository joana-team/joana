package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.util;

import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.io.parsers.ParserException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LogicUtilTest {

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

	@Test void addTest() {
		Formula[] oneF = LogicUtil.asFormulaArray(one);
		Formula[] threeF = LogicUtil.asFormulaArray(three);
		Formula[] fourF = LogicUtil.asFormulaArray(four);
		Formula[] sevenF = LogicUtil.asFormulaArray(seven);

		Formula[] minusFourF = LogicUtil.asFormulaArray(minusFour);

		assertArrayEquals(LogicUtil.add(oneF, threeF), fourF);
		assertArrayEquals(LogicUtil.add(threeF, fourF), sevenF);
		assertArrayEquals(LogicUtil.add(minusFourF, sevenF), threeF);
	}

	@Test void multTest() {
		Formula[] oneF = LogicUtil.asFormulaArray(one);
		Formula[] threeF = LogicUtil.asFormulaArray(three);
		Formula[] twoF = LogicUtil.asFormulaArray(two);
		Formula[] minusTwoF = LogicUtil.asFormulaArray(minusTwo);
		Formula[] minusThreeF = LogicUtil.asFormulaArray(minusThree);
		Formula[] minusOneF = LogicUtil.asFormulaArray(minusOne);
		Formula[] sixF = LogicUtil.asFormulaArray(six);
		Formula[] minusSixF = LogicUtil.asFormulaArray(minusSix);

		assertArrayEquals(threeF, LogicUtil.mult(oneF, threeF));
		assertArrayEquals(oneF, LogicUtil.mult(minusOneF, minusOneF));
		assertArrayEquals(sixF, LogicUtil.mult(threeF, twoF));
		assertArrayEquals(sixF, LogicUtil.mult(minusTwoF, minusThreeF));
		assertArrayEquals(minusSixF, LogicUtil.mult(twoF, minusThreeF));
		assertArrayEquals(minusSixF, LogicUtil.mult(minusTwoF, threeF));
	}

	@Test void subTest() {
		Formula[] oneF = LogicUtil.asFormulaArray(one);
		Formula[] threeF = LogicUtil.asFormulaArray(three);
		Formula[] twoF = LogicUtil.asFormulaArray(two);
		Formula[] minusTwoF = LogicUtil.asFormulaArray(minusTwo);

		assertArrayEquals(twoF, LogicUtil.sub(threeF, oneF));
		assertArrayEquals(minusTwoF, LogicUtil.sub(oneF, threeF));

		Formula[] minusFiveF = LogicUtil.asFormulaArray(minusFive);
		Formula[] minusThreeF = LogicUtil.asFormulaArray(minusThree);
		Formula[] minusOneF = LogicUtil.asFormulaArray(minusOne);

		assertArrayEquals(minusOneF, LogicUtil.sub(minusThreeF, minusTwoF));
		assertArrayEquals(minusFiveF, LogicUtil.sub(minusThreeF, twoF));
	}

	@Test void twosComplementTest() {
		char[] res = LogicUtil.twosComplement(1, 4);
		assertArrayEquals(one, res);
		res = LogicUtil.twosComplement(2, 4);
		assertArrayEquals(two, res);
		res = LogicUtil.twosComplement(3, 4);
		assertArrayEquals(three, res);
		res = LogicUtil.twosComplement(4, 4);
		assertArrayEquals(four, res);
		res = LogicUtil.twosComplement(5, 4);
		assertArrayEquals(five, res);
		res = LogicUtil.twosComplement(6, 4);
		assertArrayEquals(six, res);
		res = LogicUtil.twosComplement(7, 4);
		assertArrayEquals(seven, res);
		res = LogicUtil.twosComplement(0, 4);
		assertArrayEquals(zero, res);
	}

	@Test void twosComplementTestNegNumbers() {
		char[] one = LogicUtil.twosComplement(-1, 4);
		assertArrayEquals(new char[] { '1', '1', '1', '1' }, one);
		char[] two = LogicUtil.twosComplement(-2, 4);
		assertArrayEquals(new char[] { '1', '1', '1', '0' }, two);
		char[] three = LogicUtil.twosComplement(-3, 4);
		assertArrayEquals(new char[] { '1', '1', '0', '1' }, three);
		char[] four = LogicUtil.twosComplement(-4, 4);
		assertArrayEquals(new char[] { '1', '1', '0', '0' }, four);
		char[] five = LogicUtil.twosComplement(-5, 4);
		assertArrayEquals(new char[] { '1', '0', '1', '1' }, five);
		char[] six = LogicUtil.twosComplement(-6, 4);
		assertArrayEquals(new char[] { '1', '0', '1', '0' }, six);
		char[] seven = LogicUtil.twosComplement(-7, 4);
		assertArrayEquals(new char[] { '1', '0', '0', '1' }, seven);
		char[] eight = LogicUtil.twosComplement(-8, 4);
		assertArrayEquals(new char[] { '1', '0', '0', '0' }, eight);
	}

	@Test void negTest() {
		Formula[] twoF = LogicUtil.asFormulaArray(two);
		Formula[] minusTwoF = LogicUtil.asFormulaArray(minusTwo);
		Formula[] zeroF = LogicUtil.asFormulaArray(zero);

		assertArrayEquals(twoF, LogicUtil.neg(minusTwoF));
		assertArrayEquals(minusTwoF, LogicUtil.neg(twoF));
		assertArrayEquals(zeroF, LogicUtil.neg(zeroF));
	}

	@Test public void cnfTest() throws ParserException {
		Formula p = LogicUtil.ff.parse("A & ~(B | ~C)");
		System.out.println(p.cnf());
	}

	@Test public void numericValueTest() {
		Formula[] oneF = LogicUtil.asFormulaArray(one);
		Formula[] threeF = LogicUtil.asFormulaArray(three);
		Formula[] twoF = LogicUtil.asFormulaArray(two);
		Formula[] minusTwoF = LogicUtil.asFormulaArray(minusTwo);
		Formula[] minusThreeF = LogicUtil.asFormulaArray(minusThree);
		Formula[] minusOneF = LogicUtil.asFormulaArray(minusOne);
		Formula[] sixF = LogicUtil.asFormulaArray(six);
		Formula[] minusSixF = LogicUtil.asFormulaArray(minusSix);

		assertEquals(1, LogicUtil.numericValue(oneF));
		assertEquals(3, LogicUtil.numericValue(threeF));
		assertEquals(2, LogicUtil.numericValue(twoF));
		assertEquals(-1, LogicUtil.numericValue(minusOneF));
		assertEquals(-6, LogicUtil.numericValue(minusSixF));
		assertEquals(-2, LogicUtil.numericValue(minusTwoF));
		assertEquals(-3, LogicUtil.numericValue(minusThreeF));
		assertEquals(6, LogicUtil.numericValue(sixF));

	}

}