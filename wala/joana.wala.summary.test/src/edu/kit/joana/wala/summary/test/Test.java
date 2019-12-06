package edu.kit.joana.wala.summary.test;

import com.ibm.wala.util.CancelException;
import de.uni.trier.infsec.core.Setup;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.util.Pair;
import edu.kit.joana.wala.summary.NullProgressMonitor;
import edu.kit.joana.wala.summary.SummaryComputationType;
import edu.kit.joana.wala.summary.parex.*;
import edu.kit.joana.wala.summary.test.cases.*;
import joana.api.testdata.seq.*;
import joana.api.testdata.toy.declass.Declass1;
import joana.api.testdata.toy.demo.Demo1;
import joana.api.testdata.toy.demo.NonNullFieldParameter;
import joana.api.testdata.toy.pw.PasswordFile;
import joana.api.testdata.toy.rec.MyList;
import joana.api.testdata.toy.rec.MyList2;
import joana.api.testdata.toy.rec.PrimitiveEndlessRecursion;
import joana.api.testdata.toy.rec.PrimitiveEndlessRecursion2;
import joana.api.testdata.toy.sensitivity.FlowSens;
import joana.api.testdata.toy.simp.*;
import joana.api.testdata.toy.test.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.kit.joana.wala.summary.test.Util.*;

public class Test {

