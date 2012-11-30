/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.pointsto;

public interface PartialOrder<T> {

	public enum Cmp { SMALLER, BIGGER, EQUAL, UNCOMPARABLE }

	/**
	 * Compares this object to the parameter. If this is bigger then the parameter
	 * Cmp.BIGGER is returned. If this is smaller, Cmp.SMALLER is returned. Cmp.EQUAL is
	 * returned iff both are equal and Cmp.UNCOMPARABLE iff they are not comparable.
	 * @param elem The other element this is compared to.
	 * @return A Cmp object describing how <tt>this</tt> compares to the parameter object.
	 */
	public Cmp compareTo(T elem);

}
