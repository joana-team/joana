/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.annotations;

/**
 * Joana annotation helper methods.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class Joana {

	private Joana() {}
	
	public static int declassify(final String from, final String to, final int value) {
		return 0;
	}

	public static int declassify(final int value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static boolean declassify(final String from, final String to, final boolean value) {
		return false;
	}

	public static boolean declassify(final boolean value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static byte declassify(final String from, final String to, final byte value) {
		return 0;
	}

	public static byte declassify(final byte value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static double declassify(final String from, final String to, final double value) {
		return 0.0d;
	}

	public static double declassify(final double value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static float declassify(final String from, final String to, final float value) {
		return 0.0f;
	}

	public static float declassify(final float value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static char declassify(final String from, final String to, final char value) {
		return '\u0000';
	}

	public static char declassify(final char value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static long declassify(final String from, final String to, final long value) {
		return 0L;
	}

	public static long declassify(final long value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static short declassify(final String from, final String to, final short value) {
		return 0;
	}

	public static short declassify(final short value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static String declassify(final String from, final String to, final String value) {
		return "declassify";
	}

	public static String declassify(final String value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static int[] declassify(final String from, final String to, final int[] value) {
		return new int[1];
	}

	public static int[] declassify(final int[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static boolean[] declassify(final String from, final String to, final boolean[] value) {
		return new boolean[1];
	}

	public static boolean[] declassify(final boolean[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static byte[] declassify(final String from, final String to, final byte[] value) {
		return new byte[1];
	}

	public static byte[] declassify(final byte[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static double[] declassify(final String from, final String to, final double[] value) {
		return new double[1];
	}

	public static double[] declassify(final double[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static float[] declassify(final String from, final String to, final float[] value) {
		return new float[1];
	}

	public static float[] declassify(final float[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static char[] declassify(final String from, final String to, final char[] value) {
		return new char[1];
	}

	public static char[] declassify(final char[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static long[] declassify(final String from, final String to, final long[] value) {
		return new long[1];
	}

	public static long[] declassify(final long[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

	public static short[] declassify(final String from, final String to, final short[] value) {
		return new short[1];
	}

	public static short[] declassify(final short[] value) {
		return declassify(Level.HIGH, Level.LOW, value);
	}

}
