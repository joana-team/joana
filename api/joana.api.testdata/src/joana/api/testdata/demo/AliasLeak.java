/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.demo;

import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

/**
 * @author Simon Bischof <simon.bischof@kit.edu>
 */
public class AliasLeak {
	
	public static class O {
		int c;
	}
	
	public static void main(String[] argv) throws InterruptedException {
		O o1 = new O();
		O o2 = new O();
		o2 = o1;
		aliasTest(o1, o2);
	}
	
	public static void aliasTest(O o1, O o2) { // aliasTest leaks iff o1 and o2 alias
		o1.c = 0;
		o2.c = 0;
		o1.c = inputPIN();
		print(o2.c);
	}

	@Source
	public static int inputPIN() { return 42; }
	@Sink
	public static void print(int s) {}
	public static int input() { return 13; }
	
}
