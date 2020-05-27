package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationClass;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationOptions;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.types.TypeReference;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.*;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.Pattern;
import edu.kit.joana.component.connector.Flows;
import edu.kit.joana.component.connector.Method;
import edu.kit.joana.component.connector.MethodParameter;
import edu.kit.joana.component.connector.MethodReturn;
import edu.kit.joana.ui.ifc.wala.console.io.PrintStreamConsoleWrapper;
import edu.kit.joana.util.NullPrintStream;
import gnu.trove.map.TObjectIntMap;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

  public BasicFlowAnalyzer(){
    this(false);
  }

  public BasicFlowAnalyzer(boolean connectReturnWithParams) {
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    this.console = new IFCConsole(in,
        new PrintStreamConsoleWrapper(new NullPrintStream(), new NullPrintStream(), in, System.out, new NullPrintStream()));
    this.console.setPointsTo("OBJECT_SENSITIVE"); // use a slower but more precise analysis by default
    this.console.setUninitializedFieldTypeMatcher(typeReference -> true);
    this.console.setAnnotateOverloadedMethods(true);
    this.connectReturnWithParams = connectReturnWithParams;
  }

  @Override public void setClassPath(String classPath) {
    console.setClassPath(classPath);
  }

  private List<String> byteCodeNamesOfInterfacesToImplement(Collection<String> interfacesToImplement){
    return searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, false, false).stream()
        .map(p -> {
          return p.acceptVisitor(new SDGProgramPartVisitorWithDefault<String, Object>(){

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
        })
        .filter(Objects::nonNull)
        .distinct().collect(Collectors.toList());
  }

  private List<IMethod> getIMethods(InterfaceImplementationClass klass, Method method){
    return searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, false, false).stream()
        .map(p -> {
          return p.acceptVisitor(new SDGProgramPartVisitorWithDefault<IMethod, Object>(){

            @Override protected IMethod visitProgramPart(SDGProgramPart programPart, Object data) {
              return null;
            }

            @Override protected IMethod visitMethod(SDGMethod m, Object data) {
              if (method.getClassName().equals(m.getSignature().getDeclaringType().toHRStringShort())) {
                return klass.getClassHierarchy().lookupClass(m.getSignature().getDeclaringType().toBCString()).getAllMethods().stream().filter(im -> im.getName().toString().equals(m.getSignature().getMethodName())).findFirst().get();
              }
              return null;
            }
          }, null);
        })
        .filter(Objects::nonNull)
        .distinct().collect(Collectors.toList());
  }

  @Override public Flows analyze(List<Method> sources, List<Method> sinks, Collection<String> interfacesToImplement) {
    configureInterfaceImplementation(interfacesToImplement);
    selectEntryPoints(sources);
    if (!console.buildSDGIfNeeded()){
      throw new AnalysisException("Cannot build SDG");
    }
    clear();
    selectSources(sources);
    selectSinks(sinks);
    return analyze(false);
  }

  @Override public void saveDebugGraph(Path path) {
    console.new Wrapper().saveSDG(path.toFile());
  }

  private Pattern getPatternForMethod(Method method){
    return new Pattern(method.toRegexp(), true);
  }

  private String getRegexpForMethods(List<Method> methods){
    return "(" + methods.stream().map(Method::toRegexp).collect(Collectors.joining(")|(")) + ")";
  }

  private Pattern getPatternForMethods(List<Method> methods){
    return new Pattern(getRegexpForMethods(methods), true);
  }

  private void selectEntryPoints(List<Method> methods){
    String regexp = getRegexpForMethods(methods);
    List<String> entities = searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, true, false).stream()
        .map(p -> {
          String s = programPartToString(p);
          if (s != null && s.matches(regexp)){
            return programPartToString(p.getOwningMethod());
          }
          return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    if (entities.isEmpty()){
      throw new AnalysisException("No EntryPoints found");
    }
    console.setAdditionalEntryMethods(entities);
  }

  private void configureInterfaceImplementation(Collection<String> interfacesToImplement){
     this.console.setInterfaceImplOptions(new InterfaceImplementationOptions(byteCodeNamesOfInterfacesToImplement(interfacesToImplement),
        createGenerator()));
  }

  private InterfaceImplementationClass.FunctionBodyGenerator createGenerator(){
    return new InterfaceImplementationClass.FunctionBodyGenerator() {
      @Override public void generate(InterfaceImplementationClass klass, AbstractRootMethod method, IMethod origMethod) {

        Method compMethod = Connector.methodForIMethod(origMethod);

        int[] depForRetLocal = new int[]{method.addBinaryInstruction(IBinaryOpInstruction.Operator.OR,
            method.getValueNumberForIntConstant(0),
            method.getValueNumberForIntConstant(0))}; // see below
        TypeReference depForRetLocalType = TypeReference.Int;

        boolean usedDepForLocal = false;

        // flows from the parameters to the return through parameters of other functions that are present
        // and flows from the parameters to the parameters of other functions
        for (Map.Entry<Method, Set<Method>> entry : knownFlows.forMethod(compMethod).onlyParameterSources()
            .filterSinks(m -> isPresent(m.discardMiscInformation()))) {
          usedDepForLocal = true;
          MethodParameter parameter = (MethodParameter)entry.getKey();
            for (Method intermMethod : entry.getValue()) {
              // call the methods on this object
              getIMethods(klass, intermMethod).forEach(intermIMethod -> {
              int[] params = IntStream.range(0, intermIMethod.getNumberOfParameters())
                  .map(i -> {
                    if (i == parameter.parameter){
                      return parameter.parameter + 1; // the parameter we want to create a dependency for
                    }
                    return klass.addLoadForType(method, intermIMethod.getParameterType(i));
                  }).toArray();
                int methodRet = klass.addInvocation(method, intermIMethod, params).getDef(); // TODO: correct?
                if (knownFlows.contains(intermMethod, new MethodReturn(compMethod))){
                  // param → some method → return
                  // boolean depForRet = false;
                  // …
                  // if (some method() == some method()){ depForRet = depForRet | 1; }
                  // …
                  // dummy return with depForRet

                  method.addConditionalBranchInstruction(IConditionalBranchInstruction.Operator.NE,
                      intermIMethod.getReturnType(), methodRet, methodRet, () -> {
                        depForRetLocal[0] = method.addBinaryInstruction(IBinaryOpInstruction.Operator.OR,
                            method.getValueNumberForIntConstant(1), depForRetLocal[0]);
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

        if (connectReturnWithParams){
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
    return wrapper.getAnnotatableEntities(method.toRegexp()).size() > 0;
  }

  /**
   * Remove all annotations
   */
  private void clear(){
    console.reset();
  }

  private void selectSourcesOrSinks(List<Method> methods, boolean source){
    IFCConsole.Wrapper wrapper = console.new Wrapper();
    List<String> annotatableEntities = wrapper.getAnnotatableEntities(getRegexpForMethods(methods));
    int matchedMethods = 0;
    if (annotatableEntities.size() > 0) {
      for (String annotatableEntity : annotatableEntities) {
        // System.out.println(String.format("%s %s", source ? "Source" : "Sink", annotatableEntity));
        if (source){
          wrapper.selectSource(annotatableEntity, "high");
        } else {
          wrapper.selectSink(annotatableEntity, "low");
        }
        if (!annotatableEntity.endsWith("->") && annotatableEntity.contains("(")){
          matchedMethods += 1;
        }
      }
    }
    wrapper.setAnnotationsInIFCAnalysis();
    if (matchedMethods < methods.size()){
      throw new AnalysisException("Cannot fully select " + (source ? "sources" : "sinks"));
    }
  }

  private void selectSources(List<Method> methods){
    selectSourcesOrSinks(methods, true);
  }

  private void selectSinks(List<Method> methods){
    selectSourcesOrSinks(methods, false);
  }

  private Flows analyze(boolean includeClasses){
    Map<Method, Set<Method>> flows = new HashMap<>();

    Optional<Collection<? extends IViolation<SecurityNode>>> viosOpt = console.doIFCAndOptAndCatch(CLASSICAL_NI);
    if (!viosOpt.isPresent()){
      return new Flows(flows);
    }
    Collection<? extends IViolation<SecurityNode>> vios = viosOpt.get();

    YamlSequenceBuilder flowsBuilder = Yaml.createYamlSequenceBuilder();
    TObjectIntMap<IViolation<SDGProgramPart>> groupedIFlows = console.getAnalysis().groupByPPPart(vios);

    for (IViolation<SDGProgramPart> vio : groupedIFlows.keySet()) {
      vio.accept(new IViolationVisitor<SDGProgramPart>() {

        Optional<Method> convertProgramPartToMethod(SDGProgramPart part, boolean includeClasses){
          return part.acceptVisitor(new SDGProgramPartVisitorWithDefault<Optional<Method>, Object>() {

            @Override protected Optional<Method> visitClass(SDGClass cl, Object data) {
              if (includeClasses) {
                String[] parts = cl.getTypeName().toHRString().split("\\.");
                return Optional.of(new Method(parts[parts.length - 1], ""));
              }
              return Optional.empty();
            }

            @Override protected Optional<Method> visitMethod(SDGMethod m, Object data) {
              return Optional.of(visitMethod(m.getSignature()));
            }

            protected Method visitMethod(JavaMethodSignature signature){
              String[] parts = signature.getDeclaringType().toHRString().split("\\.");
              return new Method(parts[parts.length - 1], signature.getMethodName());
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

        @Override
        public void visitIllegalFlow(IIllegalFlow<SDGProgramPart> iFlow) {
          Optional<Method> source = convertProgramPartToMethod(iFlow.getSource(), includeClasses);
          Optional<Method> sink = convertProgramPartToMethod(iFlow.getSink(), includeClasses);
          if (source.isPresent() && sink.isPresent()){
            if (!flows.containsKey(source.get())){
              flows.put(source.get(), new HashSet<>());
            }
            flows.get(source.get()).add(sink.get());
          }
        }

        @Override
        public void visitDataConflict(DataConflict<SDGProgramPart> dataConf) {
        }

        @Override
        public void visitOrderConflict(OrderConflict<SDGProgramPart> orderConf) {
        }

        @Override
        public <L> void visitUnaryViolation(IUnaryViolation<SDGProgramPart, L> unVio) {
        }

        @Override
        public <L> void visitBinaryViolation(IBinaryViolation<SDGProgramPart, L> binVio) {
        }

      });
    }
    return new Flows(flows);
  }
}
