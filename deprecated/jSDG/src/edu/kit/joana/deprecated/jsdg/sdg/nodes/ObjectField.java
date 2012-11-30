/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;


import com.ibm.wala.classLoader.IField;

import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Represents normal object fields.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ObjectField extends ParameterField {

	private final IField field;

	ObjectField(IField field) {
		this.field = field;
	}

	public final IField getField() {
		return field;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isField() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return field.isStatic();
	}

	@Override
	public boolean isPrimitiveType() {
		return field.getFieldTypeReference().isPrimitiveType();
	}

	@Override
	public String getName() {
		return field.getName().toString();
	}

	public String getBytecodeName() {
		return BytecodeLocation.getBCFieldName(field);
	}
}
