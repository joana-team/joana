package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.setter.misc.AnnotatedEntityFinder;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Backend pass
 */
public interface Pass {

  /**
   * Process a folder, {@link Pass#requiresKnowledgeOnAnnotations()} has to be true if the analysis
   * parameter is used
   *
   * @param ana
   * @param cfg overall config (paths might not be correct, especially classpath and out fields should be ignored)
   * @param libClassPath
   * @param sourceFolder source, contains the class files
   * @param targetFolder target, might be the same source
   */
  default void process(IFCAnalysis ana, SDGConfig cfg, String libClassPath, Path sourceFolder, Path targetFolder) throws IOException {
    process(cfg, libClassPath, sourceFolder, targetFolder);
  }

  /**
   * Process a folder
   *  @param cfg overall config (paths might not be correct, especially classpath and out fields should be ignored)
   * @param libClassPath
   * @param sourceFolder source, contains the class files
   * @param targetFolder target, might be the same source
   */
  default void process(SDGConfig cfg, String libClassPath, Path sourceFolder, Path targetFolder) throws IOException {}

  /**
   * Find classes, methods and fields annotated with a JOANA annotation.
   */
  static AnnotatedEntityFinder findAnnotatedEntities(Path folder) throws IOException {
    AnnotatedEntityFinder finder = new AnnotatedEntityFinder(a -> a.startsWith("edu.kit.joana."));
    walk(folder, finder::addAnnotatedEntities);
    return finder;
  }

  static void walk(Path folder, ThrowingConsumer<Path> func) throws IOException {
    List<String> errors = new ArrayList<>();
    try (Stream<Path> walk = Files.walk(folder)) {
      walk.filter(f -> f.toString().endsWith(".class")).forEach(f -> {
        try {
          func.accept(f);
        } catch (Throwable e) {
          e.printStackTrace();
          errors.add(String.format("%s: %s", f, e.getMessage()));
        }
      });
    }
    if (errors.size() > 0){
      throw new IOException(String.join("; ", errors));
    }
  }

  boolean requiresKnowledgeOnAnnotations();
}
