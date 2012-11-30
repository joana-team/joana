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
public class StaticFieldAccess<T> extends FieldAccess<T> {

	public StaticFieldAccess(final int id, final int iindex, final ParameterField field, final RW rw,
			final Value<Integer> value) {
		super(id, iindex, field, rw, value);
	}

	@Override
	public Kind getKind() {
		return Kind.STATIC;
	}

	@Override
	public boolean isSameAccess(Access<T> other) {
		if (other != null && other.getKind() == Kind.STATIC) {
			return getField().equals(other.getField());
		}

		return false;
	}

	@Override
	public FieldAccess<T> createCopy(final int newId) {
		return new StaticFieldAccess<T>(newId, iindex, getField(), (isRead() ? RW.READ : RW.WRITE), getValue());
	}

	public String toString() {
		if (isRead()) {
			return getValue() + " = " + getField();
		} else {
			return getField() + " = " + getValue();
		}
	}

}
