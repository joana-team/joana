package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import com.ibm.wala.classLoader.IMethod;
import edu.kit.joana.component.connector.Method;

public class Connector {

  public static Method methodForIMethod(IMethod method) {
    return new Method(method.getDeclaringClass().getName().getClassName().toString(),
        method.getName().toString());
  }
}
