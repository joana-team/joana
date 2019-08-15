package edu.kit.joana.ui.annotations;

import com.ibm.wala.ipa.callgraph.pruned.ApplicationLoaderPolicy;
import com.ibm.wala.ipa.callgraph.pruned.DoNotPrune;
import edu.kit.joana.wala.core.ThreadAwareApplicationLoaderPolicy;

/**
 * Enum of common instances of {@link com.ibm.wala.ipa.callgraph.pruned.PruningPolicy}
 */
public enum PruningPolicy {

  APPLICATION(ApplicationLoaderPolicy.INSTANCE, "Keeps a given CGNode if it stems from application code"),
  THREAD_AWARE(ThreadAwareApplicationLoaderPolicy.INSTANCE, "Keeps a given CGNode if it stems from application code, but does not prune away thread entries"),
  DO_NOT_PRUNE(DoNotPrune.INSTANCE, "Do not prune at all");

  public final com.ibm.wala.ipa.callgraph.pruned.PruningPolicy policy;
  public final String description;

  PruningPolicy(com.ibm.wala.ipa.callgraph.pruned.PruningPolicy policy, String description) {
    this.policy = policy;
    this.description = description;
  }
}
