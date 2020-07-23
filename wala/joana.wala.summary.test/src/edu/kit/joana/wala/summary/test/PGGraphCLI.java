package edu.kit.joana.wala.summary.test;

import edu.kit.joana.wala.summary.parex.Graph;
import edu.kit.joana.wala.summary.parex.Loader;
import edu.kit.joana.wala.summary.parex.UtilKt;

public class PGGraphCLI {

  public static void main(String[] args) {
    Graph g = new Loader().load(args[0]);
    UtilKt.exportDot(g, UtilKt.nodeGraphT(g, null, false), args[1]);
  }
}
