package edu.kit.joana.api.sdg.opt.asm;

import edu.kit.joana.util.TypeNameUtils;
import io.github.classgraph.MethodInfo;

import java.util.Objects;
import java.util.StringJoiner;

public class BaseMethodDescriptor {
  /** java class name */
  public final String methodClass;
  public final String methodName;
  public final String methodDescriptor;

  public BaseMethodDescriptor(String methodClass, String methodName, String methodDescriptor) {
    assert methodClass.contains("L") || !methodClass.contains("/");
    this.methodClass = methodClass.startsWith("L") ? TypeNameUtils.toJavaClassName(methodClass) : methodClass;
    this.methodName = methodName;
    this.methodDescriptor = methodDescriptor;
  }

  @Override public String toString() {
    return new StringJoiner(", ", BaseMethodDescriptor.class.getSimpleName() + "[", "]").add("methodClass='" + methodClass + "'")
        .add("methodName='" + methodName + "'").add("methodDescriptor='" + methodDescriptor + "'").toString();
  }

  public static BaseMethodDescriptor create(MethodInfo methodInfo) {
    return new BaseMethodDescriptor(methodInfo.getClassName(), methodInfo.getName(), methodInfo.getTypeDescriptorStr());
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof BaseMethodDescriptor))
      return false;
    BaseMethodDescriptor that = (BaseMethodDescriptor) o;
    return Objects.equals(methodClass, that.methodClass) && Objects.equals(methodName, that.methodName) && Objects.equals(
        methodDescriptor, that.methodDescriptor);
  }

  @Override public int hashCode() {
    return Objects.hash(methodClass, methodName, methodDescriptor);
  }
}