package edu.kit.joana.api.sdg.opt;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.collections.Iterator2List;
import com.ibm.wala.util.strings.Atom;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.api.sdg.opt.asm.BaseMethodDescriptor;
import edu.kit.joana.api.sdg.opt.asm.DeletingMethodVisitor;
import edu.kit.joana.api.sdg.opt.asm.HeuristicReflectionFinder;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.TypeNameUtils;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.openapi.OpenApiClientDetector;
import edu.kit.joana.wala.util.NotImplementedException;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import nonapi.io.github.classgraph.classpath.SystemJarFinder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.objectweb.asm.Opcodes.*;

/**
 * This pass identifies OpenAPI client methods and replaces their return values with dummy return values
 */
public class OpenApiPreProcessorPass implements FilePass {

  // all stringified class names that are stored in the following are java class names


  public static class Config {

    public static final Config DEFAULT = new Config(true, true);

    public final boolean deleteRestOfMethod;
    public final boolean deleteConstructorBody;

    public Config(boolean deleteRestOfMethod, boolean deleteConstructorBody) {
      this.deleteRestOfMethod = deleteRestOfMethod;
      this.deleteConstructorBody = deleteConstructorBody;
    }
  }

  protected final OpenApiClientDetector detector;
  /** java class name → info */
  protected Map<String, ClassInfo> classInfoMap;
  private SDGBuilder.CGResult cgr;
  private final boolean debug = true;

  private final Config config;

  /** java names of detected open api classes */
  private Set<String> openApiClasses;

  /** Types that might be used in reflection. An approximation for GSON related reflection. */
  private Set<ClassInfo> typesUsedInReflection;

  public OpenApiPreProcessorPass(OpenApiClientDetector detector, Config config) {
    this.detector = detector;
    this.config = config;
  }

  @Override public void setup(SDGConfig cfg, String libClassPath, Path sourceFolder) {
    classInfoMap = new ClassGraph().enableMethodInfo().overrideClasspath(
        libClassPath + (libClassPath.isEmpty() ? "" : ":") + SystemJarFinder.getJreRtJarPath() + ":" + sourceFolder)
        .enableSystemJarsAndModules().enableInterClassDependencies().scan().getAllClassesAsMap();
    try {
      cfg.setClassPath(sourceFolder.toString());
      cfg.setPointsToPrecision(SDGBuilder.PointsToPrecision.TYPE_BASED);
      cgr = SDGProgram.buildCallGraph(cfg);

      openApiClasses = cgr.cg.getClassHierarchy().getClasses().stream().filter(detector::isReallyOpenOpiClass)
          .map(TypeNameUtils::toJavaClassName)
          .collect(Collectors.toSet());
      checkForCalledNonOpenApiMethodsOfOpenApiClasses(cgr.cg);
      HeuristicReflectionFinder heuristicReflectionFinder = new HeuristicReflectionFinder(sourceFolder, cgr, classInfoMap);
      heuristicReflectionFinder.run();
      typesUsedInReflection = heuristicReflectionFinder.getFoundTypes().stream().map(TypeNameUtils::toJavaClassName).map(classInfoMap::get).collect(
          Collectors.toSet());
    } catch (CallGraphBuilderCancelException | ClassHierarchyException | IOException e) {
      e.printStackTrace();
    }
  }

  public static class OpenApiInvariantException extends RuntimeException {
    private List<IMethod> calledNonOpenApiMethodsOfOpenApiClasses;

    public OpenApiInvariantException(List<IMethod> calledNonOpenApiMethodsOfOpenApiClasses) {
      super("The following are non open api methods of open api classes that are called by non open api class methods: " +
          calledNonOpenApiMethodsOfOpenApiClasses.stream().map(Object::toString).collect(Collectors.joining(", ")));
      this.calledNonOpenApiMethodsOfOpenApiClasses = calledNonOpenApiMethodsOfOpenApiClasses;
    }
  }

