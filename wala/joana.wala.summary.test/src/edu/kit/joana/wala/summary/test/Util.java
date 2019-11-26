package edu.kit.joana.wala.summary.test;

import com.google.common.io.Files;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.NullProgressMonitor;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Builder;
import edu.kit.joana.util.Pair;
import edu.kit.joana.wala.summary.ISummaryComputer;
import edu.kit.joana.wala.summary.SummaryComputationType;
import edu.kit.joana.wala.summary.WorkPackage;
import edu.kit.joana.wala.summary.parex.*;
import org.junit.jupiter.api.Assertions;

import javax.annotation.Nullable;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Util {

  private static boolean PRINT_TIME = true;
  private static boolean ASSERT_NO_DUPLICATES = false;
  private static boolean EXPORT_GRAPHS = false;

  private static void compute(ISummaryComputer computer, SDG sdg, @Nullable Graph graph, @Nullable String prefix){
    if (computer instanceof Analysis){
      if (graph == null){
        graph = new SDGToGraph().convert(sdg);
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
      boolean modifySDG, @Nullable String prefix){
    SDG firstSDG = modifySDG ? sdg.clone() : sdg;
    SDG secondSDG = sdg.clone();
    compute(computer, firstSDG, graph, prefix);
    compute(computer2, secondSDG, graph, prefix);
    System.out.println("first " + UtilKt.getSummaryEdgesOfSDG(firstSDG));
    for (Map.Entry<SDGNode, Set<SDGNode>> sdgNodeSetEntry : UtilKt.getSummaryEdgesOfSDG(firstSDG).entrySet()) {
      //System.out.println(sdgNodeSetEntry.getKey().getId() + " = " + sdg.getFormalIns(sdgNodeSetEntry.getKey()).stream().map(SDGNode::getId).map(Object::toString).collect(Collectors.joining()) + ": ");
      for (SDGNode node : sdgNodeSetEntry.getValue()) {
        //System.out.println("  " + node.getId() + " = " + sdg.getFormalOuts(node).stream().map(SDGNode::getId).map(Object::toString).collect(Collectors.joining()));
      }
    }
    System.out.println("second " + UtilKt.getSummaryEdgesOfSDG(secondSDG));
    return UtilKt.diffRelatedTo(UtilKt.getSummaryEdgesOfSDG(firstSDG), UtilKt.getSummaryEdgesOfSDG(secondSDG));
  }

  private static String formatSDGNode(SDGNode node){
    return node.getLabel() + "(" + node.getProc() + "," + node.getId() + ")";
  }

  private static SDG build(Class<?> klass) {
    return new Builder().entry(klass).enableDumpAfterBuild().omitSummaryEdges().buildOrDie().analysis.getProgram().getSDG();
  }

  private static SDGNode procRootForRegexp(SDG sdg, String regexp){
    if (regexp.equals("")){
      return sdg.getRoot();
    }
    return sdg.vertexSet().stream().filter(n -> n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().matches(regexp)).findFirst().get();
  }

  private static final SummaryComputationType BASE_COMPUTATION = SummaryComputationType.DEFAULT;

  private static void exportGraph(Graph graph, SDG sdg, String prefix){
    File file = new File(prefix + "full.pdg");
    try {
      Files.createParentDirs(file);
      SDGSerializer.toPDGFormat(sdg, new BufferedOutputStream(new FileOutputStream(file)));
    } catch (IOException e) {
      e.printStackTrace();
    }
    UtilKt.exportDot(UtilKt.nodeGraphT(graph, null,false), prefix + "full.dot", sdg);
    UtilKt.exportDot(UtilKt.nodeGraphT(graph, graph.getEntry(), false), prefix + "fullRooted.dot", sdg);
    UtilKt.exportDot(UtilKt.nodeGraphT(graph, null, false,
        n -> n instanceof InNode || n instanceof CallNode || n instanceof FuncNode || sdg.getNode(n.getId())
            .getKind().name().toLowerCase().matches(".*(act|for|exit|entry).*")), prefix + "purged.dot", sdg);
    UtilKt.exportDot(graph.getCallGraph(), prefix + "callGraph.dot", sdg);
    for (FuncNode value : graph.getFuncMap().values()) {
      UtilKt.exportDot(UtilKt.nodeGraphT(graph, value, true), prefix + sdg.getNode(value.getId()).getLabel(), sdg);
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
  compareCheck(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest, Consumer<Graph> preproc,
      boolean cacheGraph){
    String name = klass.getSimpleName() + methodRegexp + analysis.getName();
    long start = System.currentTimeMillis();
    SDG sdg = build(klass);
    logTime(String.format("build sdg %s", name), System.currentTimeMillis() - start);
    sdg.setRoot(procRootForRegexp(sdg, methodRegexp));
    start = System.currentTimeMillis();
    Graph graph = new SDGToGraph().convert(sdg);
    logTime("convert graph", System.currentTimeMillis() - start);
    start = System.currentTimeMillis();
    preproc.accept(graph);
    logTime("preproc", System.currentTimeMillis() - start);
    if (ASSERT_NO_DUPLICATES) {
      start = System.currentTimeMillis();
      assertNoDuplicates(graph);
      logTime("check for duplicates", System.currentTimeMillis() - start);
    }
    if (graphDest != null && EXPORT_GRAPHS){
      exportGraph(graph, sdg, graphDest + "/" + name);
    }
    String prefix = graphDest == null ? graphDest : (graphDest + "/" + name);
    return Pair.pair(Util.compare(sdg, cacheGraph ? graph : null, BASE_COMPUTATION.getSummaryComputer(), analysis, true, prefix), graph);
  }

  static void assertAnalysis(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest) {
    assertAnalysis(klass, methodRegexp, analysis, graphDest, (g) -> {});
  }

  static void assertAnalysis(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest, Consumer<Graph> preproc){
    Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph> res = compareCheck(klass, methodRegexp, analysis, graphDest, preproc, true);
    Assertions.assertTrue(res.getFirst().matches(),
        () -> klass.getSimpleName() + methodRegexp + analysis.getName() + "\n" + res.getFirst().format(Util::formatSDGNode, Util::formatSDGNode));
  }

  static void assertAnalyses(Class<?> klass, String methodRegexp, List<Analysis> analyses, @Nullable String graphDest, Consumer<Graph> preproc){
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

  public static void withDisabledGraphExport(Runnable runnable){
    boolean cur = EXPORT_GRAPHS;
    EXPORT_GRAPHS = false;
    runnable.run();
    EXPORT_GRAPHS = cur;
  }
}
