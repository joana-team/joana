package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.setter.SearchVisitor;
import edu.kit.joana.setter.SetValueStore;
import edu.kit.joana.setter.Tool;

import java.io.IOException;
import java.nio.file.Path;

/**
 * This pass uses the <a href="git.scc.kit.edu/gp1285/parameter-setter">parameter setter</a> tool
 * to preset the value of method parameters, fields and method returns.
 */
public class SetValuePass implements FilePass {

  protected final Tool tool;

  public SetValuePass(Tool tool) {
    this.tool = tool;
  }

  @Override
  public void setup(String libClassPath) {
  }

  @Override
  public void collect(Path file) throws IOException {
  }

  @Override
  public void store(Path source, Path target) throws IOException {
    tool.applyAnnotations(source, target);
  }

  @Override
  public void teardown() {
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return false;
  }
}
