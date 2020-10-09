package edu.kit.joana.wala.summary.test;

import com.google.gson.Gson;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Builder;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.summary.parex.*;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision.*;
import static edu.kit.joana.wala.summary.test.Util.withClassPath;
import static edu.kit.joana.wala.summary.test.Util.withDisabledGraphExport;

/**
 * CLI to create .pg, .pdg and .ssv files besides JARs.
 *
 * Call it like `java -Xmx25g -cp dist/joana.wala.summary.test.jar edu.kit.joana.wala.summary.test.TestCLI example/joana.example.jars/freecs/freecs.jar`
 */
@CommandLine.Command(mixinStandardHelpOptions = true)
public class TestCLI implements Callable<Integer> {

  @Parameters(paramLabel = "JARS_OR_CLASSES_OR_PG",
      description = "for each class: place the files into last name.* and for each JAR do the same")
  String[] jarsOrClasses;

  @Option(names = {"-e", "--use_existing_pdg"},
      description = "Use the exisiting pdg if the PDG modification date is >= the JAR, ignore for classes")
  boolean use_existing_pdg = false;

  @Option(names = {"-g", "--export_graphs"})
  boolean export_graphs = false;

  @Option(names = {"-s", "--omit_summary"}, description = "Create summary files")
  boolean omit_summary = false;

  @Option(names = "--store_pdg", description = "Store the PDG file")
  boolean store_pdg = true;

  @Option(names = "--config", description = "SDGConfig", defaultValue = "DEFAULT")
  SDGConfigs config;

  @Option(names = "--max_sums", description = "Alert if number of summary edges is greater than this value")
  long max_sums = 100_000_000;

  @Option(names = "--check_all", description = "Just outputs the maximum number of summary edges for various configs (based on the chosen)")
  boolean check_all = false;

  @Option(names = "--remove_normal", description = "Remove normal nodes")
  boolean remove_normal = false;

  @Option(names = {"-d", "--remove_nodes"}, description = "Remove the nodes with the given ids, use old graph ids")
  Integer[] remove_nodes = new Integer[0];

  @Option(names = "--remove_unreachable", description = "Remove all nodes that are unreachable from the entry node, "
      + "run after nodes are removed")
  boolean remove_unreachable = false;

  @Option(names = "--set_entry", description = "Use the node with the given old id as entry, uses last set_entry")
  int[] entry = new int[0];

  @Option(names = "--remove_neighbor_edge", description = "Remove the neighbor edge between 'START,END', use old graph ids")
  String[] remove_neighbor_edges = new String[0];

  @Option(names = "--list_nodes")
  boolean list_nodes = false;

  @Option(names = "--no_reorder")
  boolean no_reorder = false;

  @Option(names = "--use_pg_for_cpp")
  String use_pg_for_cpp = "";

  @Override public Integer call() throws Exception {
    TestCLIUtil util = new TestCLIUtil(config);
    for (String jarOrClass : jarsOrClasses) {
      if (jarsOrClasses.length > 1) {
        System.out.println("Process " + jarOrClass);
      }
      Optional<Path> export_folder = export_graphs ? Optional.of(util.exportedGraphsFolder(jarOrClass)) : Optional.empty();
      if (check_all) {
        util.checkAll(jarOrClass);
      } else if (util.isPg(jarOrClass)) {
        if (!omit_summary) {
          if (export_graphs) {
            util.exportGraphs(new Loader().load(jarOrClass), export_folder.get(), Paths.get(util.baseFile(jarOrClass)).toFile().getName(), Optional.empty());
          }
          util.writeSSVFromPG(Paths.get(jarOrClass), util.summaryFilePath(jarOrClass));
        }
      } else {
        util.createBenchFiles(jarOrClass, util.pdgFilePath(jarOrClass), util.pgFilePath(jarOrClass),
            !omit_summary ? Optional.of(util.summaryFilePath(jarOrClass)) : Optional.empty(),
            use_existing_pdg && util.useOldPdg(jarOrClass, util.pdgFilePath(jarOrClass)),
            export_folder, store_pdg, max_sums,
            remove_normal, new HashSet<>(Arrays.asList(remove_nodes)), remove_unreachable, entry.length > 0 ? entry[entry.length - 1] : -1,
            remove_neighbor_edges, list_nodes, !no_reorder, use_pg_for_cpp);
      }
    }
    return 1;
  }

