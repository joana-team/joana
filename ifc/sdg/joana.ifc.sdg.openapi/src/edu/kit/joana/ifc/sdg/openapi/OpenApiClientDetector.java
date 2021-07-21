package edu.kit.joana.ifc.sdg.openapi;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.types.TypeReference;

import java.util.Arrays;

/**
 * Detects Java clients that are generated using the
 * "java" template and the openapi-generator, using
 * a few heuristics.
 */
class OpenApiClientDetector {

  private final String openApiPackage;

  public OpenApiClientDetector() {
    this("");
  }

  public OpenApiClientDetector(String openApiPackage) {
    this.openApiPackage = openApiPackage;
  }

  public boolean isOpenApiClass(String name) {
    return (openApiPackage.length() == 0 || name.startsWith(openApiPackage + ".")) && name.endsWith("Api");
  }

  public boolean isOpenApiClass(TypeReference type) {
    return type.isClassType() && isOpenApiClass(type.getName().toString().replace("/", "."));
  }

  /**
   * Only applies to public methods, all other methods are not wrappable by default
   */
  public boolean isWrappableOpenApiMethod(String className, String methodName, String returnType) {
    return isOpenApiClass(className) && !returnType.equals("okhttp3.Call") && !returnType.equals("ApiResponse");
  }

  public boolean isWrappableOpenApiMethod(IMethod method) {
    try {
      return
          isWrappableOpenApiMethod(method.getDeclaringClass().getName().toString().replace("/", "."), method.getName().toString(),
              method.getReturnType().toString().replace("/", ".")) && method.isPublic() && !method.isAbstract() && !method
              .isClinit() && !method.isNative() && method.getDeclaredExceptions() != null && Arrays
              .stream(method.getDeclaredExceptions()).allMatch(t -> t.getName().toString().endsWith("ApiException"));
    } catch (com.ibm.wala.shrikeCT.InvalidClassFileException e) {
      e.printStackTrace();
    }
    return false;
  }

  /**
   * Returns the operation id represented by a given (wrappable api) method
   *
   * @param method
   * @return
   */
  public String getOperationId(IMethod method) throws IllegalArgumentException {
    if (!isWrappableOpenApiMethod(method)) {
      throw new IllegalArgumentException();
    }
    String className = method.getClass().getName();
    return method.getName().toString();
  }

}
