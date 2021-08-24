package edu.kit.joana.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages temporary folders and cleans up properly, thread safe
 *
 * See https://dzone.com/articles/working-with-temporary-filesfolders-in-java
 */
public class TmpDirectoryManager {

  private static List<Path> tempDirs = new ArrayList<>();

  static {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      for (Path tempDir : tempDirs) {
        File toFile = tempDir.toFile();
        try {
          FileUtils.deleteDirectory(toFile);
        } catch (IOException e) {
        }
      }
    }));
  }

  public static synchronized Path createTempDirectory(String prefix) throws IOException {
    Path newDirectory = Files.createTempDirectory(prefix);
    tempDirs.add(newDirectory);
    return newDirectory;
  }
}
