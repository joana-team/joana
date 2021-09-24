package edu.kit.joana.api.sdg.opt;

@FunctionalInterface
public interface ThrowingTriConsumer<S, T, R> {

  void accept(S s, T t, R r) throws Throwable;

}