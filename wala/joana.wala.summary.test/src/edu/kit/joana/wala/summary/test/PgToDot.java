package edu.kit.joana.wala.summary.test;

import edu.kit.joana.wala.summary.parex.Graph;
import edu.kit.joana.wala.summary.parex.Loader;
import edu.kit.joana.wala.summary.parex.UtilKt;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(mixinStandardHelpOptions = true)
public class PgToDot implements Callable<Integer> {

  @CommandLine.Parameters(paramLabel = "PG", description = "pg file", arity = "1", index = "0")
  String pg;

  @CommandLine.Parameters(paramLabel = "DOT", description = "output file base", arity = "1", index = "1")
  String out;

  @Override public Integer call() throws Exception {
    Graph g = new Loader().load(pg);
    UtilKt.exportDot(g, UtilKt.nodeGraphT(g, null, false), out + "_full.dot");
    UtilKt.exportDot(g, g.getCallGraph(), out + "_callGraph.dot");
    return 0;
  }

  public static void main(String[] args) {
    new CommandLine(new PgToDot()).execute(args);
  }
}
