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
    if (!Logger.getGlobal().isLoggable(Level.FINEST)) {
      PrintStream out = System.out;
      System.setOut(new NullPrintStream());
      execute(ana, libClassPath, sourceFolder, targetFolder);
      System.setOut(out);
    } else {
      execute(ana, libClassPath, sourceFolder, targetFolder);
    }
  }

  private void execute(IFCAnalysis ana, String libClassPath, Path sourceFolder, Path targetFolder){
    String fullLibClassPath = System.getProperty("java.class.path");
    if (libClassPath.length() > 0) {
      fullLibClassPath += ":" + libClassPath;
    }
    List<String> args = new ArrayList<>(Arrays.asList(
        "-injars", sourceFolder.toString(), "-libraryjars", fullLibClassPath,
        "-dontwarn", "-dontshrink", "-dontnote", "-ignorewarnings",
        "-keep", "class Basic { void bla(int); }", "-keep", "class Basic { void blub(int); }",
        "-dontobfuscate", "-outjars", targetFolder.toString(),
        "-dontwarn", "**", "-skipnonpubliclibraryclasses"
    ));
    if (ana.getProgram().hasDefinedEntryMethod()){
      partsToSpec(partsToKeep(ana)).stream().forEach(p -> {
        args.add("-keep");
        args.add(p);
      });
    } else {
      args.add("-dontoptimize");
    }
    execute(args.toArray(new String[0]));
  }

  private void execute(String[] args){
    // Source: proguard.ProGuard

    // Create the default options.
    Configuration configuration = new Configuration();

    try
    {
      // Parse the options specified in the command line arguments.
      ConfigurationParser parser = new ConfigurationParser(args,
          System.getProperties());
      try
      {
        parser.parse(configuration);
      }
      finally
      {
        parser.close();
      }

      // Execute ProGuard with these options.
      new ProGuard(configuration).execute();
    }
    catch (Exception ex)
    {
      if (configuration.verbose)
      {
        // Print a verbose stack trace.
        ex.printStackTrace();
      }
      else
      {
        // Print just the stack trace message.
        System.err.println("Error: "+ex.getMessage());
      }
    }
  }

  private List<String> partsToSpec(List<SDGProgramPart> parts){

    SDGProgramPartVisitor<String, Object> visitor = new SDGProgramPartVisitor<String, Object>() {

      @Override
      protected String visitClass(SDGClass cl, Object data) {
          return forJavaType(cl.getTypeName(), "");
      }
      
      String forJavaType(JavaType type, String inner){
        return String.format("class %s { %s }", type.toHRString(), inner);
      }

      @Override
      protected String visitAttribute(SDGAttribute a, Object data) {
        return forJavaType(a.getDeclaringType(), String.format("%s %s;", a.getDeclaringType().toHRString(), a.getName()));
      }

      @Override
      protected String visitMethod(SDGMethod m, Object data) {
        return forJavaType(m.getSignature().getDeclaringType(), String.format("%s %s(%s);",
            m.getSignature().getReturnType().toHRString(),
            m.getSignature().getMethodName(), m.getSignature().getArgumentTypes().stream().map(JavaType::toHRString).collect(Collectors.joining(","))));
      }

      @Override
      protected String visitActualParameter(SDGActualParameter ap, Object data) {
        return visitMethod(ap.getOwningMethod(), null);
      }

      @Override
      protected String visitParameter(SDGFormalParameter p, Object data) {
        return visitMethod(p.getOwningMethod(), null);
      }

      @Override
      protected String visitExit(SDGMethodExitNode e, Object data) {
        return visitMethod(e.getOwningMethod(), null);
      }

      @Override
      protected String visitException(SDGMethodExceptionNode e, Object data) {
        return visitMethod(e.getOwningMethod(), null);
      }

      @Override
      protected String visitInstruction(SDGInstruction i, Object data) {
        return visitMethod(i.getOwningMethod(), null);
      }

      @Override
      protected String visitCall(SDGCall c, Object data) {
        return visitMethod(c.getOwningMethod(), null);
      }

      @Override
      protected String visitCallReturnNode(SDGCallReturnNode c, Object data) {
        return visitMethod(c.getOwningMethod(), null);
      }

      @Override
      protected String visitCallExceptionNode(SDGCallExceptionNode c, Object data) {
        return visitMethod(c.getOwningMethod(), null);
      }

      @Override
      protected String visitPhi(SDGPhi phi, Object data) {
        return visitMethod(phi.getOwningMethod(), null);
      }

      @Override
      protected String visitFieldOfParameter(SDGFieldOfParameter fop, Object data) {
        return visitMethod(fop.getOwningMethod(), null);
      }

      @Override
      protected String visitLocalVariable(SDGLocalVariable local, Object data) {
        return visitMethod(local.getOwningMethod(), null);
      }
    };
    return parts.stream().map(p -> p.acceptVisitor(visitor, null)).collect(Collectors.toList());
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
    ProGuard.main(new String[]{
        "-injars", "/tmp/dir/Basic.class", "-libraryjars", "/usr/lib/jvm/java-8-openjdk-amd64/jre/lib/rt.jar",
        "-dontwarn", "-dontshrink", "-keep", "class Basic { void bla(int); }", "-keep", "class Basic { void blub(int); }", "-dontobfuscate", "-outjars",
        "/tmp/dir/bla"
    });
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return true;
  }
}
