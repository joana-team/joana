package edu.kit.joana.api.sdg.opt;

@FunctionalInterface
public interface ThrowingBiConsumer<S, T> {

  void accept(S t, T t1) throws Throwable;

}
