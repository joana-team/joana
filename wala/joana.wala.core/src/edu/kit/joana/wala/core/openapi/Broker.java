package edu.kit.joana.wala.core.openapi;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Broker {

  public Map<OperationId, Set<EdgeCallback>> callbacks;

  public void submit(OperationId id, Set<Element> from, Set<Element> to) {
    callbacks.getOrDefault(id, Collections.emptySet()).forEach(c -> c.apply(from, to));
  }

  /**
   * Registers a call back
   */
  public void register(OperationId id, EdgeCallback callback) {
    callbacks.computeIfAbsent(id, k -> new HashSet<>()).add(callback);
  }
}
