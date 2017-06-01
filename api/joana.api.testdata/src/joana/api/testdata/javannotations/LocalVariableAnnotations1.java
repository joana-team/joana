/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.javannotations;

import static edu.kit.joana.ui.annotations.Level.HIGH;
import static edu.kit.joana.ui.annotations.Level.LOW;

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

	public static int test22(int value) {
		@Source(level=HIGH)
		int x = value;
		
		int y = x;
		
		@Sink(level=LOW)
		int z = y;
		
		return z;
	}
	
	public static int test23(int value) {
		@Source(level=HIGH)
		int x = value;
		
		int y = x;
		
		@Sink(level=LOW)
		int z = y;
		
		return 0;
	}
	
	public static int test24(int value) {
		@Source(level=HIGH)
		int x = value;
		
		@Sink(level=LOW)
		int z = x;
		
		return 0;
	}
	
	
	
	
	// failing tests
	public static int test2() {
		@Source(level=HIGH)
		int x = 5;
		
		int y = x;
		
		@Sink(level=LOW)
		int z = y;
		
		return z;
	}
	
	public static int test3() {
		@Source(level=HIGH)
		int x = 5;
		
		int y = x;
		
		@Sink(level=LOW)
		int z = y;
		
		return 0;
	}
	
	public static int test4() {
		@Source(level=HIGH)
		int x = 5;
		
		@Sink(level=LOW)
		int z = x;
		
		return 0;
	}

	
	public static class Test2 {
		public static void main(String[] args) {
			test2();
		}
	}
	
	public static class Test3 {
		public static void main(String[] args) {
			test3();
		}
	}
	
	public static class Test4 {
		public static void main(String[] args) {
			test4();
		}
	}
	
	public static class Test22 {
		public static void main(String[] args) {
			test22(17);
		}
	}
	
	public static class Test23 {
		public static void main(String[] args) {
			test23(17);
		}
	}
	
	public static class Test24 {
		public static void main(String[] args) {
			test24(17);
		}
	}

}
