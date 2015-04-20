/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class BooleanExpression {

	int i, i2;
	
	public void doSomething(boolean param1) {
		i = (param1 ? 23 : 42);
		runStuff(i);
	}
	
	public void runStuff(int param1) {
		i2 = param1 + 1;
	}
	
	public static void main(String[] args) {
		BooleanExpression be = new BooleanExpression();
		be.doSomething(args.length > 3);
	}

}
