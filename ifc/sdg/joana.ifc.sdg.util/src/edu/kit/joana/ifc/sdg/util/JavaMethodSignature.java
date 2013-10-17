/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.kit.joana.ifc.sdg.util.JavaType.Format;

public final class JavaMethodSignature {
	
	public static final String JavaLangThreadStart = "java.lang.Thread.start()V";
	public static final String JavaLangThreadJoin = "java.lang.Thread.join()V";
	
	private final JavaType declaringType;
	private final String methodName;
	private final List<JavaType> argumentTypes;
	private final JavaType returnType;

	private static final Pattern pMethodBC = Pattern
	.compile("(.+)\\((.*)\\)(.+)");
	private static final Pattern pMethodHR = Pattern
	.compile("(.+)\\s+(.+)\\((.*)\\)");

	private JavaMethodSignature(String methodName,
			List<JavaType> argumentTypes, JavaType returnType) {
		this.methodName = methodName.substring(methodName.lastIndexOf('.') + 1);
		this.argumentTypes = new ArrayList<JavaType>(argumentTypes);
		this.returnType = returnType;
		this.declaringType = JavaType.parseSingleTypeFromString(methodName.substring(0, methodName.lastIndexOf('.')), Format.HR);
		assert this.declaringType != null;
	}

	public String getMethodName() {
		return methodName;
	}

	public String getFullyQualifiedMethodName() {
		return declaringType.toHRString() + "." + getMethodName();
	}

	public JavaType getDeclaringType() {
		return declaringType;
	}

	public List<JavaType> getArgumentTypes() {
		return argumentTypes;
	}

	public JavaType getReturnType() {
		return returnType;
	}

	public String toHRString() {
		StringBuilder sbHR = new StringBuilder();
		sbHR.append(returnType.toHRString());
		sbHR.append(" ");
		sbHR.append(declaringType.toHRString());
		sbHR.append(".");
		sbHR.append(methodName);
		sbHR.append("(");
		for (int i = 0; i < argumentTypes.size(); i++) {
			JavaType nextType = argumentTypes.get(i);
			sbHR.append(nextType.toHRString());
			if (i < argumentTypes.size() - 1) {
				sbHR.append(", ");
			}
		}
		sbHR.append(")");
		return sbHR.toString();
	}

	public String toBCString() {
		StringBuilder sbBC = new StringBuilder();
		sbBC.append(declaringType.toHRString());
		sbBC.append(".");
		sbBC.append(methodName);
		sbBC.append("(");
		for (int i = 0; i < argumentTypes.size(); i++) {
			sbBC.append(argumentTypes.get(i).toBCString());
		}
		sbBC.append(")");
		sbBC.append(returnType.toBCString());
		return sbBC.toString();
	}
	
	public String getSelector() {
		StringBuilder sbBC = new StringBuilder();
		sbBC.append(methodName);
		sbBC.append("(");
		for (int i = 0; i < argumentTypes.size(); i++) {
			sbBC.append(argumentTypes.get(i).toBCString());
		}
		sbBC.append(")");
		sbBC.append(returnType.toBCString());
		return sbBC.toString();
	}

	private static boolean isHumanReadable(String methodSig) {
		Matcher m = pMethodHR.matcher(methodSig);
		if (!m.matches()) {
			return false;
		} else {
			return true;
			/** m.group(1) -> return type
				m.group(2) -> declaring class + method name
				m.group(3) -> argument types
			 */

		}
	}

	private static boolean isBytecode(String methodSig) {
		Matcher m = pMethodBC.matcher(methodSig);
		if (!m.matches()) {
			return false;
		} else {
			return true;
			/**
			 * m.group(1) -> declaring class + method name
			 * m.group(2) -> argument types
			 * m.group(3) -> return type
			 */
		}
	}

	/**
	 * Given a fully qualified class name, returns the signature of the main method of that class.
	 * The expected format of the class name is (<packageName>(.<packageName>)*.)?<className> where
	 * <packageName> denotes allowed package names in Java and <className> denotes allowed class names
	 * in Java.
	 * @param className fully-qualified name of a class
	 * @return method signature for the main method of the given class
	 */
	public static JavaMethodSignature mainMethodOfClass(String className) {
		return fromString(className+(".main([Ljava/lang/String;)V"));
	}

