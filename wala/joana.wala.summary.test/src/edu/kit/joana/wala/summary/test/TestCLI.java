package edu.kit.joana.wala.summary.test;

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
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;

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
  boolean store_pdg = false;

  @Option(names = "--config", description = "SDGConfig", defaultValue = "DEFAULT")
  SDGConfigs config;

  @Option(names = "--max_sums", description = "Alert if number of summary edges is greater than this value")
  long max_sums = 100_000_000;

  @Option(names = "--check_all", description = "Just outputs the maximum number of summary edges for various configs (based on the chosen)")
  boolean check_all = false;

  @Override public Integer call() throws Exception {
    TestCLIUtil util = new TestCLIUtil(config);
    for (String jarOrClass : jarsOrClasses) {
      System.out.println("Process " + jarOrClass);
      if (check_all){
        util.checkAll(jarOrClass);
      } else if (util.isPg(jarOrClass)) {
        if (!omit_summary) {
          util.writeSSVFromPG(Paths.get(jarOrClass), util.summaryFilePath(jarOrClass));
        }
      } else {
        util.createBenchFiles(jarOrClass, util.pdgFilePath(jarOrClass), util.pgFilePath(jarOrClass),
            !omit_summary ? Optional.of(util.summaryFilePath(jarOrClass)) : Optional.empty(),
            use_existing_pdg && util.useOldPdg(jarOrClass, util.pdgFilePath(jarOrClass)),
            export_graphs ? Optional.of(util.exportedGraphsFolder(jarOrClass)) : Optional.empty(), store_pdg, max_sums);
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
        Optional<Path> exportedGraphsFolder, boolean storePDG, long max_sums){
      Runnable inner = () -> {
        try {
          writeCPPTestSource(createAndStoreSDG(classOrFile, pdgFile, useOldPdg, storePDG), pgPath, summaryFile, exportedGraphsFolder, max_sums);
        } catch (IOException | ClassNotFoundException e) {
          e.printStackTrace();
        }
      };
      Runnable wGraphExport = exportedGraphsFolder.isPresent() ?
          inner :
          () -> withDisabledGraphExport(inner);
      if (!isClassName(classOrFile)){
        withClassPath(classOrFile, wGraphExport);
      } else {
        withClassPath(".", wGraphExport);
      }
    }

    private void writeCPPTestSource(SDG sdg, Path pgPath, Optional<Path> summaryFile, Optional<Path> exportedGraphsFolder,
        long max_sums){
      Graph g = new SDGToGraph().convert(sdg, true).reorderNodes();
      long max = g.calculateMaximumNumberOfSummaryEdges();
      if (max > max_sums){
        System.err.println(String.format("Maximum number of summary edges is %d", max));
      }
      new Dumper().dump(g, pgPath.toString());
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
          Test.computeOracleSummaryEdges(sdg, (a, b) -> {
            try {
              assert g.printableId(a.getId()) != -1;
              writer.write(String.format("%d %d\n", g.printableId(a.getId()), g.printableId(b.getId())));
              count.val++;
            } catch (IOException e) {
              e.printStackTrace();
            }
          });
          System.out.println("summary edges = " + count.val);
          writer.flush();
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      exportedGraphsFolder.ifPresent(folder -> {
        try {
          Files.deleteIfExists(folder);
          Files.createDirectory(folder);
        } catch (IOException e) {
          e.printStackTrace();
        }
        Util.exportGraph(g, sdg, folder.toString());
      });
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
