package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.Objects;

/**
 * A method with a name and a class, a method without any addition refers to the method signature (all parameters), but not
 * to the return value
 */
public class Method {

  public final String className;
  public final String methodName;

  public Method(String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Method))
      return false;
    Method method = (Method) o;
    return Objects.equals(className, method.className) && Objects.equals(methodName, method.methodName);
  }

  @Override public int hashCode() {
    return Objects.hash(className, methodName);
  }

  @Override public String toString() {
    return "Method{" + className + "." + methodName + "}";
  }

  /**
   * Returns a regular expression that matches the method
   */
  public String toRegexp(){
    return String.format(".*%s\\.%s(\\(.*\\)[^-]*(->[^-]+)?)?$", className, methodName);
  }

  public Method discardMiscInformation(){
    return this;
  }

  <T> T accept(Visitor<T> visitor){
    return visitor.visit(this);
  }

  public String getClassName() {
    return className;
  }
}
