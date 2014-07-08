/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.types.TypeReference;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

/**
 * Represents a field of an array.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class ArrayField extends ParameterField {

	private final TypeReference elemType;

	ArrayField(final TypeReference elemType) {
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
		return getBCArrayFieldName(elemType);
	}
	
	/**
	 * Returns the bytecode type of an array field for a given type reference
	 * of the array elements.
	 * Examples:
	 * <pre>
	 * [Ljava/lang/Object
	 * [C
	 * [Ljava/lang/String
	 * </pre>
	 * @param elementType Type reference of the lements of the array field.
	 * @return Bytecode type of an array field for a given type reference
	 * of the array elements.
	 */
	private static String getBCArrayFieldName(TypeReference elementType) {
		return "[" + elementType.getName();
	}

	public TypeReference getElementType() {
		return elemType;
	}

	@Override
	public int hashCode() {
		return elemType.hashCode() * 23;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj instanceof ArrayField) {
			final ArrayField other = (ArrayField) obj;

			return elemType.equals(other.elemType);
		}

		return false;
	}

	@Override
	public int getBytecodeIndex() {
		return BytecodeLocation.ARRAY_FIELD;
	}

	@Override
	public TypeReference getType() {
		return elemType;
	}

}
