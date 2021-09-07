package edu.kit.joana.api.sdg.opt.asm;

import edu.kit.joana.util.TypeNameUtils;

import java.util.StringJoiner;

public class BaseMethodDescriptor {
  public final String methodClass;
  public final String methodName;
  public final String methodDescriptor;

  public BaseMethodDescriptor(String methodClass, String methodName, String methodDescriptor) {
    this.methodClass = methodClass.startsWith("L") ? TypeNameUtils.toJavaClassName(methodClass) : methodClass;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
  }

  @Override public String toString() {
    return new StringJoiner(", ", BaseMethodDescriptor.class.getSimpleName() + "[", "]").add("methodClass='" + methodClass + "'")
        .add("methodName='" + methodName + "'").add("methodDescriptor='" + methodDescriptor + "'").toString();
  }
}
