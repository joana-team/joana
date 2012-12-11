/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

/**
 * @author Martin Hecker
 */
public class Annotations {
	public static boolean SECRET_BOOL = true;
	public static boolean PUBLIC_BOOL = false;
	
	public static int SECRET = 13;
	public static int PUBLIC = 21;
	
	public static String SECRET_STRING = "Password: Swordfish";
	public static String PUBLIC_STRING = "Password: Password";
	
	public static Object SECRET_OBJECT = new Object();
	public static Object PUBLIC_OBJECT = new Object();

	public static void leak(int i) {
		PUBLIC += i;
	}
	
	public static void leak(boolean b) {
		PUBLIC_BOOL &= b;
	}

	public static void leak(String s) {
		PUBLIC_STRING = s;
	}

	public static void leak(Object o) {
		PUBLIC_OBJECT = o;
	}

	public static void influence(int i) {
		SECRET += i;
	}

	public static int toggle(int x) {
		return x;
	}

	public static boolean toggle(boolean b) {
		return b;
	}
	
	public static String toggleString(String s) {
		return s;
	}

	public static <T> T toggle(T o) {
		return o;
	}

}