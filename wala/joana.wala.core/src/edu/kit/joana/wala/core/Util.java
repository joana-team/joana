/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core;

import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.BinaryOpInstruction;
import com.ibm.wala.shrikeBT.ConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.UnaryOpInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

public final class Util {

	private Util() {
		throw new UnsupportedOperationException("No instance of this class allowed.");
	}

	public static final String methodName(IMethod method) {
		return methodName(method.getReference());
	}

	public static final String methodName(MethodReference method) {
		StringBuilder name =
			new StringBuilder(simpleTypeName(method.getDeclaringClass().getName()));

		name.append(".");
		String mName = method.getName().toString();
		mName = mName.replace("<", "_").replace(">", "_");
		name.append(mName);
		name.append("(");
		for (int i = 0; i < method.getNumberOfParameters(); i++) {
			TypeReference tRef = method.getParameterType(i);
			if (i != 0) {
				name.append(",");
			}
			if (tRef.isPrimitiveType()) {
				if (tRef == TypeReference.Char) {
					name.append("char");
				} else if (tRef == TypeReference.Byte) {
					name.append("byte");
				} else if (tRef == TypeReference.Boolean) {
					name.append("boolean");
				} else if (tRef == TypeReference.Int) {
					name.append("int");
				} else if (tRef == TypeReference.Long) {
					name.append("long");
				} else if (tRef == TypeReference.Short) {
					name.append("short");
				} else if (tRef == TypeReference.Double) {
					name.append("double");
				} else if (tRef == TypeReference.Float) {
					name.append("float");
				} else {
					name.append("?" + tRef.getName());
				}
			} else {
				name.append(simpleTypeName(tRef.getName()));
			}
		}

		name.append(")");

		return name.toString();
	}

	public static final String simpleTypeName(TypeReference tref) {
		return simpleTypeName(tref.getName());
	}

	/**
	 * Create a human readable typename from a TypeName object
	 * convert sth like [Ljava/lang/String to java.lang.String[]
	 * @param tName
	 * @return type name
	 */
	public static final String simpleTypeName(TypeName tName) {
		StringBuilder test =
			new StringBuilder(tName.toString().replace('/', '.'));

		while (test.charAt(0) == '[') {
			test.deleteCharAt(0);
			test.append("[]");
		}

		// remove 'L' in front of object type
		test.deleteCharAt(0);

		return test.toString();
	}


	public static final String bcTypeName(IClass cls) {
		return bcTypeName(cls.getReference());
	}

	public static final String bcTypeName(TypeReference tRef) {
		String bcType = tRef.getName().toString();

		return bcType;
	}

	public static final String simpleFieldName(IField f) {
		String bcField = f.getName().toString();
//		String bcClass = simpleTypeName(f.getDeclaringClass().getReference());
//		return bcClass + "." + bcField;
		return bcField;
	}

	public static final String bcFieldName(IField f) {
		String bcField = f.getName().toString();
		String bcClass = bcTypeName(f.getDeclaringClass().getReference());
		return bcClass + "." + bcField;
	}

	public static final String sourceFileName(TypeName name) {
		assert (name.isClassType());

		String source = name.toString();
		if (source.indexOf('$') > 0) {
			// remove inner classes stuff
			source = source.substring(0, source.indexOf('$'));
		}

		// remove 'L'
		source = source.substring(1);
		source = source + ".java";

		return source;
	}

	/**
	 * Transcribes the elements of the given integer set into an array and returns that array.
	 * @param intSet integer set to transcribe
	 * @return the elements of the given integer set as an array
	 */
	public static int[] intSetToArray(Set<Integer> intSet) {
		if (intSet == null) {
			throw new IllegalArgumentException();
		}
		int[] ret = new int[intSet.size()];
		int count = 0;
		for (int x : intSet) {
			ret[count] = x;
			count++;
		}
		return ret;
	}

	public static String op2str(UnaryOpInstruction.IOperator op) {
		if (op instanceof UnaryOpInstruction.Operator) {
			switch ((UnaryOpInstruction.Operator) op) {
			case NEG:
				return "-";
			}
		}

		return "?";
	}

	public static String op2str(BinaryOpInstruction.IOperator op) {
		if (op instanceof BinaryOpInstruction.Operator) {
			switch ((BinaryOpInstruction.Operator) op) {
			case ADD:
				return "+";
			case AND:
				return "&&";
			case DIV:
				return "/";
			case MUL:
				return "*";
			case OR:
				return "||";
			case REM:
				return "%";
			case SUB:
				return "-";
			case XOR:
				return "^";
			}
		}

		return "?";
	}

	public static String op2str(ConditionalBranchInstruction.IOperator op) {
		if (op instanceof ConditionalBranchInstruction.Operator) {
			switch ((ConditionalBranchInstruction.Operator) op) {
			case EQ:
				return "==";
			case GE:
				return ">=";
			case GT:
				return ">";
			case LE:
				return "<=";
			case LT:
				return "<";
			case NE:
				return "!=";
			}
		}

		return "?";
	}

	public static String op2str(IComparisonInstruction.Operator opcode) {
		return opcode.name();
	}

}
