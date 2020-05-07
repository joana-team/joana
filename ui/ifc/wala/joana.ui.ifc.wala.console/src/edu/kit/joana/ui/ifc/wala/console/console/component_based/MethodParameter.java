package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.Objects;

/**
 * A method with an associated parameters
 *
 * <b>equals, hashCode and toRegexp ignore the parameter number</b>
 */
public class MethodParameter extends Method {

  public final int parameter;

  public MethodParameter(String className, String methodName, int parameter) {
    super(className, methodName);
    this.parameter = parameter;
  }

  public MethodParameter(Method method, int parameter){
    this(method.className, method.methodName, parameter);
  }

  @Override public String toString() {
    return "Method{" + className + "." + methodName + "->" + parameter + "}";
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof MethodParameter))
      return false;
    if (!super.equals(o))
      return false;
    MethodParameter that = (MethodParameter) o;
    return parameter == that.parameter;
  }

  @Override public int hashCode() {
    return Objects.hash(super.hashCode(), parameter);
  }

  /**
   * Creates a method object that ignores the parameter information
   */
  public Method discardMiscInformation(){
    return new Method(className, methodName);
  }

  public boolean hasAllParameters(){
    return false;
  }

  public int getParameterNumber(){
    return parameter;
  }
}
