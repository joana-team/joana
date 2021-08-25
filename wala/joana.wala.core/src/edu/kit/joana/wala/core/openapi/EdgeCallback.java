package edu.kit.joana.wala.core.openapi;

import java.util.Set;

@FunctionalInterface
public interface EdgeCallback {
  void apply(Set<Element> from, Set<Element> to);
}
