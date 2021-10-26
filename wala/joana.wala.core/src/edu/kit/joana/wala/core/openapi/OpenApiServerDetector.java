package edu.kit.joana.wala.core.openapi;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.collections.Iterator2List;
import com.ibm.wala.util.collections.Iterator2Set;
import edu.kit.joana.util.TypeNameUtils;
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

  private boolean isOpenApiClass(String name) {
    return (openApiPackage.length() == 0 || name.startsWith(openApiPackage + ".")) && name.endsWith("Api");
  }

  private boolean isOpenApiClass(TypeReference type) {
    return type.isClassType() && isOpenApiClass(type.getName().toString().replace("/", "."));
  }

  /**
   * Only applies to public methods, all other methods are not wrappable by default
   */
  private boolean isOpenApiServerMethod(String className, String methodName, String returnType) {
    return isOpenApiClass(className) && !returnType.equals("okhttp3.Call") && !returnType.endsWith(".ApiResponse");
  }

  public boolean isOpenApiServerMethod(IMethod method) {
    return
        isOpenApiServerMethod(method.getDeclaringClass().getName().toString().replace("/", ".").substring(1),
            method.getName().toString(),
            method.getReturnType().getName().toString().replace("/", ".").substring(1))
            && method.isPublic() && method.isAbstract() && !method.isInit() && !method
            .isClinit() && !method.isNative();
  }

  /**
   * Returns the operation id represented by a given (wrappable api) method
   *
   * @param method
   * @return
   */
  private String getOperationIdString(IMethod method) throws IllegalArgumentException {
    if (!isOpenApiServerMethod(method)) {
      throw new IllegalArgumentException();
    }
    return method.getName().toString();
  }

  /**
   * Returns a normalized tag
   */
  public String getTag(IMethod method) throws IllegalArgumentException {
    if (!isOpenApiServerMethod(method)) {
      throw new IllegalArgumentException();
    }
    String className = method.getDeclaringClass().getName().getClassName().toString();
    return OperationId.normalizeTag(className.substring(0, className.length() - 3)); // remove "Api" at the end
  }

  public OperationId getOperationId(IMethod method) {
    return new OperationId(getTag(method), getOperationIdString(method));
  }

  public Set<OperationId> detectUsedClientOperations(CallGraph cg) {
    return cg.vertexSet().stream().map(node -> node.node.getMethod()).filter(this::isOpenApiServerMethod).map(this::getOperationId).collect(
        Collectors.toSet());
  }

  /** Also checks that at least one API like method exists */
  public boolean isReallyOpenOpiClass(IClass klass) {
    return isOpenApiClass(klass.getReference()) && klass.getDeclaredMethods().stream().anyMatch(this::isOpenApiServerMethod);
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
          if (isOpenApiServerMethod(n.getMethod()) || n.getMethod().isInit()) {
            return false;
          }
          Iterator2List<CGNode> callers = Iterator2Set.toList(cg.getOrig().getPredNodes(n));
          return callers.stream().anyMatch(caller -> !caller.getMethod().getDeclaringClass().equals(n.getMethod().getDeclaringClass()));
        });
  }

  public boolean isOpenApiServerMethod(String klassName, String methodName, String descriptor, String signature, String[] exceptions) {
    return isOpenApiServerMethod(klassName.replace("/", "."), methodName,
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
          if (isOpenApiServerMethod(n.getMethod()) || n.getMethod().isInit()) {
            return false;
          }
          Iterator2List<CGNode> callers = Iterator2Set.toList(cg.getPredNodes(n));
          return callers.stream().anyMatch(caller -> !caller.getMethod().getDeclaringClass().equals(n.getMethod().getDeclaringClass()));
        });
  }

  private static boolean hasAnnotation(IMethod method, String javaClassName) {
    return method.getAnnotations().stream()
        .anyMatch(a -> TypeNameUtils.toJavaClassName(a.getType().getName().toString() + ";").equals(javaClassName));
  }

  private static boolean isApiServerMethod(IMethod method) {
    return method.getAnnotations().stream()
        .anyMatch(a -> TypeNameUtils.toJavaClassName(a.getType().getName().toString() + ";").matches("javax\\.ws\\.rs\\.[A-Z]+")) &&
        hasAnnotation(method, "io.swagger.annotations.ApiResponses") &&
        hasAnnotation(method, "io.swagger.annotations.ApiOperation") &&
        !method.isPrivate();
  }


  public Optional<IMethod> getImplementingServerMethod(IMethod method) {
    return Stream.concat(method.getDeclaringClass().getAllImplementedInterfaces().stream(), Stream.of(method.getDeclaringClass().getSuperclass()))
        .map(c -> c.getMethod(new Selector(method.getName(), method.getDescriptor()))).filter(Objects::nonNull).filter(OpenApiServerDetector::isApiServerMethod).findFirst();
  }
}
