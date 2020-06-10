package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.google.common.collect.Iterators;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationClass;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationOptions;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.types.TypeReference;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.component.connector.*;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.*;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.Pattern;
import edu.kit.joana.ui.ifc.wala.console.io.PrintStreamConsoleWrapper;
import edu.kit.joana.util.NullPrintStream;
import gnu.trove.map.TObjectIntMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.ibm.wala.ipa.callgraph.UninitializedFieldHelperOptions.FieldTypeMatcher;
import static edu.kit.joana.api.IFCType.CLASSICAL_NI;
import static edu.kit.joana.api.sdg.SDGBuildPreparation.searchProgramParts;
import static edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI.programPartToString;

/**
 * Basic flow analyzer
 * <p/>
 * The known flows implementation supports currently:
 * <ul>
 *   <li>flows from the parameters to the return through parameters of other functions that are present
 *   <li>flows from the parameters to the parameters of other functions</li>
 *   <li>flows from the parameters to the return</li>
 * </ul>
 */
public class BasicFlowAnalyzer extends FlowAnalyzer {

  private final IFCConsole console;

  private final boolean connectReturnWithParams;

  /**
   * method annotated in the source code → incoming annotated method, for mapping the methods found in the flows
   */
  private final Map<Method, Method> annotatedToIncomingMethod;

  public BasicFlowAnalyzer() {
    this(false);
  }

  public BasicFlowAnalyzer(boolean connectReturnWithParams) {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    this.console = new IFCConsole(in, new PrintStreamConsoleWrapper(new PrintStream(System.out) {
      @Override public void print(String s) {
        if (s.trim().length() > 0) {
          LOGGER.fine(s);
        }
      }
    }, new PrintStream(System.out) {
      @Override public void print(String s) {
        if (s.trim().length() > 0) {
          LOGGER.info(s);
        }
      }
    }, in, System.out, new PrintStream(System.out) {
      @Override public void print(String s) {
        if (s.trim().length() > 0) {
          LOGGER.info(s);
        }
      }
    }));
    this.console.setPointsTo("OBJECT_SENSITIVE"); // use a slower but more precise analysis by default
    this.console.setUninitializedFieldTypeMatcher(t -> true);
    // this.console.setAnnotateOverloadedMethods(true); // TODO: reimplement directly here
    this.connectReturnWithParams = connectReturnWithParams;
    this.annotatedToIncomingMethod = new HashMap<>();
  }

  @Override public void setClassPath(String classPath) {
    console.setClassPath(classPath);
  }

