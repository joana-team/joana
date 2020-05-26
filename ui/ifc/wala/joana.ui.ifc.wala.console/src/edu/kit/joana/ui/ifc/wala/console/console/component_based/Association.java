package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.HashMap;
import java.util.Map;

/**
 * Associate ids with methods (method with name "" can represent a class)
 */
public class Association {

  private final Map<String, Method> methodPerId;
  private final Map<Method, String> idPerMethod;

  public Association() {
    methodPerId = new HashMap<>();
    idPerMethod = new HashMap<>();
  }

  public Method getMethod(String id){
    return methodPerId.get(id);
  }

  public String getId(Method method){
    return idPerMethod.get(method);
  }
  public void associate(String id, Method method){
    methodPerId.put(id, method);
    idPerMethod.put(method, id);
  }

  public boolean containsId(String id){
    return methodPerId.containsKey(id);
  }

  public boolean containsMethod(Method method){
    return idPerMethod.containsKey(method);
  }

  @Override public String toString() {
    return "Association{" + "methodPerId=" + methodPerId + '}';
  }
}
