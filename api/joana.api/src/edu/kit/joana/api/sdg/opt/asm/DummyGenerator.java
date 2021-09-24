package edu.kit.joana.api.sdg.opt.asm;

import edu.kit.joana.api.sdg.opt.OpenApiPreProcessorPass;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.TypeNameUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.objectweb.asm.Opcodes.*;

/**
 * Creator of dummy values
 * <p>
 * Does not have to deal with checked exceptions:
 * https://mail.openjdk.java.net/pipermail/coin-dev/2009-May/001861.html
 * <p>
 * Rewrite of the previous version. This version is based around a generated class with methods for each type.
 */
public class DummyGenerator {

  public static final String MAIN_FUNCTION_NAME = "$$start$$";

  /**
   * maps every type to a method name
   */
  public static class DummyMethodNames {

    /**
     * java class name with package, e.g. java.lang.String
     */
    public final String fullClassName;
    /**
     * java class name
     */
    public final String className;
    private final NameCreator nameCreator = new NameCreator();
    private final Map<Type, String> typeToName = new HashMap<>();
    /**
     * called whenever a new method names is generated
     */
    private final BiConsumer<Type, String> newCallBack;

    public DummyMethodNames(String fullClassName, BiConsumer<Type, String> newCallBack) {
      this.fullClassName = fullClassName;
      this.className = fullClassName.substring(fullClassName.lastIndexOf('/') + 1);
      this.newCallBack = newCallBack;
    }

    public String getMethodName(Type type) {
      return typeToName.computeIfAbsent(type, t -> {
        String newName = nameCreator.create(t.toString());
        newCallBack.accept(t, newName);
        return newName;
      });
    }
  }

  private final OpenApiPreProcessorPass.Config config;
  private final PossibleConstructors possibleConstructors;
  /**
   * cached type tree
   */
  private final PossibleConstructors.TypeTree possibleConstructorsTypeTree;

  public DummyGenerator(OpenApiPreProcessorPass.Config config, PossibleConstructors possibleConstructors) {
    this.config = config;
    this.possibleConstructors = possibleConstructors;
    this.possibleConstructorsTypeTree = possibleConstructors.createTypeTree();
  }