    private static Stream<Class<?>> testCases(){
        return Stream.of(
            TinyExample.class,
            BasicTestClass.class,
            FlowSens.class,
            AssChain.class,
            MicroExample.class,
            UnconnectedGraph.class,
            Nested.class,
            NestedWithException.class,
            Sick2.class,
            MathRound.class,
            ControlDep.class,
            Independent.class,
            ObjSens.class,
            SystemCallsTest.class,
            VeryImplictFlow.class,
            MyList.class,
            PrimitiveEndlessRecursion.class,
            PrimitiveEndlessRecursion2.class,
            MyList2.class,
            DynamicDispatch.class,
            OutFlush.class,
            PasswordFile.class,
            Demo1.class,
            NonNullFieldParameter.class,
            Declass1.class,
            ExampleLeakage.class,
            ArrayAccess.class,
            ArrayOverwrite.class,
            FieldAccess.class,
            FieldAccess.class,
            FieldAccess2.class,
            FieldAccess3.class,
            Constants1.class,
            Constants2.class,
            Reflection.class,
            DynamicDispatch.class,
            StrangeTryCatchFinallyWalaBug.class,
            StrangeTryCatchFinallyWalaBugComplex.class,
            MartinMohrsStrangeTryCatchFinallyWalaBug.class,
            StaticField.class,
            Setup.class,
            JLexMin.class,
            JLex.Main.class
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis(Class<?> klass){
        Util.withoutParallelConverter(() -> {
            assertAnalysis(klass, "", new SequentialAnalysis(), bigTestCase(klass) ? null : "tmppc");
        });
        assertAnalysis(klass, "", new SequentialAnalysis(), bigTestCase(klass) ? null : "tmp");
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis2(Class<?> klass){
        assertAnalysis(klass, "", new CPPAnalysis(), bigTestCase(klass) ? null : "tmp", g -> {
            String file = "tmp/" + klass.getName() + ".pg";
            new Dumper().dump(g, file);
            Graph newGraph = new Loader().load(file);
            return newGraph;
        });
      assertAnalysis(klass, "", new SequentialAnalysis2(), bigTestCase(klass) ? null : "tmp", g -> {
          String file = "tmp/" + klass.getName() + ".pg";
          new Dumper().dump(g, file);
          Graph newGraph = new Loader().load(file);
          return newGraph;
      });
        assertAnalysis(klass, "", new BasicParallelAnalysis(), bigTestCase(klass) ? null : "tmp");
        assertAnalysis(klass, "", new SequentialAnalysis(), bigTestCase(klass) ? null : "tmp");
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysisBasicParallel(Class<?> klass){
        assertAnalysis(klass, "", new BasicParallelAnalysis(), bigTestCase(klass) ? null : "tmp");
    }


    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis2WithPreproc(Class<?> klass) throws InterruptedException {
        Util.withDuplicateAndValidityCheck(!bigTestCase(klass), () ->
            assertAnalysis(klass, "", new SequentialAnalysis2(), bigTestCase(klass) ? null : "tmp", g -> {
                PreprocessKt.removeNormalNodes(g);
                return g;
            })
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis2WithParallelPreproc(Class<?> klass){
        assertAnalysis(klass, "", new SequentialAnalysis2(), bigTestCase(klass) ? null : "tmp", g -> {
            PreprocessKt.removeNormalNodes(g, true);
            return g;
        });
    }

    @org.junit.jupiter.api.Test
    public void test2(){
        assertAnalysis(TinyExample.class, "", new SequentialAnalysis2(), "tmp");
    }

    @org.junit.jupiter.api.Test
    public void testOutFlush2() throws ClassNotFoundException {
        assertAnalysis(JLexMin.class, "", new SequentialAnalysis2(), "tmp");
    }

    private static Consumer<SDG> wrap(SummaryComputationType summaryComputationType){
        return sdg -> {
            try {
                summaryComputationType.getSummaryComputer().compute(SDGProgram.createSummaryWorkpackage(sdg), true, new NullProgressMonitor());
            } catch (CancelException e) {
                e.printStackTrace();
            }
        };
    }

    private static Consumer<SDG> wrap(Function<SDG, Graph> converter, Consumer<Graph> computer){
        return wrap(converter, computer, true);
    }

    private static Consumer<SDG> wrap(Function<SDG, Graph> converter, Consumer<Graph> computer, boolean parallel){
        return sdg -> {
            Graph graph = converter.apply(sdg);
            computer.accept(graph);
            int inserted = UtilKt.insertSummaryEdgesIntoSDG(graph, sdg, SDGEdge.Kind.SUMMARY, parallel);
            System.out.printf("Inserted %d summary edges", inserted);
        };
    }

    private static List<Pair<String, Consumer<SDG>>> configurations(){

        return Arrays.asList(
            Pair.pair("JOANA default",
                wrap(SummaryComputationType.DEFAULT)),
            Pair.pair("PCon CPP", sdg -> {
                new CPPAnalysis().compute(SDGProgram.createSummaryWorkpackage(sdg), true, new NullProgressMonitor());
            }),
            /*Pair.pair("Con Seq",
                wrap(sdg -> new SDGToGraph().convert(sdg), graph -> new SequentialAnalysis2().process(graph))),*/
            Pair.pair("PCon Import Export",
                sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    String file = "tmp/bench.pg";
                    new Dumper().dump(g, file);
                    Graph newGraph = new Loader().load(file);
                }),
            /*Pair.pair("PCon",
                sdg -> new SDGToGraph().convert(sdg, true)),*/
            Pair.pair("PCon PPre Seq",
                wrap(sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    PreprocessKt.removeNormalNodes(g, true);
                    return g;
                }, graph -> new SequentialAnalysis2().process(graph))),
            /*Pair.pair("Con BP",
                wrap(sdg -> new SDGToGraph().convert(sdg), graph -> new BasicParallelAnalysis().process(graph))),*/
            Pair.pair("PCon BP",
                wrap(sdg -> new SDGToGraph().convert(sdg, true), graph -> new BasicParallelAnalysis().process(graph))),
            Pair.pair("PCon Seq",
                wrap(sdg -> new SDGToGraph().convert(sdg, true), graph -> new SequentialAnalysis2().process(graph))),
            Pair.pair("PCon PPre BP",
                wrap(sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    PreprocessKt.removeNormalNodes(g, true);
                    return g;
                }, graph -> new BasicParallelAnalysis().process(graph))),
            Pair.pair("PCon PPre BP 2x",
                wrap(sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    PreprocessKt.removeNormalNodes(g, true);
                    return g;
                }, graph -> new BasicParallelAnalysis(Runtime.getRuntime().availableProcessors() * 2).process(graph))),

            Pair.pair("PCon PPre BP 0.5x",
                wrap(sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    PreprocessKt.removeNormalNodes(g, true);
                    return g;
                }, graph -> new BasicParallelAnalysis(Runtime.getRuntime().availableProcessors() / 2).process(graph))),
            /*Pair.pair("Con",
                sdg -> new SDGToGraph().convert(sdg)),
            Pair.pair("PCon",
                sdg -> new SDGToGraph().convert(sdg, true)),*/
            Pair.pair("PCon PPre",
                sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    PreprocessKt.removeNormalNodes(g, true);
                }),
            Pair.pair("PCon Import Export",
                sdg -> {
                    Graph g = new SDGToGraph().convert(sdg, true);
                    String file = "tmp/bench.pg";
                    new Dumper().dump(g, file);
                    Graph newGraph = new Loader().load(file);
                }));
    }

