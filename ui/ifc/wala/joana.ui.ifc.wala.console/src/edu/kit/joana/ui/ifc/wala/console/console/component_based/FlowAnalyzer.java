package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import edu.kit.joana.component.connector.Flows;
import edu.kit.joana.component.connector.JoanaCall;
import edu.kit.joana.component.connector.JoanaCallReturn;
import edu.kit.joana.component.connector.Method;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
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

  protected Logger LOGGER = Logger.getLogger(getClass().getName());

  protected Flows knownFlows;

  public FlowAnalyzer(){
    this.knownFlows = new Flows();
  }

  public abstract void setClassPath(String classPath);

  public abstract void saveDebugGraph(Path path);

  public void setLogLevel(Level level){
    LOGGER.setLevel(level);
  }

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
  public abstract Flows analyze(List<Method> sources, List<Method> sinks, Collection<String> interfacesToImplement);

  public void setKnownFlows(Flows knownFlows){
    this.knownFlows = knownFlows;
  }

  public abstract void setAllowedPackagesForUninitializedFields(Optional<List<String>> allowedPackagesForUninitializedFields);

  public JoanaCallReturn processJoanaCall(JoanaCall call){
    setClassPath(call.classPath);
    setKnownFlows(call.knownFlows);
    setAllowedPackagesForUninitializedFields(call.allowedPackagesForUninitializedFields);
    return new JoanaCallReturn(analyze(call.sources, call.sinks));
  }
}
