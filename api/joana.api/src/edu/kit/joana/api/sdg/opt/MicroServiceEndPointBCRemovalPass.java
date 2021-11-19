package edu.kit.joana.api.sdg.opt;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.opt.asm.NameCreator;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.TypeNameUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * This pass EndpointMethod marked methods from the microservice-helper class and removes their byte code.
 * The semantic of these methods is known and the removal of their bytecode greatly speeds up the other
 * analyses and passes
 */
public class MicroServiceEndPointBCRemovalPass implements FilePass {

  @Override public void setup(SDGConfig cfg, String libClassPath, Path sourceFolder) {
  }

  @Override public void collect(Path file) throws IOException {
  }

  @Override public void store(Path source, Path target, Path targetBase) throws IOException {
    Files.createDirectories(target.getParent());
    if (source.toString().contains("edu/kit/joana/microservices")) {
      ClassReader cr = new ClassReader(Files.newInputStream(source));
      ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
      ModifierVisitor mod = new ModifierVisitor(cw,
          NameCreator.forClass(new ClassReader(Files.newInputStream(source))),
          findRelevantMethods(new ClassReader(Files.newInputStream(source))));
      cr.accept(mod, ClassReader.EXPAND_FRAMES);
      Files.newOutputStream(target).write(cw.toByteArray());
      Files.newOutputStream(Paths.get("/tmp/bla.class")).write(cw.toByteArray());
      System.out.printf("modified %s %n", target);
    } else {
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** @return {(name, descriptor)} */
  private Set<Pair<String, String>> findRelevantMethods(ClassReader cr) {
    Set<Pair<String, String>> relevantMethods = new HashSet<>();
    cr.accept(new ClassVisitor(ASM8) {
      @Override public MethodVisitor visitMethod(int access, String name, String mdescriptor, String signature,
          String[] exceptions) {
        return new MethodVisitor(ASM8) {
          @Override public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (descriptor.equals("Ledu/kit/joana/microservices/EndpointMethod;")) {
              relevantMethods.add(Pair.pair(name, mdescriptor));
            }
            return null;
          }
        };
      }
    }, 0);
    return relevantMethods;
  }

  @Override public void teardown() {
  }

  class ModifierVisitor extends ClassVisitor {

    private NameCreator names;
    private final Set<Pair<String, String>> relevantMethods;
    private Map<Type, String> nativeMethods;
    private Type type;

    public ModifierVisitor(ClassVisitor cv, NameCreator names, Set<Pair<String, String>> relevantMethods) {
      super(ASM8, cv);
      this.names = names;
      this.relevantMethods = relevantMethods;
      this.nativeMethods = new HashMap<>();
    }

    @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      type = Type.getType("L" + name + ";");
      super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override public MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
        String[] exceptions) {
      if (!relevantMethods.contains(Pair.pair(methodName, descriptor))) {
        return cv.visitMethod(access, methodName, descriptor, signature, exceptions);
      }
      MethodVisitor cvVis = cv.visitMethod(access, methodName, descriptor, signature, exceptions);
      LocalVariablesSorter mv = new LocalVariablesSorter(access, descriptor, cvVis);
      Type returnType = Type.getReturnType(descriptor);
      mv.visitAnnotation("Ledu/kit/joana/microservices/EndpointMethod;", true).visitEnd();
      mv.visitCode();
      if (returnType == Type.VOID_TYPE) {
        mv.visitInsn(RETURN);
      } else {
        String retMethodName = nativeMethods.computeIfAbsent(returnType, t -> names.create(t.toString()));
        mv.visitMethodInsn(INVOKESTATIC, TypeNameUtils.toInternalNameWithoutSemicolonAndL(type), retMethodName, "()" + type, false);
        mv.visitInsn(returnType.getOpcode(IRETURN));
      }
      mv.visitMaxs(1, 2);
      mv.visitEnd();
      return null;
    }
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return false;
  }
}
