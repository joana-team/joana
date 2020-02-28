package edu.kit.joana.wala.summary.test;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Builder;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.graph.TarjanStrongConnectivityInspector;
import edu.kit.joana.wala.summary.ISummaryComputer;
import edu.kit.joana.wala.summary.SummaryComputationType;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.parex.*;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Util {

  private static boolean PRINT_TIME = true;
  private static boolean ASSERT_NO_DUPLICATES = false;
  private static boolean EXPORT_GRAPHS = true;
  private static boolean USE_PARALLEL_CONVERTER = true;
  private static String classPath = null;

  private static void compute(ISummaryComputer computer, SDG sdg, @Nullable Graph graph, @Nullable String prefix, Function<Graph, Graph> preproc){
    if (computer instanceof Analysis){
      if (graph == null){
        graph = new SDGToGraph().convert(sdg, USE_PARALLEL_CONVERTER);
        graph = preproc.apply(graph);
      }
      long start = System.currentTimeMillis();
      ((Analysis) computer).compute(graph, sdg);
      logTime(computer.getClass().getSimpleName(), System.currentTimeMillis() - start);
      if (prefix != null && EXPORT_GRAPHS){
        exportGraph(graph, sdg, prefix + "_s");
      }
    } else {
      WorkPackage<SDG> pack = SDGProgram.createSummaryWorkpackage(sdg);
      try {
        long start = System.currentTimeMillis();
        computer.compute(pack, true, new NullProgressMonitor());
        logTime(computer.getClass().getSimpleName(), System.currentTimeMillis() - start);
      } catch (CancelException e) {
        e.printStackTrace();
      }
    }
  }

  private static MultiMapCompResult<SDGNode, SDGNode> compare(SDG sdg, @Nullable Graph graph, ISummaryComputer computer, ISummaryComputer computer2,
      boolean modifySDG, @Nullable String prefix, Function<Graph, Graph> preproc){
    SDG firstSDG = modifySDG ? sdg.clone() : sdg;
    SDG secondSDG = sdg.clone();
    compute(computer, firstSDG, graph, prefix, preproc);
    compute(computer2, secondSDG, graph, prefix, preproc);
    //System.out.println("first " + UtilKt.getSummaryEdgesOfSDG(firstSDG));
    for (Map.Entry<SDGNode, Set<SDGNode>> sdgNodeSetEntry : UtilKt.getSummaryEdgesOfSDG(firstSDG).entrySet()) {
      //System.out.println(sdgNodeSetEntry.getKey().getId() + " = " + sdg.getFormalIns(sdgNodeSetEntry.getKey()).stream().map(SDGNode::getId).map(Object::toString).collect(Collectors.joining()) + ": ");
      for (SDGNode node : sdgNodeSetEntry.getValue()) {
        //System.out.println("  " + node.getId() + " = " + sdg.getFormalOuts(node).stream().map(SDGNode::getId).map(Object::toString).collect(Collectors.joining()));
      }
    }
    //System.out.println("second " + UtilKt.getSummaryEdgesOfSDG(secondSDG));
    return UtilKt.diffRelatedTo(UtilKt.getSummaryEdgesOfSDG(firstSDG), UtilKt.getSummaryEdgesOfSDG(secondSDG));
  }

  private static String formatSDGNode(SDGNode node){
    return node.getLabel() + "(" + node.getProc() + "," + node.getId() + ")";
  }

  static SDG build(Class<?> klass) {
    Builder builder = new Builder().entry(klass).enableDumpAfterBuild();
    if (classPath != null){
      builder.classpath(classPath);
    }
    return builder.omitSummaryEdges().buildOrDie().analysis.getProgram().getSDG();
  }

  private static SDGNode procRootForRegexp(SDG sdg, String regexp){
    if (regexp.equals("")){
      return sdg.getRoot();
    }
    return sdg.vertexSet().stream().filter(n -> n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().matches(regexp)).findFirst().get();
  }

  private static final SummaryComputationType BASE_COMPUTATION = SummaryComputationType.DEFAULT;

  public static void exportGraph(Graph graph, SDG sdg, String prefix){
    File file = new File(prefix + "full.pdg");
    try {
      Files.createDirectories(file.toPath().getParent());
      SDGSerializer.toPDGFormat(sdg, new BufferedOutputStream(new FileOutputStream(file)));
    } catch (IOException e) {
      //e.printStackTrace();
    }
    UtilKt.exportDot(graph, UtilKt.nodeGraphT(graph, null,false), prefix + "full.dot", sdg);
    UtilKt.exportDot(graph, UtilKt.nodeGraphT(graph, graph.getEntry(), false), prefix + "fullRooted.dot", sdg);
    UtilKt.exportDot(graph, UtilKt.nodeGraphT(graph, null, false,
        n -> n instanceof InNode || n instanceof CallNode || n instanceof FuncNode || sdg.getNode(n.getId())
            .getKind().name().toLowerCase().matches(".*(act|for|exit|entry).*")), prefix + "purged.dot", sdg);
    UtilKt.exportDot(graph, graph.getCallGraph(), prefix + "callGraph.dot", sdg);
    for (FuncNode value : graph.getCallGraph().vertexSet()) {
      UtilKt.exportDot(graph, UtilKt.nodeGraphT(graph, value, true), prefix + sdg.getNode(value.getId()).getLabel() + "_" + graph.printableId(value.getId()), sdg);
    }
  }

  private static void logTime(String msg, long time){
    if (PRINT_TIME) {
      System.out.printf("%60s: %10dms\n", msg, time);
    }
  }

  /**
   * Compare the analysis to a base computer that is expected to be correct
   * @param klass
   * @param methodRegexp
   * @param analysis
   * @param graphDest
   * @param preproc
   * @return
   */
  private static Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph>
  compareCheck(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest, Function<Graph, Graph> preproc,
      boolean cacheGraph){
    String name = klass.getSimpleName() + methodRegexp + analysis.getName();
    long start = System.currentTimeMillis();
    SDG sdg = build(klass);
    logTime(String.format("build sdg %s", name), System.currentTimeMillis() - start);
    sdg.setRoot(procRootForRegexp(sdg, methodRegexp));
    start = System.currentTimeMillis();
    Graph graph = new SDGToGraph().convert(sdg, USE_PARALLEL_CONVERTER);
    logTime("convert graph", System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    graph = preproc.apply(graph);
    logTime("preproc", System.currentTimeMillis() - start);
    if (ASSERT_NO_DUPLICATES) {
      start = System.currentTimeMillis();
      assertNoDuplicates(graph);
      PreprocessKt.assertValidity(graph);
      logTime("check for duplicates and validity", System.currentTimeMillis() - start);
    }
    if (graphDest != null && EXPORT_GRAPHS){
      exportGraph(graph, sdg, graphDest + "/" + name);
    }
    String prefix = graphDest == null ? graphDest : (graphDest + "/" + name);
    return Pair.pair(Util.compare(sdg, cacheGraph ? graph : null, BASE_COMPUTATION.getSummaryComputer(), analysis, true, prefix, preproc), graph);
  }

  static void assertAnalysis(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest) {
    assertAnalysis(klass, methodRegexp, analysis, graphDest, g -> g);
  }

  static void assertAnalysis(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest, Function<Graph, Graph> preproc){
    Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph> res = compareCheck(klass, methodRegexp, analysis, graphDest, preproc, true);
    Assertions.assertTrue(res.getFirst().matches(),
        () -> klass.getSimpleName() + methodRegexp + analysis.getName() + "\n" + res.getFirst().format(Util::formatSDGNode, Util::formatSDGNode));
  }

  static void assertAnalyses(Class<?> klass, String methodRegexp, List<Analysis> analyses, @Nullable String graphDest, Function<Graph, Graph> preproc){
    for (Analysis analysis : analyses) {
      Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph> res = compareCheck(klass, methodRegexp, analysis,
          analyses.get(0) == analysis ? graphDest : null, preproc, false);
      Assertions.assertTrue(res.getFirst().matches(),
          () -> klass.getSimpleName() + methodRegexp + analysis.getName() + "\n" + res.getFirst().format(Util::formatSDGNode, Util::formatSDGNode));
    }
  }

  private static void assertNoDuplicates(Graph graph){
    Map<kotlin.Pair<Node, String>, Set<Node>> duplicateNodes = UtilKt.findDuplicateNodes(graph);
    if (!duplicateNodes.isEmpty()){
      Assertions.fail("Found duplicate nodes: \n" + duplicateNodes.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(
          Collectors.joining("\n")));
    }
    Assertions.assertTrue(true);
  }

  static void withDisabledGraphExport(Runnable runnable){
    boolean cur = EXPORT_GRAPHS;
    EXPORT_GRAPHS = false;
    runnable.run();
    EXPORT_GRAPHS = cur;
  }

  static void withClassPath(String classPath, Runnable runnable){
    String oldClassPath = Util.classPath;
    Util.classPath = classPath;
    runnable.run();
    Util.classPath = oldClassPath;
  }

  static void withoutParallelConverter(Runnable runnable){
    boolean bool = USE_PARALLEL_CONVERTER;
    USE_PARALLEL_CONVERTER = false;
    runnable.run();
    USE_PARALLEL_CONVERTER = bool;
  }

  static void withDuplicateAndValidityCheck(boolean with, Runnable runnable){
    boolean bool = ASSERT_NO_DUPLICATES;
    ASSERT_NO_DUPLICATES = with;
    runnable.run();
    ASSERT_NO_DUPLICATES = bool;
  }

  static class SummaryStatistics {

    private final List<Double> values;
    private final DoubleSummaryStatistics stats;

    SummaryStatistics(List<Double> values) {
      this.values = values;
      stats = values.stream().mapToDouble(d -> d).summaryStatistics();
    }

    double max(){
      return stats.getMax();
    }

    double min(){
      return stats.getMin();
    }

    double avg(){
      return stats.getAverage();
    }

    double n(){
      return values.size();
    }

    double std(){
      return Math.sqrt(1 / (n() - 1.0) * values.stream().mapToDouble(d -> Math.pow(d - avg(), 2)).sum());
    }

    @Override public String toString() {
      return String.format("avg = %8.2f +- %8.2f, min = %8.2f, max = %8.2f", avg(), std(), min(), max());
    }
  }

  interface Computation<T> {
    String name();
    T pre();
    void run(T pre);
  }

  static Map<String, SummaryStatistics> compare(List<Computation> runnables, int preRuns, int numberOfRuns){
    List<String> names = runnables.stream().map(Computation::name).collect(Collectors.toList());
    Map<String, List<Double>> times = new HashMap<>();
    IntStream.range(0, runnables.size()).forEach(e -> times.put(runnables.get(e).name(), new ArrayList<>()));
    Function<Computation, Double> run = c -> {
      System.out.print(c.name());
      System.out.flush();
      Object res = c.pre();
      double time = System.nanoTime() / 1000000d;
      c.run(res);
      time = System.nanoTime() / 1000000d - time;
      System.out.println(" " + time);
      return time;
    };
    if (preRuns > 0) {
      System.out.println("### Pre runs");
      for (int i = 0; i < preRuns; i++) {
        runnables.forEach(run::apply);
      }
      System.out.println("### Actual runs");
    }
    Supplier<Map<String, SummaryStatistics>> summer = () -> times.entrySet().stream()
        .collect(Collectors.toMap(e -> e.getKey(), e -> new SummaryStatistics(e.getValue())));
    for (int i = 0; i < numberOfRuns; i++) {
      Collections.shuffle(runnables);
      for (Computation computation : runnables) {
        times.get(computation.name()).add(run.apply(computation));
      }
      printSummaryStatistics(names, summer.get());
    }
    return summer.get();
  }

  static void printSummaryStatistics(List<String> runnables, Map<String, SummaryStatistics> stats){
    runnables.forEach(c -> System.out.printf("%20s: %s\n", c, stats.get(c)));
  }

  static void printSCCSizes(SDG sdg){
    List<Set<SDGNode>> sccs = new TarjanStrongConnectivityInspector<>(sdg).stronglyConnectedSets();
    Map<Integer, List<Integer>> histogram = sccs.stream().map(Set::size).collect(Collectors.groupingBy(i -> i));
    histogram.keySet().stream().sorted().forEach(i -> System.out.printf("%10d: %10d\n", i, histogram.get(i).size()));
  }
}
