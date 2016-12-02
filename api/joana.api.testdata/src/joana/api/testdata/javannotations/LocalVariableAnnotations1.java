/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.javannotations;

import static edu.kit.joana.ui.annotations.Level.*;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/**
 * @autho Martin Hecker <martin.hecker@kit.edu>
 */
public class LocalVariableAnnotations1 {
	
	
	
	
	public static int foo(int a, int b) {
		@Source(level=HIGH)
		int x = a;
		return x;
	}
	
	public static int bar(int out) {
		@Sink(level=HIGH)
		int y = out;
		return y;
	}
	
	
	public static int rofl(int x) {
		@Sink(level = LOW)
		int out = x;
		return out;
	}
	
	public static void main(String[] args) {
		bar(foo(4,42));
		rofl(foo(4,42));
	}

}
