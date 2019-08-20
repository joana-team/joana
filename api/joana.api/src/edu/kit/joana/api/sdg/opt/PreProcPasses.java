package edu.kit.joana.api.sdg.opt;

import com.ibm.wala.util.debug.Assertions;
import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.SDGProgram;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;
import java.util.function.Function;
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

  /**
   * @return new class path (library path : new byte code path)
   */
  public String process(IFCAnalysis ana, String libClassPath, String classPath) throws IOException {
    Path tempDirectory = Files.createTempDirectory("joana-passes");
    tempDirectory.toFile().deleteOnExit();
    Path baseDirectory = tempDirectory.resolve("base");
    storeClassPathInFolder(classPath, baseDirectory);
    baseDirectory.toFile().mkdir();
    Path result = applyPasses(ana, libClassPath, baseDirectory, tempDirectory);
    return (libClassPath.isEmpty() ? "" : (libClassPath + ":")) + result.toString();
  }

  public IFCAnalysis processAndUpdateSDG(IFCAnalysis ana, String libClassPath, String classPath, Function<String, SDGProgram> programCreator)
      throws IOException {
    ana.setProgram(programCreator.apply(process(ana, libClassPath, classPath)));
    return ana;
  }

  Path applyPasses(IFCAnalysis ana, String libClassPath, Path classPath, Path baseDir) throws IOException {
    Path last = classPath;
    int i = 0;
    for (Pass p : passes) {
      last = applyPass(ana, libClassPath, last, p, baseDir, i++);
    }
    return last;
  }

  Path applyPass(IFCAnalysis ana, String libClassPath, Path classPath, Pass pass, Path baseDir, int passNumber) throws IOException {
    String passName = pass.getClass().getSimpleName();
    Path target = baseDir.resolve(String.format("%d_%s", passNumber, passName));
    target.toFile().mkdir();
    logger.info(String.format("Start pass %s (libClassPath='%s', classPath='%s', target='%s')",
        passName, libClassPath, classPath, target));
    pass.process(ana, libClassPath, classPath, target);
    logger.info("Finish pass " + passName);
    return target;
  }

  void storeClassPathInFolder(String classPath, Path targetFolder) throws IOException {
    logger.info(String.format("Store classes from '%s' into '%s'", classPath, targetFolder));
    if (classPath == null) {
      throw new IllegalArgumentException("null classPath");
    }
    StringTokenizer paths = new StringTokenizer(classPath, File.pathSeparator);
    while (paths.hasMoreTokens()) {
      String path = paths.nextToken();
      if (path.endsWith(".jar")) {
        JarFile jar = new JarFile(path, false);
        try {
          if (jar.getManifest() != null) {
            String cp = jar.getManifest().getMainAttributes().getValue("Class-Path");
            if (cp != null) {
              for(String cpEntry : cp.split("")) {
                storeClassPathInFolder(new File(path).getParent() + File.separator + cpEntry, targetFolder.resolve(cpEntry));
              }
            }
          }
        } catch (RuntimeException e) {
          e.printStackTrace();
          System.err.println("warning: trouble processing class path of " + path);
        }
      } else {
        FileUtils.copyDirectory(Paths.get(path).toFile(), targetFolder.toFile());
      }
    }
  }

  boolean requiresKnowledgeOnAnnotations(){
    return passes.stream().anyMatch(Pass::requiresKnowledgeOnAnnotations);
  }
}
