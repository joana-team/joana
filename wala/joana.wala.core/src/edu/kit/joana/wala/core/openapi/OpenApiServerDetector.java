package edu.kit.joana.wala.core.openapi;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2List;
import com.ibm.wala.util.collections.Iterator2Set;
import edu.kit.joana.wala.core.CallGraph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Detects Java servers that are generated using the
 * "jaxrs-cxf" template and the openapi-generator, using
 * a few heuristics.
 */
public class OpenApiServerDetector {

  private final String openApiPackage;

  public OpenApiServerDetector() {
    this("");
  }

  public OpenApiServerDetector(String openApiPackage) {
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
    return isOpenApiClass(className) && !returnType.equals("okhttp3.Call") && !returnType.endsWith("ApiResponse");
  }

  public boolean isWrappableOpenApiMethod(IMethod method) {
    try {
      return
          isWrappableOpenApiMethod(method.getDeclaringClass().getName().toString().replace("/", ".").substring(1),
              method.getName().toString(),
              method.getReturnType().getName().toString().replace("/", ".").substring(1))
              && method.isPublic() && !method.isAbstract() && !method.isInit() && !method
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
  public String getOperationIdString(IMethod method) throws IllegalArgumentException {
    if (!isWrappableOpenApiMethod(method)) {
      throw new IllegalArgumentException();
    }
    return method.getName().toString();
  }

  /**
   * Returns a normalized tag
   */
  public String getTag(IMethod method) throws IllegalArgumentException {
    if (!isWrappableOpenApiMethod(method)) {
      throw new IllegalArgumentException();
    }
    String className = method.getDeclaringClass().getName().getClassName().toString();
    return OperationId.normalizeTag(className.substring(0, className.length() - 3)); // remove "Api" at the end
  }

  public OperationId getOperationId(IMethod method) {
    return new OperationId(getTag(method), getOperationIdString(method));
  }

  public Set<OperationId> detectUsedClientOperations(CallGraph cg) {
    return cg.vertexSet().stream().map(node -> node.node.getMethod()).filter(this::isWrappableOpenApiMethod).map(this::getOperationId).collect(
        Collectors.toSet());
  }

  /** Also checks that at least one API like method exists */
  public boolean isReallyOpenOpiClass(IClass klass) {
    return isOpenApiClass(klass.getReference()) && klass.getDeclaredMethods().stream().anyMatch(this::isWrappableOpenApiMethod);
  }

  /**
   * detects calls to Api class methods that are not supported
   */
  public Stream<CGNode> detectUnsupportedApiCalls(CallGraph cg) {
    Map<IClass, Set<CGNode>> called = new HashMap<>();
    cg.vertexSet().stream().map(node -> node.node).forEach(node -> {
      called.computeIfAbsent(node.getMethod().getDeclaringClass(), c -> new HashSet<>()).add(node);
    });
    return called.entrySet().stream().filter(e -> isReallyOpenOpiClass(e.getKey()))
        .flatMap(e -> e.getValue().stream()).filter(n -> {
          if (isWrappableOpenApiMethod(n.getMethod()) || n.getMethod().isInit()) {
            return false;
          }
          Iterator2List<CGNode> callers = Iterator2Set.toList(cg.getOrig().getPredNodes(n));
          return callers.stream().anyMatch(caller -> !caller.getMethod().getDeclaringClass().equals(n.getMethod().getDeclaringClass()));
        });
  }

  public boolean isWrappableOpenApiMethod(String klassName, String methodName, String descriptor, String signature, String[] exceptions) {
    return isWrappableOpenApiMethod(klassName.replace("/", "."), methodName,
        descriptor.split("\\)")[1].split(";")[0].substring(1).replace('/', '.'))
        && !methodName.equals("<init>") && !methodName.equals("<clinit>") && exceptions != null &&
        exceptions.length == 1 && exceptions[0].endsWith("ApiException");
  }

  public Stream<CGNode> detectUnsupportedApiCalls(com.ibm.wala.ipa.callgraph.CallGraph cg) {
    Map<IClass, Set<CGNode>> called = new HashMap<>();
    Iterator2Collection.toList(cg.iterator()).stream().forEach(node -> {
      called.computeIfAbsent(node.getMethod().getDeclaringClass(), c -> new HashSet<>()).add(node);
    });
    return called.entrySet().stream().filter(e -> isReallyOpenOpiClass(e.getKey()))
        .flatMap(e -> e.getValue().stream()).filter(n -> {
          if (isWrappableOpenApiMethod(n.getMethod()) || n.getMethod().isInit()) {
            return false;
          }
          Iterator2List<CGNode> callers = Iterator2Set.toList(cg.getPredNodes(n));
          return callers.stream().anyMatch(caller -> !caller.getMethod().getDeclaringClass().equals(n.getMethod().getDeclaringClass()));
        });
  }
}
