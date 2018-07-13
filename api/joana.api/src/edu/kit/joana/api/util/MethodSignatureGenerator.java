/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Rodrigo Andrade
 */
public class MethodSignatureGenerator {

	public void generate(String filePath, String className) throws IOException, ClassNotFoundException,
			SecurityException, InstantiationException, IllegalAccessException {

		// String filePath =
		// "C:\\analyses\\Main.java";
		// String className = "br.ufpe.cin.analyses.Main";
		if (inputIsNotValid(filePath, className)) {
			System.out.println("Input not valid");
			return;
		}

		File file = new File(filePath);
		URL url = file.toURI().toURL();
		URLClassLoader classLoader = new URLClassLoader(new URL[] { url });

		Class<?> classDefinition = classLoader.loadClass(className);
		classLoader.close();

		Method[] methods = classDefinition.newInstance().getClass().getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			Class<?>[] parameterTypes = methods[i].getParameterTypes();
			String fullMethodName = methods[i].getDeclaringClass().getName() + "." + methods[i].getName() + "(";
			fullMethodName = formatParameters(parameterTypes, fullMethodName);
			fullMethodName = fullMethodName + ")";
			fullMethodName = formatReturningType(methods, i, fullMethodName);
			System.out.println(fullMethodName);
		}
		// TODO constructor is missing
	}

	private boolean inputIsNotValid(String filePath, String className) {
		return filePath == null || filePath.isEmpty() || className == null || className.isEmpty();
	}

	private static String formatReturningType(Method[] methods, int i, String fullMethodName) {
		Class<?> returnType = methods[i].getReturnType();
		String returningTypeWithBars = returnType.getName().replace('.', '/');
		if (returningTypeWithBars.equals("void")) {
			fullMethodName = fullMethodName + "V";
		} else if (returnType.isArray()) {
			fullMethodName = fullMethodName + returningTypeWithBars;
		} else if (returnType.isPrimitive()) {
			fullMethodName = fullMethodName + parseJavaNotation(returnType.getName());
		} else {
			fullMethodName = fullMethodName + "L" + returningTypeWithBars + ";";
		}
		return fullMethodName;
	}

	private static String formatParameters(Class<?>[] parameterTypes, String fullMethodName) {
		for (Class<?> paramType : parameterTypes) {
			String paramTypeNameWithBars = paramType.getName().replace('.', '/');
			if (paramType.isArray()) {
				fullMethodName = fullMethodName + paramTypeNameWithBars;
			} else if (paramType.isPrimitive()) {
				fullMethodName = fullMethodName + parseJavaNotation(paramType.getName());
			} else {
				fullMethodName = fullMethodName + "L" + paramTypeNameWithBars + ";";
			}
		}
		return fullMethodName;
	}

	private static String parseJavaNotation(String name) {
		String returning = "";
		switch (name) {
		case "long":
			returning = "J";
			break;
		case "byte":
			returning = "B";
			break;
		case "char":
			returning = "C";
			break;
		case "double":
			returning = "D";
			break;
		case "float":
			returning = "F";
			break;
		case "int":
			returning = "I";
			break;
		case "short":
			returning = "S";
			break;
		case "boolean":
			returning = "Z";
			break;
		default:
			break;
		}
		return returning;
	}
}
