/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg.wala;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.types.TypeReference;

/**
 * Represents a location in the bytecode. For most nodes this maps to an instruction
 * in the bytecode, which is identified by the bytecode name of the method and the
 * index of the bytecode instruction.
 *
 * There is a special case for parameter nodes. The bytecode locations of these
 * nodes do not correspond to a location in the bytecode, they contain information
 * about the field type and name. They can be identified by their bytecode index
 * that matches one of the constants ROOT_PARAMETER, STATIC_FIELD, OBJECT_FIELD
 * or ARRAY_FIELD.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class BytecodeLocation {

	public final static int UNDEFINED_POS_IN_BYTECODE = -1;
	public final static int ROOT_PARAMETER = -2;
	public final static int STATIC_FIELD = -3;
	public final static int OBJECT_FIELD = -4;
	public final static int ARRAY_FIELD = -5;

	public final String bcMethod;
	public final int bcIndex;

	public BytecodeLocation(final String bcMethod, final int bcIndex) {
		if (bcMethod == null) {
			throw new IllegalArgumentException("bcMethod may not be null.");
		} else if (bcIndex < 0 && bcIndex != UNDEFINED_POS_IN_BYTECODE && bcIndex != ROOT_PARAMETER
				&& bcIndex != STATIC_FIELD && bcIndex != OBJECT_FIELD && bcIndex != ARRAY_FIELD) {
			throw new IllegalArgumentException("bytecode index must be >= 0 or correspond to one of the "
					+"special constants defined in BytecodeLocation class.");
		}

		this.bcMethod = bcMethod;
		this.bcIndex = bcIndex;
	}

	public final String toString() {
		return "\"" + bcMethod + "\":" + bcIndex;
	}

	public static final String ROOT_PARAM_PREFIX = "<param> ";

	public static final String EXCEPTION_PARAM = "<exception>";
	public static final String RETURN_PARAM = "<exit>";
	public static final String BASE_PARAM = "<base>";
	public static final String ARRAY_PARAM = "<[]>";
	public static final String INDEX_PARAM = "<index>";
	public static final String UNKNOWN_PARAM = "<???>";

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
	public static String getBCFieldName(IField field) {
//		System.err.println("FIELD(" + field.getReference().getSignature() +")");
		return field.getReference().getSignature();
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
	public static String getBCArrayFieldName(TypeReference elementType) {
//		System.err.println("ARRAY(" + "[" + elementType.getName() + ")");
		return "[" + elementType.getName();
	}


}