  /** checks for non open api methods of open api classes that are called by non open api class methods
   * (excludes constructor methods, as they might be called and are handled somewhat properly)
   *
   * @throws OpenApiInvariantException
   */
  private void checkForCalledNonOpenApiMethodsOfOpenApiClasses(CallGraph cg) {
    List<IMethod> violatingMethods = StreamSupport.stream(cg.spliterator(), false).filter(
            node -> openApiClasses.contains(node.getMethod().getDeclaringClass().getName().toString()) && // method is in open api class
                !detector.isWrappableOpenApiMethod(node.getMethod()) && !node.getMethod().isInit() && !node.getMethod().isClinit()
                && new Iterator2List<>(cg.getPredNodes(node), new ArrayList<>()).stream()   // checks for callers
                .anyMatch(caller -> !openApiClasses.contains(caller.getMethod().getDeclaringClass().getName().toString())))
        .map(CGNode::getMethod).collect(Collectors.toList());
    if (violatingMethods.size() > 0) {
      throw new OpenApiInvariantException(violatingMethods);
    }
  }

  /** important: reflection is handled properly here */
  private boolean isCalled(MethodDescriptor method) {
    return method.toRef().map(ref -> !cgr.cg.getNodes(ref).isEmpty()).orElse(false);
  }

  public static Pair<Type, String> cgNodeToTypeMethodDescriptorPair(CGNode n) {
    return Pair.pair(Type.getType(TypeNameUtils.toInternalName(TypeNameUtils.toJavaClassName(n.getMethod().getDeclaringClass()))),
        n.getMethod().getDescriptor().toString());
  }

  public static <F, S> Collector<Pair<F, S>, ?, Map<F, Set<S>>> pairGroupingCollector() {
    return Collectors.groupingBy(Pair::getFirst, new Collector<Pair<F, S>, Set<S>, Set<S>>() {
      @Override public Supplier<Set<S>> supplier() {
        return HashSet::new;
      }

      @Override public BiConsumer<Set<S>, Pair<F, S>> accumulator() {
        return (x, y) -> x.add(y.getSecond());
      }

      @Override public BinaryOperator<Set<S>> combiner() {
        return (BinaryOperator<Set<S>>) Collectors.toSet().combiner();
      }

      @Override public Function<Set<S>, Set<S>> finisher() {
        return x -> x;
      }

      @Override public Set<Characteristics> characteristics() {
        return new HashSet<>(Arrays.asList(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH));
      }
    });
  }

  private class MethodDescriptor extends BaseMethodDescriptor {

    private MethodDescriptor(String methodClass, String methodName, String methodDescriptor) {
      super(methodClass, methodName, methodDescriptor);
    }

    private Optional<MethodReference> toRef() {
      return Optional.ofNullable(cgr.cg.getClassHierarchy().lookupClass(TypeNameUtils.toInternalName(methodClass))).map(klass ->
          klass.getMethod(new Selector(Atom.findOrCreateUnicodeAtom(methodName), Descriptor.findOrCreateUTF8(methodDescriptor))))
          .map(IMethod::getReference);
    }

    /** might return an empty set */
    private Set<CGNode> nodes() {
      return toRef().map(ref -> cgr.cg.getNodes(ref)).orElse(Collections.emptySet());
    }

    @Override public String toString() {
      return new StringJoiner(", ", MethodDescriptor.class.getSimpleName() + "[", "]").add("methodClass='" + methodClass + "'")
          .add("methodName='" + methodName + "'").add("methodDescriptor='" + methodDescriptor + "'").toString();
    }
  }

  /** might return an empty set */
  private Set<Type> getReturnTypes(MethodDescriptor methodDescriptor) {
    return methodDescriptor.nodes().stream().flatMap(n -> {
      System.out.println(cgr.pts.getHeapModel().getPointerKeyForReturnValue(n));
      return cgr.pts.getPointsToSet(cgr.pts.getHeapModel().getPointerKeyForReturnValue(n)).stream()
          .map(i -> Type.getType(i.getConcreteType().getName() + ";"));
    }).collect(Collectors.toSet());
  }

  public static <T> Collection<T> iteratorToCollection(Iterator<T> iterator) {
    List<T> l = new ArrayList<>();
    iterator.forEachRemaining(l::add);
    return l;
  }

  /**
   * Augments the possible constructors
   */
  class PossibleConstructors {
    final Map<Type, Set<String>> map;

    PossibleConstructors(Map<Type, Set<String>> map) {
      this.map = map;
    }

    PossibleConstructors forTypes(Collection<Type> types) {
      Map<Type, Set<String>> out = new HashMap<>();
      for (Type type : types) {
        forType(type, out);
      }
      return new PossibleConstructors(out);
    }

