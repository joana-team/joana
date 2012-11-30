/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import com.ibm.wala.classLoader.CallSiteReference;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.killdef.AccessManager.Value;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <T>
 */
public abstract class Access<T> {

	public final int iindex;
	private T node;

	public static enum Kind { STATIC, FIELD, ARRAY, DUMMY, CALL, PHI }
	public static enum RW { READ, WRITE };

	public Access(final int iindex) {
		this.iindex = iindex;
	}

	public abstract Kind getKind();

	public abstract boolean isSameAccess(final Access<T> other);

	public Value<Integer> getValue() {
		throw new UnsupportedOperationException();
	}

	public ParameterField getField() {
		throw new UnsupportedOperationException();
	}

	public Value<Integer> getBase() {
		throw new UnsupportedOperationException();
	}

	public Value<Integer> getIndex() {
		throw new UnsupportedOperationException();
	}

	public CallSiteReference getCallSite() {
		throw new UnsupportedOperationException();
	}

	public boolean isWrite() {
		return false;
	}

	public boolean isRead() {
		return false;
	}

	public final void setNode(final T node) {
		this.node = node;
	}

	public final T getNode() {
		return node;
	}
}
