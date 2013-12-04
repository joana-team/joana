/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

/**
 * @author Kuzman Katkalov, Martin Mohr
 */
public class AttributeAsParameter {
	static X low = new X();		// low sink - low and low.a are considered sinks
	static int high = 1;		// high source
	
	public static void main(String[] args) {
		test(low); // 'low' is passed as parameter here; so we also have to annotate all actual parameter nodes
				   // at this call which belong to 'low' or 'low.a'
	}
	static void test(X param){
		param.a = high;
	}
}

class X {
	int a = 2;
}