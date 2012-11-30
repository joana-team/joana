/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.sdg.nodes;


import com.ibm.wala.types.TypeReference;

import edu.kit.joana.deprecated.jsdg.wala.BytecodeLocation;

/**
 * Represents a field of an array.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ArrayField extends ParameterField {

	private final TypeReference elemType;

	ArrayField(TypeReference elemType) {
		this.elemType = elemType;
	}

	@Override
	public boolean isArray() {
		return true;
	}

	@Override
	public boolean isField() {
		return false;
	}

	@Override
	public boolean isStatic() {
		// array field accesses are never static they always refer to an array
		// object
		return false;
	}

	@Override
	public boolean isPrimitiveType() {
		return elemType.isPrimitiveType();
	}

	@Override
	public String getName() {
		return "[]";
	}

	public String getBytecodeName() {
		return BytecodeLocation.getBCArrayFieldName(elemType);
	}

	public TypeReference getElementType() {
		return elemType;
	}

}
