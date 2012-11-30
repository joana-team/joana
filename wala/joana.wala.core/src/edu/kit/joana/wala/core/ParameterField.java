/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.types.TypeReference;

/**
 * Base class for object fields: May be an array field or an normal object field.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public abstract class ParameterField {

	public abstract boolean isField();
	public abstract boolean isArray();
	public abstract boolean isStatic();
	public abstract boolean isPrimitiveType();
	public abstract String getName();
	public abstract String getBytecodeName();
	public abstract int getBytecodeIndex();
	public abstract TypeReference getType();

	public IField getField() {
		throw new UnsupportedOperationException("No IField for this param field.");
	}

	public TypeReference getElementType() {
		throw new UnsupportedOperationException("No element type for this param field.");
	}

	public String toString() {
		return getName();
	}

	public abstract int hashCode();

	public abstract boolean equals(Object obj);

}
