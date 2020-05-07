package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Found flows
 */
public class Flows {
  private final Map<Method, Set<Method>> flows;

  public Flows(Map<Method, Set<Method>> flows) {
    this.flows = flows;
  }

  public Map<Method, Set<Method>> flows(){
    return Collections.unmodifiableMap(flows);
  }

  public Set<Method> flows(Method method){
    return flows.getOrDefault(method, Collections.emptySet());
  }

  public boolean isEmpty(){
    return flows.isEmpty();
  }

  /**
   * @return this - other
   */
  public Flows remove(Flows other){
    Map<Method, Set<Method>> newFlows = new HashMap<>();
    for (Map.Entry<Method, Set<Method>> methodSetEntry : flows.entrySet()) {
      if (other.flows.containsKey(methodSetEntry.getKey())){
        Set<Method> methods = new HashSet<>();
        for (Method method : methodSetEntry.getValue()) {
          if (!other.flows.get(methodSetEntry.getKey()).contains(method)){
            methods.add(method);
          }
        }
        newFlows.put(methodSetEntry.getKey(), methods);
      } else {
        newFlows.put(methodSetEntry.getKey(), methodSetEntry.getValue());
      }
    }
    return new Flows(newFlows);
  }

  @Override public String toString() {
    return flows.entrySet().stream().map(e -> String.format("%s -> %s", e.getKey(), e.getValue())).collect(Collectors.joining("\n"));
  }

  public boolean hasAllParameters(){
    return true;
  }

  public int getParameterNumber(){
    return -1;
  }

  public Flows discardParameterInformation(){
    Map<Method, Set<Method>> methods = new HashMap<>();
    for (Map.Entry<Method, Set<Method>> entry : flows.entrySet()) {
      methods.computeIfAbsent(entry.getKey().discardMiscInformation(), e -> new HashSet<>())
          .addAll(entry.getValue().stream().map(Method::discardMiscInformation).collect(Collectors.toList()));
    }
    return new Flows(methods);
  }
}
