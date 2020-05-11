package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Found flows
 */
public class Flows implements Iterable<Map.Entry<Method, Set<Method>>> {

  private final Map<Method, Set<Method>> flows;

  public Flows(){
    this(new HashMap<>());
  }

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

  public Flows add(Method source, Method sink){
    flows.computeIfAbsent(source, s -> new HashSet<>()).add(sink);
    return this;
  }

  public Flows forMethod(Method method) {
    return forMethod(method, false);
  }

  /**
   * Collect all flows that start in the passed method
   * @param method
   * @param onlyInnerMethodSinks only return sinks that belong to the same method
   * @return new flows instance
   */
  public Flows forMethod(Method method, boolean onlyInnerMethodSinks){
    return filter(s -> s.discardMiscInformation().equals(method), s ->
        !onlyInnerMethodSinks || s.discardMiscInformation().equals(method));
  }

  public Flows onlyParameterSources(){
    return filterSources(s -> s instanceof MethodParameter);
  }

  public boolean contains(Method sink, Method source){
    return flows(sink).contains(source);
  }

  public List<MethodParameter> getParamConnectedToReturn(Method method) {
    Flows parameterFlows = forMethod(method, true).onlyParameterSources();
    return parameterFlows.flows.keySet().stream()
        .filter(p -> parameterFlows.contains(p, new MethodReturn(method))).map(p -> (MethodParameter)p)
        .collect(Collectors.toList());
  }

  public Flows filter(Predicate<Method> sourceFilter, Predicate<Method> sinkFilter){
    return new Flows(flows.entrySet().stream().filter(e -> sourceFilter.test(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().filter(sinkFilter).collect(Collectors.toSet()))));
  }

  public Flows filterSources(Predicate<Method> sourceFilter){
    return new Flows(flows.entrySet().stream().filter(e -> sourceFilter.test(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
  }

  public Flows filterSinks(Predicate<Method> sinkFilter){
    return new Flows(flows.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().filter(sinkFilter).collect(Collectors.toSet()))));
  }

  @Override public Iterator<Map.Entry<Method, Set<Method>>> iterator() {
    return Collections.unmodifiableCollection(flows.entrySet()).iterator();
  }
}