  public BaseMethodDescriptor createClassForTypes(Path basePath, String pkg, NameCreator classNames, Type mainType,
      Set<Type> subTypes) {
    Pair<BaseMethodDescriptor, byte[]> pair = createClassForTypes(pkg, classNames.create("Gen" + mainType), mainType, subTypes);
    Path classFile = basePath.resolve(pair.getFirst().methodClass.replace('.', '/') + ".class");
    try {
      Files.createDirectories(classFile.getParent());
      Files.write(classFile, pair.getSecond());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return pair.getFirst();
  }

  Pair<BaseMethodDescriptor, byte[]> createClassForTypes(String pkg, NameCreator classNames, Type mainType,
      Set<Type> subTypes) {
    return createClassForTypes(pkg, classNames.create("Gen" + mainType), mainType, subTypes);
  }

  Pair<BaseMethodDescriptor, byte[]> createClassForTypes(String pkg, String className, Type mainType, Set<Type> subTypes) {
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    cw.visit(V1_7, ACC_PUBLIC + ACC_FINAL + ACC_SUPER, pkg.replace('.', '/') + "/" + className, null, "java/lang/Object", null);
    String fullClassName = pkg + "." + className;
    createDefaultConstructor(cw, fullClassName);
    String methodName = createMethodsForTypes(fullClassName, (returnType, name) -> {
      Type methodType = Type.getMethodType(returnType);
      return new LocalVariablesSorter(ACC_PUBLIC + ACC_STATIC, methodType.getDescriptor(),
          cw.visitMethod(ACC_PUBLIC + ACC_STATIC, name, methodType.getDescriptor(), null, null));
    }, mainType, subTypes);
    cw.visitEnd();
    return Pair.pair(new BaseMethodDescriptor(fullClassName, methodName, Type.getMethodDescriptor(mainType)), cw.toByteArray());
  }

  private void createDefaultConstructor(ClassWriter cw, String fullClassName) {
    MethodVisitor methodVisitor = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    methodVisitor.visitCode();
    Label label0 = new Label();
    methodVisitor.visitLabel(label0);
    methodVisitor.visitLineNumber(5, label0);
    methodVisitor.visitVarInsn(ALOAD, 0);
    methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    methodVisitor.visitInsn(RETURN);
    Label label1 = new Label();
    methodVisitor.visitLabel(label1);
    methodVisitor.visitLocalVariable("this", TypeNameUtils.toInternalName(fullClassName), null, label0, label1, 0);
    methodVisitor.visitMaxs(1, 1);
    methodVisitor.visitEnd();
  }

  /**
   * creates a method visitor
   */
  @FunctionalInterface public interface MethodCreator {
    LocalVariablesSorter create(Type returnType, String name);
  }

  /**
   * returns the name of the method that produces the dummy values for the main type
   */
  String createMethodsForTypes(String fullClassName, MethodCreator methodCreator, Type mainType, Set<Type> subTypes) {
    Set<Pair<Type, String>> newNames = new HashSet<>();
    DummyMethodNames names = new DummyMethodNames(fullClassName, (t, n) -> newNames.add(Pair.pair(t, n)));
    createMethod(methodCreator.create(mainType, MAIN_FUNCTION_NAME), names, mainType, Optional.of(subTypes));
    while (!newNames.isEmpty()) {
      Set<Pair<Type, String>> oldNewNames = new HashSet<>(newNames);
      newNames.clear();
      oldNewNames.forEach(p -> createMethod(methodCreator.create(p.getFirst(), p.getSecond()), names, mainType, Optional.empty()));
    }
    return MAIN_FUNCTION_NAME;
  }

  void createMethod(LocalVariablesSorter mv, DummyMethodNames methodNames, Type type, Optional<Set<Type>> subTypes) {
    if (type.getSort() == Type.ARRAY) {
      assert !subTypes.isPresent() || (subTypes.get().size() == 1 && subTypes.get().contains(type));
      createArrayMethod(mv, methodNames, type);
    } else if (type.getSort() == Type.OBJECT) {
      if (subTypes.isPresent() && subTypes.get().size() > 0) {
        createObjectMethod(mv, methodNames, type, subTypes.get());
      } else {
        createObjectMethod(mv, methodNames, type);
      }
      createObjectMethod(mv, methodNames, type);
    } else {
      createPrimitiveMethod(mv, methodNames, type);
    }
    mv.visitMaxs(-1, -1);
  }

  /**
   * idea: T[] a = new T[]{T}, does not take alias analyses into account yet
   */
  private void createArrayMethod(LocalVariablesSorter mv, DummyMethodNames methodNames, Type type) {
    mv.visitCode();
    Type subElementType = Type.getType(
        IntStream.range(0, type.getDimensions() - 1).mapToObj(i -> "[").collect(Collectors.joining("")) + type.getElementType()
            .getDescriptor());
    mv.visitIntInsn(LDC, 1);
    mv.visitTypeInsn(ANEWARRAY, subElementType.getDescriptor());
    mv.visitInsn(DUP);
    mv.visitInsn(ICONST_0);
    pushDummy(mv, methodNames, subElementType);
    mv.visitInsn(type.getOpcode(IASTORE));
    mv.visitInsn(ARETURN);
    mv.visitEnd();
  }

  private void createObjectMethod(LocalVariablesSorter mv, DummyMethodNames methodNames, Type type, Set<Type> subTypes) {
    mv.visitCode();
    randomDecision(mv, type,
        subTypes.stream().map(t -> (Consumer<LocalVariablesSorter>) (imv -> pushDummyObjectCall(imv, methodNames, t)))
            .collect(Collectors.toList()));
    mv.visitInsn(ARETURN);
    mv.visitEnd();
  }

  private void createObjectMethod(LocalVariablesSorter mv, DummyMethodNames methodNames, Type type) {
    mv.visitCode();
    Stream<Consumer<LocalVariablesSorter>> directConsumers = possibleConstructors.hasConstructors(type) ?
        possibleConstructors.forType(type).methodDescriptorStream()
            .map(d -> (Consumer<LocalVariablesSorter>) (imv -> callConstructor(imv, methodNames, d))) :
        Stream.empty();
    Stream<Consumer<LocalVariablesSorter>> childConsumers = possibleConstructorsTypeTree.getSubTypes(type).stream()
        .map(t -> (Consumer<LocalVariablesSorter>) (imv -> {
          pushDummyObjectCall(imv, methodNames, t);
        }));
    randomDecision(mv, type, Stream.concat(directConsumers, childConsumers).collect(Collectors.toList()));
    mv.visitInsn(ARETURN);
    mv.visitEnd();
  }

  /**
   * does not look for subtypes
   */
  private void createObjectMethodDirectly(LocalVariablesSorter mv, DummyMethodNames methodNames, Type type) {
    mv.visitCode();
    randomDecision(mv, type, possibleConstructors.forType(type).methodDescriptorStream()
        .map(d -> (Consumer<LocalVariablesSorter>) (imv -> callConstructor(imv, methodNames, d))).collect(Collectors.toList()));
    mv.visitInsn(ARETURN);
    mv.visitEnd();
  }

  /**
   * accepts primitive types or object types
   */
  private void createPrimitiveMethod(LocalVariablesSorter mv, DummyMethodNames methodNames, Type type) {
    mv.visitCode();
    assert !(type.getSort() == Type.ARRAY || type.getSort() == Type.OBJECT) || type.equals(Type.getType(Object.class));
    pushDummy(mv, methodNames, type);
    mv.visitInsn(ARETURN);
    mv.visitEnd();
  }

  private void randomDecision(LocalVariablesSorter mv, Type type, List<Consumer<LocalVariablesSorter>> possibilities) {
    assert possibilities.size() > 0;
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "random", "()D", false);
    int local = mv.newLocal(type);
    mv.visitLdcInsn(possibilities.size());
    mv.visitInsn(DMUL);
    mv.visitInsn(D2I);
    int randLocal = mv.newLocal(Type.INT_TYPE);
    mv.visitVarInsn(ISTORE, randLocal);
    int i = 0;
    for (Consumer<LocalVariablesSorter> possibility : possibilities) {
      org.objectweb.asm.Label end = new org.objectweb.asm.Label();
      mv.visitVarInsn(ILOAD, randLocal);
      mv.visitLdcInsn(i);
      mv.visitJumpInsn(IF_ICMPNE, end);
      possibility.accept(mv);
      mv.visitJumpInsn(GOTO, end);
      mv.visitLabel(end);
      i++;
    }
    mv.visitVarInsn(ALOAD, local);
  }

