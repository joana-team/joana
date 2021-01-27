package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class StaticAnalysisTest {

	private static StaticAnalysis sa;
	private static StaticAnalysis.SATVisitor sv;
	private static FormulaFactory ff;

	private static Formula[] zero;
	private static Formula[] one;
	private static Formula[] two;
	private static Formula[] three;
	private static Formula[] four;
	private static Formula[] five;
	private static Formula[] six;
	private static Formula[] seven;

	@BeforeEach void setUp() {
		ff = new FormulaFactory();
		sa = new StaticAnalysis(ff);
		sv = sa.new SATVisitor();

		zero = new Formula[] { ff.constant(false), ff.constant(false), ff.constant(false) };
		one = new Formula[] { ff.constant(false), ff.constant(false), ff.constant(true) };
		two = new Formula[] { ff.constant(false), ff.constant(true), ff.constant(false) };
		three = new Formula[] { ff.constant(false), ff.constant(true), ff.constant(true) };
		four = new Formula[] { ff.constant(true), ff.constant(false), ff.constant(false) };
		five = new Formula[] { ff.constant(true), ff.constant(false), ff.constant(true) };
		six = new Formula[] { ff.constant(true), ff.constant(true), ff.constant(false) };
		seven = new Formula[] { ff.constant(true), ff.constant(true), ff.constant(true) };
	}

	@AfterEach void tearDown() {
		sa = null;
		sv = null;
	}

	@Test void addTest() {
		// no carry bits
		Formula[] res = sv.add(zero, one);
		assertArrayEquals(res, one);

		// w/ carry bit
		res = sv.add(one, one);
		assertArrayEquals(two, res);

		// overflow
		res = sv.add(one, three);
		assertArrayEquals(four, res);

		// overflow + carry
		res = sv.add(three, three);
		assertArrayEquals(six, res);
	}

	@Test void multTest() {
		Formula[] res = sv.mult(zero, zero);
		assertArrayEquals(zero, res);

		res = sv.mult(zero, one);
		assertArrayEquals(zero, res);

		res = sv.mult(one, one);
		assertArrayEquals(one, res);

		res = sv.mult(two, two);
		assertArrayEquals(four, res);

		res = sv.mult(three, two);
		assertArrayEquals(six, res);
	}

	@Test void subTest() {
		Formula[] res;

		res = sv.sub(one, zero);
		assertArrayEquals(one, one);

		res = sv.sub(six, three);
		assertArrayEquals(three, res);

		res = sv.sub(four, three);
		assertArrayEquals(one, res);

		// overflow
		res = sv.sub(three, four);
		assertArrayEquals(seven, res);
	}

}