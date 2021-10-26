package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.util.TmpDirectoryManager;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Collection of passes that are applied one after other
 */
public class PreProcPasses {

  private final List<Pass> passes;
  private final Logger logger = Logger.getLogger("opt");
  {
    logger.setLevel(Level.FINE);
  }

  public PreProcPasses(List<Pass> passes) {
    this.passes = passes;
  }

  public PreProcPasses(Pass... passes){
    this(Arrays.asList(passes));
  }

  public PreProcPasses add(Pass pass) {
    List<Pass> newPasses = new ArrayList<>(passes);
    newPasses.add(pass);
    return new PreProcPasses(newPasses);
  }

  public PreProcPasses addConditionally(boolean condition, Supplier<Pass> supplier) {
    if (condition) {
      return add(supplier.get());
    }
    return this;
  }

  /**
   * @return new class path (library path : new byte code path)
   */
  public String process(IFCAnalysis ana, SDGConfig cfg, String libClassPath, String classPath) throws IOException {
    Path tempDirectory = TmpDirectoryManager.createTempDirectory("joana-passes");
    tempDirectory.toFile().deleteOnExit();
    Path baseDirectory = tempDirectory.resolve("base");
    storeClassPathInFolder(classPath, baseDirectory);
    baseDirectory.toFile().mkdir();
    Path result = applyPasses(ana, cfg, libClassPath, baseDirectory, tempDirectory);
    return (libClassPath.isEmpty() ? "" : (libClassPath + ":")) + result.toString();
  }

  public IFCAnalysis processAndUpdateSDG(IFCAnalysis ana, SDGConfig cfg, String libClassPath,
      String classPath, Function<String, SDGProgram> programCreator)
      throws IOException {
    ana.setProgram(programCreator.apply(process(ana, cfg, libClassPath, classPath)));
    return ana;
  }

  Path applyPasses(IFCAnalysis ana, SDGConfig cfg, String libClassPath, Path classPath, Path baseDir) throws IOException {
    Path last = classPath;
    int i = 0;
    for (Pass p : passes) {
      last = applyPass(ana, cfg, libClassPath, last, p, baseDir, i++);
    }
    return last;
  }

  Path applyPass(IFCAnalysis ana, SDGConfig cfg, String libClassPath, Path classPath, Pass pass, Path baseDir, int passNumber) throws IOException {
    String passName = pass.getName();
    Path target = baseDir.resolve(String.format("%d_%s", passNumber, passName));
    target.toFile().mkdir();
    logger.info(String.format("Start pass %s (libClassPath='%s', classPath='%s', target='%s')",
        passName, libClassPath, classPath, target));
    pass.process(ana, cfg, libClassPath, classPath, target);
    logger.info("Finish pass " + passName);
    return target;
  }

  void storeClassPathInFolder(String classPath, Path targetFolder) throws IOException {
    logger.info(String.format("Store classes from '%s' into '%s'", classPath, targetFolder));
    if (classPath == null) {
      throw new IllegalArgumentException("null classPath");
    }
    if (!Files.exists(targetFolder)) {
      Files.createDirectories(targetFolder);
    }
    StringTokenizer paths = new StringTokenizer(classPath, File.pathSeparator);
    while (paths.hasMoreTokens()) {
      Path path = Paths.get(paths.nextToken());
      if (path.toFile().isDirectory()) {
        for (Path entry : Files.list(path).collect(Collectors.toList())) {
          if (entry.toFile().isFile()) {
            copyFile(entry, targetFolder);
          } else {
            Path target = targetFolder.resolve(entry.getFileName());
            if (!Files.exists(target)) {
              Files.createDirectory(target);
            }
            FileUtils.copyDirectory(entry.toFile(), target.toFile());
          }
        }
      } else {
        copyFile(path, targetFolder);
      }
    }
  }

  private void copyFile(Path path, Path targetFolder) throws IOException {
    if (path.toString().endsWith(".jar")) {
      copyJar(path, targetFolder);
    } else if (path.toString().endsWith(".class")) {
      Files.copy(path, targetFolder.resolve(path.getFileName()));
    }
  }

  private void copyJar(Path path, Path targetFolder) throws IOException {
    JarFile jar = new JarFile(path.toString(), false);
    try {
      if (jar.getManifest() != null) {
        String cp = jar.getManifest().getMainAttributes().getValue("Class-Path");
        if (cp != null) {
          for(String cpEntry : cp.split("")) {
            storeClassPathInFolder(path.getParent() + File.separator + cpEntry, targetFolder.resolve(cpEntry));
          }
        } else {
          jar.stream().forEach(e -> {
            try {
              Path target = targetFolder.resolve(e.getName());
              if (e.isDirectory()) {
                if (!Files.exists(target)) {
                  Files.createDirectories(target);
                }
              } else if (e.getName().endsWith(".class")) {
                Files.copy(jar.getInputStream(e), target, StandardCopyOption.REPLACE_EXISTING);
              }
            } catch (IOException ioException) {
              ioException.printStackTrace();
            }
          });
        }
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
      System.err.println("warning: trouble processing class path of " + path);
    }
  }

  boolean requiresKnowledgeOnAnnotations(){
    return passes.stream().anyMatch(Pass::requiresKnowledgeOnAnnotations);
  }
}