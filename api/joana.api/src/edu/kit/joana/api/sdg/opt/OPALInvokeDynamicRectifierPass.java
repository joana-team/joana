package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.util.NullPrintStream;
import org.opalj.ba.ToDAConfig;
import org.opalj.ba.package$;
import org.opalj.bc.Assembler;
import org.opalj.br.analyses.Project;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This pass applies basic optimisations of OPAL, e.g. resolving INVOKEDYNAMIC
 *
 * Adapted from {@link org.opalj.support.tools.ProjectSerializer}.
 */
public class OPALInvokeDynamicRectifierPass implements Pass {

  @Override public void process(SDGConfig cfg, String libClassPath, Path sourceFolder, Path targetFolder) throws IOException {
    PrintStream out = System.out;
    System.setOut(new NullPrintStream());
    Project<URL> p = Project.apply(sourceFolder.toFile());
    p.parForeachProjectClassFile(() -> null, cf -> {
      Path classFileFolder = targetFolder.resolve(cf.thisType().packageName());
      try {
        Files.createDirectories(classFileFolder);
        Path classFile = classFileFolder.resolve(cf.thisType().simpleName() + ".class");
        byte[] b = Assembler.apply(package$.MODULE$.toDA(cf, ToDAConfig.RetainAllAttributes()), (x, y) -> null);
        org.opalj.io.package$.MODULE$.process(new BufferedOutputStream(Files.newOutputStream(classFile)), os -> {
          try {
            os.write(b);
          } catch (IOException e) {
            e.printStackTrace();
          }
          return null;
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    });
    System.setOut(out);
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return false;
  }
}