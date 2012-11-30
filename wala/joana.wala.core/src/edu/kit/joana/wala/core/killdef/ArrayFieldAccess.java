/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.killdef;

import edu.kit.joana.wala.core.ParameterField;
import edu.kit.joana.wala.core.killdef.AccessManager.Value;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 * @param <T>
 */
public class ArrayFieldAccess<T> extends FieldAccess<T> {

	private final Value<Integer> base;
	private final Value<Integer> index;

	public ArrayFieldAccess(final int id, final int iindex, final ParameterField field, final RW rw,
			final Value<Integer> value, final Value<Integer> base, final Value<Integer> index) {
		super(id, iindex, field, rw, value);
		this.base = base;
		this.index = index;
	}

	@Override
	public final Value<Integer> getBase() {
		return base;
	}

	@Override
	public final Value<Integer> getIndex() {
		return index;
	}

	@Override
	public Kind getKind() {
		return Kind.ARRAY;
	}

	@Override
	public boolean isSameAccess(final Access<T> other) {
		if (other != null && other.getKind() == Kind.ARRAY) {
			return base.equals(other.getBase()) && index.equals(other.getIndex())
					&& getField().equals(other.getField());
		}

		return false;
	}

	@Override
	public FieldAccess<T> createCopy(final int newId) {
		return new ArrayFieldAccess<T>(newId, iindex, getField(), (isRead() ? RW.READ : RW.WRITE), getValue(),
				getBase(), getIndex());
	}

	public String toString() {
		if (isRead()) {
			return getValue() + " = " + getBase() + "[" + getIndex() + "]";
		} else {
			return getBase() + "[" + getIndex() + "] = " + getValue();
		}
	}
}