    public static void compare(String klassOrSDG, List<Pair<String, Consumer<SDG>>> configurations, int preRuns, int numberOfRuns){
        System.out.println("Build sdg");
        File sdgFile = Paths.get(klassOrSDG).toFile();
        long time = System.currentTimeMillis();
        SDG preSDG = null;
        if (sdgFile.exists() || sdgFile.getName().endsWith(".pdg")){
            try {
                System.out.println("Load from file");
                preSDG = SDG.readFromAndUseLessHeap(new FileInputStream(sdgFile));
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
            System.out.printf("Took %dms\n", System.currentTimeMillis() - time);
        } else {
            try {
                preSDG = Util.build(Class.forName(klassOrSDG));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                System.exit(1);
            }
            System.out.printf("Took %dms\n", System.currentTimeMillis() - time);
        }
        SDG sdg = preSDG;
        time = System.currentTimeMillis();
        Graph graph = new SDGToGraph2().convert(sdg);
        System.out.printf("Took %dms to convert graph\n", System.currentTimeMillis() - time);
        time = System.currentTimeMillis();
        try (OutputStream out = new FileOutputStream(klassOrSDG + ".pb")){
            new Dumper().dump(graph, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Took %dms to store graph\n", System.currentTimeMillis() - time);

        System.out.println("Nodes " + sdg.vertexSet().size() + ", functions " + sdg.getEntryNodesPerProcId().size());
        //printSCCSizes(sdg);
        Map<String, SummaryStatistics> stats = Util.compare(
            configurations.stream().map(p -> new Computation<SDG>() {

                @Override public String name() {
                    return p.getFirst();
                }

                @Override public SDG pre() {
                    sdg.removeSummaryEdges();
                    return sdg;
                }

                @Override public void run(SDG sdg) {
                    p.getSecond().accept(sdg);
                }
            }).collect(Collectors.toList()), preRuns, numberOfRuns);
    }

    public static void main(String[] args){
        withClassPath(args.length >= 3 ? args[0] : ".", () -> {
            compare(args.length >= 3 ? args[1] : "JLex.Main", configurations(), args.length >= 4 ? Integer.parseInt(args[3]) : 1, args.length >= 3 ? Integer.parseInt(args[2]) : 5);
        });
        System.exit(1);
        withClassPath(args[0], () -> {
            withDisabledGraphExport(() -> {
                try {
                    try {
                        assertAnalysis(Class.forName(args[1]), "", new SequentialAnalysis2(), "tmp");
                    } catch (AssertionError e){
                        e.printStackTrace();
                        System.exit(0);
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            System.exit(1);
        });
    }

    private static boolean bigTestCase(Class<?> klass){
        return klass == JLex.Main.class;
    }
}