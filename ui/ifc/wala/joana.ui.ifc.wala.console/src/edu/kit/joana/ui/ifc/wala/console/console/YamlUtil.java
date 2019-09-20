package edu.kit.joana.ui.ifc.wala.console.console;

import com.amihaiemil.eoyaml.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Based on code by Mihai Emil Andronache (some parts are verbatim copies)
 */
class YamlUtil {

  private static class Box<T> {
    T t;

    public Box(T t) {
      this.t = t;
    }
  }

  static <T> Collector<T, ?, YamlSequence> sequenceCollector() {
    return new Collector<T, Box<YamlSequenceBuilder>, YamlSequence>() {

      @Override public Supplier<Box<YamlSequenceBuilder>> supplier() {
        return () -> new Box<>(Yaml.createYamlSequenceBuilder());
      }

      @Override public BiConsumer<Box<YamlSequenceBuilder>, T> accumulator() {
        return (b, s) -> {
          if (b instanceof YamlNode) {
            b.t = b.t.add((YamlNode) b);
          } else {
            b.t = b.t.add(s.toString());
          }
        };
      }

      @Override public BinaryOperator<Box<YamlSequenceBuilder>> combiner() {
        return (a, b) -> {
          for (YamlNode child : b.t.build().children()) {
            a.t = a.t.add(child);
          }
          return a;
        };
      }

      @Override public Function<Box<YamlSequenceBuilder>, YamlSequence> finisher() {
        return b -> b.t.build();
      }

      @Override public Set<Characteristics> characteristics() {
        return Collections.emptySet();
      }
    };
  }

  static YamlMappingBuilder mapping(){
    return Yaml.createInsertionOrderedYamlMappingBuilder();
  }

  static YamlSequenceBuilder sequence(){
    return Yaml.createYamlSequenceBuilder();
  }
}