  public static void main(String[] args) {
    new CommandLine(new TestCLI()).execute(args);
  }

  /**
   * Utility class to create .pg, .pdg and .ssv files
   */
  static class TestCLIUtil {

    private final SDGConfigs config;

    TestCLIUtil(SDGConfigs config) {
      this.config = config;
    }

    private String configApp(){
      switch (config){
      case DEFAULT:
        return "";
      default:
        return "_" + config.name().toLowerCase();
      }
    }

    private Path pgFilePath(String classOrFile){
      return Paths.get(baseFile(classOrFile) + ".pg");
    }

    private Path summaryFilePath(String classOrFile){
      return Paths.get(baseFile(classOrFile) + ".ssv");
    }

    private Path pdgFilePath(String classOrFile){
      return Paths.get(baseFile(classOrFile) + ".pdg");
    }

    private String baseFile(String classOrFile){
      if (isClassName(classOrFile)){
        String[] parts = classOrFile.split("\\.");
        return parts[parts.length - 1] + configApp();
      }
      return classOrFile.replaceAll("(\\.jar|\\.class|\\.pg)$", "") + configApp();
    }

    private Path exportedGraphsFolder(String classOrFile){
      return Paths.get(baseFile(classOrFile) + "_graphs");
    }

    private boolean isClassName(String classOrFile){
      return !classOrFile.endsWith(".class") && !classOrFile.endsWith(".jar") && !classOrFile.endsWith(".pg");
    }

    private boolean isPg(String str){
      return str.endsWith(".pg");
    }

    private boolean useOldPdg(String classOrFile, Path pdgFile){
      if (isClassName(classOrFile)){
        return false;
      }
      try {
        return Files.exists(pdgFile) && Files.getLastModifiedTime(Paths.get(classOrFile)).toInstant().isBefore(Files.getLastModifiedTime(pdgFile).toInstant());
      } catch (IOException e) {
        return false;
      }
    }

    private SDG createSDG(String classOrFile, Path pdgFile, boolean useOldPdg) throws IOException, ClassNotFoundException {
      return createSDG(classOrFile, pdgFile, useOldPdg, config.instantiate());
    }

    private SDG createSDG(String classOrFile, Path pdgFile, boolean useOldPdg, SDGConfig config) throws IOException, ClassNotFoundException {
      if (useOldPdg){
        return SDG.readFromAndUseLessHeap(pdgFile.toString());
      }
      Builder builder = new Builder(config);
      if (isClassName(classOrFile)){
        builder.entry(Class.forName(classOrFile));
        builder.classpath(".");
      } else {
        builder.classpath(classOrFile);
      }
      return builder.dontCache().omitSummaryEdges().buildOrDie().analysis.getProgram().getSDG();
    }

    private SDG createAndStoreSDG(String classOrFile, Path pdgFile, boolean useOldPdg, boolean storePDG) throws IOException, ClassNotFoundException {
      SDG sdg = createSDG(classOrFile, pdgFile, useOldPdg);
      if (!useOldPdg && storePDG) {
        BufferedOutputStream bOut = new BufferedOutputStream(Files.newOutputStream(pdgFile));
        SDGSerializer.toPDGFormat(sdg, bOut);
      }
      return sdg;
    }

    private void createBenchFiles(String classOrFile, Path pdgFile, Path pgPath, Optional<Path> summaryFile, boolean useOldPdg,
        Optional<Path> exportedGraphsFolder, boolean storePDG, long max_sums, boolean remove_normal, Set<Integer> remove_nodes,
        boolean remove_unreachable, int entry, String[] remove_neighbor_edges, boolean list_nodes, boolean reorder,
        String use_pg_for_cpp) {
      Runnable inner = () -> {
        try {
          writeCPPTestSource(use_pg_for_cpp.equals("") ? Optional.of(createAndStoreSDG(classOrFile, pdgFile, useOldPdg, storePDG)) : Optional.empty(), pgPath, summaryFile,
              exportedGraphsFolder, max_sums, remove_normal, remove_nodes, remove_unreachable, entry, remove_neighbor_edges,
              list_nodes, reorder, use_pg_for_cpp);
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
        }
      };
      Runnable wGraphExport = exportedGraphsFolder.isPresent() ? inner : () -> withDisabledGraphExport(inner);
      if (!isClassName(classOrFile)){
        withClassPath(classOrFile, wGraphExport);
      } else {
        withClassPath(".", wGraphExport);
      }
    }

