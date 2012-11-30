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

public abstract class FieldAccess<T> extends Access<T> {

	public final int id;
	private final ParameterField field;
	private final RW rw;
	private final Value<Integer> val;

	public FieldAccess(final int id, final int iindex, final ParameterField field, final RW rw, final Value<Integer> val) {
		super(iindex);
		this.id = id;
		this.field = field;
		this.val = val;
		this.rw = rw;
	}

	public abstract FieldAccess<T> createCopy(final int newId);

	public final Value<Integer> getValue() {
		return val;
	}

	public final ParameterField getField() {
		return field;
	}

	public final boolean isWrite() {
		return rw == RW.WRITE;
	}

	public final boolean isRead() {
		return rw == RW.READ;
	}

}
