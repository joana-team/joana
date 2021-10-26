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
import edu.kit.joana.api.sdg.opt.asm.*;
import edu.kit.joana.ui.annotations.openapi.InvokeAllRegisteredOpenApiServerMethods;
import edu.kit.joana.ui.annotations.openapi.ModifiedOpenApiClientMethod;
import edu.kit.joana.ui.annotations.openapi.RegisteredOpenApiServerMethod;
import edu.kit.joana.util.Iterators;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.TypeNameUtils;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.openapi.OpenApiClientDetector;
import io.github.classgraph.*;
import nonapi.io.github.classgraph.classpath.SystemJarFinder;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.objectweb.asm.Opcodes.*;

/**
 * This pass identifies OpenAPI client methods and replaces their return values with dummy return values
 */
public class OpenApiPreProcessorPass implements FilePass {

  // all stringified class names that are stored in the following are java class names


  public static class Config {

    public static final Config DEFAULT = new Config(true, true, false, false, true);

    public final boolean deleteRestOfMethod;
    public final boolean deleteConstructorBody;
    public final boolean ignorePrivateConstructors;
    public final boolean useSingleConstructorPerType;
    /**
     * reduces the precision by ignoring the collected constructors and just using "native" annotated methods
     * for creating instances of types
     */
    public final boolean useNativeMethods;
    /** ignore all information besides the type*/
    public final boolean useBasicNativeMethod = true;

    public Config(boolean deleteRestOfMethod, boolean deleteConstructorBody, boolean ignorePrivateConstructors,
        boolean useSingleConstructorPerType, boolean useNativeMethods) {
      this.deleteRestOfMethod = deleteRestOfMethod;
      this.deleteConstructorBody = deleteConstructorBody;
      this.ignorePrivateConstructors = ignorePrivateConstructors;
      this.useSingleConstructorPerType = useSingleConstructorPerType;
      this.useNativeMethods = useNativeMethods;
    }

