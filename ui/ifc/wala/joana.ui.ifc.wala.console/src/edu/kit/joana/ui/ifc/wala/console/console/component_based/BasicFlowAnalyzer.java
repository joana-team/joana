package edu.kit.joana.ui.ifc.wala.console.console.component_based;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlSequenceBuilder;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationClass;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationOptions;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.*;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.ifc.wala.console.console.IFCConsole;
import edu.kit.joana.ui.ifc.wala.console.console.ImprovedCLI;
import edu.kit.joana.ui.ifc.wala.console.console.Pattern;
import edu.kit.joana.ui.ifc.wala.console.io.PrintStreamConsoleWrapper;
import edu.kit.joana.util.NullPrintStream;
import gnu.trove.map.TObjectIntMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static edu.kit.joana.api.IFCType.CLASSICAL_NI;
import static edu.kit.joana.api.sdg.SDGBuildPreparation.searchProgramParts;

/**
 * Basic flow analyzer
 */
public class BasicFlowAnalyzer extends FlowAnalyzer {

  private final IFCConsole console;

  public BasicFlowAnalyzer(){
    this(new Association(), new Flows(new HashMap<>()));
  }

  public BasicFlowAnalyzer(Association association, Flows knownFlows) {
    super(association, knownFlows);
    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    this.console = new IFCConsole(in,
        new PrintStreamConsoleWrapper(new NullPrintStream(), new NullPrintStream(), in, System.out, new NullPrintStream()));
    this.console.setPointsTo("OBJECT_SENSITIVE"); // use a slower but more precise analysis by default
    this.console.setUninitializedFieldTypeMatcher(typeReference -> true);
    this.console.setInterfaceImplOptions(new InterfaceImplementationOptions(Collections.singletonList("Lcomponent_sample/Sink"),
        InterfaceImplementationClass.FunctionBodyGenerator.CONNECT_RETURN_WITH_PARAMS,
        InterfaceImplementationOptions.Mode.PER_INSTANCE, false));
    this.console.setAnnotateOverloadedMethods(true);
  }

  @Override public void setClassPath(String classPath) {
    console.setClassPath(classPath);
  }

  @Override public Flows analyze(List<Method> sources, List<Method> sinks) {
    selectEntryPoints(sources);
    if (!console.buildSDGIfNeeded()){
      throw new AnalysisException("Cannot build SDG");
    }
    processKnownFlows();
    selectSources(sources);
    selectSinks(sinks);
    console.new Wrapper().saveSDG(new File("blub.pdg"));
    return analyze(false);
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
    List<String> entities = searchProgramParts(new NullPrintStream(), console.getClassPath(), true, false, false).stream()
        .map(ImprovedCLI::programPartToString).filter(s -> s != null && s.matches(regexp)).collect(Collectors.toList());
    if (entities.isEmpty()){
      throw new AnalysisException("No EntryPoints found");
    }
    console.setAdditionalEntryMethods(entities);
  }

  private void processKnownFlows(){
    if (!knownFlows.isEmpty()){
      throw new RuntimeException();
    }
  }

  private void selectSourcesOrSinks(List<Method> methods, boolean source){
    IFCConsole.Wrapper wrapper = console.new Wrapper();
    List<String> annotatableEntities = wrapper.getAnnotatableEntities(getRegexpForMethods(methods));
    int matchedMethods = 0;
    if (annotatableEntities.size() > 0) {
      for (String annotatableEntity : annotatableEntities) {
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
          return part.acceptVisitor(new SDGProgramPartVisitor<Optional<Method>, Object>() {

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
              return Optional.of(new MethodParameter(visitMethod(p.getOwningMethod().getSignature()), index));
            }

            @Override protected Optional<Method> visitAttribute(SDGAttribute a, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitActualParameter(SDGActualParameter ap, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitExit(SDGMethodExitNode e, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitException(SDGMethodExceptionNode e, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitInstruction(SDGInstruction i, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitCall(SDGCall c, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitCallReturnNode(SDGCallReturnNode c, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitCallExceptionNode(SDGCallExceptionNode c, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitPhi(SDGPhi phi, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitFieldOfParameter(SDGFieldOfParameter fop, Object data) {
              return Optional.empty();
            }

            @Override protected Optional<Method> visitLocalVariable(SDGLocalVariable local, Object data) {
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
