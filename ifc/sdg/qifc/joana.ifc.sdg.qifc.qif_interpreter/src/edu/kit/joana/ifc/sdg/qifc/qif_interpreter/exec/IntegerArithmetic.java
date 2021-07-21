package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.exec;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;

/**
 * implements arithmetic functions for integers of bitwidth {@code} WIDTH
 * Allows interpreter to handle over- / underflows correctly
 */
public class IntegerArithmetic {

	private static final int WIDTH = Type.INTEGER.bitwidth();
	private static final int NUM_VALUES = (int) Math.pow(2, WIDTH);
	private static final int MAX = (int) Math.pow(2, WIDTH - 1) - 1;
	private static final int MIN = (int) -Math.pow(2, WIDTH - 1);

	private static int overflow(int i) {
		int mod = i % NUM_VALUES;
		if (mod > MAX) {
			return mod - NUM_VALUES;
		} else {
			return mod;
		}
	}

	public static int add(int x, int y) {
		return x + y;
	}

	public static int sub(int x, int y) {
		return x - y;
	}

	public static int mult(int x, int y) {
		return x * y;
	}

	public static int div(int x, int y) {
		return x / y;
	}

	public static int mod(int x, int y) {
		return x % y;
	}
}