package edu.kit.joana.ui.annotations;

/**
 * Enum of common instances of {@link com.ibm.wala.ipa.callgraph.pruned.PruningPolicy}
 */
public enum PruningPolicy {

  // ApplicationLoaderPolicy.INSTANCE
  APPLICATION("Keeps a given CGNode if it stems from application code"),
  // ThreadAwareApplicationLoaderPolicy.INSTANCE
  THREAD_AWARE("Keeps a given CGNode if it stems from application code, but does not prune away thread entries"),
  // DoNotPrune.INSTANCE
  DO_NOT_PRUNE("Do not prune at all");

  public final String description;

  PruningPolicy(String description) {
    this.description = description;
  }
}