    PossibleConstructors forType(Type type) {
      Map<Type, Set<String>> out = new HashMap<>();
      forType(type, out);
      return new PossibleConstructors(out);
    }

    private void forType(Type type, Map<Type, Set<String>> out) {
      // find all implementing types
      ClassInfo classInfo = classInfoMap.get(TypeNameUtils.toJavaClassName(type));
      (classInfo.isInterface() ? classInfo.getClassesImplementing() : classInfo.getSubclasses()).stream()
          .map(c -> Type.getType(TypeNameUtils.javaClassNameToInternalName(c.getName()))).filter(t -> map.containsKey(t) && !out.containsKey(t))
          .forEach(t -> out.put(t, map.get(t)));
      if (classInfo.isStandardClass() && !classInfo.isAbstract() && map.containsKey(type) && !out.containsKey(type)) {
        out.put(type, map.get(type));
      }
    }

    @Override public String toString() {
      return map.toString();
    }
  }

  /**
   * Find a subset of the passed set of types so that the set does contain all types that have no subclasses in the set
   */
  private Set<Type> findUnrelatedSet(Set<Type> types) {
    Map<ClassInfo, Type> pairs = types.stream().map(t -> Pair.pair(t, classInfoMap.get(TypeNameUtils.toJavaClassName(t))))
        .filter(p -> p.getSecond() != null).collect(Collectors.toMap(Pair::getSecond, Pair::getFirst));
    Set<ClassInfo> removed = new HashSet<>();
    return pairs.keySet().stream().filter(c -> {
      if (c.getSuperclasses().stream().anyMatch(removed::contains) || c.getSuperclasses().stream()
          .anyMatch(pairs.keySet()::contains)) {
        removed.add(c);
        return false;
      }
      return true;
    }).map(pairs::get).collect(Collectors.toSet());
  }

  /**
   * Returns the possible constructors called (transitively) in the passed method
   *
   * @param methodDescriptor
   * @return
   */
  private PossibleConstructors possibleConstructorsCalledIn(MethodDescriptor methodDescriptor) {
    return new PossibleConstructors(worklist(methodDescriptor.nodes(),
        n -> iteratorToCollection(cgr.cg.getSuccNodes(n)).stream().filter(c -> c.getMethod().isInit())
            .map(OpenApiPreProcessorPass::cgNodeToTypeMethodDescriptorPair).collect(Collectors.toList()),
        n -> iteratorToCollection(cgr.cg.getSuccNodes(n))).stream().collect(pairGroupingCollector()));
  }

  public static <N, R> Set<R> worklist(Iterable<N> init, Function<N, Iterable<R>> process, Function<N, Iterable<N>> next) {
    Set<R> set = new HashSet<>();
    worklist(init, n -> {
      if (StreamSupport.stream(process.apply(n).spliterator(), false).filter(set::add).count() > 0) {
        return next.apply(n);
      }
      return Collections.emptySet();
    });
    return set;
  }

  public static <N> void worklist(Iterable<N> init, Function<N, Iterable<N>> next) {
    Set<N> alreadyConsidered = new HashSet<>();
    Queue<N> worklist = new ArrayDeque<>();
    Consumer<N> push = n -> {
      if (!alreadyConsidered.contains(n)) {
        alreadyConsidered.add(n);
        worklist.add(n);
      }
    };
    init.forEach(push);
    while (!worklist.isEmpty()) {
      N cur = worklist.poll();
      next.apply(cur).forEach(push);
    }
  }

  @Override public void collect(Path file) throws IOException {
  }

  @Override public void store(Path source, Path target) throws IOException {
    if (debug && false) {
      if (!source.endsWith("Node.class")) {
        return;
      }
      target = Paths.get("/tmp/Node.class");
    }
    ClassReader cr = new ClassReader(Files.newInputStream(source));
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ModifierVisitor mod = new ModifierVisitor(cw, detector);
    cr.accept(mod, ClassReader.EXPAND_FRAMES);
    Files.createDirectories(target.getParent());
    Files.newOutputStream(target).write(cw.toByteArray());
  }

  class ModifierVisitor extends ClassVisitor {

