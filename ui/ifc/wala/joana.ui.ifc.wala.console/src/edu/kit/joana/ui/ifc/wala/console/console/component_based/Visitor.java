package edu.kit.joana.ui.ifc.wala.console.console.component_based;

public interface Visitor<T> {

  T visit(Method method);

  T visit(MethodParameter parameter);

  T visit(MethodReturn methodReturn);
}