	/**
	 * Parses a java method signature object from the given string. If parsing fails, {@code null} is returned. The given string's format has to be
	 * one of the following:<br>
	 * <ul>
	 * <li> (&lt;packageName&gt;(.&lt;packageName&gt;)*.)?&lt;className&gt;&lt;methodName&gt;((&lt;typeBC&gt;)*)&lt;typeBC&gt; </li>
	 * <li> &lt;typeHR&gt; (&lt;packageName&gt;(.&lt;packageName&gt;)*.)?&lt;className&gt;&lt;methodName&gt;(&lt;typeHR&gt;(,&lt;typeHR&gt;)*) </li>
	 * </ul>
	 * The symbols &lt;packageName&gt;, &lt;className&gt; and &lt;methodName&gt; denote allowed java package names, class names and method names,
	 * respectively. The symbols &lt;typeBC&gt; and &lt;typeHR&gt; represent java types in bytecode format and human-readable format, respectively.
	 * See {@link JavaType} for a specification of these formats.
	 * @param methodSig string from which a java method signature object is to be parsed
	 * @return java method signature object described by given string, if that string has the right format, {@code null} otherwise.
	 */
	public static JavaMethodSignature fromString(String methodSig) {
		if (isHumanReadable(methodSig)) {
			Matcher m = pMethodHR.matcher(methodSig);
			if (m.matches()) {
				/**
				 * m.group(1) -> return type
				 * m.group(2) -> declaring class + method name
				 * m.group(3) -> argument types
				 */
				return new JavaMethodSignature(m.group(2), JavaType.parseListOfTypesFromString(m.group(3), Format.HR), JavaType.parseSingleTypeFromString(m.group(1), Format.HR));
			} else {
				throw new IllegalStateException();
			}
		} else if (isBytecode(methodSig)) {
			Matcher m = pMethodBC.matcher(methodSig);
			if (m.matches()) {
				/**
				 * m.group(1) -> declaring class + method name
				 * m.group(2) -> argument types
				 * m.group(3) -> return type
				 */
				return new JavaMethodSignature(m.group(1),
						JavaType.parseListOfTypesFromString(m.group(2), Format.BC), JavaType.parseSingleTypeFromString(m.group(3), Format.BC));
			} else {
				throw new IllegalStateException();
			}
		} else {
			throw new IllegalArgumentException();
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return toHRString();
	}

	public String toStringHRShort() {
		StringBuilder sbHR = new StringBuilder();
		sbHR.append(returnType.toHRString());
		sbHR.append(" ");
		sbHR.append(methodName);
		sbHR.append("(");
		for (int i = 0; i < argumentTypes.size(); i++) {
			sbHR.append(argumentTypes.get(i).toHRString());
			if (i < argumentTypes.size() - 1)
				sbHR.append(", ");
		}
		sbHR.append(")");
		return sbHR.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
		+ ((argumentTypes == null) ? 0 : argumentTypes.hashCode());
		result = prime * result
		+ ((declaringType == null) ? 0 : declaringType.hashCode());
		result = prime * result
		+ ((methodName == null) ? 0 : methodName.hashCode());
		result = prime * result
		+ ((returnType == null) ? 0 : returnType.hashCode());
		return result;
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
		if (!(obj instanceof JavaMethodSignature)) {
			return false;
		}
		JavaMethodSignature other = (JavaMethodSignature) obj;
		if (argumentTypes == null) {
			if (other.argumentTypes != null) {
				return false;
			}
		} else if (!argumentTypes.equals(other.argumentTypes)) {
			return false;
		}
		if (declaringType == null) {
			if (other.declaringType != null) {
				return false;
			}
		} else if (!declaringType.equals(other.declaringType)) {
			return false;
		}
		if (methodName == null) {
			if (other.methodName != null) {
				return false;
			}
		} else if (!methodName.equals(other.methodName)) {
			return false;
		}
		if (returnType == null) {
			if (other.returnType != null) {
				return false;
			}
		} else if (!returnType.equals(other.returnType)) {
			return false;
		}
		return true;
	}
}
