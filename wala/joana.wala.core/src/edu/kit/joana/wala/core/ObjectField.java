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

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;

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

	public IField getField() {
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
		return getBCFieldName(field);
	}
	
	/**
	 * Returns bytecode fieldname &amp; type tuple of the given field.
	 * Examples:
	 * <pre>
	 * Ljava/lang/Throwable.elms [Ljava/lang/StackTraceElement
	 * Ljava/lang/String.count I
	 * Ljava/lang/Throwable.cause Ljava/lang/Throwable
	 * </pre>
	 * @param field The field we produce the bytecode name of.
	 * @return Bytecode fieldname &amp; type tuple of the given field.
	 */
	private static String getBCFieldName(IField field) {
		//		System.err.println("FIELD(" + field.getReference().getSignature() +")");
		return field.getReference().getSignature();
	}

	@Override
	public int hashCode() {
		return (field.hashCode() << 1) + 1;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj instanceof ObjectField) {
			final ObjectField other = (ObjectField) obj;

			return field.equals(other.field);
		}

		return false;
	}

	@Override
	public int getBytecodeIndex() {
		return (isStatic() ? BytecodeLocation.STATIC_FIELD : BytecodeLocation.OBJECT_FIELD);
	}

	@Override
	public TypeReference getType() {
		return field.getFieldTypeReference();
	}
}
