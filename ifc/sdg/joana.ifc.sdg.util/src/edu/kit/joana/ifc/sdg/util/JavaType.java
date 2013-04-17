/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util;

import java.util.LinkedList;
import java.util.List;


/**
 * This class represents java types and provides methods to parse a java type from a string. Allowed strings have to meet one
 * of the following formats:
 * <ul>
 * <li><i>human-readable:</i><p/>
 * <b>&lt;typeHR&gt;</b> ::= <b>&lt;baseTypeHR&gt;</b>([])* where <p>
 * <b>&lt;baseTypeHR&gt;</b> ::= <b>&lt;primitiveTypeHR&gt;</b>| &lt;fqRefTypeHR&gt; <p>
 * <b>&lt;primitiveTypeHR&gt;</b> ::= void|boolean|byte|char|short|int|long|float|double<p>
 * <b>&lt;fqRefTypeHR&gt;</b> ::= (<b>&lt;packageName&gt;</b>(.<b>&lt;packageName&gt;</b>)*.)?<b>&lt;className&gt;</b> </li>
 *
 * <li><i>bytecode format:</i><p/>
 * <b>&lt;typeBC&gt;</b> ::= ([)*<b>&lt;baseTypeBC&gt;</b> where <p>
 * <b>&lt;baseTypeBC&gt;</b> ::= <b>&lt;primitiveTypeBC&gt;</b>|<b>&lt;fqRefTypeBC&gt;</b><p>
 * <b>&lt;primitiveTypeBC&gt;</b> ::= V|C|Z|B|C|S|I|J|F|D<p>
 * <b>&lt;fqRefTypeBC&gt;</b> ::= L(<b>&lt;packageName&gt;</b>(\/<b>&lt;packageName&gt;</b>)*\/)?<b>&lt;className&gt;</b>; </li>
 *
 * </ul>
 * &lt;packageName&gt; and &lt;className&gt; represent allowed package and class names, respectively, in Java.
 * @author Martin Mohr
 *
 */
public class JavaType {

	/** Format descriptor */
	public static enum Format {
		/** for human-readable format */
		HR,

		/** for bytecode format */
		BC;
	}

	private final Format format;
	private final JavaPackage pack;
	private final String baseType;
	private final int arrDim;

	private static final String[] bcBaseTypes = { "V", "Z", "C", "B", "C", "S",
		"I", "J", "F", "D" };
	private static final String[] hrBaseTypes = { "void", "boolean", "char",
		"byte", "char", "short", "int", "long", "float", "double" };

	private JavaType(Format format, String baseType, int arrDim) {
		this.format = format;
		this.arrDim = arrDim;
		switch (format) {
		case HR:
			if (baseType.contains(".")) {
				int ptIndex = baseType.lastIndexOf('.');
				this.pack = new JavaPackage(baseType.substring(0, ptIndex));
			} else {
				this.pack = JavaPackage.DEFAULT;
			}
			break;
		case BC:
			for (int i = 0; i < hrBaseTypes.length; i++) {
				assert !baseType.equals(hrBaseTypes[i]);
			}
			if (baseType.contains("/")) {
				int slIndex = baseType.lastIndexOf("/");
				this.pack = new JavaPackage(baseType.substring(1, slIndex).replaceAll("/", "."));
			} else {
				this.pack = JavaPackage.DEFAULT;
			}

			if (!isPrimitiveBC(baseType) && !baseType.endsWith(";")) {
				baseType += ";";
			}

			break;
		default:
			throw new IllegalStateException();
		}
		this.baseType = baseType;
	}

	private boolean isPrimitiveBC(String baseType2) {
		for (String bcBaseType : bcBaseTypes) {
			if (bcBaseType.equals(baseType2)) {
				return true;
			}
		}

		return false;
	}

	public JavaPackage getPackage() {
		return pack;
	}

	public String toBCString() {
		StringBuilder sbBC = new StringBuilder("");
		for (int i = 0; i < arrDim; i++) {
			sbBC.append('[');
		}
		if (format == Format.HR) {
			if ("void".equals(baseType)) {
				sbBC.append('V');
			} else if ("boolean".equals(baseType)) {
				sbBC.append('Z');
			} else if ("char".equals(baseType)) {
				sbBC.append('C');
			} else if ("byte".equals(baseType)) {
				sbBC.append('B');
			} else if ("short".equals(baseType)) {
				sbBC.append('S');
			} else if ("int".equals(baseType)) {
				sbBC.append('I');
			} else if ("long".equals(baseType)) {
				sbBC.append('J');
			} else if ("float".equals(baseType)) {
				sbBC.append('F');
			} else if ("double".equals(baseType)) {
				sbBC.append('D');
			} else {
				sbBC.append('L');
				sbBC.append(baseType.replaceAll("\\.", "/"));
				sbBC.append(';');
			}
		} else {
			sbBC.append(baseType);
		}
		return sbBC.toString();
	}

