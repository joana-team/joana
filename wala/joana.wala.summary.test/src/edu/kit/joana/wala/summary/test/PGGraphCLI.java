package edu.kit.joana.wala.summary.test;

import edu.kit.joana.wala.summary.parex.Graph;
import edu.kit.joana.wala.summary.parex.Loader;
import edu.kit.joana.wala.summary.parex.UtilKt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PGGraphCLI {

  public static void main(String[] args) throws IOException {
    Graph g = new Loader().load(args[0]);
    UtilKt.exportDot(g, UtilKt.nodeGraphT(g, null, false), args[1]);
    Files.write(Paths.get(args[1]),
        Files.lines(Paths.get(args[1])).filter(l -> Stream.of("owner", "callers", "callees").noneMatch(l::contains)).collect(Collectors.toList()));
  }
}