    private void removeNeighborEdges(Graph g, String[] edges){
      for (String edge : edges) {
        List<Node> nodes = Arrays.stream(edge.split(",")).map(n -> g.getNodes().get(Integer.parseInt(n))).collect(Collectors.toList());
        nodes.get(0).getNeighbors().remove(nodes.get(1));
        if (nodes.get(0).getReducedNeighbors() != null) {
          nodes.get(0).getReducedNeighbors().remove(nodes.get(1));
        }
      }
    }

    private void printNodes(Graph g, boolean printEdgeCount){
      Gson gson = new Gson();
      Map<String, List<Integer>> obj = new HashMap<>();
      obj.put("functions", g.getFuncMap().keySet().stream().map(g::printableId).collect(Collectors.toList()));
      obj.put("nodes", g.getNodes().stream().filter(Objects::nonNull).parallel().map(Node::getId).map(g::printableId).filter(x -> x != -1).collect(Collectors.toList()));
      if (printEdgeCount) {
        obj.put("edge_count", Collections.singletonList(UtilKt.countEdges(g)));
      }
      obj.put("entry", Collections.singletonList(g.printableId(g.getEntry().getId())));
      System.out.println(gson.toJson(obj));
    }

    private void writeCPPTestSource(Optional<SDG> sdg, Path pgPath, Optional<Path> summaryFile, Optional<Path> exportedGraphsFolder,
        long max_sums, boolean remove_normal, Set<Integer> remove_nodes, boolean remove_unreachable, int entry, String[] remove_neighbor_edges, boolean list_nodes,
        boolean reorder, String use_pg_for_cpp) {
      Graph preG = use_pg_for_cpp.equals("") ? new SDGToGraph().convert(sdg.get(), true) : new Loader().load(use_pg_for_cpp);
      if (entry != -1) {
        preG = PreprocessKt.setEntry(preG, entry);
      }
      if (remove_normal) {
        PreprocessKt.removeNormalNodes(preG, true, Executors.newWorkStealingPool(), true);
      }
      removeNeighborEdges(preG, remove_neighbor_edges);
      preG = PreprocessKt.removeNodes(preG, remove_nodes.stream().map(preG.getNodes()::get).collect(Collectors.toSet()), true);
      if (remove_unreachable){
        preG = PreprocessKt.removeUnreachableNodes(preG, false, true);
      }
      if (list_nodes){
        printNodes(preG, true);
      }
      Graph preReorderG = preG;
      exportedGraphsFolder.ifPresent(folder -> {
         exportGraphs(preReorderG, folder, "_old_order", sdg);
      });
      Graph g = reorder ? preReorderG.reorderNodes() : preReorderG;
      if (list_nodes && reorder){
        printNodes(g, false);
      }
      long max = g.calculateMaximumNumberOfSummaryEdges();
      if (max > max_sums) {
        System.err.println(String.format("Maximum number of summary edges is %d", max));
      }
      new Dumper().dump(g, pgPath.toString());
      new Loader().load(pgPath.toString());
      System.out.println(pgPath.toString());
      summaryFile.ifPresent(summary -> {
        try {
          Files.deleteIfExists(summary);
        } catch (IOException e) {
          e.printStackTrace();
        }
        try (OutputStream out =
            Files.newOutputStream(summary, StandardOpenOption.CREATE_NEW)){
          BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
          System.out.println("  computing oracle summary edges");
          edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.Box<Integer> count =
              new edu.kit.joana.ifc.sdg.qifc.nildumu.util.Util.Box<>(0);
          BiConsumer<Integer, Integer> cons = (a, b) -> {
            try {
              assert a != -1;
              writer.write(String.format("%d %d\n", a, b));
              count.val++;
            } catch (IOException e) {
              e.printStackTrace();
            }
          };
          if (!remove_unreachable && remove_nodes.isEmpty() && sdg.isPresent()) {
            Test.computeOracleSummaryEdges(sdg.get(), (a, b) -> cons.accept(g.printableId(a.getId()), g.printableId(b.getId())));
          } else {
            g.removeSummaryEdges();
            new BasicParallelAnalysis().process(g);
            g.getActualIns().forEach(ai -> {
              ai.getSummaryEdges().forEach(ao -> {
                if (g.printableId(ao.getId()) != -1) {
                  cons.accept(g.printableId(ai.getId()), g.printableId(ao.getId()));
                }
              });
            });
            g.removeSummaryEdges();
          }
          System.out.println("summary edges = " + count.val);
          writer.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      exportedGraphsFolder.ifPresent(folder -> {
        exportGraphs(g, folder, "", sdg);
      });
    }

    private void exportGraphs(Graph g, Path folder, String suffix, Optional<SDG> sdg){
      sdg.ifPresent(s -> Util.exportGraph(g, s, folder.toString() + suffix));
      UtilKt.exportDot(g, UtilKt.nodeGraphT(g, null, false), folder.toString() + suffix + "_raw_full.dot");
    }

    private void writeSSVFromPG(Path pgPath, Path summaryFile) throws IOException {
      Graph graph = new Loader().load(pgPath.toFile().toString());
      graph.removeSummaryEdges();
      new BasicParallelAnalysis().process(graph);
      Files.deleteIfExists(summaryFile);
      try (OutputStream out =
               Files.newOutputStream(summaryFile, StandardOpenOption.CREATE_NEW)) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        int count = 0;
        for (ActualInNode actualIn : graph.getActualIns()) {
          for (OutNode actualOut : actualIn.getSummaryEdges()) {
            count++;
            writer.write(String.format("%d %d\n", actualIn.getId(), actualOut.getId()));
          }
        }
        writer.flush();
        System.out.println("summary edges = " + count);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public Graph createPG(String jarOrClass, SDGConfig config) {
      try {
        SDG sdg = createSDG(jarOrClass, Paths.get("tmp"), false, config);
        return new SDGToGraph().convert(sdg, true).reorderNodes();
      } catch (IOException | ClassNotFoundException e) {
        e.printStackTrace();
      }
      return null;
    }

    public long calcSumsAndStore(String jarOrClass, SDGConfig config, Path pgPath){
      Graph g = createPG(jarOrClass, config);
      new Dumper().dump(g, pgPath.toString());
      return g.calculateMaximumNumberOfSummaryEdges();
    }

    public long calcSumsAndStore(String jarOrClass, SDGBuilder.PointsToPrecision pts, SDGBuilder.FieldPropagation propagation){
      SDGConfig conf = config.instantiate();
      conf.setPointsToPrecision(pts);
      conf.setFieldPropagation(propagation);
      return calcSumsAndStore(jarOrClass, conf, Paths.get(pgFilePath(jarOrClass).toString() + "_" + pts.name() + "_" + propagation.name()));
    }

    public void checkAll(String jarOrClass) {
      Arrays.asList(SDGBuilder.FieldPropagation.NONE, SDGBuilder.FieldPropagation.OBJ_GRAPH).parallelStream().forEach(f -> {
        try {
          for (SDGBuilder.PointsToPrecision pts : Arrays.asList(TYPE_BASED, INSTANCE_BASED, OBJECT_SENSITIVE)) {
            System.out.println(String.format("%20d: %s %s", calcSumsAndStore(jarOrClass, pts, f), f.name(), pts.name()));
          }
        } catch (IllegalArgumentException ex){
          System.err.println(f + ": " + ex.getMessage());
          //ex.printStackTrace();
        }
      });
     /* for (SDGBuilder.PointsToPrecision pts : SDGBuilder.PointsToPrecision.values()) {
        try {
          System.out.println(String.format("%20d: %s", calcSumsAndStore(jarOrClass, pts, config.instantiate().getFieldPropagation()), pts.name()));
        } catch (IllegalArgumentException ex){
          System.err.println(pts + ": " + ex.getMessage());
          //ex.printStackTrace();
        }
      }*/
    }
  }
}
