/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;

/**
 * @author Simon Bischof
 */
public class NullObjectFieldRead {
	public int ignored;
	public static void main(String[] args) {
		new NullObjectFieldRead().q();
	}
	
	@SuppressWarnings("null")
	public int q() {
		NullObjectFieldRead o_null = null;
	    ignored=o_null.ignored;
		return ignored;
	}
}