	public String toHRString() {
		StringBuilder sbHR = new StringBuilder("");
		if (format == Format.BC) {
			switch (baseType.charAt(0)) {
			case 'V':
				sbHR.append("void");
				break;
			case 'Z':
				sbHR.append("boolean");
				break;
			case 'C':
				sbHR.append("char");
				break;
			case 'B':
				sbHR.append("byte");
				break;
			case 'S':
				sbHR.append("short");
				break;
			case 'I':
				sbHR.append("int");
				break;
			case 'J':
				sbHR.append("long");
				break;
			case 'F':
				sbHR.append("float");
				break;
			case 'D':
				sbHR.append("double");
				break;
			case 'L':
				sbHR.append(baseType.replace("/", ".").replace(";", "")
						.substring(1));
				break;
			}
		} else {
			sbHR.append(baseType);
		}
		for (int i = 0; i < arrDim; i++)
			sbHR.append("[]");

		return sbHR.toString();
	}

	public String toString() {
		return toBCString();
	}

	public String toHRStringShort() {
		String hr = toHRString();
		int offset = hr.lastIndexOf('.');
		if (offset >= 0) {
			return hr.substring(offset + 1);
		} else {
			return hr;
		}
	}

	/**
	 * Parse a string into a single type. Depending on the given format, the string will either be interpreted as
	 * bytecode-style or human-readable style.
	 * @param s string to parse
	 * @param f format indicating how to interpret the given string
	 * @return {@code JavaType} object representing the java type described by the given string
	 */
	public static JavaType parseSingleTypeFromString(String s, Format f) {
		int arrDim;
		String baseType;
		switch (f) {
		case BC:
			if (s.contains("[")) {
				arrDim = s.lastIndexOf('[') + 1;
				baseType = s.substring(arrDim);
			} else {
				arrDim = 0;
				baseType = s;
			}
			break;
			case HR:
				if (s.contains("[")) {
					arrDim = (s.length() - s.indexOf('[')) / 2;
					baseType = s.substring(0, s.indexOf('['));
				} else {
					arrDim = 0;
					baseType = s;
				}
				break;
			default:
				throw new IllegalStateException();
		}

		return new JavaType(f, baseType, arrDim);
	}

	/**
	 * Parse a string into a single type. Since no format is given, the right style of notation is guessed. It is assumed,
	 * that the given string does not denote a primitive type.<p>
	 * If no or both the human-readable and the bytecode formats are applicable, then {@code null} is returned.
	 * @param s string to parse
	 * @return {@code JavaType} object representing the java type described by the given string, {@code null} if
	 * given string is neither in human-readable style nor in bytecode style or if both styles are possible.
	 */
	public static JavaType parseSingleTypeFromString(String s) {
		if (s.startsWith("[") || (s.startsWith("L") && s.contains("/"))) {
			/**
			 * If s starts with "[", then it denotes an array type in bytecode format (Class names must not contain "[").
			 * If it starts with L and contains "/", it describes some reference type in some package - in bytecode notation.
			 * If s does not start with "[" and does not contain "L" or "/", then it is definitely not in bytecode format
			 * (Since we assume, that s does not represent a primitive type).
			 */
			return parseSingleTypeFromString(s, Format.BC);
		} else if (s.endsWith("[]") || (s.contains("."))) {
			/**
			 * If s ends with with "[]", then it denotes an array type in human-readable format (Class names must not contain "[" or "]").
			 * If it contains ".", it describes some reference type in some package - in human-readable format (class names must not contain "."
			 * and the package-separator character in bytecode format is "/").
			 * In every other case, s is definitely not in human-readable format.
			 */
			return parseSingleTypeFromString(s, Format.HR);

		} else {
			/**
			 * s does not start with "[", does not end with "[]", and contains neither "/" nor "."
			 * Either s is some malformed junk or a class in the default package. In the former case,
			 * no format is applicable, in the latter case, no format can be excluded.
			 */
			return null;
		}
	}


	public static List<JavaType> parseListOfTypesFromString(String s, Format f) {
		List<JavaType> ret = new LinkedList<JavaType>();
		switch (f) {
		case BC:
			StringBuilder rest = new StringBuilder(s);
			while (rest.length() > 0) {
				int curLen = rest.length();
				JavaType next = parseSingleBCTypeFromStringPrefix(rest);
				if (next == null || rest.length()==curLen) {
					return null;
				} else {
					ret.add(next);
				}
			}
			break;
		case HR:
			String[] singleTypes = s.split("\\s*,\\s*");
			for (String singleType : singleTypes) {
				ret.add(parseSingleTypeFromString(singleType, f));
			}
			break;
		}

		return ret;
	}

	private static JavaType parseSingleBCTypeFromStringPrefix(StringBuilder s) {
		int arrDim = 0;
		String baseType;
		int cutoff;
		if (s.charAt(0) == '[') {
			do {
				arrDim++;
			} while (s.charAt(arrDim)=='[');
		}

		if (s.charAt(arrDim) == 'L') {
			if (s.indexOf(";") >= 0) {
				baseType = s.substring(arrDim, s.indexOf(";") + 1);
			} else {
				baseType = s.toString().substring(arrDim) + ";";
			}
			cutoff = arrDim + baseType.length();
		} else {
			baseType = s.substring(arrDim, arrDim + 1);
			cutoff = arrDim + 1;
		}

		JavaType ret = new JavaType(Format.BC, baseType, arrDim);
		s.delete(0, cutoff);
		return ret;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return toBCString().hashCode();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof JavaType)) {
			return false;
		}
		JavaType other = (JavaType) obj;
		return toBCString().equals(other.toBCString());
	}
}
