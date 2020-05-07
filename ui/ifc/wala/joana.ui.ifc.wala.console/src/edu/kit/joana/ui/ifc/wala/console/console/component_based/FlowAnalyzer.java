package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import java.util.List;

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
  protected final Flows knownFlows;

  public FlowAnalyzer(Association association, Flows knownFlows){
    this.association = association;
    this.knownFlows = knownFlows;
  }

  public abstract void setClassPath(String classPath);

  /**
   * Find the connections between the given sources and sinks
   */
  public abstract Flows analyze(List<Method> sources, List<Method> sinks);

}
