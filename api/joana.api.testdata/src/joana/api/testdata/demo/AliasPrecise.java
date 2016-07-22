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
public class AliasPrecise {
	
	public static class O {
		int c;
	}
	
	public static void main(String[] argv) throws InterruptedException {
		O o1 = new O();//first object
		O o2 = new O();//second object
		O temp = o1;
		o1 = o2;
		o2 = temp;
		/*
		 * When considering the whole method, o1 can point to both the first and the second object
		 * (same for o2), and we would find that o1 and o2 may-alias.
		 * However, at this point, o1 points to the second and o2 to the first object,
		 * so o1 and o2 point to different objects. Therefore, we increase precision
		 * because we only look at the points-to sets at this point in execution.
		 */
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
