package edu.kit.joana.wala.summary.test;

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
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Util {

  static final boolean PRINT_TIME = true;

  static void compute(ISummaryComputer computer, SDG sdg){
    WorkPackage<SDG> pack = SDGProgram.createSummaryWorkpackage(sdg);
    try {
      long start = System.currentTimeMillis();
      computer.compute(pack, true, new NullProgressMonitor());
      logTime(computer.getClass().getSimpleName(), System.currentTimeMillis() - start);
    } catch (CancelException e) {
      e.printStackTrace();
    }
  }

  static MultiMapCompResult<SDGNode, SDGNode> compare(SDG sdg, ISummaryComputer computer, ISummaryComputer computer2, boolean modifySDG){
    SDG firstSDG = modifySDG ? sdg.clone() : sdg;
    SDG secondSDG = sdg.clone();
    compute(computer, firstSDG);
    compute(computer2, secondSDG);
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

  static String formatSDGNode(SDGNode node){
    return node.getLabel() + "(" + node.getProc() + "," + node.getId() + ")";
  }

  static SDG build(Class<?> klass) {
    return new Builder().entry(klass).enableDumpAfterBuild().omitSummaryEdges().buildOrDie().analysis.getProgram().getSDG();
  }

  static int prevProc(SDG sdg, int lastProc){
    return Arrays.stream(sdg.sortNodesByProcedure().keys()).filter(i -> i < lastProc).max().getAsInt();
  }

  static SDGNode procRootForRegexp(SDG sdg, String regexp){
    if (regexp.equals("")){
      return sdg.getRoot();
    }
    return sdg.vertexSet().stream().filter(n -> n.getKind() == SDGNode.Kind.ENTRY && n.getLabel().matches(regexp)).findFirst().get();
  }

  static final SummaryComputationType BASE_COMPUTATION = SummaryComputationType.SIMON_SCC;

  static void exportGraph(Graph graph, SDG sdg, String prefix){
    try {
      SDGSerializer.toPDGFormat(sdg, new BufferedOutputStream(new FileOutputStream(prefix + "full.pdg")));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    UtilKt.exportDot(UtilKt.nodeGraphT(graph.getEntry(), false), prefix + "full.dot", sdg);
    UtilKt.exportDot(UtilKt.nodeGraphT(graph.getEntry(), false,
        n -> n instanceof InNode || n instanceof CallNode || n instanceof FuncNode || sdg.getNode(n.getId())
            .getKind().name().toLowerCase().matches(".*(act|for|exit|entry).*")), prefix + "purged.dot", sdg);
    UtilKt.exportDot(UtilKt.callGraphT(graph.getEntry()), prefix + "callGraph.dot", sdg);
    for (FuncNode value : graph.getFuncMap().values()) {
      UtilKt.exportDot(UtilKt.nodeGraphT(value, true), prefix + sdg.getNode(value.getId()).getLabel(), sdg);
    }
  }

  static void logTime(String msg, long time){
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
   * @return
   */
  static Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph>
  compareCheck(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest){
    String name = klass.getSimpleName() + methodRegexp + analysis.getName();
    long start = System.currentTimeMillis();
    SDG sdg = build(klass);
    logTime(String.format("build sdg %s", name), System.currentTimeMillis() - start);
    sdg.setRoot(procRootForRegexp(sdg, methodRegexp));
    start = System.currentTimeMillis();
    Graph graph = new SDGToGraph().convert(sdg);
    logTime("convert graph", System.currentTimeMillis() - start);
    if (graphDest != null){
      exportGraph(graph, sdg, graphDest + "/" + name);
    }
    return Pair.pair(Util.compare(sdg, BASE_COMPUTATION.getSummaryComputer(), analysis, true), graph);
  }

  static void assertAnalysis(Class<?> klass, String methodRegexp, Analysis analysis, @Nullable String graphDest){
    Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph> res = compareCheck(klass, methodRegexp, analysis,
        graphDest);
    Assertions.assertTrue(res.getFirst().matches(),
        () -> klass.getSimpleName() + methodRegexp + analysis.getName() + "\n" + res.getFirst().format(Util::formatSDGNode, Util::formatSDGNode));
  }

  static void assertAnalyses(Class<?> klass, String methodRegexp, List<Analysis> analyses, @Nullable String graphDest){
    for (Analysis analysis : analyses) {
      Pair<MultiMapCompResult<SDGNode, SDGNode>, Graph> res = compareCheck(klass, methodRegexp, analysis,
          analyses.get(0) == analysis ? graphDest : null);
      Assertions.assertTrue(res.getFirst().matches(),
          () -> klass.getSimpleName() + methodRegexp + analysis.getName() + "\n" + res.getFirst().format(Util::formatSDGNode, Util::formatSDGNode));
    }
  }
}
