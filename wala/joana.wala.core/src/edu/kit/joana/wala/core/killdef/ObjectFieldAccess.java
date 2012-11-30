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
public class ObjectFieldAccess<T> extends FieldAccess<T> {

	private final Value<Integer> base;

	public ObjectFieldAccess(final int id, final int iindex, final ParameterField field, final RW rw,
			final Value<Integer> value, final Value<Integer> base) {
		super(id, iindex, field, rw, value);
		this.base = base;
	}

	@Override
	public final Value<Integer> getBase() {
		return base;
	}

	@Override
	public Kind getKind() {
		return Kind.FIELD;
	}

	@Override
	public boolean isSameAccess(final Access<T> other) {
		if (other != null && other.getKind() == Kind.FIELD) {
			return base.equals(other.getBase()) && getField().equals(other.getField());
		}

		return false;
	}

	@Override
	public FieldAccess<T> createCopy(final int newId) {
		return new ObjectFieldAccess<T>(newId, iindex, getField(), (isRead() ? RW.READ : RW.WRITE), getValue(),
				getBase());
	}

	public String toString() {
		if (isRead()) {
			return getValue() + " = " + getBase() + "." + getField();
		} else {
			return getBase() + "." + getField() + " = " + getValue();
		}
	}
}
