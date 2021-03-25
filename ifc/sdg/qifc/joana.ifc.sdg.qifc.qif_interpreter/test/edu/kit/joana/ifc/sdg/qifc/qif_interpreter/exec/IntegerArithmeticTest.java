package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegerArithmeticTest {

	@Test void addTest() {
		assertEquals(-2, IntegerArithmetic.add(2, -4));
		assertEquals(-1, IntegerArithmetic.add(2, -3));
		assertEquals(0, IntegerArithmetic.add(2, -2));
		assertEquals(1, IntegerArithmetic.add(2, -1));
		assertEquals(2, IntegerArithmetic.add(2, 0));
		assertEquals(3, IntegerArithmetic.add(2, 1));
		assertEquals(-4, IntegerArithmetic.add(2, 2));
		assertEquals(-3, IntegerArithmetic.add(2, 3));
		assertEquals(0, IntegerArithmetic.add(-4, -4));
	}

	@Test void subTest() {
		assertEquals(3, IntegerArithmetic.sub(-4, 1));
	}

}