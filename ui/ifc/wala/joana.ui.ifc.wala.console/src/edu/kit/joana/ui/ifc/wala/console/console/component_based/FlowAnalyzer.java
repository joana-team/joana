package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TODO: add inner interface stuff (flow between parameters and/or return value of the same interface method)
 */
public abstract class FlowAnalyzer {

  static class AnalysisException extends RuntimeException {
    public AnalysisException(String message) {
      super(message);
    }
  }

  protected final Association association;
  protected Flows knownFlows;

  public FlowAnalyzer(Association association){
    this.association = association;
    this.knownFlows = new Flows();
  }

  public abstract void setClassPath(String classPath);

  /**
   * Find the connections between the given sources and sinks
   *
   * Implements the sink interfaces
   */
  public Flows analyze(List<Method> sources, List<Method> sinks){
    return analyze(sources, sinks, sinks.stream().map(Method::getClassName).collect(Collectors.toSet()));
  }

  /**
   * Find the connections between the given sources and sinks
   */
  public abstract Flows analyze(List<Method> sources, List<Method> sinks, Collection<String> interfaceToImplement);

  public void setKnownFlows(Flows knownFlows){
    this.knownFlows = knownFlows;
  }
}
