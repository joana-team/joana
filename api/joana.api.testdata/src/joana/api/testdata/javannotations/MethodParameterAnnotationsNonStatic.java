/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.javannotations;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class MethodParameterAnnotationsNonStatic {
	
	@Sink(level = "low")
	static int out;
	
	static int tmp;
	
	public void foo(@Source(level="high") int a, int b) {
		tmp = a;
	}
	
	public void bar(@Sink(level="high") int out) {
	}
	
	public static int bar() {
		return tmp;
	}
	
	public void rofl(int x) {
		out = x;
	}
	
	public static void main(String[] args) {
		MethodParameterAnnotationsNonStatic instance = new MethodParameterAnnotationsNonStatic();
		instance.foo(4,42);
		instance.rofl(bar());
	}

}