    private final OpenApiClientDetector detector;
    /** important: "L$klass;" is the real internal name */
    private String klass;
    private String javaKlassName;
    private Set<String> usedMethodNames;

    public ModifierVisitor(ClassVisitor cv, OpenApiClientDetector detector) {
      super(ASM8, cv);
      this.detector = detector;
    }

    @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.klass = name;
      this.javaKlassName = TypeNameUtils.toJavaClassName("L" + name + ";");
      ClassInfo classInfo = classInfoMap.get(javaKlassName);
      this.usedMethodNames = classInfo != null ? new HashSet<>(classInfo.getMethodInfo().getNames()) : new HashSet<>();
      super.visit(version, access, name, signature, superName, interfaces);
    }

    private String createNewName(Type retType) {
      String candidate = "____$$__gen" + retType.toString().replaceAll("[^a-zA-Z]", "");
      if (usedMethodNames == null) {
        return candidate + Math.random();
      }
      while (usedMethodNames.contains(candidate)) {
        candidate += usedMethodNames.size();
      }
      usedMethodNames.add(candidate);
      return candidate;
    }

    @Override public org.objectweb.asm.MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
        String[] exceptions) {
      MethodDescriptor descr = new MethodDescriptor(javaKlassName, methodName, descriptor);
      if (openApiClasses.contains(javaKlassName) && methodName.equals("<init>") && config.deleteConstructorBody) { // a constructor
        return new DeletingMethodVisitor(api, true);
      }
      if (!isCalled(descr) && !debug) {
        return cv.visitMethod(access, methodName, descriptor, signature, exceptions);
      }
      if (!openApiClasses.contains(javaKlassName) ||
          (access & ACC_PUBLIC) == 0 || (access & ACC_STATIC) != 0 || (access & ACC_ABSTRACT) != 0 || (access & ACC_NATIVE) != 0
          || !detector.isWrappableOpenApiMethod(javaKlassName, methodName, descriptor, signature, exceptions)) {
        if (!debug) {
          return cv.visitMethod(access, methodName, descriptor, signature, exceptions);
        }
      }
      System.out.println("Transformed method " + methodName);
      org.objectweb.asm.MethodVisitor cvVis = cv.visitMethod(access, methodName, descriptor, signature, exceptions);
      return new DeletingMethodVisitor(api, new LocalVariablesSorter(access, descriptor, cvVis), config.deleteRestOfMethod) {

        String generatedMethodName = null;
        String generatedMethodDescriptor = null;
        int lastInstruction = -1;
        private final org.objectweb.asm.Label start = new org.objectweb.asm.Label();
        private final org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        private final org.objectweb.asm.Label handler = new org.objectweb.asm.Label();

        @Override public void visitCode() {
          generatedMethodDescriptor = null;
          mv.visitCode();
          if (!config.deleteRestOfMethod) {
            mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable");
            mv.visitLabel(start);
          }
        }

        @Override public void visitMaxs(int maxStack, int maxLocals) {
          closeTryCatch();
          mv.visitMaxs(maxStack, maxLocals);
        }

        boolean closedTryCatchAlready = false;

        void closeTryCatch() {
          if (!closedTryCatchAlready && !config.deleteRestOfMethod) {
            mv.visitLabel(end);
            mv.visitLabel(handler);
            mv.visitInsn(NOP);
            org.objectweb.asm.Label handlerEnd = new org.objectweb.asm.Label();
            mv.visitJumpInsn(GOTO, handlerEnd);
            mv.visitLabel(handlerEnd);
            mv.visitInsn(NOP);
            closedTryCatchAlready = true;
          }
        }

        @Override public void visitInsn(int opcode) {
          if (opcode == IRETURN || opcode == DRETURN || opcode == LRETURN || opcode == FRETURN || opcode == ARETURN) {
            closeTryCatch();
            if (!config.deleteRestOfMethod) {
              mv.visitInsn(POP);
            }
            switch (opcode) {
            case IRETURN:
              mv.visitInsn(ICONST_1);
              break;
            case DRETURN:
              mv.visitInsn(DCONST_1);
              break;
            case LRETURN:
              mv.visitInsn(LCONST_1);
              break;
            case FRETURN:
              mv.visitInsn(FCONST_1);
              break;
            case ARETURN:
              Type ret = Type.getObjectType(descriptor.split("\\)[L\\[]")[1].split(";")[0]);
              generatedMethodName = createNewName(ret);
              generatedMethodDescriptor = "()" + ret;
              mv.visitMethodInsn(INVOKESTATIC, klass, generatedMethodName, generatedMethodDescriptor, false);
            }
            mv.visitInsn(opcode);
          } else if (!config.deleteRestOfMethod) {
            mv.visitInsn(opcode);
          }
          lastInstruction = opcode;
        }

        @Override public void visitEnd() {
          mv.visitEnd();
          if (generatedMethodDescriptor == null) {
            return;
          }
          Type ret = Type.getObjectType(descriptor.split("\\)[L\\[]")[1].split(";")[0]);
          Set<Type> retTypes = findUnrelatedSet(getReturnTypes(descr));
          //assert retTypes.size() > 0; // TODO: might happen with serealization, darn reflection :(
          org.objectweb.asm.MethodVisitor genMv = ModifierVisitor.super
              .visitMethod(ACC_PRIVATE | ACC_STATIC, generatedMethodName, generatedMethodDescriptor, null, new String[0]);
          final DummyGenerator generator = new DummyGenerator(
              new LocalVariablesSorter(ACC_PRIVATE | ACC_STATIC, generatedMethodDescriptor, genMv),
              possibleConstructorsCalledIn(descr));
          genMv.visitCode();
          int retLocal = ((LocalVariablesSorter) mv).newLocal(ret);
          genMv.visitInsn(ACONST_NULL);
          genMv.visitVarInsn(ASTORE, retLocal);
          generator.createDummyInitCodeForComplexType(retLocal, retTypes);
          genMv.visitVarInsn(ALOAD, retLocal);
          genMv.visitInsn(ARETURN);
          genMv.visitEnd();
        }
      };
    }

  }

  /**
   * Creator of dummy values
   * <p>
   * Does not have to deal with checked exceptions:
   * https://mail.openjdk.java.net/pipermail/coin-dev/2009-May/001861.html
   */
  class DummyGenerator {
    private final LocalVariablesSorter mv;
    private final PossibleConstructors possibleConstructors;
    private final Map<Type, Optional<Integer>> localForType;

    public DummyGenerator(LocalVariablesSorter mv, PossibleConstructors possibleConstructors) {
      this.mv = mv;
      this.possibleConstructors = possibleConstructors;
      this.localForType = new HashMap<>();
    }

    class ClassNotFoundInMapException extends RuntimeException {

    }

    public void createDummyInitCodeForType(Type type) {
      if (localForType.containsKey(type)) {
        Optional<Integer> local = localForType.get(type);
        if (local.isPresent()) { // we already have a local variable
          mv.visitVarInsn(ALOAD, local.get());
        } else {
          mv.visitInsn(ACONST_NULL); // we're currently working on it. Break circles using null for now
        }
        return;
      }
      switch (type.getDescriptor().charAt(0)) {
      case 'B':
      case 'C':
      case 'I':
      case 'S':
      case 'Z':
        mv.visitInsn(ICONST_1);
        return;
      case 'D':
        mv.visitInsn(DCONST_1);
        return;
      case 'F':
        mv.visitInsn(FCONST_1);
        return;
      case 'J':
        mv.visitInsn(LCONST_1);
        return;
      case 'L':
      case '[':
        localForType.put(type, Optional.empty());
        int newLocal = mv.newLocal(type);
        createDummyInitCodeForComplexType(newLocal, Collections.singleton(type));
        localForType.put(type, Optional.of(newLocal));
        return;
      case 'V':
        return;
      }
      throw new RuntimeException();
    }

    /**
     * either array or object type
     */
    private void createDummyInitCodeForComplexType(int local, Set<Type> types) {
      List<Runnable> constructorCallers = types.stream().flatMap(type -> {
        if (type.getSort() == Type.ARRAY) {
          return Stream.of(() -> {
            createDummyInitCodeForArrayType(type);
            mv.visitVarInsn(ASTORE, local);
          });
        }
        if (type.getDescriptor().equals("Ljava/lang/String;")) { // special case for strings
          return Stream.of(() -> {
            mv.visitLdcInsn("non empty string");
            mv.visitVarInsn(ASTORE, local);
          });
        }
        return possibleConstructors.forType(type).map.entrySet().stream()
            .flatMap(e -> e.getValue().stream().map(d -> new MethodDescriptor(e.getKey().getClassName(), "<init>", d)))
            .map(d -> (Runnable) () -> {
              try {
                callConstructor(d);
                mv.visitVarInsn(ASTORE, local);
              } catch (ClassNotFoundInMapException ex) {
                mv.visitInsn(ACONST_NULL);
              }
            });
      }).collect(Collectors.toList());
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "random", "()D", false);
      mv.visitLdcInsn((double) constructorCallers.size());
      mv.visitInsn(DMUL);
      mv.visitInsn(D2I);
      int randLocal = mv.newLocal(Type.INT_TYPE);
      mv.visitVarInsn(ISTORE, randLocal);
      int i = 0;
      for (Runnable constructorCaller : constructorCallers) {
        org.objectweb.asm.Label end = new org.objectweb.asm.Label();
        mv.visitVarInsn(ILOAD, randLocal);
        mv.visitLdcInsn(i);
        mv.visitJumpInsn(IF_ICMPNE, end);
        constructorCaller.run();
        mv.visitJumpInsn(GOTO, end);
        mv.visitLabel(end);
        i++;
      }
    }

    private void callConstructor(MethodDescriptor constructor) {
      mv.visitTypeInsn(NEW, constructor.methodClass);
      mv.visitInsn(DUP);
      for (Type param : Type.getMethodType(constructor.methodDescriptor).getArgumentTypes()) {
        createDummyInitCodeForType(param);
      }
      mv.visitMethodInsn(INVOKESPECIAL, constructor.methodClass, "<init>", constructor.methodDescriptor, false);
    }

    /**
     * idea: T[] a = Math.random() < 0.5 ? new boolean[]{T} : new boolean[]{T, T};
     * <p>
     * to different arrays to trip of alias analyses…
     */
    private void createDummyInitCodeForArrayType(Type type) {
      String elementTypeDescriptor = type.getElementType().getDescriptor();
      Type subElementType = Type.getType(
          IntStream.range(0, type.getDimensions()).mapToObj(i -> "[").collect(Collectors.joining("")) + type.getElementType()
              .getDescriptor());
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "random", "()D", false);
      mv.visitLdcInsn(new Double("0.5"));
      mv.visitInsn(DCMPG);
      org.objectweb.asm.Label l1 = new org.objectweb.asm.Label();
      mv.visitJumpInsn(IFGE, l1);
      mv.visitInsn(ICONST_1);
      mv.visitMultiANewArrayInsn(elementTypeDescriptor, type.getDimensions());
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      createDummyInitCodeForType(subElementType);
      storeIntoArray(subElementType);
      org.objectweb.asm.Label l2 = new org.objectweb.asm.Label();
      mv.visitJumpInsn(GOTO, l2);
      mv.visitLabel(l1);
      mv.visitInsn(ICONST_2);
      mv.visitIntInsn(NEWARRAY, T_BOOLEAN);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      createDummyInitCodeForType(subElementType);
      storeIntoArray(subElementType);
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_1);
      createDummyInitCodeForType(subElementType);
      storeIntoArray(subElementType);
      mv.visitLabel(l2);
      mv.visitVarInsn(ASTORE, 1);
      org.objectweb.asm.Label l3 = new org.objectweb.asm.Label();
      mv.visitLabel(l3);
    }

    /**
     * Create code to store the current stack element into an array
     */
    void storeIntoArray(Type type) {
      switch (type.getDescriptor().charAt(0)) {
      case 'B':
      case 'Z':
        mv.visitInsn(BASTORE);
        break;
      case 'C':
        mv.visitInsn(CASTORE);
        break;
      case 'I':
        mv.visitInsn(IASTORE);
        break;
      case 'S':
        mv.visitInsn(SASTORE);
        break;
      case 'D':
        mv.visitInsn(DASTORE);
        break;
      case 'F':
        mv.visitInsn(FASTORE);
        break;
      case 'J':
        mv.visitInsn(LASTORE);
        break;
      case 'L':
      case '[':
        mv.visitInsn(AASTORE);
        break;
      default:
        throw new NotImplementedException();
      }
    }
  }

  @Override public void teardown() {
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return false;
  }
}
