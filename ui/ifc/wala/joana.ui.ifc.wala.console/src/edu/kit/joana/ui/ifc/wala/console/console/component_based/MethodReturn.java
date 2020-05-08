package edu.kit.joana.ui.ifc.wala.console.console.component_based;

public class MethodReturn extends Method {

  public MethodReturn(Method method){
    this(method.className, method.methodName);
  }

  public MethodReturn(String className, String methodName) {
    super(className, methodName);
  }
  
  @Override public Method discardMiscInformation() {
    return new Method(className, methodName);
  }

  <T> T accept(Visitor<T> visitor){
    return visitor.visit(this);
  }

  @Override public String toRegexp() {
    return String.format(".*%s\\.%s\\(.*\\)[^-]*->-1$", className, methodName);
  }

  @Override public String toString() {
    return "MethodReturn{" + className + "." + methodName + "}";
  }
}