  private List<String> byteCodeNamesOfInterfacesToImplement(Collection<String> interfacesToImplement) {
    return searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, false, false).stream().map(p -> {
      return p.acceptVisitor(new SDGProgramPartVisitorWithDefault<String, Object>() {

        @Override protected String visitProgramPart(SDGProgramPart programPart, Object data) {
          return null;
        }

        @Override protected String visitMethod(SDGMethod m, Object data) {
          if (interfacesToImplement.contains(m.getSignature().getDeclaringType().toHRStringShort())) {
            return m.getSignature().getDeclaringType().toBCString();
          }
          return null;
        }
      }, null);
    }).filter(Objects::nonNull).distinct().collect(Collectors.toList());
  }

  private List<IMethod> getIMethods(InterfaceImplementationClass klass, Method method) {
    return searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, false, false).stream().map(p -> {
      return p.acceptVisitor(new SDGProgramPartVisitorWithDefault<IMethod, Object>() {

        @Override protected IMethod visitProgramPart(SDGProgramPart programPart, Object data) {
          return null;
        }

        @Override protected IMethod visitMethod(SDGMethod m, Object data) {
          if (method.getClassName().equals(m.getSignature().getDeclaringType().toHRStringShort())) {
            return klass.getClassHierarchy().lookupClass(m.getSignature().getDeclaringType().toBCString()).getAllMethods().stream()
                .filter(im -> im.getName().toString().equals(m.getSignature().getMethodName())).findFirst().get();
          }
          return null;
        }
      }, null);
    }).filter(Objects::nonNull).distinct().collect(Collectors.toList());
  }

  @Override public Flows analyze(List<Method> sources, List<Method> sinks, Collection<String> interfacesToImplement) {
    LOGGER.info(() -> String
        .format("Start analysis with sources=%s, sinks=%s, interfacesToImplement=%s", Arrays.toString(sources.toArray()),
            Arrays.toString(sinks.toArray()), Arrays.toString(interfacesToImplement.toArray())));
    configureInterfaceImplementation(interfacesToImplement);
    LOGGER.info(() -> "Configured interfaces, select entry points");
    selectEntryPoints(sources);
    LOGGER.info(() -> "Selected interfaces, build SDG");
    if (!console.buildSDGIfNeeded()) {
      throw new AnalysisException("Cannot build SDG");
    }
    clear();
    LOGGER.info("Built SDG, select sources and sinks");
    selectSources(sources);
    selectSinks(sinks);
    LOGGER.info("Start analysis");
    return analyze(false);
  }

  @Override public void saveDebugGraph(Path path) {
    console.new Wrapper().saveSDG(path.toFile());
  }

  private String getRegexpForMethod(Method method) {
    return method.accept(new Visitor<String>() {
      @Override public String visit(Method method) {
        return String.format("%s(\\(.*\\)[^-]*(->[^-]+)?)?$", getClassAndMethodNameRegexp(method));
      }

      protected String getClassAndMethodNameRegexp(Method method) {
        if (method.getRealClassName().contains(";")) { // its byte code
          return method.getRealClassName().substring(1).replace("/", "\\.").replace(";", "") + "\\." + method.methodName;
        }
        return String.format("(.*\\.|)%s\\.%s", method.getRealClassName(), method.methodName);
      }

      @Override public String visit(MethodParameter parameter) {
        return String.format("%s\\(.*\\)[^-]*->%d$", getClassAndMethodNameRegexp(parameter), parameter.parameter);
      }

      @Override public String visit(MethodReturn methodReturn) {
        return String.format("%s\\(.*\\)[^-]*->-1$", getClassAndMethodNameRegexp(methodReturn));
      }
    });
  }

  private Pattern getPatternForMethod(Method method) {
    return new Pattern(getRegexpForMethod(method), true);
  }

  private String getRegexpForMethods(List<Method> methods) {
    return "(" + methods.stream().map(this::getRegexpForMethod).collect(Collectors.joining(")|(")) + ")";
  }

  private Pattern getPatternForMethods(List<Method> methods) {
    return new Pattern(getRegexpForMethods(methods), true);
  }

  private void selectEntryPoints(List<Method> methods) {
    String regexp = getRegexpForMethods(methods);
    List<String> entities = searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, true, false).stream()
        .map(p -> {
          String s = programPartToString(p);
          if (s != null && s.matches(regexp)) {
            return programPartToString(p.getOwningMethod());
          }
          return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    if (entities.isEmpty()) {
      throw new AnalysisException("No EntryPoints found");
    }
    console.setAdditionalEntryMethods(entities);
  }

  private void configureInterfaceImplementation(Collection<String> interfacesToImplement) {
    this.console.setInterfaceImplOptions(
        new InterfaceImplementationOptions(byteCodeNamesOfInterfacesToImplement(interfacesToImplement), createGenerator()));
  }

  private InterfaceImplementationClass.FunctionBodyGenerator createGenerator() {
    return new InterfaceImplementationClass.FunctionBodyGenerator() {
      @Override public void generate(InterfaceImplementationClass klass, AbstractRootMethod method, IMethod origMethod) {

        Method compMethod = Connector.methodForIMethod(origMethod);

        int[] depForRetLocal = new int[] {
            method.addBinaryInstruction(IBinaryOpInstruction.Operator.OR, method.getValueNumberForIntConstant(0),
                method.getValueNumberForIntConstant(0)) }; // see below
        TypeReference depForRetLocalType = TypeReference.Int;

        boolean usedDepForLocal = false;

        // flows from the parameters to the return through parameters of other functions that are present
        // and flows from the parameters to the parameters of other functions
        for (Map.Entry<Method, Set<Method>> entry : knownFlows.forMethod(compMethod).onlyParameterSources()
            .filterSinks(m -> isPresent(m.discardMiscInformation()))) {
          usedDepForLocal = true;
          MethodParameter parameter = (MethodParameter) entry.getKey();
          for (Method intermMethod : entry.getValue()) {
            // call the methods on this object
            getIMethods(klass, intermMethod).forEach(intermIMethod -> {
              int[] params = IntStream.range(0, intermIMethod.getNumberOfParameters()).map(i -> {
                if (i == parameter.parameter) {
                  return parameter.parameter + 1; // the parameter we want to create a dependency for
                }
                return klass.addLoadForType(method, intermIMethod.getParameterType(i));
              }).toArray();
              int methodRet = klass.addInvocation(method, intermIMethod, params).getDef(); // TODO: correct?
              if (knownFlows.contains(intermMethod, new MethodReturn(compMethod))) {
                // param → some method → return
                // boolean depForRet = false;
                // …
                // if (some method() == some method()){ depForRet = depForRet | 1; }
                // …
                // dummy return with depForRet

                method.addConditionalBranchInstruction(IConditionalBranchInstruction.Operator.NE, intermIMethod.getReturnType(),
                    methodRet, methodRet, () -> {
                      depForRetLocal[0] = method
                          .addBinaryInstruction(IBinaryOpInstruction.Operator.OR, method.getValueNumberForIntConstant(1),
                              depForRetLocal[0]);
                    });
              } else {
                // param → some method
                // just call the method
              }
            });
          }
        }

        if (usedDepForLocal) {

          int ret = klass.addLoadForType(method, method.getReturnType());
          // return depends on depForRet
          method.addDummyReturnCondition(depForRetLocalType, depForRetLocal[0], method.getReturnType(), ret);
        }

        // flows inside this method from parameter to return
        for (MethodParameter parameter : knownFlows.getParamConnectedToReturn(compMethod)) {

          int ret = klass.addLoadForType(method, method.getReturnType());
          TypeReference type = method.getParameterType(parameter.parameter);
          method.addDummyReturnCondition(type, parameter.parameter + 1, method.getReturnType(), ret);
        }

        if (connectReturnWithParams) {
          CONNECT_RETURN_WITH_PARAMS.generate(klass, method, origMethod);
        } else {
          InterfaceImplementationClass.FunctionBodyGenerator.generateReturn(klass, method);
        }
      }
    };
  }

  /**
   * Is this method present in the class path?
   */
  private boolean isPresent(Method method) {
    IFCConsole.Wrapper wrapper = console.new Wrapper();
    return wrapper.getAnnotatableEntities(getRegexpForMethod(method)).size() > 0;
  }

  /**
   * Remove all annotations
   */
  private void clear() {
    console.reset();
  }

  /**
   * Also includes interface helper classes
   *
   * @return
   */
  private List<String> getAnnotatableEntities(List<Method> methods) {
    String regexp = getRegexpForMethods(methods);
    List<String> annotatableEntities = console.new Wrapper().getAnnotatableEntities(regexp);
    List<String> others = new ArrayList<>();
    for (IClass iClass : console.getAnalysis().getProgram().getClassHierarchy()) {
      if (iClass instanceof InterfaceImplementationClass) {
        for (IMethod iMethod : iClass.getAllMethods()) {
          String methodStr = iClass.getName().toString().substring(1).replace("/", ".") + "." + iMethod.getSelector();
          //others.add(methodStr);
          IntStream.range(0, iMethod.getNumberOfParameters()).forEach(i -> others.add(methodStr + "->" + i));
          others.add(methodStr + "->-1");
        }
      }
    }
    annotatableEntities.addAll(others.stream().filter(e -> e.matches(regexp)).collect(Collectors.toList()));
    return annotatableEntities;
  }

  private void annotate(List<Method> methods, boolean source, boolean checkNumberOfMatched) {
    IFCConsole.Wrapper wrapper = console.new Wrapper();
    List<String> annotatableEntities = getAnnotatableEntities(methods);
    int matchedMethods = 0;
    if (annotatableEntities.size() > 0) {
      for (String annotatableEntity : annotatableEntities) {
         System.out.println(String.format("%s %s", source ? "Source" : "Sink", annotatableEntity));
        if (source) {
          wrapper.selectSource(annotatableEntity, "high");
        } else {
          wrapper.selectSinkWithoutCheck(annotatableEntity, "low");
        }
        if (!annotatableEntity.endsWith("->") && annotatableEntity.contains("(")) {
          matchedMethods += 1;
        }
      }
    }
    wrapper.setAnnotationsInIFCAnalysis();
    if (checkNumberOfMatched && matchedMethods < methods.size()) {
      throw new AnalysisException("Cannot fully select " + (source ? "sources" : "sinks"));
    }
  }

  private void selectSources(List<Method> methods) {
    selectSinksOrSources(methods, this::getConcreteSource, true);
  }

  private void selectSinks(List<Method> methods) {
    selectSinksOrSources(methods, this::getConcreteSink, false);
  }

  private void selectSinksOrSources(List<Method> methods, Function<Method, Method> concrete, boolean source)
      throws AnalysisException {
    List<String> errors = new ArrayList<>();
    List<Method> concreteMethods = methods.stream().flatMap(m -> {
      try {
        Method concreteMethod = concrete.apply(m);
        if (annotatedToIncomingMethod.containsKey(m)){
          throw new AnalysisException(String.format("Concrete method %s is already mapped to %s", concreteMethod, m));
        }
        annotatedToIncomingMethod.put(concreteMethod, m);
        return Stream.of(concreteMethod);
      } catch (AnalysisException ex) {
        errors.add(ex.getMessage());
      }
      return Stream.empty();
    }).collect(Collectors.toList());
    if (errors.isEmpty()) {
      annotate(concreteMethods, source, false);
    } else {
      throw new AnalysisException(
          errors.stream().filter(InterfaceImplementationClass.distinctByKey(s -> s)).collect(Collectors.joining("\n")));
    }
  }

  private Method getConcreteSink(Method method) throws AnalysisException {
    if (method.concreteName.length() > 0){
      return method.setClassName(normalizeClassName(method.getRealClassName()));
    }
    List<String> impls = getImplementingClasses(method.getRealClassName());
    if (impls.isEmpty()) {
      return method.setClassName(getImplementingHelperClass(method.getRealClassName()).get());
    }
    if (impls.size() > 1) {
      throw new AnalysisException(
          String.format("Multiple possible implementations for %s: %s", method.getRealClassName(), String.join(", ", impls)));
    }
    return method.setClassName(normalizeClassName(impls.get(0)));
  }

  private Method getConcreteSource(Method method) throws AnalysisException {
    if (method.concreteName.length() > 0) {
      return method.setClassName(normalizeClassName(method.concreteName));
    }
    List<String> impls = getImplementingClasses(method.getRealClassName());
    if (impls.isEmpty()) {
      return method.setClassName(normalizeClassName(method.getClassName()));
    }
    if (impls.size() > 1) {
      throw new AnalysisException(
          String.format("Multiple possible implementations for %s: %s", method.getRealClassName(), String.join(", ", impls)));
    }
    return method.setClassName(impls.get(0));
  }

  private Optional<String> getImplementingHelperClass(String klass) {
    List<String> klasses = getImplementors(normalizeClassName(klass)).stream()
        .filter(c -> c instanceof InterfaceImplementationClass).map(c -> c.getName().toString() + ";").collect(Collectors.toList());
    if (klasses.size() > 1) {
      throw new AnalysisException(
          String.format("%s implemented by multiple interface helper classes: %s", klass, String.join(", ", klasses)));
    }
    return klasses.stream().findFirst();
  }

  private List<String> getImplementingClasses(String klass) {
    return getImplementingIClasses(normalizeClassName(klass)).stream().map(c -> c.getName().toString() + ";")
        .collect(Collectors.toList());
  }

  private Collection<IClass> getImplementingIClasses2(String klass) {
    return console.getAnalysis().getProgram().getClassHierarchy().computeSubClasses(getTypeReference(klass)).stream()
        .filter(c -> !c.getReference().equals(getTypeReference(klass))).collect(Collectors.toList());
  }

  private Collection<IClass> getImplementingIClasses(String klass) {
    IClass iClass = console.getAnalysis().getProgram().getClassHierarchy().lookupClass(getTypeReference(klass));
    return Arrays.asList(Iterators.toArray(Iterators.filter(console.getAnalysis().getProgram().getClassHierarchy().iterator(), c -> {
      if (c.getClassLoader().getName().toString()
          .equals(SDGProgram.ClassLoader.APPLICATION.getName()) && !c.equals(iClass)){
        if (c.getAllImplementedInterfaces().contains(iClass)){
          return true;
        }
      }
      return false;
    }), IClass.class));
  }

  private Collection<IClass> getImplementors(String klass) {
    return console.getAnalysis().getProgram().getClassHierarchy().getImplementors(
        console.getAnalysis().getProgram().getClassHierarchy().lookupClass(normalizeClassName(klass)).getReference());
  }

  private TypeReference getTypeReference(String klass) {
    for (IClass iClass : console.getAnalysis().getProgram().getClassHierarchy()) {
      if ((iClass.getReference().getName().toString() + ";").equals(klass) && iClass.getClassLoader().getName().toString()
          .equals(SDGProgram.ClassLoader.APPLICATION.getName())) {
        return iClass.getReference();
      }
    }
    throw new AnalysisException(String.format("No such class %s", klass));
  }

  private String normalizeClassName(String classNameWithoutPackage) {
    if (classNameWithoutPackage.contains("/")) {
      return classNameWithoutPackage;
    }
    List<String> classNames = new ArrayList<>();
    for (IClass iClass : console.getAnalysis().getProgram().getClassHierarchy()) {
      if (iClass.getReference().getName().getClassName().toString().equals(classNameWithoutPackage) && iClass.getClassLoader()
          .getName().toString().equals(SDGProgram.ClassLoader.APPLICATION.getName())) {
        classNames.add(iClass.getName().toString() + ";");
      }
    }
    if (classNames.isEmpty()) {
      throw new AnalysisException(String.format("%s is not present in the source code", classNameWithoutPackage));
    }
    if (classNames.size() > 1) {
      throw new AnalysisException(
          String.format("Multiple classes have name %s: %s", classNameWithoutPackage, String.join(", ", classNames)));
    }
    return classNames.get(0);
  }

  @Override public void setAllowedPackagesForUninitializedFields(Optional<List<String>> allowedPackagesForUninitializedFields) {
    this.console.setUninitializedFieldTypeMatcher(typeReference ->
        allowedPackagesForUninitializedFields.map(l -> l.stream().anyMatch(p -> FieldTypeMatcher.matchesPackage(typeReference, p)))
            .orElse(true) || FieldTypeMatcher.matchesPackage(typeReference, "fieldhelper") || FieldTypeMatcher.matchesPackage(typeReference, "interfacehelper"));
  }

  private Flows analyze(boolean includeClasses) {
    Map<Method, Set<Method>> flows = new HashMap<>();

    Optional<Collection<? extends IViolation<SecurityNode>>> viosOpt = console.doIFCAndOptAndCatch(CLASSICAL_NI);
    if (!viosOpt.isPresent()) {
      return new Flows(flows);
    }
    Collection<? extends IViolation<SecurityNode>> vios = viosOpt.get();

    YamlSequenceBuilder flowsBuilder = Yaml.createYamlSequenceBuilder();
    TObjectIntMap<IViolation<SDGProgramPart>> groupedIFlows = console.getAnalysis().groupByPPPart(vios);

    for (IViolation<SDGProgramPart> vio : groupedIFlows.keySet()) {
      vio.accept(new IViolationVisitor<SDGProgramPart>() {

        Optional<Method> convertProgramPartToOrigMethod(SDGProgramPart part, boolean includeClasses){
          return convertProgramPartToMethod(part, includeClasses).map(m -> {
            if (!annotatedToIncomingMethod.containsKey(m)){
              if (annotatedToIncomingMethod.containsKey(m.discardMiscInformation())){
                Method origMethod = annotatedToIncomingMethod.get(m.discardMiscInformation());
                return m.accept(new Visitor<Method>() {
                  @Override public Method visit(Method method) {
                    return method;
                  }

                  @Override public Method visit(MethodParameter parameter) {
                    return new MethodParameter(origMethod, parameter.parameter);
                  }

                  @Override public Method visit(MethodReturn methodReturn) {
                    return new MethodReturn(origMethod);
                  }
                });
              }
              throw new RuntimeException(String.format("Cannot map %s (map=%s)", m, annotatedToIncomingMethod));
            }
            return annotatedToIncomingMethod.get(m);
          });
        }

        Optional<Method> convertProgramPartToMethod(SDGProgramPart part, boolean includeClasses) {
          return part.acceptVisitor(new SDGProgramPartVisitorWithDefault<Optional<Method>, Object>() {

            @Override protected Optional<Method> visitClass(SDGClass cl, Object data) {
              if (includeClasses) {
                return Optional.of(new Method(cl.getTypeName().toBCString(), ""));
              }
              return Optional.empty();
            }

            @Override protected Optional<Method> visitMethod(SDGMethod m, Object data) {
              return Optional.of(visitMethod(m.getSignature()));
            }

            protected Method visitMethod(JavaMethodSignature signature) {
              return new Method(signature.getDeclaringType().toBCString(), signature.getMethodName());
            }

            @Override protected Optional<Method> visitParameter(SDGFormalParameter p, Object data) {
              int index = p.getIndex();
              Method method = visitMethod(p.getOwningMethod().getSignature());
              if (index >= 0) {
                return Optional.of(new MethodParameter(method, index));
              }
              return Optional.of(new MethodReturn(method));
            }

            @Override protected Optional<Method> visitProgramPart(SDGProgramPart programPart, Object data) {
              return Optional.empty();
            }
          }, null);
        }

        @Override public void visitIllegalFlow(IIllegalFlow<SDGProgramPart> iFlow) {
          Optional<Method> source = convertProgramPartToOrigMethod(iFlow.getSource(), includeClasses);
          Optional<Method> sink = convertProgramPartToOrigMethod(iFlow.getSink(), includeClasses);
          if (source.isPresent() && sink.isPresent()) {
            if (!flows.containsKey(source.get())) {
              flows.put(source.get(), new HashSet<>());
            }
            flows.get(source.get()).add(sink.get());
          }
        }

        @Override public void visitDataConflict(DataConflict<SDGProgramPart> dataConf) {
        }

        @Override public void visitOrderConflict(OrderConflict<SDGProgramPart> orderConf) {
        }

        @Override public <L> void visitUnaryViolation(IUnaryViolation<SDGProgramPart, L> unVio) {
        }

        @Override public <L> void visitBinaryViolation(IBinaryViolation<SDGProgramPart, L> binVio) {
        }

      });
    }
    return new Flows(flows);
  }
}
