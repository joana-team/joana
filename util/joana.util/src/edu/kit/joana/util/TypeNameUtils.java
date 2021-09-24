package edu.kit.joana.util;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.StringStuff;
import org.objectweb.asm.Type;

import java.util.stream.Collectors;

public class TypeNameUtils {
  public static String javaClassNameToInternalName(String name) {
    String str = StringStuff.deployment2CanonicalTypeString(name);
    if (str.startsWith("L") || (str.startsWith("[") && !str.endsWith(";"))) {
      return str + ";";
    }
    return str;
  }

  public static String internalNameToJavaClassName(String name) {
    return internalNameToJavaClassName(name, 0, name.length());
  }

  private static String internalNameToJavaClassName(String name, int from, int to) {
    assert !name.contains(".");
    while (to > 1 && name.charAt(to - 2) == ';') {
      to--;
    }
    if (name.charAt(from) == '[') { // definitely an array
      return internalNameToJavaClassName(name, from + 1, to) + "[]";
    }
    if (name.charAt(to - 1) == ';') {
      if (name.charAt(from) == 'L') {
        return name.substring(from + 1, to - 1).replace('/', '.');
      }
    }
    return Type.getType(name.substring(from, to)).getClassName();
  }

  public static String toJavaClassName(IClass klass) {
    return toJavaClassName(klass.getReference());
  }
  public static String toJavaClassName(TypeReference klass) {
    return internalNameToJavaClassName(klass.getName() + (klass.getArrayElementType().isReferenceType() ? ";" : ""));
  }

  public static String toJavaClassName(String internalName) {
    return internalNameToJavaClassName(internalName);
  }

  public static String toJavaClassName(Type type) {
    return type.getClassName();
  }

  public static String toJavaSignatureWOReturn(IMethod method) {
    return method.getName() + "(" + method.getParameterTypes().stream()
        .map(TypeNameUtils::toJavaClassName).collect(Collectors.joining(",")) + ")";
  }

  public static String toInternalName(String klass) {
    return javaClassNameToInternalName(klass);
  }

  /** remove semicolon at the end */
  public static String removeSemicolon(String klass) {
    if (klass.charAt(klass.length() - 1) == ';') {
      return klass.substring(0, klass.length() - 1);
    }
    return klass;
  }

  /** remove prepended 'L' for internal names */
  public static String removeL(String klass) {
    assert !klass.contains(".");
    if (klass.charAt(0) == 'L') {
      return klass.substring(1);
    }
    return klass;
  }

  /**
   * Required for class names for class creation and method calling
   */
  public static String toInternalNameWithoutSemicolonAndL(String klass) {
    return removeL(removeSemicolon(javaClassNameToInternalName(klass)));
  }
}