    @Override public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof Config))
        return false;
      Config config = (Config) o;
      return deleteRestOfMethod == config.deleteRestOfMethod && deleteConstructorBody == config.deleteConstructorBody
          && ignorePrivateConstructors == config.ignorePrivateConstructors
          && useSingleConstructorPerType == config.useSingleConstructorPerType
          && useNativeMethods == config.useNativeMethods;
    }

    @Override public int hashCode() {
      return Objects.hash(deleteRestOfMethod, deleteConstructorBody, ignorePrivateConstructors, useSingleConstructorPerType,
          useNativeMethods);
    }
  }

  protected final OpenApiClientDetector clientDetector;
  protected final OpenApiServerDetector serverDetector;
  /** java class name → info */
  protected Class2ClassInfo classInfoMap;
  private SDGBuilder.CGResult cgr;
  private final boolean debug = true;
  private final boolean debug2 = false;

  private final Config config;

  /** java names of detected open api classes */
  private Set<String> openApiClientClasses;

  /** Types that might be used in reflection. An approximation for GSON related reflection. */
  private Set<ClassInfo> typesUsedInReflection;

  private Set<ClassInfo> concreteTypesUsedInReflection;

  /** java class names */
  private NameCreator classNames;

  public OpenApiPreProcessorPass(OpenApiClientDetector clientDetector, Config config) {
    this.clientDetector = clientDetector;
    this.config = config;
    this.serverDetector = new OpenApiServerDetector();
  }

  @Override public void setup(SDGConfig cfg, String libClassPath, Path sourceFolder) {
    classInfoMap = new Class2ClassInfo(new ClassGraph().enableMethodInfo().overrideClasspath(
        libClassPath + (libClassPath.isEmpty() ? "" : ":") + SystemJarFinder.getJreRtJarPath() + ":" + sourceFolder)
        .enableSystemJarsAndModules().enableInterClassDependencies().enableMethodInfo().scan().getAllClassesAsMap());
    classNames = new NameCreator(new HashSet<>(classInfoMap.keySet()));
    try {
      cfg.setClassPath(sourceFolder.toString());
      cfg.setPointsToPrecision(SDGBuilder.PointsToPrecision.TYPE_BASED);
      cgr = SDGProgram.buildCallGraph(cfg);
      openApiClientClasses = cgr.cg.getClassHierarchy().getClasses().stream().filter(clientDetector::isReallyOpenOpiClass)
          .map(TypeNameUtils::toJavaClassName)
          .collect(Collectors.toSet());
      checkForCalledNonOpenApiMethodsOfOpenApiClasses(cgr.cg);
      HeuristicReflectionFinder heuristicReflectionFinder = new HeuristicReflectionFinder(sourceFolder, cgr, classInfoMap);
      heuristicReflectionFinder.run();
      typesUsedInReflection = heuristicReflectionFinder.getFoundTypes().stream().map(TypeNameUtils::toJavaClassName)
          .map(n -> classInfoMap.get(n)).filter(Objects::nonNull).collect(Collectors.toSet());
      concreteTypesUsedInReflection = typesUsedInReflection.stream().filter(t -> !t.isAbstract() && !t.isInterface()).collect(
          Collectors.toSet());
      clientDetector.detectUnsupportedApiCalls(cgr.cg).forEach(System.err::println);
      serverDetector.setup(cgr.cg, classInfoMap.values());
    } catch (CallGraphBuilderCancelException | ClassHierarchyException | IOException e) {
      e.printStackTrace();
    }
  }

  public static class OpenApiInvariantException extends RuntimeException {
    private final List<IMethod> calledNonOpenApiMethodsOfOpenApiClasses;

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
            node -> openApiClientClasses.contains(node.getMethod().getDeclaringClass().getName().toString()) && // method is in open api class
                !clientDetector.isWrappableOpenApiMethod(node.getMethod()) && !node.getMethod().isInit() && !node.getMethod().isClinit()
                && new Iterator2List<>(cg.getPredNodes(node), new ArrayList<>()).stream()   // checks for callers
                .anyMatch(caller -> !openApiClientClasses.contains(caller.getMethod().getDeclaringClass().getName().toString())))
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

    public MethodDescriptor(MethodInfo methodInfo) {
      super(methodInfo.getClassName(), methodInfo.getName(), methodInfo.getTypeDescriptorStr());
    }

    public MethodDescriptor(CGNode cgNode) {
      this(cgNode.getMethod().getReference());
    }

    public MethodDescriptor(MethodReference method) {
      super(TypeNameUtils.toJavaClassName(method.getDeclaringClass()), method.getName().toString(), method.getDescriptor().toString());
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

    /** might be null, e.g. for methods of {@link java.lang.Object}*/
    @Nullable
    private MethodInfo toMethodInfo() {
      ClassInfo info = classInfoMap.get(methodClass);
      if (info == null) {
        return null;
      }
      MethodInfoList methods = info.getMethodInfo(methodName).filter(m -> m.getTypeDescriptorStr().equals(methodDescriptor));
      if (methods.isEmpty()) {
        return null;
      }
      return methods.get(0);
    }
  }

  /** … self */
  private Set<ClassInfo> getReflectionSubTypes(ClassInfo klass) {
    ClassInfoList subclasses = klass.getSubclasses();
    Set<ClassInfo> found = new HashSet<>();
    Predicate<ClassInfo> valid = k -> k.isStandardClass() && !k.isAbstract();
    if (typesUsedInReflection.contains(klass) && valid.test(klass)) {
      found.add(klass);
    }
    if (!subclasses.isEmpty()) {
      typesUsedInReflection.stream()
          .filter(k -> k.isStandardClass() && !k.isAbstract() && subclasses.contains(k))
          .forEach(found::add);
    }
    return found;
  }

  private Type classInfoToType(ClassInfo klass) {
    return Type.getType(TypeNameUtils.toInternalName(klass.getName()));
  }

  /** might return an empty set */
  private Set<Type> getReturnTypes(MethodDescriptor methodDescriptor) {
    return methodDescriptor.nodes().stream().flatMap(n -> {
      List<Type> collectedTypes = cgr.pts.getPointsToSet(cgr.pts.getHeapModel().getPointerKeyForReturnValue(n)).stream()
          .map(i -> Type.getType(i.getConcreteType().getName() + ";")).collect(Collectors.toList());
      if (collectedTypes.isEmpty()) {
        collectedTypes.addAll(Objects.requireNonNull(
                getReflectionSubTypes(classInfoMap.get(TypeNameUtils.toJavaClassName(n.getMethod().getReturnType()))))
            .stream().map(this::classInfoToType).collect(
            Collectors.toList()));
        collectedTypes.add(Type.getType(n.getMethod().getReturnType().getName().toString() + ";"));
      }
      return collectedTypes.stream();
    }).collect(Collectors.toSet());
  }

  /** might return an empty set */
  private Set<Type> getReturnTypesWoReflection(MethodDescriptor methodDescriptor) {
    return methodDescriptor.nodes().stream().flatMap(n -> {
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

  /** remove all interfaces and abstract classes */
  private Set<Type> findImplementingTypes(Set<Type> types) {
    return types.stream().filter(t -> {
      ClassInfo classInfo = classInfoMap.get(TypeNameUtils.toJavaClassName(t));
      return classInfo.isStandardClass() && !classInfo.isAbstract();
    }).collect(Collectors.toSet());
  }

  /**
   * Returns the possible constructors called (transitively) in the passed method
   *
   * @param methodDescriptor
   * @return
   */
  private PossibleConstructors possibleConstructorsCalledIn(MethodDescriptor methodDescriptor) {
    return new PossibleConstructors(classInfoMap, config, methodDescriptor.toRef().map(d -> worklist(cgr.cg.getNodes(d),
        no -> iteratorToCollection(cgr.cg.getSuccNodes(no)),
        no -> iteratorToCollection(cgr.cg.getSuccNodes(no))).stream()
            .map(CGNode::getMethod)
            .filter(m -> m.isInit() && !m.getDeclaringClass().isAbstract()))
        .orElseGet(Stream::empty)
        .map(m -> Pair.pair(Type.getType(m.getDeclaringClass().getName().toString() + ";"),
            m.getDescriptor().toString()))
        .collect(pairGroupingCollector()), concreteTypesUsedInReflection);
  }

  public static <N, R> Set<R> worklist(Iterable<N> init, Function<N, Iterable<R>> process, Function<N, Iterable<N>> next) {
    Set<R> set = new HashSet<>();
    worklist(init, n -> {
      if (StreamSupport.stream(process.apply(n).spliterator(), false).anyMatch(set::add)) {
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

  @Override public void store(Path source, Path target, Path targetBase) throws IOException {
    if (debug) {
      Path newTargetBase = Paths.get("/tmp/oa");
      Path newTarget = newTargetBase.resolve(targetBase.relativize(target));
      storeImpl(source, newTarget, newTargetBase);
      Files.createDirectories(target.getParent());
      Files.copy(newTarget, target);
    } else {
      storeImpl(source, target, targetBase);
    }
  }

  private void storeImpl(Path source, Path target, Path targetBase) throws IOException {
    ClassReader cr = new ClassReader(Files.newInputStream(source));
    ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
    ModifierVisitor mod = new ModifierVisitor(cw, targetBase, clientDetector,
        new DummyGeneratorStore(targetBase, "edu.kit.joana.gen", classNames, config));
    cr.accept(mod, ClassReader.EXPAND_FRAMES);
    Files.createDirectories(target.getParent());
    Files.newOutputStream(target).write(cw.toByteArray());
  }

  @Override public void process(SDGConfig cfg, String libClassPath, Path sourceFolder, Path targetFolder) throws IOException {
    if (debug) {
      FileUtils.deleteDirectory(Paths.get("/tmp/oa").toFile());
    }
    FilePass.super.process(cfg, libClassPath, sourceFolder, targetFolder);
    storeNeededFiles(targetFolder);
    if (debug) {
      try {
        Files.createDirectories(targetFolder);
        FileUtils.copyDirectory(Paths.get("/tmp/oa").toFile(), targetFolder.toFile());
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void storeNeededFiles(Path path) {
    try {
      createAnnotationIfNotPresent(path, Type.getType(ModifiedOpenApiClientMethod.class));
      createAnnotationIfNotPresent(path, Type.getType(RegisteredOpenApiServerMethod.class));
      createAnnotationIfNotPresent(path, Type.getType(InvokeAllRegisteredOpenApiServerMethods.class));
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private void createAnnotationIfNotPresent(Path basePath, Type type) throws IOException {
    Path path = basePath.resolve(TypeNameUtils.toInternalNameWithoutSemicolonAndL(type) + ".class");
    if (Files.exists(path)) {
      return;
    }
    Files.createDirectories(path.getParent());
    ClassWriter cw = new ClassWriter(0);
    FieldVisitor fv;
    MethodVisitor mv;
    AnnotationVisitor av0;

    cw.visit(52, ACC_PUBLIC + ACC_ANNOTATION + ACC_ABSTRACT + ACC_INTERFACE,
        TypeNameUtils.toInternalNameWithoutSemicolonAndL(type.toString()), null, "java/lang/Object",
        new String[] { "java/lang/annotation/Annotation" });

    {
      av0 = cw.visitAnnotation("Ljava/lang/annotation/Retention;", true);
      av0.visitEnum("value", "Ljava/lang/annotation/RetentionPolicy;", "CLASS");
      av0.visitEnd();
    }
    {
      av0 = cw.visitAnnotation("Ljava/lang/annotation/Target;", true);
      {
        AnnotationVisitor av1 = av0.visitArray("value");
        av1.visitEnum(null, "Ljava/lang/annotation/ElementType;", "METHOD");
        av1.visitEnd();
      }
      av0.visitEnd();
    }
    cw.visitEnd();
    Files.newOutputStream(path).write(cw.toByteArray());
  }

  @Override public void teardown() {
  }

  class ModifierVisitor extends ClassVisitor {

    private final Path basePath;
    private final OpenApiClientDetector detector;
    private final DummyGeneratorStore dummyGeneratorStore;
    /** important: "L$klass;" is the real internal name */
    private String klass;
    private String javaKlassName;
    private Set<String> usedMethodNames;
    /** used for all-method-invokes on server objects */
    private final Map<ClassInfo, MethodDescriptor> allInvokeMethods = new HashMap<>();

    private PossibleConstructors possibleConstructorsForServers = new PossibleConstructors(classInfoMap, config);

    public ModifierVisitor(ClassVisitor cv, Path basePath, OpenApiClientDetector detector, DummyGeneratorStore dummyGeneratorStore) {
      super(ASM8, cv);
      this.basePath = basePath;
      this.detector = detector;
      this.dummyGeneratorStore = dummyGeneratorStore;
    }

    @Override public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.klass = name;
      this.javaKlassName = TypeNameUtils.toJavaClassName("L" + name + ";");
      ClassInfo classInfo = classInfoMap.get(javaKlassName);
      this.usedMethodNames = classInfo != null ? new HashSet<>(classInfo.getMethodInfo().getNames()) : new HashSet<>();
      super.visit(version, access, name, signature, superName, interfaces);
    }

    private String createNewName(Type retType) {
      return createNewName("gen" + retType.toString().replaceAll("[^a-zA-Z]", ""));
    }

    private String createNewName(String namePart) {
      String candidate = "____$$__" + namePart.replaceAll("[^a-z_0-9A-Z]", "");
      if (usedMethodNames == null) {
        return candidate + Math.random();
      }
      while (usedMethodNames.contains(candidate)) {
        candidate += usedMethodNames.size();
      }
      usedMethodNames.add(candidate);
      return candidate;
    }

    private MethodDescriptor getAllInvokeMethod(ClassInfo apiInterface) {
      return allInvokeMethods.computeIfAbsent(apiInterface, inter -> {
        String name = createNewName("all");
        String descr = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(TypeNameUtils.toInternalName(apiInterface.getName())));
        return new MethodDescriptor("L" + klass + ";", name, descr);
      });
    }

    @Override public org.objectweb.asm.MethodVisitor visitMethod(int access, String methodName, String descriptor, String signature,
        String[] exceptions) {
      MethodDescriptor descr = new MethodDescriptor(javaKlassName, methodName, descriptor);
      boolean isPossibleServerImplMethod = serverDetector.isPossibleImplMethod(descr);
      if (isPossibleServerImplMethod) {
        System.out.println("Annotated Api impl method " + klass + "." + methodName);
        return getServerMethodTransformer(access, descriptor, descr,
            cv.visitMethod(access, methodName, descriptor, signature, exceptions));
      }
      if (openApiClientClasses.contains(javaKlassName) && methodName.equals("<init>") && config.deleteConstructorBody) { // a constructor
        return new DeletingMethodVisitor(api, true);
      }
      if (!isCalled(descr) && !debug2) {
        return cv.visitMethod(access, methodName, descriptor, signature, exceptions);
      }
      boolean isOpenApiClientMethod =
          openApiClientClasses.contains(javaKlassName) && (access & ACC_PUBLIC) != 0 && (access & ACC_STATIC) == 0
              && (access & ACC_ABSTRACT) == 0 && (access & ACC_NATIVE) == 0 && detector.isWrappableOpenApiMethod(javaKlassName,
              methodName, descriptor, signature, exceptions);
      boolean isServerEndPointCallingMethod = serverDetector.isPossibleEndpointCallingMethod(descr);
      org.objectweb.asm.MethodVisitor cvVis = cv.visitMethod(access, methodName, descriptor, signature, exceptions);
      if (isOpenApiClientMethod) {
        System.out.println("Transformed client method " + klass + "." + methodName);
        return getClientMethodTransformer(access, descriptor, descr, cvVis);
      } else if (isServerEndPointCallingMethod) {
        System.out.println("Transformed endpoint caller method " + klass + "." + methodName);
        return getServerEndpointCallerTransformer(access, descriptor, descr, cvVis,
            serverDetector.apiClasses.stream()
                .map(classInfoMap::get).collect(Collectors.toMap(k -> k, this::getAllInvokeMethod)) /* TODO: be context sensitive */);
      }
      return cvVis;
    }

    /**
     * Create visitor to transform the body of OpenApi client methods:
     * They then return a newly created return object without a dependency on any method parameter
     */
    @NotNull private DeletingMethodVisitor getClientMethodTransformer(int access, String descriptor, MethodDescriptor descr,
        MethodVisitor cvVis) {
      return new DeletingMethodVisitor(api, new LocalVariablesSorter(access, descriptor, cvVis), config.deleteRestOfMethod) {

        @Nullable
        BaseMethodDescriptor dummyMethod = null;
        int lastInstruction = -1;
        private final Label start = new Label();
        private final Label end = new Label();
        private final Label handler = new Label();

        @Override public void visitCode() {
          dummyMethod = null;
          mv.visitAnnotation(Type.getType(ModifiedOpenApiClientMethod.class).toString(), true).visitEnd();
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
            Label handlerEnd = new Label();
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
            callDummyMethod();
            mv.visitInsn(opcode);
          } else if (!config.deleteRestOfMethod) {
            mv.visitInsn(opcode);
          }
          lastInstruction = opcode;
        }

        private void callDummyMethod() {
          if (dummyMethod == null) {
            dummyMethod = createDummyMethod();
          }
          mv.visitMethodInsn(INVOKESTATIC, TypeNameUtils.toInternalNameWithoutSemicolonAndL(dummyMethod.methodClass),
              dummyMethod.methodName, dummyMethod.methodDescriptor, false);
        }

        private BaseMethodDescriptor createDummyMethod() {
          Set<Type> retTypes = getReturnTypesWoReflection(descr);//findImplementingTypes(getReturnTypes(descr));
          //assert retTypes.size() > 0; // TODO: might happen with serialization, darn reflection :(

          PossibleConstructors possibleConstructors = possibleConstructorsCalledIn(descr);
          return dummyGeneratorStore.createDummyCreatorMethodForTypes(possibleConstructors,
              Type.getReturnType(descr.methodDescriptor), Optional.of(retTypes));
        }
      };
    }


    /**
     * Idea: search for calls to server endpoint methods with OpenApi implementations
     * (like <code>Endpoint.publish(address, implementor, new LoggingFeature())</code> and prepend them by a call
     * to method that calls server api methods.
     *
     * for each call to a possible endpoint method it
     * 1. stores all argument in new local variables (pop as many arguments as a method has)
     * 2. creates an `if (isinstance(param, Api1)) {…}` cascade for every Object typed argument of the method
     *    in which the appropriate allInvoke method is called
     */
    private MethodVisitor getServerEndpointCallerTransformer(int access, String descriptor,
        MethodDescriptor descr, MethodVisitor cvVis, Map<ClassInfo, MethodDescriptor> allInvokeMethods) {
      LocalVariablesSorter lmv = new LocalVariablesSorter(access, descriptor, cvVis);
      possibleConstructorsForServers = possibleConstructorsForServers.combine(possibleConstructorsCalledIn(descr));
      return new MethodVisitor(api, lmv) {

        @Override public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
          MethodDescriptor methodDescriptor = new MethodDescriptor("L" + owner + ";", name, descriptor);
          if (serverDetector.isPossibleEndPointMethod(methodDescriptor)) {
            MethodInfo methodInfo = methodDescriptor.toMethodInfo();
            // we know here that the called method is a possible endpoint method
            // first: store all arguments in new local variables
            List<Pair<Type, Integer>> localVariables = Arrays.stream(methodInfo.getParameterInfo())
                .map(p -> Type.getType(TypeNameUtils.toInternalName(p.getTypeSignatureOrTypeDescriptor().toString())))
                .map(type -> {
              int local = lmv.newLocal(type);
              lmv.visitVarInsn(type.getOpcode(ISTORE), local);
              return Pair.pair(type, local);
            }).collect(Collectors.toList());   // (type, local variable)

            // collect all object typed parameters, they might be the API endpoint
            List<Pair<Type, Integer>> apiParameters = localVariables.stream()
                .filter(p -> p.getFirst().equals(Type.getType(Object.class))).collect(Collectors.toList());;
            // create `if (isinstance(param, Api1)) {}` chain for every suitable parameter
            for (Pair<Type, Integer> apiParameter : apiParameters) {
              allInvokeMethods.forEach((classInfo, allInvokeMethod) -> {
                Label endLabel = new Label();
                // load parameter
                lmv.visitVarInsn(apiParameter.getFirst().getOpcode(ILOAD), apiParameter.getSecond());
                // if instanceof
                lmv.visitTypeInsn(INSTANCEOF, TypeNameUtils.toInternalNameWithoutSemicolonAndL(classInfo.getName()));
                lmv.visitJumpInsn(IFEQ, endLabel);
                // call method
                lmv.visitVarInsn(apiParameter.getFirst().getOpcode(ILOAD), apiParameter.getSecond());
                lmv.visitTypeInsn(CHECKCAST, TypeNameUtils.toInternalNameWithoutSemicolonAndL(apiParameter.getFirst()));
                lmv.visitMethodInsn(INVOKESTATIC, TypeNameUtils.toInternalNameWithoutSemicolonAndL(allInvokeMethod.methodClass),
                    allInvokeMethod.methodName, allInvokeMethod.methodDescriptor, false);
                lmv.visitLabel(endLabel);
              });
            }

            // push locals back onto the stack
            localVariables.forEach(p -> lmv.visitVarInsn(p.getFirst().getOpcode(ILOAD), p.getSecond()));
          }
          super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
      };
    }

    @Override public void visitEnd() {
      super.visitEnd();
      allInvokeMethods.forEach((k, m) -> createAllInvokeMethodForSingleApi(k, m, cv));
    }

    /**
     * Create a method that calls all api methods on its only argument
     */
    private void createAllInvokeMethodForSingleApi(ClassInfo apiImplClass, MethodDescriptor method, ClassVisitor cv) {
      Type apiClassType = Type.getType(TypeNameUtils.toInternalName(apiImplClass.getName()));
      MethodVisitor genMv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC, method.methodName, method.methodDescriptor, null, new String[0]);
      // start method
      genMv.visitAnnotation(Type.getType(InvokeAllRegisteredOpenApiServerMethods.class).toString(), true).visitEnd();
      genMv.visitCode();
      // call all API methods of the impl class with the passed parameter (local variable 1)
      apiImplClass.getMethodInfo().stream().filter(serverDetector::isApiMethod).forEach(m -> {
        // load parameter, we call the method on
        genMv.visitVarInsn(apiClassType.getOpcode(ILOAD), 0);
        // push dummy arguments
        for (MethodParameterInfo param : m.getParameterInfo()) {
          Type paramType = Type.getType(TypeNameUtils.toInternalName(param.getTypeDescriptor().toString()));
          BaseMethodDescriptor dummyMethod = dummyGeneratorStore.createDummyCreatorMethodForTypes(possibleConstructorsForServers,
              paramType, Optional.empty());
          genMv.visitMethodInsn(INVOKESTATIC, TypeNameUtils.toInternalNameWithoutSemicolonAndL(dummyMethod.methodClass),
              dummyMethod.methodName, dummyMethod.methodDescriptor, false);
        }
        // call method
        genMv.visitMethodInsn(INVOKEINTERFACE, TypeNameUtils.toInternalNameWithoutSemicolonAndL(apiClassType),
            m.getName(), m.getTypeDescriptorStr(), false);
        Type resultType = Type.getReturnType(m.getTypeDescriptorStr());
        if (resultType != Type.VOID_TYPE) {
          if (resultType == Type.DOUBLE_TYPE || resultType == Type.LONG_TYPE) {
            genMv.visitInsn(POP2);
          } else {
            genMv.visitInsn(POP);
          }
        }
      });
      genMv.visitInsn(RETURN);
      genMv.visitMaxs(1, 1);
      genMv.visitEnd();
    }

    private MethodVisitor getServerMethodTransformer(int access, String descriptor, MethodDescriptor descr,
        MethodVisitor cvVis) {
      return new MethodVisitor(api, cvVis) {
        @Override public void visitCode() {
          visitAnnotation(Type.getType(RegisteredOpenApiServerMethod.class).toString(), true).visitEnd();
          super.visitCode();
        }
      };
    }
  }

  @Override public boolean requiresKnowledgeOnAnnotations() {
    return false;
  }

  public class OpenApiServerDetector {

    // all class names are java class names

    private Set<String> apiClasses;

    /** implementing class → implementing api classes */
    private Map<String, Set<String>> implementingClasses;

    private Set<MethodDescriptor> possibleEndPointMethods;

    private Set<MethodDescriptor> possibleEndPointCallers;

    public void setup(CallGraph cg, Collection<ClassInfo> klasses) {
      apiClasses = klasses.stream().filter(this::isApiClass)
          .map(ClassInfo::getName).collect(Collectors.toSet());
      implementingClasses = klasses.stream()
          .filter(k -> isImplementingClass(apiClasses, k))
          .collect(Collectors.toMap(ClassInfo::getName, k -> k.getInterfaces().stream()
              .map(ClassInfo::getName)
              .filter(apiClasses::contains)
              .collect(Collectors.toSet())));
      possibleEndPointMethods = getPossibleEndPointMethods(klasses);
      possibleEndPointCallers = getCallersOfMethods(possibleEndPointMethods, cg);
      if (debug) {
        System.out.println("possibleEndPointMethods = " + possibleEndPointMethods);
        System.out.println("possibleEndPointCallers = " + possibleEndPointCallers);
        System.out.println("apiClasses = " + apiClasses);
        System.out.println("implementingClasses = " + implementingClasses);
      }
    }

    private Set<MethodDescriptor> getPossibleEndPointMethods(Collection<ClassInfo> klasses) {
      return klasses.stream().filter(this::isPossibleEndPointClass)
          .flatMap(k -> k.getDeclaredMethodInfo().stream().filter(this::isPossibleEndPointMethod))
          .map(MethodDescriptor::new).collect(Collectors.toSet());
    }

    private Set<MethodDescriptor> getCallersOfMethods(Set<MethodDescriptor> endpoints, CallGraph cg) {
      return endpoints.stream()
          .flatMap(e -> e.toRef()
              .map(ee -> cg.getNodes(ee).stream())
              .orElse(Stream.empty()))
          .flatMap(node -> Iterators.stream(cg.getPredNodes(node)))
          .map(MethodDescriptor::new)
          .collect(Collectors.toSet());
    }

    private boolean isPossibleEndPointClass(ClassInfo klass) {
      return klass.getPackageName().startsWith("javax.xml.ws") && !klass.isAnnotation();
    }

    private boolean isPossibleEndPointMethod(MethodInfo method) {
      return Arrays.stream(method.getParameterInfo()).anyMatch(p -> p.getTypeDescriptor().toString().equals("java.lang.Object")) &&
          !Modifier.isPrivate(method.getModifiers());
    }

    /** checks both class and method. End point methods are methods that belong to the javax.xml.ws package,
     * like Endpoint.publish */
    public boolean isPossibleEndPointMethod(MethodDescriptor descriptor) {
      MethodInfo methodInfo = descriptor.toMethodInfo();
      return methodInfo != null && isPossibleEndPointClass(methodInfo.getClassInfo()) && isPossibleEndPointMethod(methodInfo);
    }

    private boolean isApiClass(ClassInfo klass) {
      return klass.isInterface() && klass.getName().endsWith("Api") &&
          klass.hasAnnotation("io.swagger.annotations.Api") &&
          klass.getMethodInfo().stream().anyMatch(this::isApiMethod);
    }

    private boolean isImplementingClass(Set<String> apiClasses, ClassInfo klass) {
      return klass.isStandardClass() &&
          cgr.cg.getClassHierarchy().lookupClass(TypeNameUtils.toInternalName(klass.getName())) != null &&
          klass.getInterfaces().stream().anyMatch(k -> apiClasses.contains(k.getName()));
    }

    private boolean isApiMethod(MethodInfo method) {
      return method.getAnnotationInfo().stream().anyMatch(a -> a.getName().matches("javax\\.ws\\.rs\\.[A-Z]+")) &&
          method.hasAnnotation("io.swagger.annotations.ApiResponses") &&
          method.hasAnnotation("io.swagger.annotations.ApiOperation") &&
          !Modifier.isPrivate(method.getModifiers());
    }

    public boolean isPossibleEndpointCallingMethod(MethodDescriptor method) {
      return possibleEndPointCallers.contains(method);
    }

    public boolean isPossibleImplMethod(MethodDescriptor descr) {
      MethodInfo methodInfo = descr.toMethodInfo();
      return methodInfo != null && isPossibleImplMethod(methodInfo);
    }

    public boolean isPossibleImplMethod(MethodInfo method) {
      ClassInfo klass = method.getClassInfo();
      return implementingClasses.getOrDefault(klass.getName(), Collections.emptySet()).stream().map(classInfoMap::get)
          .anyMatch(inter -> inter.getDeclaredMethodInfo().get(method.getName()).stream()
              .anyMatch(m -> method.getTypeDescriptor().equals(m.getTypeDescriptor()) && isApiMethod(m)));
    }

  }
}
