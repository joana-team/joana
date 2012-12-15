/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.util;


/**
 * @author Juergen Graf <graf@kit.edu>
 */
public class LogUtil {

	private LogUtil() {
		throw new IllegalStateException();
	}
	
	public static String attributesToString(final Object obj) {
		if (obj == null) {
			return "null";
		}
		
		final StringBuilder sb = new StringBuilder(obj.getClass().toString() + ":\n");
		final Class<?> thisClass = obj.getClass();

		for (final java.lang.reflect.Field f : thisClass.getFields()) {
			try {
				sb.append(f.getName() + " = ");
				final Class<?> fType = f.getType();

				if (fType.isPrimitive()) {
					final String  n = fType.getName();
					if (n.equals("boolean")) {
						sb.append(f.getBoolean(obj) + "\n");
					} else if (n.equals("byte")) {
						sb.append(f.getByte(obj) + "\n");
					} else if (n.equals("char")) {
						sb.append(f.getChar(obj) + "\n");
					} else if (n.equals("double")) {
						sb.append(f.getDouble(obj) + "\n");
					} else if (n.equals("float")) {
						sb.append(f.getFloat(obj) + "\n");
					} else if (n.equals("int")) {
						sb.append(f.getInt(obj) + "\n");
					} else if (n.equals("long")) {
						sb.append(f.getLong(obj) + "\n");
					} else if (n.equals("short")) {
						sb.append(f.getShort(obj) + "\n");
					} else {
						throw new IllegalStateException("unknown primitive type: " + fType.getName());
					}
				} else {
					final Object val = f.get(obj);
					if (val == null) {
						sb.append("null\n");
					} else if (val.getClass().isArray()) {
						final Class<?> comp = val.getClass().getComponentType();
						if (comp.isPrimitive()) {
							sb.append("[...]\n");
						} else {
							Object[] arr = (Object[]) val;
							sb.append("[");
							for (final Object o : arr) {
								sb.append(o == null ? "null" : shorten(o.toString()));
								sb.append(",");
							}
							sb.append("]\n");
						}
					} else {
						sb.append(shorten(val.toString()) + "\n");
					}
				}
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			}
		}

		return sb.toString();
	}
	
	public static String shorten(final String str) {
		return (str != null && str.length() > 200 ? str.substring(0, 199) + "..." : str);
	}

}
