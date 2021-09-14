package edu.kit.joana.util;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Iterators {

  public static <T>  Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
  }

  public static <T>  Stream<T> stream(Iterable<T> iterator) {
    return StreamSupport.stream(iterator.spliterator(), false);
  }
}