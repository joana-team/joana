package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.api.sdg.SDGConfig;
import kotlin.NotImplementedError;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Collects information over all files first and then processes each file and stores the modified version.
 *
 * The implicit constraint is that the methods are called in the following order:
 * setup → collect* → store* → teardown (the order in which the process method calls them)
 */
public interface FilePass extends Pass {

  @Override
  default void process(SDGConfig cfg, String libClassPath, Path sourceFolder, Path targetFolder) throws IOException {
    setup(cfg, libClassPath, sourceFolder);
    walk(sourceFolder, targetFolder, (s, t, r) -> {
      s.toFile().getParentFile().mkdirs();
      collect(s);
    });
    walk(sourceFolder, targetFolder, this::store);
    teardown();
  }

  default void walk(Path sourceFolder, Path targetFolder, ThrowingTriConsumer<Path, Path, Path> func) throws IOException {
    Pass.walk(sourceFolder, f -> {
      func.accept(f, targetFolder.resolve(sourceFolder.relativize(f)), targetFolder);
    });
  }

  default void setup(String libClassPath) {}

  default void setup(SDGConfig cfg, String libClassPath, Path sourceFolder) { setup(libClassPath); }

  /** might collect information and is called on every file */
  void collect(Path file) throws IOException;

  /** implementation of one of the store methods is required */
  default void store(Path source, Path target) throws IOException {
    throw new NotImplementedError();
  }

  default void store(Path source, Path target, Path baseTargetPath) throws IOException {
    store(source, target);
  }

  void teardown();
}