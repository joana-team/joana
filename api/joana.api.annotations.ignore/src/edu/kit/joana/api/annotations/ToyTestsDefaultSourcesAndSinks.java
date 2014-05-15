/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.annotations;

import edu.kit.joana.ui.annotations.AnnotationPolicy;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

//TODO: Klasse in sinnvolleren package und projektnamen verschieben

/**
 * @author Martin Hecker
 */
public class ToyTestsDefaultSourcesAndSinks {
	@Source(level = Level.HIGH)
	public static boolean SECRET_BOOL = true;
	
	@Sink(level = Level.LOW)
	public static boolean PUBLIC_BOOL = false;
	
	@Source(level = Level.HIGH)
	public static int SECRET = 13;
	
	@Sink(level = Level.LOW)
	public static int PUBLIC = 21;
	
	@Source(level = Level.HIGH)
	public static String SECRET_STRING = "Password: Swordfish";
	
	public static String PUBLIC_STRING = "Password: Password";
	
	@Source(level = Level.HIGH)
	public static Object SECRET_OBJECT = new Object();
	
	public static Object PUBLIC_OBJECT = new Object();

	@Sink(level = Level.LOW, annotate = AnnotationPolicy.ANNOTATE_CALLEE)
	public static void leak(int i) {
		PUBLIC += i;
	}
	
	@Sink(level = Level.LOW, annotate = AnnotationPolicy.ANNOTATE_CALLEE)
	public static void leak(boolean b) {
		PUBLIC_BOOL &= b;
	}

	@Sink(level = Level.LOW, annotate = AnnotationPolicy.ANNOTATE_CALLEE)
	public static void leak(String s) {
		PUBLIC_STRING = s;
	}

	@Sink(level = Level.LOW, annotate = AnnotationPolicy.ANNOTATE_CALLEE)
	public static void leak(Object o) {
		PUBLIC_OBJECT = o;
	}

	public static void influence(int i) {
		SECRET += i;
	}

	public static int toggle(int x) {
		return 0;
	}
	
	public static boolean toggle(boolean b) {
		return false;
	}

	public static String toggleString(String s) {
		return "";
	}

	public static <T> T toggle(T o) {
		return null;
	}
	

}