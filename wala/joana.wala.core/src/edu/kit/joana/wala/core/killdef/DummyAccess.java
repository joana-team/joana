/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <T>
 */
public final class DummyAccess<T> extends Access<T> {

	public DummyAccess(final int iindex) {
		super(iindex);
	}

	@Override
	public Kind getKind() {
		return Kind.DUMMY;
	}

	@Override
	public boolean isSameAccess(final Access<T> other) {
		return other == this || (other != null && other.iindex == this.iindex);
	}

	public String toString() {
		return "dummy";
	}
}
