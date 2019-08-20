package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.setter.misc.AnnotatedEntityFinder;
import edu.kit.joana.util.NullPrintStream;
import proguard.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Uses the <a href="https://www.guardsquare.com/en/products/proguard">ProGuard</a> tool to optimize byte code.
 */
public class ProGuardPass implements Pass {

  @Override
  public void process(IFCAnalysis ana, String libClassPath, Path sourceFolder, Path targetFolder)
      throws IOException {
    Configuration config = createConfig(ana, libClassPath, sourceFolder, targetFolder);
    ProGuard proGuard = new ProGuard(config);
    if (!config.verbose) {
      PrintStream out = System.out;
      System.setOut(new NullPrintStream());
      proGuard.execute();
      System.setOut(out);
    } else {
      proGuard.execute();
    }
  }

  Configuration createConfig(IFCAnalysis ana, String libClassPath, Path sourceFolder, Path targetFolder){
    Configuration configuration = new Configuration();
    ClassPath sfCP = new ClassPath();
    sfCP.add(new ClassPathEntry(sourceFolder.toFile(), false));
    sfCP.add(new ClassPathEntry(targetFolder.toFile(), true));
    configuration.programJars = sfCP;
    String fullLibClassPath = System.getProperty("java.class.path");
    if (libClassPath.length() > 0) {
      fullLibClassPath += ":" + libClassPath;
    }
    ClassPath libCP = new ClassPath();
    Arrays.stream(fullLibClassPath.split(":")).map(c -> new ClassPathEntry(new File(c), false)).forEach(libCP::add);
    configuration.libraryJars = libCP;
    configuration.keepDirectories = Collections.singletonList("..");
    configuration.keep = createKeepMemberSpecifications(ana);
    configuration.obfuscate = false;
    configuration.verbose = Logger.getGlobal().isLoggable(Level.FINEST);
    configuration.warn = Collections.singletonList("**");
    configuration.ignoreWarnings = true;
    configuration.skipNonPublicLibraryClasses = true;
    configuration.skipNonPublicLibraryClassMembers = true;
    configuration.optimize = true;
    configuration.note = Collections.singletonList("**");
    return configuration;
  }

  private List<KeepClassSpecification> createKeepMemberSpecifications(IFCAnalysis ana){
    if (keepAllMembers(ana)){
      return Collections.singletonList(new KeepClassSpecification(true, false, true, true, false, true, false, null, new ClassSpecification(null, 0, 0, null, "**", null, null)));
    }
    return partsToSpec(partsToKeep(ana));
  }

  private List<KeepClassSpecification> partsToSpec(List<SDGProgramPart> parts){

    Map<String, KeepClassSpecification> classSpecs = new HashMap<>();

    SDGProgramPartVisitor<Object, Object> visitor = new SDGProgramPartVisitor<Object, Object>() {

      @Override
      protected Object visitClass(SDGClass cl, Object data) {
        addForJavaType(cl.getTypeName());
          return null;
      }
      
      KeepClassSpecification addForJavaType(JavaType type){
        return classSpecs.computeIfAbsent(type.toHRString(),
            k -> new KeepClassSpecification(true, false, true, true, false, false, false, null, new ClassSpecification(null, 0, 0, null, type.toHRString().replace(".", "/"), null, null)));
      }

      @Override
      protected Object visitAttribute(SDGAttribute a, Object data) {
        addForJavaType(a.getDeclaringType()).addField(new MemberSpecification(0, 0, null, a.getName(), null));
        return null;
      }

      @Override
      protected Object visitMethod(SDGMethod m, Object data) {
        KeepClassSpecification cs = addForJavaType(m.getSignature().getDeclaringType());
        cs.addMethod(new MemberSpecification(0, 0, null, m.getSignature().getMethodName(), m.getSignature().getSelector().substring(m.getSignature().getMethodName().length())));
        return null;
      }

      @Override
      protected Object visitActualParameter(SDGActualParameter ap, Object data) {
        visitMethod(ap.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitParameter(SDGFormalParameter p, Object data) {
        visitMethod(p.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitExit(SDGMethodExitNode e, Object data) {
        visitMethod(e.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitException(SDGMethodExceptionNode e, Object data) {
        visitMethod(e.getOwningMethod(), null);
        return null;
      }

      @Override protected Object visitInstruction(SDGInstruction i, Object data) {
        visitMethod(i.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitCall(SDGCall c, Object data) {
        visitMethod(c.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitCallReturnNode(SDGCallReturnNode c, Object data) {
        visitMethod(c.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitCallExceptionNode(SDGCallExceptionNode c, Object data) {
        visitMethod(c.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitPhi(SDGPhi phi, Object data) {
        visitMethod(phi.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitFieldOfParameter(SDGFieldOfParameter fop, Object data) {
        visitMethod(fop.getOwningMethod(), null);
        return null;
      }

      @Override
      protected Object visitLocalVariable(SDGLocalVariable local, Object data) {
        visitMethod(local.getOwningMethod(), null);
        return null;
      }
    };
    parts.forEach(p -> p.acceptVisitor(visitor, null));
    return new ArrayList<>(classSpecs.values());
  }

  boolean keepAllMembers(IFCAnalysis ana){
    return !ana.getProgram().hasDefinedEntryMethod();
  }

  List<SDGProgramPart> partsToKeep(IFCAnalysis ana){
    List<SDGProgramPart> parts = new ArrayList<>();
    ana.getAnnotations().stream().map(a -> a.getProgramPart()).forEach(parts::add);
    if (ana.getProgram().hasDefinedEntryMethod()){
      parts.add(ana.getProgramPart(ana.getProgram().getEntryMethod()));
    }
    return parts;
  }

  public static void main(String[] args) throws IOException, ParseException {
    Configuration conf = new Configuration();
    new ConfigurationParser("-keep class Basic {\n" + " public void bla(int);\n" + "}", "", new File("."), System.getProperties()).parse(conf);
    System.out.println(conf.keep);
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return true;
  }
}