  /**
   * creates a call that pushes a dummy instance on the stack, does not create a call for primitive values (and Strings)
   */
  private void pushDummy(MethodVisitor mv, DummyMethodNames methodNames, Type type) {
    if (type.getSort() == Type.OBJECT) {
      if (type.equals(Type.getType(String.class))) {
        mv.visitLdcInsn("n");
      } else if (type.equals(Type.getType(Object.class))) {
        mv.visitInsn(ACONST_NULL); // hopefully okay
      } else {
        pushDummyObjectCall(mv, methodNames, type);
      }
    }
    if (type.getSort() == Type.ARRAY) {
      pushDummyObjectCall(mv, methodNames, type);
    } else {
      mv.visitInsn(type.getOpcode(ICONST_1));
    }
  }

  /**
   * creates a call that pushes a dummy instance on the stack, does not create a call for primitive values (and Strings)
   */
  private void pushDummyObjectCall(MethodVisitor mv, DummyMethodNames methodNames, Type type) {
    mv.visitMethodInsn(INVOKESTATIC, TypeNameUtils.toInternalNameWithoutSemicolonAndL(methodNames.fullClassName),
        methodNames.getMethodName(type), "()" + type.getDescriptor(), false);
  }

  private void callConstructor(LocalVariablesSorter mv, DummyMethodNames methodNames, BaseMethodDescriptor constructor) {
    mv.visitTypeInsn(NEW, constructor.methodClass);
    mv.visitInsn(DUP);
    for (Type param : Type.getMethodType(constructor.methodDescriptor).getArgumentTypes()) {
      pushDummy(mv, methodNames, param);
    }
    mv.visitMethodInsn(INVOKESPECIAL, constructor.methodClass, "<init>", constructor.methodDescriptor, false);
  }
}